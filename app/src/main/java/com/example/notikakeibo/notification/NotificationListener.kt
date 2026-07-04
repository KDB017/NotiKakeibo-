package com.example.notikakeibo.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

// システムが「通知が出た」ときに、このクラスに教えてくれる。
// NotificationListenerService を継承すると、その仕組みに乗れる。
class NotificationListener : NotificationListenerService() {

    // 通知が出るたびに、システムがこのメソッドを自動で呼ぶ。
    // sbn（StatusBarNotification）に、出た通知の中身が入ってる。
    override fun onNotificationPosted(sbn: StatusBarNotification) {

        // どのアプリが出した通知か（パッケージ名）。
        val packageName = sbn.packageName

        // 通知の中身は extras に入ってる。
        val extras = sbn.notification.extras

        // タイトルと本文を取り出す。
        // BIG_TEXT は「展開したときの全文」。PayPayは複数行なのでこっちを優先。
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.bigText")?.toString()
            ?: extras.getCharSequence("android.text")?.toString()
            ?: ""

        // まずは「どのアプリからどんな通知が来たか」を全部ログに出して観察する。
        Log.d("NotiKakeibo", "package=$packageName / title=$title / text=$text")

        // PayPayの通知だけを対象にする。
        // （PayPayアプリのパッケージ名は後で実物を見て確定する）
        if (packageName == "jp.ne.paypay.android.app") {
            val payment = PayPayParser.parse(text)
            Log.d("NotiKakeibo", "PayPay payment = $payment")
        }
    }
}