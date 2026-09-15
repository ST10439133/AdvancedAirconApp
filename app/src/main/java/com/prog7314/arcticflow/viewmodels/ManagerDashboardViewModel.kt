// app/src/main/java/com/prog7314/arcticflow/viewmodels/ManagerDashboardViewModel.kt
package com.prog7314.arcticflow.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.prog7314.arcticflow.data.ArcticFlowDatabase
import com.prog7314.arcticflow.data.entities.Job
import com.prog7314.arcticflow.data.entities.JobStatus
import com.prog7314.arcticflow.data.entities.Notification
import com.prog7314.arcticflow.data.entities.NotificationType
import com.prog7314.arcticflow.data.entities.Quote
import com.prog7314.arcticflow.data.entities.QuoteStatus
import com.prog7314.arcticflow.data.entities.RequestStatus
import com.prog7314.arcticflow.data.entities.ServiceRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ManagerDashboardViewModel(
    private val database: ArcticFlowDatabase,
    private val userId: String
) : ViewModel() {

    private val TAG = "ManagerDashboardVM"

    // ===== Buildings =====
    val buildings = database.buildingDao().getBuildingsByUser(userId)

    // ===== Service requests by this manager =====
    val requests: Flow<List<ServiceRequest>> =
        database.serviceRequestDao().getRequestsByUser(userId)

    // ===== Pending requests only =====
    val pendingRequests: Flow<List<ServiceRequest>> =
        requests.map { list -> list.filter { it.status == RequestStatus.PENDING } }

    // ===== Quotes for this manager's requests =====
    val quotes: Flow<List<Quote>> = requests.map { reqs ->
        val ids = reqs.map { it.id }.toSet()
        database.quoteDao().getAllQuotesOnce()
            .filter { it.requestId in ids }
            .sortedByDescending { it.createdAt }
    }

    // ===== Pending quotes only =====
    val pendingQuotes: Flow<List<Quote>> =
        quotes.map { list -> list.filter { it.status == QuoteStatus.PENDING } }

    // ===== Accepted quotes only =====
    val acceptedQuotes: Flow<List<Quote>> =
        quotes.map { list -> list.filter { it.status == QuoteStatus.ACCEPTED } }

    // ===== ACCEPT A QUOTE =====
    suspend fun acceptQuote(quoteId: Int, scheduledDate: Long, timeSlot: String) {
        val quoteDao = database.quoteDao()
        val requestDao = database.serviceRequestDao()
        val jobDao = database.jobDao()
        val notificationDao = database.notificationDao()

        quoteDao.updateQuoteStatus(quoteId, QuoteStatus.ACCEPTED)
        val quote = quoteDao.getQuoteById(quoteId) ?: return

        // Fetch the request to get the snapshotted full address
        val request = requestDao.getRequestById(quote.requestId)

        android.util.Log.d(
            TAG,
            "acceptQuote: quote.requestId=${quote.requestId}, " +
                    "request=${request?.id}, " +
                    "request.fullAddress='${request?.fullAddress}'"
        )

        val jobAddress = request?.fullAddress.orEmpty()

        // Create the Job
        jobDao.insertJob(
            Job(
                quoteId = quote.id,
                requestId = quote.requestId,
                technicianId = quote.technicianId,
                customerId = quote.customerId,
                buildingName = quote.buildingName,
                issueType = quote.issueType,
                description = quote.description,
                status = JobStatus.SCHEDULED,
                scheduledDate = scheduledDate,
                notes = "Time Slot: $timeSlot",
                fullAddress = jobAddress              // ← NEW
            )
        )

        Log.d(
            TAG,
            "Job created for quote=${quote.id} tech=${quote.technicianId} " +
                    "cust=${quote.customerId} addr='$jobAddress'"
        )

        // Update request status
        requestDao.updateRequestStatus(quote.requestId, RequestStatus.ACCEPTED)

        // Notify technician
        notificationDao.insertNotification(
            Notification(
                title = "Job Scheduled",
                message = "Your quote for ${quote.buildingName} was accepted on $timeSlot.",
                type = NotificationType.JOB,
                userId = quote.technicianId
            )
        )
    }

    // ===== DECLINE A QUOTE =====
    suspend fun declineQuote(quoteId: Int) {
        val quoteDao = database.quoteDao()
        val requestDao = database.serviceRequestDao()
        val notificationDao = database.notificationDao()

        quoteDao.updateQuoteStatus(quoteId, QuoteStatus.DECLINED)
        val quote = quoteDao.getQuoteById(quoteId) ?: return

        // Reset the request to PENDING so the technician can re-quote
        requestDao.updateRequestStatus(quote.requestId, RequestStatus.PENDING)

        notificationDao.insertNotification(
            Notification(
                title = "Quote Declined",
                message = "Your quote for ${quote.buildingName} was declined.",
                type = NotificationType.QUOTE,
                userId = quote.technicianId
            )
        )
    }

    companion object {
        fun Factory(database: ArcticFlowDatabase, userId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ManagerDashboardViewModel(database, userId) as T
                }
            }
    }
}