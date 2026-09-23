package com.insy7315.advancedairconapp.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.insy7315.advancedairconapp.data.ArcticFlowDatabase
import com.insy7315.advancedairconapp.data.api.ApiRepository
import com.insy7315.advancedairconapp.data.entities.*
import com.insy7315.advancedairconapp.data.network.NetworkMonitor
import com.insy7315.advancedairconapp.data.sync.DataSyncManager
import com.insy7315.advancedairconapp.data.sync.SyncManager
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

    val currentUser: Flow<User?> =
        database.userDao().observeUserById(userId)

    val buildings: Flow<List<BuildingEntity>> =
        database.buildingDao().getBuildingsByUser(userId)

    val requests: Flow<List<ServiceRequest>> =
        database.serviceRequestDao().getRequestsByUser(userId)

    val pendingRequests: Flow<List<ServiceRequest>> =
        requests.map { list -> list.filter { it.status == RequestStatus.PENDING } }

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

    suspend fun refreshFromServer(userRole: String) {
        try { DataSyncManager.syncEverything(appContext, userRole) }
        catch (e: Exception) { Log.e(TAG, "refreshFromServer failed", e) }
    }

    // ============================================================
    // ACCEPT QUOTE
    // ============================================================
    suspend fun acceptQuote(quoteId: Int, scheduledDate: Long, timeSlot: String) {
        val quoteDao = database.quoteDao()
        val requestDao = database.serviceRequestDao()
        val jobDao = database.jobDao()
        val notificationDao = database.notificationDao()

        try {
            val quote = quoteDao.getQuoteById(quoteId)
            if (quote == null) {
                Log.e(TAG, "acceptQuote: quote $quoteId not found")
                return
            }

            val request = requestDao.getRequestById(quote.requestId)
            if (request == null) {
                Log.e(TAG, "acceptQuote: request ${quote.requestId} not found " +
                        "for quote $quoteId (this is a sync bug — pull-sync " +
                        "created a different local id)")
                return
            }

            // 1. Local quote status
            quoteDao.updateQuoteStatus(quoteId, QuoteStatus.ACCEPTED)

            // 2. Local job
            val newJob = Job(
                id = 0,
                serverId = null,
                quoteId = quote.id,
                serverQuoteId = quote.serverId,
                requestId = request.id,
                serverRequestId = request.serverId,
                technicianId = quote.technicianId,
                customerId = quote.customerId,
                buildingName = quote.buildingName,
                issueType = quote.issueType,
                description = quote.description,
                status = JobStatus.SCHEDULED,
                scheduledDate = scheduledDate,
                notes = "Time Slot: $timeSlot",
                fullAddress = request.fullAddress
            )
            val newJobId = jobDao.insertJob(newJob)

            // 3. Local request status
            requestDao.updateRequestStatus(request.id, RequestStatus.ACCEPTED)

            // 4. Local notification for the technician
            notificationDao.insertNotification(
                Notification(
                    title = "Job Scheduled",
                    message = "Your quote for ${quote.buildingName} was accepted on $timeSlot.",
                    type = NotificationType.JOB,
                    userId = quote.technicianId
                )
            )

            // 5. Push to server (or queue if offline)
            pushAcceptance(
                quote = quote,
                request = request,
                newJobId = newJobId.toInt(),
                newJob = newJob.copy(id = newJobId.toInt())
            )

        } catch (e: Exception) {
            Log.e(TAG, "acceptQuote failed for quote=$quoteId", e)
        }
    }

    private suspend fun pushAcceptance(
        quote: Quote,
        request: ServiceRequest,
        newJobId: Int,
        newJob: Job
    ) {
        val online = NetworkMonitor.isOnline(appContext)

        // QUOTE STATUS
        val serverQuoteId = quote.serverId
        if (serverQuoteId != null && online) {
            val ok = ApiRepository.updateQuoteStatus(appContext, serverQuoteId, QuoteStatus.ACCEPTED.name)
            if (!ok) SyncManager.enqueue(
                appContext, SyncManager.TYPE_QUOTE_STATUS, quote.id,
                mapOf("status" to QuoteStatus.ACCEPTED.name)
            )
        } else {
            SyncManager.enqueue(
                appContext, SyncManager.TYPE_QUOTE_STATUS, quote.id,
                mapOf("status" to QuoteStatus.ACCEPTED.name)
            )
        }

        // JOB CREATE
        if (online) {
            val dto = ApiRepository.pushJob(
                context = appContext,
                job = newJob,
                serverQuoteId = quote.serverId,
                serverRequestId = request.serverId
            )
            if (dto != null) {
                database.jobDao().setServerId(newJobId, dto.id)
            } else {
                SyncManager.enqueue(
                    appContext, SyncManager.TYPE_JOB_CREATE, newJobId, emptyMap()
                )
            }
        } else {
            SyncManager.enqueue(
                appContext, SyncManager.TYPE_JOB_CREATE, newJobId, emptyMap()
            )
        }

        // REQUEST STATUS
        val serverRequestId = request.serverId
        if (serverRequestId != null && online) {
            val ok = ApiRepository.updateRequestStatus(appContext, serverRequestId, RequestStatus.ACCEPTED.name)
            if (!ok) SyncManager.enqueue(
                appContext, SyncManager.TYPE_REQUEST_STATUS, request.id,
                mapOf("status" to RequestStatus.ACCEPTED.name)
            )
        } else {
            SyncManager.enqueue(
                appContext, SyncManager.TYPE_REQUEST_STATUS, request.id,
                mapOf("status" to RequestStatus.ACCEPTED.name)
            )
        }
    }

    // ============================================================
    // DECLINE QUOTE
    // ============================================================
    suspend fun declineQuote(quoteId: Int) {
        val quoteDao = database.quoteDao()
        val requestDao = database.serviceRequestDao()
        val notificationDao = database.notificationDao()

        try {
            val quote = quoteDao.getQuoteById(quoteId)
            if (quote == null) {
                Log.w(TAG, "declineQuote: quote $quoteId not found")
                return
            }
            val request = requestDao.getRequestById(quote.requestId)

            quoteDao.updateQuoteStatus(quoteId, QuoteStatus.DECLINED)
            if (request != null) {
                requestDao.updateRequestStatus(request.id, RequestStatus.PENDING)
            }

            notificationDao.insertNotification(
                Notification(
                    title = "Quote Declined",
                    message = "Your quote for ${quote.buildingName} was declined.",
                    type = NotificationType.QUOTE,
                    userId = quote.technicianId
                )
            )

            val online = NetworkMonitor.isOnline(appContext)

            // Quote status
            val sq = quote.serverId
            if (sq != null && online) {
                val ok = ApiRepository.updateQuoteStatus(appContext, sq, QuoteStatus.DECLINED.name)
                if (!ok) SyncManager.enqueue(
                    appContext, SyncManager.TYPE_QUOTE_STATUS, quote.id,
                    mapOf("status" to QuoteStatus.DECLINED.name)
                )
            } else {
                SyncManager.enqueue(
                    appContext, SyncManager.TYPE_QUOTE_STATUS, quote.id,
                    mapOf("status" to QuoteStatus.DECLINED.name)
                )
            }

            // Request status
            if (request != null) {
                val sr = request.serverId
                if (sr != null && online) {
                    val ok = ApiRepository.updateRequestStatus(appContext, sr, RequestStatus.PENDING.name)
                    if (!ok) SyncManager.enqueue(
                        appContext, SyncManager.TYPE_REQUEST_STATUS, request.id,
                        mapOf("status" to RequestStatus.PENDING.name)
                    )
                } else {
                    SyncManager.enqueue(
                        appContext, SyncManager.TYPE_REQUEST_STATUS, request.id,
                        mapOf("status" to RequestStatus.PENDING.name)
                    )
                }
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