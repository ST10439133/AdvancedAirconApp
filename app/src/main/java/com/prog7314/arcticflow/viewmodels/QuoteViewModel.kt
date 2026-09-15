package com.prog7314.arcticflow.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.prog7314.arcticflow.data.ArcticFlowDatabase
import com.prog7314.arcticflow.data.entities.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

class QuoteViewModel(
    private val database: ArcticFlowDatabase
) : ViewModel() {

    private val buildingDao = database.buildingDao()
    private val requestDao = database.serviceRequestDao()
    private val quoteDao = database.quoteDao()
    private val jobDao = database.jobDao()
    private val notificationDao = database.notificationDao()

    private val TAG = "QuoteViewModel"

    // ==================== BUILDINGS ====================
    suspend fun addBuilding(userId: String, building: BuildingEntity): Long =
        try { buildingDao.insertBuilding(building) } catch (e: Exception) { 0L }

    fun getBuildingsForUser(userId: String): Flow<List<BuildingEntity>> =
        try { buildingDao.getBuildingsByUser(userId) }
        catch (e: Exception) { flow { emit(emptyList()) } }

    suspend fun getBuildingById(buildingId: Int): BuildingEntity? =
        try { buildingDao.getBuildingById(buildingId) } catch (e: Exception) { null }

    // ==================== SERVICE REQUESTS ====================
    suspend fun createServiceRequest(request: ServiceRequest): Long =
        try { requestDao.insertRequest(request) } catch (e: Exception) { 0L }

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
        try { requestDao.updateRequestStatus(requestId, status) } catch (_: Exception) { }
    }

    suspend fun updateServiceRequest(request: ServiceRequest) {
        try { requestDao.updateRequestFull(request) } catch (_: Exception) { }
    }

    suspend fun deleteServiceRequest(request: ServiceRequest) {
        try { requestDao.deleteRequest(request) } catch (_: Exception) { }
    }

    // ==================== QUOTES ====================
    suspend fun createQuote(quote: Quote): Long =
        try { quoteDao.insertQuote(quote) } catch (e: Exception) { 0L }

    suspend fun createQuoteForRequest(
        requestId: Int,
        technicianId: String,
        serviceName: String,
        serviceFee: Double,
        lineItems: List<Pair<String, Pair<Int, Double>>>,
        notes: String
    ): Long {
        val request = getRequestById(requestId) ?: return 0L
        val partsSubtotal = lineItems.sumOf { it.second.first * it.second.second }
        val total = serviceFee + partsSubtotal
        val partsDescription = lineItems.joinToString("\n") {
            "${it.first} x${it.second.first} - R${String.format("%.2f", it.second.first * it.second.second)}"
        }

        val quote = Quote(
            requestId = requestId,
            technicianId = technicianId,
            customerId = request.userId,
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
        createNotification(
            userId = request.userId,
            title = "New Quote Received",
            message = "Technician quoted R${String.format("%.2f", total)} for ${request.buildingName}",
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
            if (status == QuoteStatus.ACCEPTED) {
                val quote = quoteDao.getQuoteById(quoteId)
                quote?.let { createJobFromQuoteWithSchedule(it, null, null) }
            }
        } catch (e: Exception) { e.printStackTrace() }
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
                }
                else -> { }
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateQuoteStatusWithSchedule failed", e)
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
                Log.e(TAG, "Quote #${quote.id} has blank technicianId — aborting job creation")
                return
            }

            // Look up the address from the original service request.
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

            val newJobId = jobDao.insertJob(job)
            Log.d(
                TAG,
                "Job created: roomId=$newJobId tech=${quote.technicianId} " +
                        "cust=${quote.customerId} addr='$jobAddress'"
            )

            requestDao.updateRequestStatus(quote.requestId, RequestStatus.ACCEPTED)

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
            Log.e(TAG, "createJobFromQuoteWithSchedule failed", e)
        }
    }

    fun getJobsForTechnician(technicianId: String): Flow<List<Job>> =
        try { jobDao.getJobsByTechnician(technicianId) }
        catch (e: Exception) { flow { emit(emptyList()) } }

    fun getJobsForCustomer(customerId: String): Flow<List<Job>> =
        try { jobDao.getJobsByCustomer(customerId) }
        catch (e: Exception) { flow { emit(emptyList()) } }

    suspend fun updateJobStatus(jobId: Int, status: JobStatus) {
        try { jobDao.updateJobStatus(jobId, status) } catch (_: Exception) { }
    }

    suspend fun getJobById(jobId: Int): Job? =
        try { jobDao.getJobById(jobId) } catch (e: Exception) { null }

    /**
     * Resolves the best available address for a job by walking the chain:
     *
     *   1. Job.fullAddress            (fastest, if populated)
     *   2. ServiceRequest.fullAddress (via job.requestId)
     *   3. Building's fullAddress     (via request.buildingId)
     *   4. Compose from Building's individual fields (address/suburb/city/province/postal)
     *   5. Legacy Building.address + Building.city + Building.postalCode
     *
     * Returns an empty string only if truly nothing is available.
     */
    suspend fun resolveJobAddress(job: Job): String {
        // 1. Direct hit on the job
        if (job.fullAddress.isNotBlank()) return job.fullAddress.trim()

        // 2. Look up the service request
        val request = try {
            requestDao.getRequestById(job.requestId)
        } catch (e: Exception) {
            Log.w(TAG, "resolveJobAddress: request lookup failed", e)
            null
        }

        if (request != null) {
            if (request.fullAddress.isNotBlank()) return request.fullAddress.trim()

            // 3. Look up the building
            val building = try {
                buildingDao.getBuildingById(request.buildingId)
            } catch (e: Exception) {
                Log.w(TAG, "resolveJobAddress: building lookup failed", e)
                null
            }

            if (building != null) {
                // 4. Cached building fullAddress
                if (building.fullAddress.isNotBlank()) return building.fullAddress.trim()

                // 4b. Compose from individual building fields
                val composed = listOf(
                    building.address,
                    building.suburb,
                    building.city,
                    building.province,
                    building.postalCode
                )
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString(", ")

                if (composed.isNotBlank()) return composed

                // 5. Legacy fallback
                val legacy = listOf(building.address, building.city, building.postalCode)
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString(", ")

                if (legacy.isNotBlank()) return legacy
            }
        }

        return ""
    }

    suspend fun setTechnicianOnWay(jobId: Int, onWay: Boolean) {
        try {
            jobDao.updateTechnicianOnWay(jobId, onWay)
            val job = jobDao.getJobById(jobId) ?: return
            if (onWay) {
                createNotification(
                    userId = job.customerId,
                    title = "Technician On The Way",
                    message = "Your technician is on the way to ${job.buildingName}.",
                    type = NotificationType.JOB
                )
            }
        } catch (_: Exception) { }
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
        fun Factory(database: ArcticFlowDatabase): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(QuoteViewModel::class.java)) {
                        return QuoteViewModel(database) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
}