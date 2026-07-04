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

    // 取引に、小ジャンル名・大ジャンル名をJOINでくっつけて取得する。
    // LEFT JOIN なので、未分類（subcategoryId が null）の取引も含まれる。
    @Query("""
        SELECT
            t.transactionId AS transactionId,
            t.amount AS amount,
            t.storeName AS storeName,
            t.timestamp AS timestamp,
            s.name AS minorName,
            c.name AS majorName
        FROM transactions AS t
        LEFT JOIN subcategories AS s ON t.subcategoryId = s.id
        LEFT JOIN categories AS c ON s.categoryId = c.id
        ORDER BY t.timestamp DESC
    """)
    suspend fun getAllWithCategory(): List<TransactionWithCategory>

    // 指定した取引の分類（subcategoryId）を更新する。
    @Query("UPDATE transactions SET subcategoryId = :subcategoryId WHERE transactionId = :transactionId")
    suspend fun updateCategory(transactionId: String, subcategoryId: Long)

    // 未分類（subcategoryId が null）の取引だけ取得。
    @Query("SELECT * FROM transactions WHERE subcategoryId IS NULL")
    suspend fun getUnclassified(): List<TransactionEntity>

}