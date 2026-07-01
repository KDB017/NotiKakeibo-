package com.example.notikakeibo

data class PayPayPayment(
    val amount: Int,
    val transactionId: String,
    val storeName: String
)

object PayPayParser {
    private val amountRegex = Regex("""金額：([\d,]+)円""")
    private val idRegex = Regex("""取引番号：(\d+)""")
    private val storeRegex = Regex("""店舗名：(.+)""")

    fun parse(text: String): PayPayPayment? {
        val amount = amountRegex.find(text)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull() ?: return null
        val id = idRegex.find(text)?.groupValues?.get(1) ?: return null
        val store = storeRegex.find(text)?.groupValues?.get(1)?.trim() ?: return null
        return PayPayPayment(amount, id, store)
    }
}