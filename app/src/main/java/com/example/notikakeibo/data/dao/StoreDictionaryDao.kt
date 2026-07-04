package com.example.notikakeibo.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.notikakeibo.data.entity.StoreDictionaryEntity

@Dao
interface StoreDictionaryDao {

    // 辞書に登録。店名が既にあれば無視（＝既存は触らない）。
    // これで「新規だけ追加、既存はいじらない」が自動で実現する。
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: StoreDictionaryEntity)

    // 店名で辞書を引く。無ければ null。
    @Query("SELECT * FROM store_dictionary WHERE storeName = :storeName")
    suspend fun findByStoreName(storeName: String): StoreDictionaryEntity?

    // 全件取得（確認用）。
    @Query("SELECT * FROM store_dictionary")
    suspend fun getAll(): List<StoreDictionaryEntity>
}