package com.example.notikakeibo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notikakeibo.classifier.AnthropicClassifier
import com.example.notikakeibo.classifier.ClassificationService
import com.example.notikakeibo.data.AppDatabase
import com.example.notikakeibo.data.dao.CategorySummary
import com.example.notikakeibo.data.dao.SubcategoryWithParent
import com.example.notikakeibo.data.dao.TransactionWithCategory
import com.example.notikakeibo.ui.theme.NotiKakeiboTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotiKakeiboTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        context = this
                    )
                }
            }
        }
    }
}

// 今月の開始（今月1日0時）と終了（来月1日0時）のtimestampを返す。
fun currentMonthRange(): Pair<Long, Long> {
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val start = cal.timeInMillis
    cal.add(Calendar.MONTH, 1)   // 来月へ
    val end = cal.timeInMillis
    return start to end
}

@Composable
fun MainScreen(modifier: Modifier = Modifier, context: android.content.Context) {
    // 選択中のタブ。0=一覧, 1=集計
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("一覧") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("集計") }
            )
        }

        when (selectedTab) {
            0 -> TransactionListScreen(context = context)
            1 -> SummaryScreen(context = context)
        }
    }
}

@Composable
fun SummaryScreen(context: android.content.Context) {
    var summary by remember { mutableStateOf<List<CategorySummary>>(emptyList()) }

    LaunchedEffect(Unit) {
        val (start, end) = currentMonthRange()
        val dao = AppDatabase.getInstance(context).transactionDao()
        summary = withContext(Dispatchers.IO) { dao.getCategorySummary(start, end) }
    }

    val total = summary.sumOf { it.totalAmount }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("今月の支出", fontSize = 20.sp)
        Text("合計 ¥$total", fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp))
        HorizontalDivider()

        if (summary.isEmpty()) {
            Text("分類済みの支出がまだありません", modifier = Modifier.padding(top = 16.dp))
        } else {
            LazyColumn {
                items(summary) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(row.majorName, fontSize = 16.sp)
                        Text("¥${row.totalAmount}", fontSize = 16.sp)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun TransactionListScreen(context: android.content.Context) {
    var transactions by remember { mutableStateOf<List<TransactionWithCategory>>(emptyList()) }
    var subcategories by remember { mutableStateOf<List<SubcategoryWithParent>>(emptyList()) }
    var editingTx by remember { mutableStateOf<TransactionWithCategory?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        val db = AppDatabase.getInstance(context)
        transactions = withContext(Dispatchers.IO) { db.transactionDao().getAllWithCategory() }
        subcategories = withContext(Dispatchers.IO) { db.categoryDao().getAllSubcategoriesWithParent() }
    }

    LaunchedEffect(Unit) {
        reload()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        Button(
            onClick = {
                scope.launch {
                    val db = AppDatabase.getInstance(context)
                    val classifier = AnthropicClassifier(db.categoryDao())
                    val service = ClassificationService(context, classifier)
                    withContext(Dispatchers.IO) {
                        val unclassified = db.transactionDao().getUnclassified()
                        for (tx in unclassified) {
                            service.classifyTransaction(tx.transactionId, tx.storeName, tx.amount)
                        }
                    }
                    reload()
                }
            },
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text("未分類を分類")
        }

        HorizontalDivider()

        LazyColumn {
            items(transactions) { tx ->
                TransactionRow(tx = tx, onClick = { editingTx = tx })
                HorizontalDivider()
            }
        }
    }

    val target = editingTx
    if (target != null) {
        CategoryPickerDialog(
            subcategories = subcategories,
            onDismiss = { editingTx = null },
            onSelect = { selectedSubcategoryId ->
                scope.launch {
                    val dao = AppDatabase.getInstance(context).transactionDao()
                    withContext(Dispatchers.IO) {
                        dao.updateCategory(target.transactionId, selectedSubcategoryId)
                    }
                    editingTx = null
                    reload()
                }
            }
        )
    }
}

@Composable
fun TransactionRow(tx: TransactionWithCategory, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(tx.storeName, fontSize = 16.sp)
            val genre = if (tx.majorName != null) {
                "${tx.majorName} > ${tx.minorName}"
            } else {
                "未分類"
            }
            Text(genre, fontSize = 12.sp)
        }
        Text("¥${tx.amount}", fontSize = 16.sp)
    }
}

@Composable
fun CategoryPickerDialog(
    subcategories: List<SubcategoryWithParent>,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ジャンルを選択") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(subcategories) { sub ->
                    Text(
                        text = "${sub.majorName} > ${sub.minorName}",
                        fontSize = 15.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(sub.subcategoryId) }
                            .padding(vertical = 12.dp)
                    )
                    HorizontalDivider()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        }
    )
}