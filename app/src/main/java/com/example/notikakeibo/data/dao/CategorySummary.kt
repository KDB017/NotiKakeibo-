package com.example.notikakeibo.data.dao

// 大ジャンルごとの合計金額を受け取るDTO。
data class CategorySummary(
    val majorName: String,
    val totalAmount: Int
)