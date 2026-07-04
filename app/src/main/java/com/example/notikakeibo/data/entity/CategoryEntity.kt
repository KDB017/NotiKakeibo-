package com.example.notikakeibo.data.entity


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,        // 主キー。autoGenerate=trueで自動採番
    val name: String         // "食費" など
)