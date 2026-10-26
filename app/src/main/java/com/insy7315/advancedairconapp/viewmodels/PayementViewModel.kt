// app/src/main/java/com/insy7315/advancedairconapp/viewModels/PaymentViewModel.kt
package com.insy7315.advancedairconapp.viewmodels

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.insy7315.advancedairconapp.data.ArcticFlowDatabase
import com.insy7315.advancedairconapp.data.api.ApiRepository
import com.insy7315.advancedairconapp.data.entities.Job
import com.insy7315.advancedairconapp.data.entities.Notification
import com.insy7315.advancedairconapp.data.entities.NotificationType
import com.insy7315.advancedairconapp.data.entities.Quote
import com.insy7315.advancedairconapp.data.entities.QuoteStatus
import com.insy7315.advancedairconapp.data.network.NetworkMonitor
import com.insy7315.advancedairconapp.data.payment.MockPaymentProcessor
import com.insy7315.advancedairconapp.data.payment.PaymentProcessor
import com.insy7315.advancedairconapp.utils.ReceiptGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

data class PaymentUiState(
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val receiptFile: File? = null,
    val reference: String? = null
)

class PaymentViewModel(
    application: Application,
    private val database: ArcticFlowDatabase
) : AndroidViewModel(application) {

    private val tag = "PaymentViewModel"
    private val appContext = application.applicationContext

    // Swap this for a real processor when going live.
    private val processor: PaymentProcessor = MockPaymentProcessor()

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    /**
     * Charge the customer for a completed job/quote.
     * Runs the whole sequence: charge → persist → receipt → notify.
     */
    fun payNow(
        quote: Quote,
        job: Job,
        customerEmail: String,
        customerName: String,
        technicianName: String
    ) {
        if (_uiState.value.isProcessing) return
        viewModelScope.launch {
            _uiState.value = PaymentUiState(isProcessing = true)

            val amount = if (quote.finalTotal > 0) quote.finalTotal else quote.grandTotal
            val result = processor.charge(
                PaymentProcessor.Request(
                    quoteId = quote.id,
                    amountZar = amount,
                    customerEmail = customerEmail,
                    customerName = customerName,
                    description = "ArcticFlow job at ${job.buildingName}"
                )
            )

            if (!result.success || result.reference == null) {
                _uiState.value = PaymentUiState(
                    isProcessing = false,
                    error = result.errorMessage ?: "Payment failed"
                )
                return@launch
            }

            val reference = result.reference
            val paidAt = System.currentTimeMillis()

            try {
                // 1. Persist locally
                database.quoteDao().markPaid(quote.id, reference, paidAt)

                // 2. Notify technician + customer
                database.notificationDao().insertNotification(
                    Notification(
                        title = "Payment Received",
                        message = "Customer paid R${
                            String.format(Locale.US, "%.2f", amount)
                        } for ${job.buildingName}. Receipt available.",
                        type = NotificationType.QUOTE,
                        userId = quote.technicianId
                    )
                )
                database.notificationDao().insertNotification(
                    Notification(
                        title = "Payment Confirmed",
                        message = "Your payment of R${
                            String.format(Locale.US, "%.2f", amount)
                        } has been received. Receipt available.",
                        type = NotificationType.QUOTE,
                        userId = quote.customerId
                    )
                )

                // 3. Push status to server (best-effort)
                if (NetworkMonitor.isOnline(appContext)) {
                    quote.serverId?.let { sid ->
                        try {
                            ApiRepository.updateQuoteStatus(appContext, sid, "PAID")
                        } catch (e: Exception) {
                            Log.w(tag, "Server PAID push failed", e)
                        }
                    }
                }

                // 4. Generate the receipt PDF
                val paidQuote = quote.copy(
                    status = QuoteStatus.PAID,
                    finalTotal = amount,
                    paymentReference = reference,
                    paidAt = paidAt
                )
                val receiptFile = ReceiptGenerator.generate(
                    context = appContext,
                    quote = paidQuote,
                    job = job,
                    customerName = customerName,
                    technicianName = technicianName,
                    paymentReference = reference,
                    paidAt = paidAt
                )

                _uiState.value = PaymentUiState(
                    isProcessing = false,
                    isSuccess = true,
                    receiptFile = receiptFile,
                    reference = reference
                )
            } catch (e: Exception) {
                Log.e(tag, "post-charge persistence failed", e)
                _uiState.value = PaymentUiState(
                    isProcessing = false,
                    error = "Payment received but saving failed: ${e.message}"
                )
            }
        }
    }

    fun reset() {
        _uiState.value = PaymentUiState()
    }

    /**
     * Regenerate a receipt for an already-paid quote. Used by the
     * technician's "Payments Received" list.
     */
    fun buildReceipt(
        quote: Quote,
        job: Job,
        customerName: String,
        technicianName: String
    ): File? {
        val reference = quote.paymentReference ?: return null
        return ReceiptGenerator.generate(
            context = appContext,
            quote = quote,
            job = job,
            customerName = customerName,
            technicianName = technicianName,
            paymentReference = reference,
            paidAt = quote.paidAt ?: System.currentTimeMillis()
        )
    }

    companion object {
        fun Factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = ArcticFlowDatabase.getDatabase(application)
                    return PaymentViewModel(application, db) as T
                }
            }
    }
}