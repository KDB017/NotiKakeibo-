package com.example.notikakeibo.notification
import com.example.notikakeibo.classifier.ClassificationService
import com.example.notikakeibo.classifier.AnthropicClassifier
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.notikakeibo.data.AppDatabase
import com.example.notikakeibo.data.entity.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val extras = sbn.notification.extras

        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.bigText")?.toString()
            ?: extras.getCharSequence("android.text")?.toString()
            ?: ""

        Log.d("NotiKakeibo", "package=$packageName / title=$title / text=$text")

        // PayPayの通知だけを対象にする。
        if (packageName == "jp.ne.paypay.android.app") {
            val payment = PayPayParser.parse(text)
            Log.d("NotiKakeibo", "PayPay payment = $payment")

            // パースに成功したらDBに保存する。
            if (payment != null) {
                // DTO(PayPayPayment) → Entity(TransactionEntity) へ変換。
                val entity = TransactionEntity(
                    transactionId = payment.transactionId,
                    amount = payment.amount,
                    storeName = payment.storeName,
                    timestamp = System.currentTimeMillis(),  // 今の時刻
                    subcategoryId = null                       // 分類はまだ未実装なのでnull
                )

                // DB保存は時間がかかるので別スレッド(IO)で。
                CoroutineScope(Dispatchers.IO).launch {
                    val dao = AppDatabase.getInstance(applicationContext).transactionDao()
                    dao.insert(entity)
                    CoroutineScope(Dispatchers.IO).launch {
                        val db = AppDatabase.getInstance(applicationContext)
                        db.transactionDao().insert(entity)

                        // 保存した直後に自動分類する。
                        val classifier = AnthropicClassifier(db.categoryDao())
                        val service = ClassificationService(applicationContext, classifier)
                        service.classifyTransaction(
                            entity.transactionId,
                            entity.storeName,
                            entity.amount
                        )

                        // 確認ログ
                        val all = db.transactionDao().getAllWithCategory()
                        Log.d("TxSave", "保存＋分類完了。取引数=${all.size}")
                        for (t in all) {
                            val genre = if (t.majorName != null) "${t.majorName}>${t.minorName}" else "未分類"
                            Log.d("TxSave", "取引[${t.storeName}] ¥${t.amount} → $genre")
                        }
                    }

                    // 確認：保存後、全取引を数えてログに出す。
                    val all = dao.getAll()
                    Log.d("TxSave", "保存完了。現在の取引数=${all.size}")
                    for (t in all) {
                        Log.d("TxSave", "取引[${t.transactionId}] ${t.storeName} ¥${t.amount}")
                    }
                }
            }
        }
    }
}