// app/src/main/java/com/insy7315/advancedairconapp/data/payment/PaymentProcessor.kt
package com.insy7315.advancedairconapp.data.payment

/**
 * Pluggable payment processor. Implement this interface to swap in
 * a real gateway (Paystack, Stripe, Peach Payments, Yoco, etc.)
 * without touching any UI code.
 */
interface PaymentProcessor {

    data class Request(
        val quoteId: Int,
        val amountZar: Double,
        val customerEmail: String,
        val customerName: String,
        val description: String
    )

    data class Result(
        val success: Boolean,
        val reference: String?,
        val message: String?,
        val errorMessage: String? = null
    )

    /**
     * Process a payment. Must be safe to call from a coroutine.
     * Real implementations should return success=false until the
     * gateway's verification callback fires.
     */
    suspend fun charge(request: Request): Result
}