package com.example.notikakeibo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        val dao = AppDatabase.getInstance(context).transactionDao()
        transactions = withContext(Dispatchers.IO) { dao.getAllWithCategory() }
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
                TransactionRow(tx)
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun TransactionRow(tx: TransactionWithCategory) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
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