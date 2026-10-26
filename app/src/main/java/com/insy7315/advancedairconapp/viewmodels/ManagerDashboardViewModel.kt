// app/src/main/java/com/insy7315/advancedairconapp/viewmodels/ManagerDashboardViewModel.kt
//Google for Developers. 2026. Offline-first apps | Android Developers. [Online]. Available at: https://developer.android.com/topic/architecture/data-layer/offline-first [Accessed: 5 October 2026].
//Google for Developers. 2026. Kotlin flows on Android | Android Developers. [Online]. Available at: https://developer.android.com/kotlin/flow [Accessed: 5 October 2026].

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
import java.util.Calendar

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
        allQuotes
            .filter { it.requestId in requestIds || it.customerId == userId }
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

    // ACCEPT QUOTE — returns null on success, or an error message
    suspend fun acceptQuote(
        quoteId: Int,
        scheduledDate: Long,
        timeSlot: String
    ): String? {
        val quoteDao = database.quoteDao()
        val requestDao = database.serviceRequestDao()
        val jobDao = database.jobDao()
        val notificationDao = database.notificationDao()

        try {
            val quote = quoteDao.getQuoteById(quoteId)
                ?: return "Quote not found."
            val request = requestDao.getRequestById(quote.requestId)
                ?: return "Service request not found for this quote."

            val conflict = validateConflict(
                technicianId = quote.technicianId,
                scheduledDate = scheduledDate,
                timeSlot = timeSlot
            )
            if (conflict != null) return conflict

            quoteDao.updateQuoteStatus(quoteId, QuoteStatus.ACCEPTED)

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

            requestDao.updateRequestStatus(request.id, RequestStatus.ACCEPTED)

            notificationDao.insertNotification(
                Notification(
                    title = "Job Scheduled",
                    message = "Your quote for ${quote.buildingName} was accepted on $timeSlot.",
                    type = NotificationType.JOB,
                    userId = quote.technicianId
                )
            )

            pushAcceptance(
                quote = quote,
                request = request,
                newJobId = newJobId.toInt(),
                newJob = newJob.copy(id = newJobId.toInt())
            )

            return null
        } catch (e: Exception) {
            Log.e(TAG, "acceptQuote failed for quote=$quoteId", e)
            return "Could not accept quote: ${e.message}"
        }
    }

    private suspend fun validateConflict(
        technicianId: String,
        scheduledDate: Long,
        timeSlot: String
    ): String? {
        if (technicianId.isBlank() || timeSlot.isBlank()) return null

        val cal = Calendar.getInstance().apply {
            timeInMillis = scheduledDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMs = cal.timeInMillis
        val endMs = startMs + 24L * 60 * 60 * 1000

        val sameDayJobs = try {
            database.jobDao().getJobsForTechnicianInRange(
                technicianId, startMs, endMs
            )
        } catch (e: Exception) {
            return null
        }

        for (job in sameDayJobs) {
            val existingSlot = job.notes
                ?.substringAfter("Time Slot:", "")
                ?.trim()
                .orEmpty()

            if (existingSlot.isBlank()) {
                return "Technician already has a job scheduled on this day."
            }
            if (existingSlot.equals(timeSlot, ignoreCase = true)) {
                return "This time slot is already booked for ${job.buildingName}."
            }
        }

        return null
    }

    private suspend fun pushAcceptance(
        quote: Quote,
        request: ServiceRequest,
        newJobId: Int,
        newJob: Job
    ) {
        val online = NetworkMonitor.isOnline(appContext)

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

    // DECLINE QUOTE — with optional reason
    suspend fun declineQuote(quoteId: Int) {
        declineQuoteWithReason(quoteId, "")
    }

    suspend fun declineQuoteWithReason(quoteId: Int, reason: String) {
        val quoteDao = database.quoteDao()
        val requestDao = database.serviceRequestDao()
        val notificationDao = database.notificationDao()

        try {
            val quote = quoteDao.getQuoteById(quoteId)
            if (quote == null) {
                Log.w(TAG, "declineQuoteWithReason: quote $quoteId not found")
                return
            }
            val request = requestDao.getRequestById(quote.requestId)

            quoteDao.updateQuoteDecline(
                quoteId = quoteId,
                status = QuoteStatus.DECLINED,
                reason = reason.trim(),
                by = "CUSTOMER"
            )
            if (request != null) {
                requestDao.updateRequestStatus(request.id, RequestStatus.PENDING)
            }

            notificationDao.insertNotification(
                Notification(
                    title = "Quote Declined",
                    message = "Your quote for ${quote.buildingName} was declined." +
                            if (reason.trim().isNotBlank()) " Reason: \"${reason.trim()}\"" else "",
                    type = NotificationType.QUOTE,
                    userId = quote.technicianId
                )
            )

            val online = NetworkMonitor.isOnline(appContext)

            val sq = quote.serverId
            if (sq != null && online) {
                val ok = ApiRepository.updateQuoteStatus(appContext, sq, QuoteStatus.DECLINED.name)
                if (!ok) SyncManager.enqueue(
                    appContext, SyncManager.TYPE_QUOTE_STATUS, quote.id,
                    mapOf("status" to QuoteStatus.DECLINED.name)
                )
                try {
                    ApiRepository.updateQuoteDeclineReason(
                        context = appContext,
                        serverId = sq,
                        reason = reason.trim(),
                        declinedBy = "CUSTOMER"
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Decline reason sync failed", e)
                }
            } else {
                SyncManager.enqueue(
                    appContext, SyncManager.TYPE_QUOTE_STATUS, quote.id,
                    mapOf("status" to QuoteStatus.DECLINED.name)
                )
            }

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
            Log.e(TAG, "declineQuoteWithReason failed for quote=$quoteId", e)
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