package com.example.notikakeibo.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    // 外部キー：subcategoryId は subcategories.id を指す。
    foreignKeys = [
        ForeignKey(
            entity = SubcategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["subcategoryId"],
            onDelete = ForeignKey.SET_NULL  // 小ジャンルが消えたら未分類(null)に戻す
        )
    ],
    indices = [Index("subcategoryId")]
)
data class TransactionEntity(
    @PrimaryKey
    val transactionId: String,   // PayPayの取引番号（一意なので主キー）

    val amount: Int,
    val storeName: String,
    val timestamp: Long,

    // どの小ジャンルか。未分類なら null。
    val subcategoryId: Long? = null
)