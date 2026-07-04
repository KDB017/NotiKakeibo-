package com.example.notikakeibo.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subcategories",
    // 外部キー制約：categoryId は categories.id を指す。
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],        // 親（categories）の主キー
            childColumns = ["categoryId"], // このテーブルの参照カラム
            onDelete = ForeignKey.CASCADE  // 親が消えたら子も消える
        )
    ],
    // 外部キーのカラムにはindexを張るのがRoomの作法（検索高速化）
    indices = [Index("categoryId")]
)
data class SubcategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,        // "外食" など
    val categoryId: Long     // どの大ジャンルに属すか
)