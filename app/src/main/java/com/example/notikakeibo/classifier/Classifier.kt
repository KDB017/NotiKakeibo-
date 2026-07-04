package com.example.notikakeibo.classifier

// 分類器の共通インターフェース。
// 「店名と金額を渡すと、小ジャンルIDを返す（分類できなければ null）」という契約。
// ダミー実装でも本物のAI実装でも、この形を満たせば差し替えられる。
interface Classifier {
    suspend fun classify(storeName: String, amount: Int): Long?
}