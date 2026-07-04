package com.example.notikakeibo.classifier

import com.example.notikakeibo.data.dao.CategoryDao

// AIの代わりの仮実装。今は本物のAIを呼ばず、固定ルールで返す。
// 後でこれを本物のAI実装に差し替える。
class DummyClassifier(private val categoryDao: CategoryDao) : Classifier {

    override suspend fun classify(storeName: String, amount: Int): Long? {
        // 全小ジャンルを取得。
        val subs = categoryDao.getAllSubcategories()

        // 仮のルール：店名に特定の語が含まれたら対応ジャンルにする。
        // （本物のAIならここで文脈推論する。今はダミーなので簡単なキーワード一致）
        val targetName = when {
            storeName.contains("Google") -> "アプリ課金"
            storeName.contains("セブン") || storeName.contains("ローソン") -> "コンビニ"
            storeName.contains("Amazon") -> "消耗品"
            else -> "未分類"
        }

        // ジャンル名から subcategoryId を引いて返す。
        return subs.firstOrNull { it.name == targetName }?.id
    }
}