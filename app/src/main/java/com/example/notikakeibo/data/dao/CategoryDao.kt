package com.example.notikakeibo.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.notikakeibo.data.entity.CategoryEntity
import com.example.notikakeibo.data.entity.SubcategoryEntity

@Dao
interface CategoryDao {

    @Insert
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert
    suspend fun insertSubcategory(subcategory: SubcategoryEntity): Long

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun countCategories(): Int

    @Query("SELECT * FROM categories")
    suspend fun getAllCategories(): List<CategoryEntity>

    @Query("SELECT * FROM subcategories")
    suspend fun getAllSubcategories(): List<SubcategoryEntity>

    // 全小ジャンルを、親の大ジャンル名つきで取得（選択肢表示用）。
    @Query("""
        SELECT
            s.id AS subcategoryId,
            s.name AS minorName,
            c.name AS majorName
        FROM subcategories AS s
        INNER JOIN categories AS c ON s.categoryId = c.id
        ORDER BY c.id, s.id
    """)
    suspend fun getAllSubcategoriesWithParent(): List<SubcategoryWithParent>
}