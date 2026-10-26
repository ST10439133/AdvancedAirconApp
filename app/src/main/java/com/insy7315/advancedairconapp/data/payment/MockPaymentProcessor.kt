// app/src/main/java/com/insy7315/advancedairconapp/data/payment/MockPaymentProcessor.kt
package com.insy7315.advancedairconapp.data.payment

import kotlinx.coroutines.delay
import java.util.UUID

/**
 * Simulated gateway. Waits a moment, then returns a fake reference.
 * Swap for a real PaystackPaymentProcessor when you're ready to go live.
 */
class MockPaymentProcessor : PaymentProcessor {

    override suspend fun charge(request: PaymentProcessor.Request): PaymentProcessor.Result {
        // Simulate network round-trip
        delay(1500)

        return try {
            // Simulate a 5% random failure to prove the error path works
            if (System.currentTimeMillis() % 20L == 0L) {
                PaymentProcessor.Result(
                    success = false,
                    reference = null,
                    message = null,
                    errorMessage = "Card declined (simulated). Please try again."
                )
            } else {
                PaymentProcessor.Result(
                    success = true,
                    reference = "MOCK-${UUID.randomUUID().toString().take(8).uppercase()}",
                    message = "Payment of R${"%.2f".format(request.amountZar)} received",
                    errorMessage = null
                )
            }
        } catch (e: Exception) {
            PaymentProcessor.Result(
                success = false,
                reference = null,
                message = null,
                errorMessage = e.message ?: "Payment failed"
            )
        }
    }
}