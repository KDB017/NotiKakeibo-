package com.example.notikakeibo

import android.app.Application
import com.example.notikakeibo.data.AppDatabase
import com.example.notikakeibo.data.entity.CategoryEntity
import com.example.notikakeibo.data.entity.SubcategoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Application を継承すると「アプリ起動時に1度だけ動くクラス」になる。
class NotiKakeiboApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // 起動時に初期ジャンルを投入（まだ入ってなければ）。
        seedCategoriesIfNeeded()
    }

    private fun seedCategoriesIfNeeded() {
        val dao = AppDatabase.getInstance(this).categoryDao()

        // DB操作は時間がかかるので、別スレッド(IO)で実行する。
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 既にジャンルが入っていれば何もしない（二重投入防止）。
                if (dao.countCategories() > 0) {
                    android.util.Log.d("SeedCheck", "既にシード済み。スキップ")
                    return@launch
                }

                // 大ジャンル → その小ジャンルたち、の対応表。
                val seed = mapOf(
                    "食費" to listOf("外食", "コンビニ", "自炊"),
                    "交際費" to listOf("飲み会", "デート", "プレゼント"),
                    "趣味・娯楽" to listOf("ゲーム", "サッカー観戦", "書籍", "イベント"),
                    "サブスク" to listOf("配信サービス", "ツール", "アプリ課金"),
                    "日用品" to listOf("消耗品", "家電", "衣類"),
                    "交通費" to listOf("電車", "タクシー", "ガソリン"),
                    "住居・固定費" to listOf("家賃", "水道光熱費", "通信費"),
                    "健康・医療" to listOf("病院", "薬", "ジム"),
                    "自己投資" to listOf("技術書", "学習サービス", "資格試験"),
                    "その他" to listOf("未分類")
                )

                // 大ジャンルを1件入れて、返ってきたidで小ジャンルを紐づける。
                for ((majorName, minorNames) in seed) {
                    val categoryId = dao.insertCategory(CategoryEntity(name = majorName))
                    for (minorName in minorNames) {
                        dao.insertSubcategory(
                            SubcategoryEntity(name = minorName, categoryId = categoryId)
                        )
                    }
                }

                // 確認ログ：投入後、DBの中身を出す。
                val cats = dao.getAllCategories()
                val subs = dao.getAllSubcategories()
                android.util.Log.d("SeedCheck", "大ジャンル数=${cats.size} / 小ジャンル数=${subs.size}")
                for (c in cats) {
                    android.util.Log.d("SeedCheck", "大[${c.id}] ${c.name}")
                }
                for (s in subs) {
                    android.util.Log.d("SeedCheck", "小[${s.id}] ${s.name} (親=${s.categoryId})")
                }

            } catch (e: Exception) {
                android.util.Log.e("SeedCheck", "シード失敗", e)
            }
        }
    }
}