package com.insy7315.advancedaircornapp.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.insy7315.advancedaircornapp.data.ArcticFlowDatabase
import com.insy7315.advancedaircornapp.data.api.ApiRepository
import com.insy7315.advancedaircornapp.data.api.IdMap
import com.insy7315.advancedaircornapp.data.entities.BuildingEntity
import com.insy7315.advancedaircornapp.data.entities.Job
import com.insy7315.advancedaircornapp.data.entities.JobStatus
import com.insy7315.advancedaircornapp.data.entities.Notification
import com.insy7315.advancedaircornapp.data.entities.NotificationType
import com.insy7315.advancedaircornapp.data.entities.Quote
import com.insy7315.advancedaircornapp.data.entities.QuoteStatus
import com.insy7315.advancedaircornapp.data.entities.RequestStatus
import com.insy7315.advancedaircornapp.data.entities.ServiceRequest
import com.insy7315.advancedaircornapp.data.entities.User
import com.insy7315.advancedaircornapp.data.network.NetworkMonitor
import com.insy7315.advancedaircornapp.data.sync.SyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class ManagerDashboardViewModel(
    application: Application,
    private val database: ArcticFlowDatabase,
    private val userId: String
) : AndroidViewModel(application) {

    private val TAG = "ManagerDashboardVM"

    private val appContext get() = getApplication<Application>()

    // Live user record for the header
    val currentUser: Flow<User?> =
        database.userDao().observeUserById(userId)

    // Buildings owned by this manager
    val buildings: Flow<List<BuildingEntity>> =
        database.buildingDao().getBuildingsByUser(userId)

    // Service requests submitted by this manager
    val requests: Flow<List<ServiceRequest>> =
        database.serviceRequestDao().getRequestsByUser(userId)

    // Pending requests only
    val pendingRequests: Flow<List<ServiceRequest>> =
        requests.map { list -> list.filter { it.status == RequestStatus.PENDING } }

    // quotes stream from Room, and filter client-side.
    val quotes: Flow<List<Quote>> = combine(
        requests.map { reqs -> reqs.map { it.id }.toSet() },
        database.quoteDao().getAllQuotesFlow()
    ) { requestIds, allQuotes ->
        if (requestIds.isEmpty()) emptyList()
        else allQuotes
            .filter { it.requestId in requestIds }
            .sortedByDescending { it.createdAt }
    }

    val pendingQuotes: Flow<List<Quote>> =
        quotes.map { list -> list.filter { it.status == QuoteStatus.PENDING } }

    val acceptedQuotes: Flow<List<Quote>> =
        quotes.map { list -> list.filter { it.status == QuoteStatus.ACCEPTED } }

    // ACCEPT QUOTE — offline-aware
    suspend fun acceptQuote(quoteId: Int, scheduledDate: Long, timeSlot: String) {
        val quoteDao = database.quoteDao()
        val requestDao = database.serviceRequestDao()
        val jobDao = database.jobDao()
        val notificationDao = database.notificationDao()

        try {
            quoteDao.updateQuoteStatus(quoteId, QuoteStatus.ACCEPTED)
            val quote = quoteDao.getQuoteById(quoteId)
            if (quote == null) {
                Log.w(TAG, "acceptQuote: quote $quoteId not found")
                return
            }

            val request = requestDao.getRequestById(quote.requestId)

            val jobAddress = request?.fullAddress ?: ""

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
            val newJobId = jobDao.insertJob(newJob)

            requestDao.updateRequestStatus(quote.requestId, RequestStatus.ACCEPTED)

            notificationDao.insertNotification(
                Notification(
                    title = "Job Scheduled",
                    message = "Your quote for ${quote.buildingName} was accepted on $timeSlot.",
                    type = NotificationType.JOB,
                    userId = quote.technicianId
                )
            )

            // API SYNC (offline-aware)
            if (NetworkMonitor.isOnline(appContext)) {
                val okQuote = ApiRepository.updateQuoteStatus(
                    appContext, quote.id, QuoteStatus.ACCEPTED.name
                )
                if (!okQuote) {
                    SyncManager.enqueue(
                        appContext,
                        SyncManager.TYPE_QUOTE_STATUS,
                        quote.id,
                        mapOf("status" to QuoteStatus.ACCEPTED.name)
                    )
                }

                val serverQuoteId   = IdMap.getQuote(appContext, quote.id)
                val serverRequestId = IdMap.getRequest(appContext, quote.requestId)

                val dto = ApiRepository.pushJob(
                    context = appContext,
                    job = newJob.copy(id = newJobId.toInt()),
                    serverQuoteId = serverQuoteId,
                    serverRequestId = serverRequestId
                )
                if (dto != null) {
                    IdMap.putJob(appContext, newJobId.toInt(), dto.id)
                } else {
                    SyncManager.enqueue(
                        appContext,
                        SyncManager.TYPE_JOB_CREATE,
                        newJobId.toInt(),
                        emptyMap()
                    )
                }

                val okReq = ApiRepository.updateRequestStatus(
                    appContext, quote.requestId, RequestStatus.ACCEPTED.name
                )
                if (!okReq) {
                    SyncManager.enqueue(
                        appContext,
                        SyncManager.TYPE_REQUEST_STATUS,
                        quote.requestId,
                        mapOf("status" to RequestStatus.ACCEPTED.name)
                    )
                }
            } else {
                // Offline: queue all three writes
                SyncManager.enqueue(
                    appContext,
                    SyncManager.TYPE_QUOTE_STATUS,
                    quote.id,
                    mapOf("status" to QuoteStatus.ACCEPTED.name)
                )
                SyncManager.enqueue(
                    appContext,
                    SyncManager.TYPE_JOB_CREATE,
                    newJobId.toInt(),
                    emptyMap()
                )
                SyncManager.enqueue(
                    appContext,
                    SyncManager.TYPE_REQUEST_STATUS,
                    quote.requestId,
                    mapOf("status" to RequestStatus.ACCEPTED.name)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "acceptQuote failed for quote=$quoteId", e)
        }
    }

    // DECLINE QUOTE — offline-aware
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

            requestDao.updateRequestStatus(quote.requestId, RequestStatus.PENDING)

            notificationDao.insertNotification(
                Notification(
                    title = "Quote Declined",
                    message = "Your quote for ${quote.buildingName} was declined.",
                    type = NotificationType.QUOTE,
                    userId = quote.technicianId
                )
            )

            if (NetworkMonitor.isOnline(appContext)) {
                val okQuote = ApiRepository.updateQuoteStatus(
                    appContext, quote.id, QuoteStatus.DECLINED.name
                )
                if (!okQuote) {
                    SyncManager.enqueue(
                        appContext,
                        SyncManager.TYPE_QUOTE_STATUS,
                        quote.id,
                        mapOf("status" to QuoteStatus.DECLINED.name)
                    )
                }

                val okReq = ApiRepository.updateRequestStatus(
                    appContext, quote.requestId, RequestStatus.PENDING.name
                )
                if (!okReq) {
                    SyncManager.enqueue(
                        appContext,
                        SyncManager.TYPE_REQUEST_STATUS,
                        quote.requestId,
                        mapOf("status" to RequestStatus.PENDING.name)
                    )
                }
            } else {
                SyncManager.enqueue(
                    appContext,
                    SyncManager.TYPE_QUOTE_STATUS,
                    quote.id,
                    mapOf("status" to QuoteStatus.DECLINED.name)
                )
                SyncManager.enqueue(
                    appContext,
                    SyncManager.TYPE_REQUEST_STATUS,
                    quote.requestId,
                    mapOf("status" to RequestStatus.PENDING.name)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "declineQuote failed for quote=$quoteId", e)
        }
    }

    companion object {
        fun Factory(
            application: Application,
            database: ArcticFlowDatabase,
            userId: String
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ManagerDashboardViewModel(application, database, userId) as T
                }
            }
    }
}