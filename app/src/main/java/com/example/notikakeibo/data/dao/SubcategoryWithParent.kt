package com.example.notikakeibo.data.dao

// ジャンル選択肢を出すためのDTO。小ジャンル＋親の大ジャンル名。
data class SubcategoryWithParent(
    val subcategoryId: Long,
    val minorName: String,
    val majorName: String
)