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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notikakeibo.data.AppDatabase
import com.example.notikakeibo.data.dao.SubcategoryWithParent
import com.example.notikakeibo.data.dao.TransactionWithCategory
import com.example.notikakeibo.data.entity.TransactionEntity
import com.example.notikakeibo.ui.theme.NotiKakeiboTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotiKakeiboTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TransactionListScreen(
                        modifier = Modifier.padding(innerPadding),
                        context = this
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionListScreen(
    modifier: Modifier = Modifier,
    context: android.content.Context
) {
    var transactions by remember { mutableStateOf<List<TransactionWithCategory>>(emptyList()) }
    var subcategories by remember { mutableStateOf<List<SubcategoryWithParent>>(emptyList()) }
    // 今どの取引をタップして編集中か。null なら編集ダイアログは閉じてる。
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

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {

        Text("取引一覧", fontSize = 20.sp)

        Button(
            onClick = {
                scope.launch {
                    val dao = AppDatabase.getInstance(context).transactionDao()
                    withContext(Dispatchers.IO) {
                        dao.insert(
                            TransactionEntity(
                                transactionId = "TEST-" + System.currentTimeMillis(),
                                amount = 1234,
                                storeName = "テスト店舗",
                                timestamp = System.currentTimeMillis(),
                                subcategoryId = null
                            )
                        )
                    }
                    reload()
                }
            },
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text("テスト取引を追加")
        }

        HorizontalDivider()

        LazyColumn {
            items(transactions) { tx ->
                // 行をタップしたら、その取引を編集対象にする（ダイアログが開く）。
                TransactionRow(
                    tx = tx,
                    onClick = { editingTx = tx }
                )
                HorizontalDivider()
            }
        }
    }

    // 編集中の取引があるとき、ジャンル選択ダイアログを表示。
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
                    editingTx = null   // ダイアログを閉じる
                    reload()           // 一覧を再読み込みして反映
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
            .clickable { onClick() }   // タップ可能にする
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
            // ジャンル候補を縦スクロールで並べる。
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
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}