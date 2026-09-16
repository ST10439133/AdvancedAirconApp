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

    // ===== Buildings owned by this manager =====
    val buildings: Flow<List<com.prog7314.arcticflow.data.entities.BuildingEntity>> =
        database.buildingDao().getBuildingsByUser(userId)

    // ===== Service requests submitted by this manager =====
    val requests: Flow<List<ServiceRequest>> =
        database.serviceRequestDao().getRequestsByUser(userId)

    // ===== Pending requests only =====
    val pendingRequests: Flow<List<ServiceRequest>> =
        requests.map { list -> list.filter { it.status == RequestStatus.PENDING } }

    // ===== Quotes tied to this manager's requests =====
    val quotes: Flow<List<Quote>> = requests.map { reqs ->
        val ids = reqs.map { it.id }.toSet()
        database.quoteDao()
            .getAllQuotesOnce()
            .filter { it.requestId in ids }
            .sortedByDescending { it.createdAt }
    }

    // ===== Pending quotes only =====
    val pendingQuotes: Flow<List<Quote>> =
        quotes.map { list -> list.filter { it.status == QuoteStatus.PENDING } }

    // ===== Accepted quotes only =====
    val acceptedQuotes: Flow<List<Quote>> =
        quotes.map { list -> list.filter { it.status == QuoteStatus.ACCEPTED } }

    /**
     * Accept a quote and schedule the job.
     *  - Marks the quote ACCEPTED
     *  - Creates a Job for the technician
     *  - Marks the service request ACCEPTED
     *  - Notifies the technician
     */
    suspend fun acceptQuote(quoteId: Int, scheduledDate: Long, timeSlot: String) {
        val quoteDao = database.quoteDao()
        val requestDao = database.serviceRequestDao()
        val jobDao = database.jobDao()
        val notificationDao = database.notificationDao()

        try {
            // 1. Update the quote status
            quoteDao.updateQuoteStatus(quoteId, QuoteStatus.ACCEPTED)
            val quote = quoteDao.getQuoteById(quoteId)
            if (quote == null) {
                Log.w(TAG, "acceptQuote: quote $quoteId not found")
                return
            }

            // 2. Fetch the linked service request (for the full address)
            val request = requestDao.getRequestById(quote.requestId)

            Log.d(
                TAG,
                "acceptQuote: quote=$quoteId requestId=${quote.requestId} " +
                        "request=${request?.id} address='${request?.fullAddress ?: ""}'"
            )

            val jobAddress = request?.fullAddress ?: ""

            // 3. Create the Job
            val newJob = Job(
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
                fullAddress = jobAddress
            )
            jobDao.insertJob(newJob)

            Log.d(
                TAG,
                "Job created: quote=${quote.id} tech=${quote.technicianId} " +
                        "cust=${quote.customerId} addr='$jobAddress'"
            )

            // 4. Update request status
            requestDao.updateRequestStatus(quote.requestId, RequestStatus.ACCEPTED)

            // 5. Notify the technician
            notificationDao.insertNotification(
                Notification(
                    title = "Job Scheduled",
                    message = "Your quote for ${quote.buildingName} was accepted on $timeSlot.",
                    type = NotificationType.JOB,
                    userId = quote.technicianId
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "acceptQuote failed for quote=$quoteId", e)
        }
    }

    /**
     * Decline a quote.
     *  - Marks the quote DECLINED
     *  - Resets the service request to PENDING so the technician can re-quote
     *  - Notifies the technician
     */
    suspend fun declineQuote(quoteId: Int) {
        val quoteDao = database.quoteDao()
        val requestDao = database.serviceRequestDao()
        val notificationDao = database.notificationDao()

        try {
            quoteDao.updateQuoteStatus(quoteId, QuoteStatus.DECLINED)
            val quote = quoteDao.getQuoteById(quoteId)
            if (quote == null) {
                Log.w(TAG, "declineQuote: quote $quoteId not found")
                return
            }

            // Reset the request so the technician can re-quote
            requestDao.updateRequestStatus(quote.requestId, RequestStatus.PENDING)

            notificationDao.insertNotification(
                Notification(
                    title = "Quote Declined",
                    message = "Your quote for ${quote.buildingName} was declined.",
                    type = NotificationType.QUOTE,
                    userId = quote.technicianId
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "declineQuote failed for quote=$quoteId", e)
        }
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