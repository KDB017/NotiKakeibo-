package com.example.notikakeibo.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// 店名 → 小ジャンル の対応を覚える辞書。
// 店名を主キーにすることで「同じ店名は1件だけ」＝重複しない。
@Entity(
    tableName = "store_dictionary",
    foreignKeys = [
        ForeignKey(
            entity = SubcategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["subcategoryId"],
            onDelete = ForeignKey.CASCADE  // 小ジャンルが消えたら辞書エントリも消す
        )
    ],
    indices = [Index("subcategoryId")]
)
data class StoreDictionaryEntity(
    @PrimaryKey
    val storeName: String,    // 店名が主キー。"Google" など

    val subcategoryId: Long   // この店に紐づく小ジャンル
)