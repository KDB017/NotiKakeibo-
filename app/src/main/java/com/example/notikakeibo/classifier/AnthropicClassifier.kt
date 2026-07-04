package com.example.notikakeibo.classifier

import com.example.notikakeibo.BuildConfig
import com.example.notikakeibo.data.dao.CategoryDao
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

// 本物のAI分類。Anthropic Haiku APIに店名＋金額を投げて、小ジャンルを判定させる。
class AnthropicClassifier(private val categoryDao: CategoryDao) : Classifier {

    private val client = OkHttpClient()

    override suspend fun classify(storeName: String, amount: Int): Long? {
        // 全小ジャンルを取得（AIに候補として渡す＆名前→id変換に使う）。
        val subs = categoryDao.getAllSubcategories()
        val candidateNames = subs.joinToString(", ") { it.name }

        // AIへの指示。候補の中から1つだけ選ばせ、ジャンル名のみ返させる。
        val prompt = """
            次の決済を、以下のジャンル候補から最も適切なもの1つに分類してください。
            店名: $storeName
            金額: ${amount}円
            ジャンル候補: $candidateNames

            必ず候補の中の1つだけを、ジャンル名のみで回答してください。説明や記号は不要です。
        """.trimIndent()

        // リクエストのJSONを組み立てる。
        val bodyJson = JSONObject().apply {
            put("model", "claude-haiku-4-5-20251001")
            put("max_tokens", 50)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", BuildConfig.ANTHROPIC_API_KEY)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        // 通信実行。失敗したら null（分類できず）。
        val responseText: String = try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.string() ?: return null
            }
        } catch (e: Exception) {
            android.util.Log.e("AIClassify", "API通信失敗", e)
            return null
        }

        // レスポンスJSONから、AIが返したテキスト（ジャンル名）を取り出す。
        val answer: String = try {
            val json = JSONObject(responseText)
            val contentArray = json.getJSONArray("content")
            contentArray.getJSONObject(0).getString("text").trim()
        } catch (e: Exception) {
            android.util.Log.e("AIClassify", "レスポンス解析失敗: $responseText", e)
            return null
        }

        android.util.Log.d("AIClassify", "店名=$storeName → AI回答=$answer")

        // AIが返したジャンル名に一致する小ジャンルのidを返す。
        // 完全一致を優先、なければ部分一致で救済。
        return subs.firstOrNull { it.name == answer }?.id
            ?: subs.firstOrNull { answer.contains(it.name) }?.id
    }
}