package com.example.notikakeibo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.notikakeibo.data.dao.CategoryDao
import com.example.notikakeibo.data.dao.TransactionDao
import com.example.notikakeibo.data.entity.CategoryEntity
import com.example.notikakeibo.data.entity.SubcategoryEntity
import com.example.notikakeibo.data.entity.TransactionEntity

@Database(
    entities = [
        CategoryEntity::class,
        SubcategoryEntity::class,
        TransactionEntity::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notikakeibo.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}