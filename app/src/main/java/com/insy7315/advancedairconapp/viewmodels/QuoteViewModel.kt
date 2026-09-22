package com.insy7315.advancedairconapp.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.insy7315.advancedairconapp.data.ArcticFlowDatabase
import com.insy7315.advancedairconapp.data.api.ApiRepository
import com.insy7315.advancedairconapp.data.api.IdMap
import com.insy7315.advancedairconapp.data.entities.*
import com.insy7315.advancedairconapp.data.network.NetworkMonitor
import com.insy7315.advancedairconapp.data.sync.SyncManager
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.insy7315.advancedairconapp.data.sync.DataSyncManager

class QuoteViewModel(
    private val database: ArcticFlowDatabase,
    private val appContext: Context
) : ViewModel() {

    private val buildingDao = database.buildingDao()
    private val requestDao = database.serviceRequestDao()
    private val quoteDao = database.quoteDao()
    private val jobDao = database.jobDao()
    private val notificationDao = database.notificationDao()

    /**
     * Triggers a background pull from the API. Room's Flow will
     * automatically emit the new data to any collector.
     */
    suspend fun refreshFromServer(userRole: String) {
        try {
            DataSyncManager.syncEverything(appContext, userRole)
        } catch (e: Exception) {
            Log.e(tag, "refreshFromServer failed", e)
        }
    }
    private val tag = "QuoteViewModel"

    // ==================== BUILDINGS ====================
    suspend fun addBuilding(userId: String, building: BuildingEntity): Long =
        try {
            val localId = buildingDao.insertBuilding(building)

            if (NetworkMonitor.isOnline(appContext)) {
                val dto = ApiRepository.pushBuilding(
                    appContext, building.copy(id = localId.toInt())
                )
                if (dto != null) {
                    IdMap.putBuilding(appContext, localId.toInt(), dto.id)
                }
            }

            localId
        } catch (e: Exception) {
            Log.e(tag, "addBuilding failed", e)
            0L
        }

    fun getBuildingsForUser(userId: String): Flow<List<BuildingEntity>> =
        try { buildingDao.getBuildingsByUser(userId) }
        catch (e: Exception) { flow { emit(emptyList()) } }

    suspend fun getBuildingById(buildingId: Int): BuildingEntity? =
        try { buildingDao.getBuildingById(buildingId) } catch (e: Exception) { null }

    // ==================== SERVICE REQUESTS ====================
    suspend fun createServiceRequest(request: ServiceRequest): Long =
        try {
            val localId = requestDao.insertRequest(request)
            val building = try { buildingDao.getBuildingById(request.buildingId) } catch (_: Exception) { null }
            val serverBuildingId = IdMap.getBuilding(appContext, request.buildingId)

            if (NetworkMonitor.isOnline(appContext)) {
                val dto = ApiRepository.pushServiceRequest(
                    context = appContext,
                    request = request.copy(id = localId.toInt()),
                    buildingName = building?.name,
                    fullAddress = building?.fullAddress,
                    serverBuildingId = serverBuildingId
                )
                if (dto != null) {
                    IdMap.putRequest(appContext, localId.toInt(), dto.id)
                } else {
                    SyncManager.enqueue(
                        appContext, SyncManager.TYPE_REQUEST_CREATE, localId.toInt(),
                        mapOf("buildingId" to request.buildingId)
                    )
                }
            } else {
                SyncManager.enqueue(
                    appContext, SyncManager.TYPE_REQUEST_CREATE, localId.toInt(),
                    mapOf("buildingId" to request.buildingId)
                )
            }

            localId
        } catch (e: Exception) {
            Log.e(tag, "createServiceRequest failed", e)
            0L
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

            if (NetworkMonitor.isOnline(appContext)) {
                val ok = ApiRepository.updateRequestStatus(appContext, requestId, status.name)
                if (!ok) {
                    SyncManager.enqueue(
                        appContext, SyncManager.TYPE_REQUEST_STATUS, requestId,
                        mapOf("status" to status.name)
                    )
                }
            } else {
                SyncManager.enqueue(
                    appContext, SyncManager.TYPE_REQUEST_STATUS, requestId,
                    mapOf("status" to status.name)
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "updateRequestStatus failed", e)
        }
    }

    suspend fun updateServiceRequest(request: ServiceRequest) {
        try { requestDao.updateRequestFull(request) } catch (_: Exception) { }
    }

    suspend fun deleteServiceRequest(request: ServiceRequest) {
        try { requestDao.deleteRequest(request) } catch (_: Exception) { }
    }

    // ==================== QUOTES ====================
    suspend fun createQuote(quote: Quote): Long =
        try {
            val localId = quoteDao.insertQuote(quote)
            val serverRequestId = IdMap.getRequest(appContext, quote.requestId)

            if (NetworkMonitor.isOnline(appContext)) {
                val dto = ApiRepository.pushQuote(
                    context = appContext,
                    quote = quote.copy(id = localId.toInt()),
                    serverRequestId = serverRequestId
                )
                if (dto != null) {
                    IdMap.putQuote(appContext, localId.toInt(), dto.id)
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
            Log.e(tag, "createQuote failed", e)
            0L
        }

    suspend fun createQuoteForRequest(
        requestId: Int,
        technicianId: String,
        customerId: String,
        serviceName: String,
        serviceFee: Double,
        lineItems: List<Pair<String, Pair<Int, Double>>>,
        notes: String
    ): Long {
        val request = getRequestById(requestId) ?: return 0L

        val partsSubtotal = lineItems.sumOf { it.second.first * it.second.second }
        val total = serviceFee + partsSubtotal
        val partsDescription = lineItems.joinToString("\n") {
            "${it.first} x${it.second.first} - R${String.format(Locale.getDefault(), "%.2f", it.second.first * it.second.second)}"
        }

        val quote = Quote(
            requestId = requestId,
            technicianId = technicianId,
            customerId = customerId.ifBlank { request.userId },
            buildingName = request.buildingName,
            issueType = request.issueType,
            description = request.description,
            scopeOfWork = "$serviceName\n\nParts:\n$partsDescription",
            partsRequired = partsDescription,
            laborCost = serviceFee,
            partsCost = partsSubtotal,
            totalCost = total,
            grandTotal = total,
            notes = notes
        )

        val quoteId = createQuote(quote)

        requestDao.updateRequestStatus(requestId, RequestStatus.QUOTED)
        if (NetworkMonitor.isOnline(appContext)) {
            val ok = ApiRepository.updateRequestStatus(
                appContext, requestId, RequestStatus.QUOTED.name
            )
            if (!ok) {
                SyncManager.enqueue(
                    appContext, SyncManager.TYPE_REQUEST_STATUS, requestId,
                    mapOf("status" to RequestStatus.QUOTED.name)
                )
            }
        } else {
            SyncManager.enqueue(
                appContext, SyncManager.TYPE_REQUEST_STATUS, requestId,
                mapOf("status" to RequestStatus.QUOTED.name)
            )
        }

        createNotification(
            userId = quote.customerId,
            title = "New Quote Received",
            message = "Technician quoted R${String.format(Locale.getDefault(), "%.2f", total)} for ${request.buildingName}",
            type = NotificationType.QUOTE
        )
        return quoteId
    }

    fun getQuotesForCustomer(customerId: String): Flow<List<Quote>> =
        try { quoteDao.getQuotesByCustomer(customerId) }
        catch (e: Exception) { flow { emit(emptyList()) } }

    fun getQuotesForTechnician(technicianId: String): Flow<List<Quote>> =
        try { quoteDao.getQuotesByTechnician(technicianId) }
        catch (e: Exception) { flow { emit(emptyList()) } }

    suspend fun getQuoteById(quoteId: Int): Quote? =
        try { quoteDao.getQuoteById(quoteId) } catch (e: Exception) { null }

    // ==================== QUOTE ACCEPT / DECLINE ====================
    suspend fun updateQuoteStatus(quoteId: Int, status: QuoteStatus) {
        try {
            quoteDao.updateQuoteStatus(quoteId, status)

            if (NetworkMonitor.isOnline(appContext)) {
                val ok = ApiRepository.updateQuoteStatus(appContext, quoteId, status.name)
                if (!ok) {
                    SyncManager.enqueue(
                        appContext, SyncManager.TYPE_QUOTE_STATUS, quoteId,
                        mapOf("status" to status.name)
                    )
                }
            } else {
                SyncManager.enqueue(
                    appContext, SyncManager.TYPE_QUOTE_STATUS, quoteId,
                    mapOf("status" to status.name)
                )
            }

            if (status == QuoteStatus.ACCEPTED) {
                val quote = quoteDao.getQuoteById(quoteId)
                quote?.let { createJobFromQuoteWithSchedule(it, null, null) }
            }
        } catch (e: Exception) {
            Log.e(tag, "updateQuoteStatus failed", e)
        }
    }

    suspend fun updateQuoteStatusWithSchedule(
        quoteId: Int,
        status: QuoteStatus,
        scheduledDate: Long? = null,
        timeSlot: String? = null
    ) {
        try {
            quoteDao.updateQuoteStatus(quoteId, status)

            if (NetworkMonitor.isOnline(appContext)) {
                val ok = ApiRepository.updateQuoteStatus(appContext, quoteId, status.name)
                if (!ok) {
                    SyncManager.enqueue(
                        appContext, SyncManager.TYPE_QUOTE_STATUS, quoteId,
                        mapOf("status" to status.name)
                    )
                }
            } else {
                SyncManager.enqueue(
                    appContext, SyncManager.TYPE_QUOTE_STATUS, quoteId,
                    mapOf("status" to status.name)
                )
            }

            val quote = quoteDao.getQuoteById(quoteId) ?: return

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
                    requestDao.updateRequestStatus(quote.requestId, RequestStatus.PENDING)

                    if (NetworkMonitor.isOnline(appContext)) {
                        val ok = ApiRepository.updateRequestStatus(
                            appContext, quote.requestId, RequestStatus.PENDING.name
                        )
                        if (!ok) {
                            SyncManager.enqueue(
                                appContext, SyncManager.TYPE_REQUEST_STATUS, quote.requestId,
                                mapOf("status" to RequestStatus.PENDING.name)
                            )
                        }
                    } else {
                        SyncManager.enqueue(
                            appContext, SyncManager.TYPE_REQUEST_STATUS, quote.requestId,
                            mapOf("status" to RequestStatus.PENDING.name)
                        )
                    }
                }
                else -> { }
            }
        } catch (e: Exception) {
            Log.e(tag, "updateQuoteStatusWithSchedule failed", e)
        }
    }

    // ==================== JOBS ====================
    private suspend fun createJobFromQuoteWithSchedule(
        quote: Quote,
        scheduledDate: Long?,
        timeSlot: String?
    ) {
        try {
            if (quote.technicianId.isBlank()) {
                Log.e(tag, "Quote #${quote.id} has blank technicianId — aborting job creation")
                return
            }

            val request = requestDao.getRequestById(quote.requestId)
            val jobAddress = request?.fullAddress.orEmpty()

            val job = Job(
                quoteId = quote.id,
                requestId = quote.requestId,
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

            if (NetworkMonitor.isOnline(appContext)) {
                val serverQuoteId   = IdMap.getQuote(appContext, quote.id)
                val serverRequestId = IdMap.getRequest(appContext, quote.requestId)

                val dto = ApiRepository.pushJob(
                    context = appContext,
                    job = job.copy(id = localJobId.toInt()),
                    serverQuoteId = serverQuoteId,
                    serverRequestId = serverRequestId
                )
                if (dto != null) {
                    IdMap.putJob(appContext, localJobId.toInt(), dto.id)
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

            requestDao.updateRequestStatus(quote.requestId, RequestStatus.ACCEPTED)
            if (NetworkMonitor.isOnline(appContext)) {
                val ok = ApiRepository.updateRequestStatus(
                    appContext, quote.requestId, RequestStatus.ACCEPTED.name
                )
                if (!ok) {
                    SyncManager.enqueue(
                        appContext, SyncManager.TYPE_REQUEST_STATUS, quote.requestId,
                        mapOf("status" to RequestStatus.ACCEPTED.name)
                    )
                }
            } else {
                SyncManager.enqueue(
                    appContext, SyncManager.TYPE_REQUEST_STATUS, quote.requestId,
                    mapOf("status" to RequestStatus.ACCEPTED.name)
                )
            }

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

            if (NetworkMonitor.isOnline(appContext)) {
                val ok = ApiRepository.updateJobStatus(appContext, jobId, status.name)
                if (!ok) {
                    SyncManager.enqueue(
                        appContext, SyncManager.TYPE_JOB_STATUS, jobId,
                        mapOf("status" to status.name)
                    )
                }
            } else {
                SyncManager.enqueue(
                    appContext, SyncManager.TYPE_JOB_STATUS, jobId,
                    mapOf("status" to status.name)
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "updateJobStatus failed", e)
        }
    }

    suspend fun getJobById(jobId: Int): Job? =
        try { jobDao.getJobById(jobId) } catch (e: Exception) { null }

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

            if (NetworkMonitor.isOnline(appContext)) {
                val ok = ApiRepository.setJobOnWay(appContext, jobId, onWay)
                if (!ok) {
                    SyncManager.enqueue(
                        appContext, SyncManager.TYPE_JOB_ON_WAY, jobId,
                        mapOf("onWay" to onWay)
                    )
                }
            } else {
                SyncManager.enqueue(
                    appContext, SyncManager.TYPE_JOB_ON_WAY, jobId,
                    mapOf("onWay" to onWay)
                )
            }

            val job = jobDao.getJobById(jobId) ?: return
            if (onWay) {
                createNotification(
                    userId = job.customerId,
                    title = "Technician On The Way",
                    message = "Your technician is on the way to ${job.buildingName}.",
                    type = NotificationType.JOB
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "setTechnicianOnWay failed", e)
        }
    }

    // ==================== NOTIFICATIONS ====================
    private suspend fun createNotification(
        userId: String,
        title: String,
        message: String,
        type: NotificationType
    ) {
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
        } catch (_: Exception) { }
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