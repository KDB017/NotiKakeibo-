package com.example.notikakeibo.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import com.example.notikakeibo.data.entity.TransactionEntity

@Dao
interface TransactionDao {

    // 取引を1件保存。取引番号が既にあれば無視（重複排除）。
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity)

    // 全取引を新しい順で取得。
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAll(): List<TransactionEntity>
}