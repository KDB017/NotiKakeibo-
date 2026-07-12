package com.example.notikakeibo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notikakeibo.classifier.AnthropicClassifier
import com.example.notikakeibo.classifier.ClassificationService
import com.example.notikakeibo.data.AppDatabase
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

// monthOffset = 0 なら今月、-1 なら先月、+1 なら来月。
// 戻り値：その月の (開始timestamp, 終了timestamp, 表示ラベル)
fun monthRange(monthOffset: Int): Triple<Long, Long, String> {
    val cal = Calendar.getInstance()
    cal.add(Calendar.MONTH, monthOffset)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val start = cal.timeInMillis
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH) + 1
    val label = "${year}年${month}月"
    cal.add(Calendar.MONTH, 1)
    val end = cal.timeInMillis
    return Triple(start, end, label)
}

@Composable
fun MainScreen(modifier: Modifier = Modifier, context: android.content.Context) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("一覧") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("集計") })
        }

        when (selectedTab) {
            0 -> TransactionListScreen(context = context)
            1 -> SummaryScreen(context = context)
        }
    }
}

@Composable
fun SummaryScreen(context: android.content.Context) {
    val dao = remember { AppDatabase.getInstance(context).transactionDao() }

    var monthOffset by remember { mutableIntStateOf(0) }
    val (start, end, label) = remember(monthOffset) { monthRange(monthOffset) }

    // monthOffsetが変わったらFlowを作り直す（別の月のクエリに切り替わる）。
    val summaryFlow = remember(monthOffset) { dao.getCategorySummaryFlow(start, end) }
    val summary by summaryFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val total = summary.sumOf { it.totalAmount }

    val colors = listOf(
        Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFF9800), Color(0xFFE91E63),
        Color(0xFF9C27B0), Color(0xFF00BCD4), Color(0xFFFFEB3B), Color(0xFF795548),
        Color(0xFF607D8B), Color(0xFFF44336)
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { monthOffset -= 1 }) { Text("◀ 前月") }
            Text(label, fontSize = 18.sp)
            TextButton(
                onClick = { if (monthOffset < 0) monthOffset += 1 },
                enabled = monthOffset < 0
            ) { Text("翌月 ▶") }
        }

        Text("合計 ¥$total", fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp))
        HorizontalDivider()

        if (summary.isEmpty()) {
            Text("この月の分類済み支出はありません", modifier = Modifier.padding(top = 16.dp))
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 200.dp)
                    .padding(vertical = 16.dp)
            ) {
                var startAngle = -90f
                val diameter = size.minDimension
                val topLeft = Offset((size.width - diameter) / 2f, 0f)
                summary.forEachIndexed { index, row ->
                    val sweep = if (total > 0) row.totalAmount.toFloat() / total * 360f else 0f
                    drawArc(
                        color = colors[index % colors.size],
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = true,
                        topLeft = topLeft,
                        size = Size(diameter, diameter)
                    )
                    startAngle += sweep
                }
            }

            LazyColumn {
                items(summary.size) { index ->
                    val row = summary[index]
                    val percent = if (total > 0) row.totalAmount * 100 / total else 0
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row {
                            Canvas(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .heightIn(min = 16.dp, max = 16.dp)
                                    .fillMaxWidth(0.05f)
                            ) {
                                drawRect(color = colors[index % colors.size])
                            }
                            Text(row.majorName, fontSize = 16.sp)
                        }
                        Text("¥${row.totalAmount} (${percent}%)", fontSize = 16.sp)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun TransactionListScreen(context: android.content.Context) {
    val dao = remember { AppDatabase.getInstance(context).transactionDao() }

    val transactions by dao.getAllWithCategoryFlow()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var subcategories by remember { mutableStateOf<List<SubcategoryWithParent>>(emptyList()) }
    var editingTx by remember { mutableStateOf<TransactionWithCategory?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val db = AppDatabase.getInstance(context)
        subcategories = withContext(Dispatchers.IO) { db.categoryDao().getAllSubcategoriesWithParent() }
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
                    val dao2 = AppDatabase.getInstance(context).transactionDao()
                    withContext(Dispatchers.IO) {
                        dao2.updateCategory(target.transactionId, selectedSubcategoryId)
                    }
                    editingTx = null
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