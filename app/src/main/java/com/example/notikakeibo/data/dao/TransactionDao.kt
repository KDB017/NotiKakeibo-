package com.example.notikakeibo.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.notikakeibo.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    // 取引を1件保存。取引番号が既にあれば無視（重複排除）。
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity)

    // 全取引を新しい順で取得（1回きり）。
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAll(): List<TransactionEntity>

    // 未分類（subcategoryId が null）の取引だけ取得。
    @Query("SELECT * FROM transactions WHERE subcategoryId IS NULL")
    suspend fun getUnclassified(): List<TransactionEntity>

    // 指定した取引の分類（subcategoryId）を更新する。
    @Query("UPDATE transactions SET subcategoryId = :subcategoryId WHERE transactionId = :transactionId")
    suspend fun updateCategory(transactionId: String, subcategoryId: Long)

    // 取引＋ジャンル名（1回きり取得）。
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

    // 取引＋ジャンル名（Flow版：データが変わると自動で流れる）。
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
    fun getAllWithCategoryFlow(): Flow<List<TransactionWithCategory>>

    // 大ジャンルごとの合計（1回きり取得）。
    @Query("""
        SELECT
            c.name AS majorName,
            SUM(t.amount) AS totalAmount
        FROM transactions AS t
        INNER JOIN subcategories AS s ON t.subcategoryId = s.id
        INNER JOIN categories AS c ON s.categoryId = c.id
        WHERE t.timestamp >= :startTime AND t.timestamp < :endTime
        GROUP BY c.id
        ORDER BY totalAmount DESC
    """)
    suspend fun getCategorySummary(startTime: Long, endTime: Long): List<CategorySummary>

    // 大ジャンルごとの合計（Flow版：データが変わると自動で流れる）。
    @Query("""
        SELECT
            c.name AS majorName,
            SUM(t.amount) AS totalAmount
        FROM transactions AS t
        INNER JOIN subcategories AS s ON t.subcategoryId = s.id
        INNER JOIN categories AS c ON s.categoryId = c.id
        WHERE t.timestamp >= :startTime AND t.timestamp < :endTime
        GROUP BY c.id
        ORDER BY totalAmount DESC
    """)
    fun getCategorySummaryFlow(startTime: Long, endTime: Long): Flow<List<CategorySummary>>
}