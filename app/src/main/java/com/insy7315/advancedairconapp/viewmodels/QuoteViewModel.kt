// app/src/main/java/com/insy7315/advancedairconapp/viewmodels/QuoteViewModel.kt
//Google for Developers. 2026. Offline-first apps | Android Developers. [Online]. Available at: https://developer.android.com/topic/architecture/data-layer/offline-first [Accessed: 5 October 2026].
//Google for Developers. 2026. ViewModel factories | Android Developers. [Online]. Available at: https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-factories [Accessed: 5 October 2026].

package com.insy7315.advancedairconapp.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.insy7315.advancedairconapp.data.ArcticFlowDatabase
import com.insy7315.advancedairconapp.data.api.ApiRepository
import com.insy7315.advancedairconapp.data.entities.*
import com.insy7315.advancedairconapp.data.network.NetworkMonitor
import com.insy7315.advancedairconapp.data.sync.DataSyncManager
import com.insy7315.advancedairconapp.data.sync.SyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class QuoteViewModel(
    private val database: ArcticFlowDatabase,
    private val appContext: Context
) : ViewModel() {

    private val tag = "QuoteViewModel"
    private val buildingDao = database.buildingDao()
    private val requestDao = database.serviceRequestDao()
    private val quoteDao = database.quoteDao()
    private val jobDao = database.jobDao()
    private val notificationDao = database.notificationDao()

    suspend fun refreshFromServer(userRole: String) {
        try { DataSyncManager.syncEverything(appContext, userRole) }
        catch (e: Exception) { Log.e(tag, "refreshFromServer failed", e) }
    }

    // BUILDINGS
    suspend fun addBuilding(userId: String, building: BuildingEntity): Long =
        try {
            val localId = buildingDao.insertBuilding(building.copy(userId = userId))
            val localRow = buildingDao.getBuildingById(localId.toInt()) ?: return localId

            if (NetworkMonitor.isOnline(appContext)) {
                val dto = ApiRepository.pushBuilding(appContext, localRow)
                if (dto != null) {
                    buildingDao.setServerId(localId.toInt(), dto.id)
                } else {
                    SyncManager.enqueue(
                        appContext, SyncManager.TYPE_BUILDING_CREATE, localId.toInt(), emptyMap()
                    )
                }
            } else {
                SyncManager.enqueue(
                    appContext, SyncManager.TYPE_BUILDING_CREATE, localId.toInt(), emptyMap()
                )
            }
            localId
        } catch (e: Exception) {
            Log.e(tag, "addBuilding failed", e); 0L
        }

    fun getBuildingsForUser(userId: String): Flow<List<BuildingEntity>> =
        try { buildingDao.getBuildingsByUser(userId) }
        catch (e: Exception) { flow { emit(emptyList()) } }

    suspend fun getBuildingById(buildingId: Int): BuildingEntity? =
        try { buildingDao.getBuildingById(buildingId) } catch (e: Exception) { null }

    // SERVICE REQUESTS
    suspend fun createServiceRequest(request: ServiceRequest): Long =
        try {
            val localId = requestDao.insertRequest(request)
            val localRow = requestDao.getRequestById(localId.toInt()) ?: return localId
            val building = buildingDao.getBuildingById(localRow.buildingId)

            if (NetworkMonitor.isOnline(appContext)) {
                val dto = ApiRepository.pushServiceRequest(
                    context = appContext,
                    request = localRow,
                    buildingName = building?.name,
                    fullAddress = building?.fullAddress,
                    serverBuildingId = building?.serverId
                )
                if (dto != null) {
                    requestDao.setServerId(localId.toInt(), dto.id)
                } else {
                    SyncManager.enqueue(
                        appContext, SyncManager.TYPE_REQUEST_CREATE, localId.toInt(), emptyMap()
                    )
                }
            } else {
                SyncManager.enqueue(
                    appContext, SyncManager.TYPE_REQUEST_CREATE, localId.toInt(), emptyMap()
                )
            }
            localId
        } catch (e: Exception) {
            Log.e(tag, "createServiceRequest failed", e); 0L
        }

    fun getRequestsForUser(userId: String): Flow<List<ServiceRequest>> =
        try { requestDao.getRequestsByUser(userId) }
        catch (e: Exception) { flow { emit(emptyList()) } }

    fun getPendingServiceRequests(): Flow<List<ServiceRequest>> =
        try { requestDao.getPendingRequests() }
        catch (e: Exception) { flow { emit(emptyList()) } }

    fun getRequestsByStatus(status: RequestStatus): Flow<List<ServiceRequest>> =
        try { requestDao.getRequestsByStatus(status) }
        catch (e: Exception) { flow { emit(emptyList()) } }

    suspend fun getRequestById(requestId: Int): ServiceRequest? =
        try { requestDao.getRequestById(requestId) } catch (e: Exception) { null }

    suspend fun updateRequestStatus(requestId: Int, status: RequestStatus) {
        try {
            requestDao.updateRequestStatus(requestId, status)
            val req = requestDao.getRequestById(requestId) ?: return

            val online = NetworkMonitor.isOnline(appContext)
            val sr = req.serverId
            if (sr != null && online) {
                val ok = ApiRepository.updateRequestStatus(appContext, sr, status.name)
                if (!ok) SyncManager.enqueue(
                    appContext, SyncManager.TYPE_REQUEST_STATUS, requestId,
                    mapOf("status" to status.name)
                )
            } else {
                SyncManager.enqueue(
                    appContext, SyncManager.TYPE_REQUEST_STATUS, requestId,
                    mapOf("status" to status.name)
                )
            }
        } catch (e: Exception) { Log.e(tag, "updateRequestStatus failed", e) }
    }

    suspend fun updateServiceRequest(request: ServiceRequest) {
        try { requestDao.updateRequestFull(request) } catch (_: Exception) {}
    }

    suspend fun deleteServiceRequest(request: ServiceRequest) {
        try { requestDao.deleteRequest(request) } catch (_: Exception) {}
    }

    // QUOTES
    suspend fun createQuote(quote: Quote): Long =
        try {
            val localId = quoteDao.insertQuote(quote)
            val localRow = quoteDao.getQuoteById(localId.toInt()) ?: return localId
            val request = requestDao.getRequestById(localRow.requestId)

            if (NetworkMonitor.isOnline(appContext)) {
                val dto = ApiRepository.pushQuote(
                    context = appContext,
                    quote = localRow,
                    serverRequestId = request?.serverId
                )
                if (dto != null) {
                    quoteDao.setServerId(localId.toInt(), dto.id)
                } else {
                    SyncManager.enqueue(
                        appContext, SyncManager.TYPE_QUOTE_CREATE, localId.toInt(), emptyMap()
                    )
                }
            } else {
                SyncManager.enqueue(
                    appContext, SyncManager.TYPE_QUOTE_CREATE, localId.toInt(), emptyMap()
                )
            }
            localId
        } catch (e: Exception) {
            Log.e(tag, "createQuote failed", e); 0L
        }

    suspend fun createQuoteForRequest(
        requestId: Int,
        technicianId: String,
        customerId: String,
        serviceName: String,
        serviceFee: Double,
        lineItems: List<Pair<String, Pair<Int, Double>>>,
        notes: String,
        discountType: String = "NONE",
        discountValue: Double = 0.0
    ): Long {
        val request = getRequestById(requestId) ?: return 0L

        val resolvedCustomerId = request.userId
            .ifBlank { customerId }
            .ifBlank { "" }

        val partsSubtotal = lineItems.sumOf { it.second.first * it.second.second }
        val subtotal = serviceFee + partsSubtotal

        // Compute discount using the same rules as EditQuotePricingScreen
        val normalizedType = discountType.uppercase(Locale.US)
        val discountAmount = when (normalizedType) {
            "PERCENTAGE" -> subtotal * (discountValue / 100.0)
            "FIXED" -> discountValue
            else -> 0.0
        }.coerceIn(0.0, subtotal)
        val finalTotal = (subtotal - discountAmount).coerceAtLeast(0.0)

        val partsDescription = lineItems.joinToString("\n") {
            "${it.first} x${it.second.first} - R${
                String.format(Locale.getDefault(), "%.2f", it.second.first * it.second.second)
            }"
        }

        val quote = Quote(
            requestId = requestId,
            technicianId = technicianId,
            customerId = resolvedCustomerId,
            buildingName = request.buildingName,
            issueType = request.issueType,
            description = request.description,
            scopeOfWork = "$serviceName\n\nParts:\n$partsDescription",
            partsRequired = partsDescription,
            laborCost = serviceFee,
            partsCost = partsSubtotal,
            totalCost = subtotal,
            grandTotal = subtotal,
            discountType = normalizedType,
            discountValue = discountValue,
            discountAmount = discountAmount,
            finalTotal = finalTotal,
            notes = notes
        )

        val quoteId = createQuote(quote)

        updateRequestStatus(requestId, RequestStatus.QUOTED)

        if (resolvedCustomerId.isNotBlank()) {
            val message = if (discountAmount > 0) {
                "Technician quoted R${String.format(Locale.getDefault(), "%.2f", finalTotal)} " +
                        "(after R${String.format(Locale.getDefault(), "%.2f", discountAmount)} discount) " +
                        "for ${request.buildingName}"
            } else {
                "Technician quoted R${String.format(Locale.getDefault(), "%.2f", finalTotal)} " +
                        "for ${request.buildingName}"
            }
            createNotification(
                userId = resolvedCustomerId,
                title = "New Quote Received",
                message = message,
                type = NotificationType.QUOTE
            )
        }
        return quoteId
    }

    fun getQuotesForCustomer(customerId: String): Flow<List<Quote>> =
        try { quoteDao.getQuotesByCustomer(customerId) }
        catch (e: Exception) { flow { emit(emptyList()) } }

    fun getQuotesVisibleToCustomer(customerId: String): Flow<List<Quote>> =
        try { quoteDao.getQuotesVisibleToCustomer(customerId) }
        catch (e: Exception) { flow { emit(emptyList()) } }

    fun getQuotesForTechnician(technicianId: String): Flow<List<Quote>> =
        try { quoteDao.getQuotesByTechnician(technicianId) }
        catch (e: Exception) { flow { emit(emptyList()) } }

    /**
     * Quotes that belong to a customer and are awaiting payment.
     * Used by the manager's "Awaiting Payment" section on the dashboard.
     */
    fun getAwaitingPaymentQuotesForCustomer(customerId: String): Flow<List<Quote>> =
        try {
            quoteDao.getQuotesVisibleToCustomer(customerId).map { list ->
                list.filter { it.status == QuoteStatus.AWAITING_PAYMENT }
            }
        } catch (e: Exception) {
            flow { emit(emptyList()) }
        }

    /**
     * Quotes belonging to this technician that have been PAID.
     * Used by the technician's "Payments Received" section.
     */
    fun getPaidQuotesForTechnician(technicianId: String): Flow<List<Quote>> =
        try {
            quoteDao.getQuotesByTechnician(technicianId).map { list ->
                list.filter { it.status == QuoteStatus.PAID }
            }
        } catch (e: Exception) {
            flow { emit(emptyList()) }
        }

    suspend fun getQuoteById(quoteId: Int): Quote? =
        try { quoteDao.getQuoteById(quoteId) } catch (e: Exception) { null }

    // QUOTE ACCEPT/DECLINE
    suspend fun updateQuoteStatus(quoteId: Int, status: QuoteStatus) {
        try {
            quoteDao.updateQuoteStatus(quoteId, status)
            val quote = quoteDao.getQuoteById(quoteId) ?: return

            val online = NetworkMonitor.isOnline(appContext)
            val sq = quote.serverId
            if (sq != null && online) {
                val ok = ApiRepository.updateQuoteStatus(appContext, sq, status.name)
                if (!ok) SyncManager.enqueue(
                    appContext, SyncManager.TYPE_QUOTE_STATUS, quoteId,
                    mapOf("status" to status.name)
                )
            } else {
                SyncManager.enqueue(
                    appContext, SyncManager.TYPE_QUOTE_STATUS, quoteId,
                    mapOf("status" to status.name)
                )
            }

            if (status == QuoteStatus.ACCEPTED) {
                createJobFromQuoteWithSchedule(quote, null, null)
            }
        } catch (e: Exception) { Log.e(tag, "updateQuoteStatus failed", e) }
    }

    suspend fun updateQuoteStatusWithSchedule(
        quoteId: Int,
        status: QuoteStatus,
        scheduledDate: Long? = null,
        timeSlot: String? = null
    ) {
        try {
            quoteDao.updateQuoteStatus(quoteId, status)
            val quote = quoteDao.getQuoteById(quoteId) ?: return

            val online = NetworkMonitor.isOnline(appContext)
            val sq = quote.serverId
            if (sq != null && online) {
                val ok = ApiRepository.updateQuoteStatus(appContext, sq, status.name)
                if (!ok) SyncManager.enqueue(
                    appContext, SyncManager.TYPE_QUOTE_STATUS, quoteId,
                    mapOf("status" to status.name)
                )
            } else {
                SyncManager.enqueue(
                    appContext, SyncManager.TYPE_QUOTE_STATUS, quoteId,
                    mapOf("status" to status.name)
                )
            }

            when (status) {
                QuoteStatus.ACCEPTED ->
                    createJobFromQuoteWithSchedule(quote, scheduledDate, timeSlot)

                QuoteStatus.DECLINED -> {
                    createNotification(
                        userId = quote.technicianId,
                        title = "Quote Declined",
                        message = "Your quote for ${quote.buildingName} was declined.",
                        type = NotificationType.QUOTE
                    )
                    updateRequestStatus(quote.requestId, RequestStatus.PENDING)
                }
                else -> {}
            }
        } catch (e: Exception) { Log.e(tag, "updateQuoteStatusWithSchedule failed", e) }
    }

    // ============================================================
    // QUOTE PRICING / DISCOUNTS
    // ============================================================

    suspend fun updateQuotePricing(
        quoteId: Int,
        discountType: String,
        discountValue: Double,
        notes: String
    ): Boolean {
        return try {
            val quote = quoteDao.getQuoteById(quoteId) ?: return false
            val originalTotal = quote.grandTotal
            val normalizedType = discountType.uppercase(Locale.US)

            val discountAmount = when (normalizedType) {
                "PERCENTAGE" -> originalTotal * (discountValue / 100.0)
                "FIXED" -> discountValue
                else -> 0.0
            }.coerceIn(0.0, originalTotal)

            val finalTotal = (originalTotal - discountAmount).coerceAtLeast(0.0)

            quoteDao.updateQuotePricing(
                quoteId = quoteId,
                grandTotal = originalTotal,
                discountType = normalizedType,
                discountValue = discountValue,
                discountAmount = discountAmount,
                finalTotal = finalTotal,
                notes = notes
            )

            if (NetworkMonitor.isOnline(appContext)) {
                val serverId = quote.serverId
                if (serverId != null) {
                    try {
                        ApiRepository.updateQuotePricing(
                            context = appContext,
                            serverId = serverId,
                            grandTotal = originalTotal,
                            discountType = normalizedType,
                            discountValue = discountValue,
                            discountAmount = discountAmount,
                            finalTotal = finalTotal,
                            notes = notes
                        )
                    } catch (e: Exception) {
                        Log.w(tag, "Server quote pricing sync failed (will retry later)", e)
                        SyncManager.enqueue(
                            appContext,
                            SyncManager.TYPE_QUOTE_PRICING,
                            quoteId,
                            mapOf(
                                "discountType" to normalizedType,
                                "discountValue" to discountValue,
                                "discountAmount" to discountAmount,
                                "finalTotal" to finalTotal,
                                "notes" to notes
                            )
                        )
                    }
                }
            } else {
                SyncManager.enqueue(
                    appContext,
                    SyncManager.TYPE_QUOTE_PRICING,
                    quoteId,
                    mapOf(
                        "discountType" to normalizedType,
                        "discountValue" to discountValue,
                        "discountAmount" to discountAmount,
                        "finalTotal" to finalTotal,
                        "notes" to notes
                    )
                )
            }

            true
        } catch (e: Exception) {
            Log.e(tag, "updateQuotePricing failed", e)
            false
        }
    }

    suspend fun markQuoteAsPaid(quoteId: Int): Boolean {
        return try {
            quoteDao.updateQuoteStatusAndTotal(quoteId, QuoteStatus.PAID)
            val quote = quoteDao.getQuoteById(quoteId) ?: return false

            createNotification(
                userId = quote.customerId,
                title = "Payment Received",
                message = "Payment of R${
                    String.format(Locale.US, "%.2f", quote.finalTotal)
                } for ${quote.buildingName} has been received. Receipt available.",
                type = NotificationType.QUOTE
            )
            createNotification(
                userId = quote.technicianId,
                title = "Payment Received",
                message = "Customer paid R${
                    String.format(Locale.US, "%.2f", quote.finalTotal)
                } for ${quote.buildingName}.",
                type = NotificationType.QUOTE
            )

            if (NetworkMonitor.isOnline(appContext)) {
                val serverId = quote.serverId
                if (serverId != null) {
                    try {
                        ApiRepository.updateQuoteStatus(
                            appContext, serverId, QuoteStatus.PAID.name
                        )
                    } catch (e: Exception) {
                        Log.w(tag, "markQuoteAsPaid server sync failed", e)
                        SyncManager.enqueue(
                            appContext, SyncManager.TYPE_QUOTE_STATUS, quoteId,
                            mapOf("status" to QuoteStatus.PAID.name)
                        )
                    }
                }
            } else {
                SyncManager.enqueue(
                    appContext, SyncManager.TYPE_QUOTE_STATUS, quoteId,
                    mapOf("status" to QuoteStatus.PAID.name)
                )
            }

            true
        } catch (e: Exception) {
            Log.e(tag, "markQuoteAsPaid failed", e)
            false
        }
    }

    // JOBS
    private suspend fun createJobFromQuoteWithSchedule(
        quote: Quote,
        scheduledDate: Long?,
        timeSlot: String?
    ) {
        try {
            if (quote.technicianId.isBlank()) {
                Log.e(tag, "Quote #${quote.id} has blank technicianId — aborting")
                return
            }

            val request = requestDao.getRequestById(quote.requestId)
            val jobAddress = request?.fullAddress.orEmpty()

            val job = Job(
                quoteId = quote.id,
                serverQuoteId = quote.serverId,
                requestId = quote.requestId,
                serverRequestId = request?.serverId,
                technicianId = quote.technicianId,
                customerId = quote.customerId,
                buildingName = quote.buildingName,
                issueType = quote.issueType,
                description = quote.description,
                status = JobStatus.SCHEDULED,
                scheduledDate = scheduledDate
                    ?: (System.currentTimeMillis() + 24L * 60 * 60 * 1000),
                notes = "Time Slot: ${timeSlot ?: "TBC"}",
                fullAddress = jobAddress
            )

            val localJobId = jobDao.insertJob(job)
            val localJob = jobDao.getJobById(localJobId.toInt()) ?: return

            val online = NetworkMonitor.isOnline(appContext)
            if (online) {
                val dto = ApiRepository.pushJob(
                    context = appContext,
                    job = localJob,
                    serverQuoteId = quote.serverId,
                    serverRequestId = request?.serverId
                )
                if (dto != null) {
                    jobDao.setServerId(localJobId.toInt(), dto.id)
                } else {
                    SyncManager.enqueue(
                        appContext, SyncManager.TYPE_JOB_CREATE, localJobId.toInt(), emptyMap()
                    )
                }
            } else {
                SyncManager.enqueue(
                    appContext, SyncManager.TYPE_JOB_CREATE, localJobId.toInt(), emptyMap()
                )
            }

            Log.d(tag, "Job created: roomId=$localJobId")

            updateRequestStatus(quote.requestId, RequestStatus.ACCEPTED)

            val dateStr = scheduledDate?.let { formatDate(it) } ?: "TBD"
            createNotification(
                userId = quote.technicianId,
                title = "Job Scheduled",
                message = "Quote #${quote.id} accepted for ${quote.buildingName} on $dateStr",
                type = NotificationType.JOB
            )
            createNotification(
                userId = quote.customerId,
                title = "Job Confirmed",
                message = "Job for ${quote.buildingName} scheduled for $dateStr",
                type = NotificationType.JOB
            )
        } catch (e: Exception) {
            Log.e(tag, "createJobFromQuoteWithSchedule failed", e)
        }
    }

    fun getJobsForTechnician(technicianId: String): Flow<List<Job>> =
        try { jobDao.getJobsByTechnician(technicianId) }
        catch (e: Exception) { flow { emit(emptyList()) } }

    fun getJobsForCustomer(customerId: String): Flow<List<Job>> =
        try { jobDao.getJobsByCustomer(customerId) }
        catch (e: Exception) { flow { emit(emptyList()) } }

    suspend fun updateJobStatus(jobId: Int, status: JobStatus) {
        try {
            jobDao.updateJobStatus(jobId, status)
            val job = jobDao.getJobById(jobId) ?: return
            val online = NetworkMonitor.isOnline(appContext)
            val sj = job.serverId
            if (sj != null && online) {
                val ok = ApiRepository.updateJobStatus(appContext, sj, status.name)
                if (!ok) SyncManager.enqueue(
                    appContext, SyncManager.TYPE_JOB_STATUS, jobId,
                    mapOf("status" to status.name)
                )
            } else {
                SyncManager.enqueue(
                    appContext, SyncManager.TYPE_JOB_STATUS, jobId,
                    mapOf("status" to status.name)
                )
            }
        } catch (e: Exception) { Log.e(tag, "updateJobStatus failed", e) }
    }

    suspend fun getJobById(jobId: Int): Job? =
        try { jobDao.getJobById(jobId) } catch (e: Exception) { null }

    /**
     * Finds the job that was created from this quote.
     * Used by the receipt viewer and the "Pay Now" flow to link
     * a quote back to its job.
     */
    suspend fun getJobForQuote(quoteId: Int): Job? =
        try { jobDao.getJobByLocalQuoteId(quoteId) }
        catch (e: Exception) { null }

    suspend fun resolveJobAddress(job: Job): String {
        if (job.fullAddress.isNotBlank()) return job.fullAddress.trim()

        val request = try { requestDao.getRequestById(job.requestId) } catch (e: Exception) { null }
        if (request != null) {
            if (request.fullAddress.isNotBlank()) return request.fullAddress.trim()
            val building = try { buildingDao.getBuildingById(request.buildingId) } catch (e: Exception) { null }
            if (building != null) {
                if (building.fullAddress.isNotBlank()) return building.fullAddress.trim()
                val composed = listOf(
                    building.address, building.suburb, building.city,
                    building.province, building.postalCode
                ).map { it.trim() }.filter { it.isNotEmpty() }.joinToString(", ")
                if (composed.isNotBlank()) return composed
            }
        }
        return ""
    }

    suspend fun setTechnicianOnWay(jobId: Int, onWay: Boolean) {
        try {
            jobDao.updateTechnicianOnWay(jobId, onWay)
            val job = jobDao.getJobById(jobId) ?: return
            val online = NetworkMonitor.isOnline(appContext)
            val sj = job.serverId
            if (sj != null && online) {
                val ok = ApiRepository.setJobOnWay(appContext, sj, onWay)
                if (!ok) SyncManager.enqueue(
                    appContext, SyncManager.TYPE_JOB_ON_WAY, jobId,
                    mapOf("onWay" to onWay)
                )
            } else {
                SyncManager.enqueue(
                    appContext, SyncManager.TYPE_JOB_ON_WAY, jobId,
                    mapOf("onWay" to onWay)
                )
            }

            if (onWay) {
                createNotification(
                    userId = job.customerId,
                    title = "Technician On The Way",
                    message = "Your technician is on the way to ${job.buildingName}.",
                    type = NotificationType.JOB
                )
            }
        } catch (e: Exception) { Log.e(tag, "setTechnicianOnWay failed", e) }
    }

    // NOTIFICATIONS
    private suspend fun createNotification(
        userId: String,
        title: String,
        message: String,
        type: NotificationType
    ) {
        if (userId.isBlank()) return
        try {
            notificationDao.insertNotification(
                Notification(
                    title = title,
                    message = message,
                    type = type,
                    userId = userId,
                    timestamp = System.currentTimeMillis()
                )
            )
        } catch (_: Exception) {}
    }

    private fun formatDate(timestamp: Long): String =
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))

    companion object {
        fun Factory(database: ArcticFlowDatabase, context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(QuoteViewModel::class.java)) {
                        return QuoteViewModel(database, context.applicationContext) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
}