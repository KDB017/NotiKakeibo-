package com.example.notikakeibo.data.dao

// JOINの結果を受け取る箱（DTO）。
// テーブルではないので @Entity は付けない。ただのデータの入れ物。
data class TransactionWithCategory(
    val transactionId: String,
    val amount: Int,
    val storeName: String,
    val timestamp: Long,
    val minorName: String?,  // 小ジャンル名。未分類なら null
    val majorName: String?   // 大ジャンル名。未分類なら null
)