package com.example.notikakeibo.classifier

import com.example.notikakeibo.data.AppDatabase
import com.example.notikakeibo.data.entity.StoreDictionaryEntity
import android.content.Context

// 分類の司令塔。辞書と分類器(AI or ダミー)を繋ぐ。
// Kazuの設計：辞書にあれば適用、なければ分類器→適用＋辞書に新規登録。既存は触らない。
class ClassificationService(
    private val context: Context,
    private val classifier: Classifier
) {

    // 1件の取引を分類して、subcategoryId を確定・保存する。
    suspend fun classifyTransaction(transactionId: String, storeName: String, amount: Int) {
        val db = AppDatabase.getInstance(context)
        val dictDao = db.storeDictionaryDao()
        val txDao = db.transactionDao()

        // ① 辞書を引く。
        val known = dictDao.findByStoreName(storeName)

        val subcategoryId: Long? = if (known != null) {
            // 辞書にある → そのジャンルを使う（辞書は触らない）。
            known.subcategoryId
        } else {
            // ② 辞書にない → 分類器(AI/ダミー)に判定させる。
            val result = classifier.classify(storeName, amount)

            // ③ 判定できたら辞書に新規登録（onConflict=IGNOREなので既存は触らない）。
            if (result != null) {
                dictDao.insert(StoreDictionaryEntity(storeName = storeName, subcategoryId = result))
            }
            result
        }

        // ④ 取引に分類結果を適用（判定できた場合のみ）。
        if (subcategoryId != null) {
            txDao.updateCategory(transactionId, subcategoryId)
        }
    }
}