// app/src/main/java/com/prog7314/arcticflow/viewmodels/QuoteViewModel.kt
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

    // ============ BUILDING OPERATIONS ============
    suspend fun addBuilding(userId: String, building: BuildingEntity): Long {
        return try {
            buildingDao.insertBuilding(building)
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    fun getBuildingsForUser(userId: String): Flow<List<BuildingEntity>> {
        return try {
            buildingDao.getBuildingsByUser(userId)
        } catch (e: Exception) {
            e.printStackTrace()
            flow { emit(emptyList()) }
        }
    }

    suspend fun getBuildingById(buildingId: Int): BuildingEntity? {
        return try {
            buildingDao.getBuildingById(buildingId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ============ SERVICE REQUEST OPERATIONS ============
    suspend fun createServiceRequest(request: ServiceRequest): Long {
        Log.d(TAG, "Creating service request: $request")
        return try {
            val id = requestDao.insertRequest(request)
            Log.d(TAG, "Service request created with ID: $id")
            id
        } catch (e: Exception) {
            Log.e(TAG, "Error creating service request", e)
            e.printStackTrace()
            0L
        }
    }

    fun getRequestsForUser(userId: String): Flow<List<ServiceRequest>> {
        return try {
            requestDao.getRequestsByUser(userId)
        } catch (e: Exception) {
            e.printStackTrace()
            flow { emit(emptyList()) }
        }
    }

    // Get ALL pending service requests (for technicians)
    fun getPendingServiceRequests(): Flow<List<ServiceRequest>> {
        Log.d(TAG, "Getting all pending service requests")
        return try {
            requestDao.getPendingRequests()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting pending requests", e)
            e.printStackTrace()
            flow { emit(emptyList()) }
        }
    }

    fun getRequestsByStatus(status: RequestStatus): Flow<List<ServiceRequest>> {
        return try {
            requestDao.getRequestsByStatus(status)
        } catch (e: Exception) {
            e.printStackTrace()
            flow { emit(emptyList()) }
        }
    }

    suspend fun getRequestById(requestId: Int): ServiceRequest? {
        return try {
            requestDao.getRequestById(requestId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateRequestStatus(requestId: Int, status: RequestStatus) {
        try {
            requestDao.updateRequestStatus(requestId, status)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateServiceRequest(request: ServiceRequest) {
        requestDao.updateRequestFull(request)
    }

    suspend fun deleteServiceRequest(request: ServiceRequest) {
        requestDao.deleteRequest(request)
    }


    // ============ QUOTE OPERATIONS ============
    suspend fun createQuote(quote: Quote): Long {
        Log.d(TAG, "Creating quote: $quote")
        return try {
            val id = quoteDao.insertQuote(quote)
            Log.d(TAG, "Quote created with ID: $id")
            id
        } catch (e: Exception) {
            Log.e(TAG, "Error creating quote", e)
            e.printStackTrace()
            0L
        }
    }

    // ===== NEW: Build quote from a service request (Technician auto-assigned) =====
    suspend fun createQuoteForRequest(
        requestId: Int,
        technicianId: String,
        customerId: String,
        serviceName: String,
        serviceFee: Double,
        lineItems: List<Pair<String, Pair<Int, Double>>>, // name -> (qty, price)
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
            technicianId = technicianId,          // technician auto-bound
            customerId = request.userId,          // customer auto-bound from the request
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
        // Mark request as QUOTED so it disappears from the pending list
        requestDao.updateRequestStatus(requestId, RequestStatus.QUOTED)
        return quoteId
    }

    fun getQuotesForCustomer(customerId: String): Flow<List<Quote>> {
        return try {
            quoteDao.getQuotesByCustomer(customerId)
        } catch (e: Exception) {
            e.printStackTrace()
            flow { emit(emptyList()) }
        }
    }

    fun getQuotesForTechnician(technicianId: String): Flow<List<Quote>> {
        return try {
            quoteDao.getQuotesByTechnician(technicianId)
        } catch (e: Exception) {
            e.printStackTrace()
            flow { emit(emptyList()) }
        }
    }

    suspend fun getQuoteById(quoteId: Int): Quote? {
        return try {
            quoteDao.getQuoteById(quoteId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateQuoteStatus(quoteId: Int, status: QuoteStatus) {
        try {
            quoteDao.updateQuoteStatus(quoteId, status)
            if (status == QuoteStatus.ACCEPTED) {
                val quote = quoteDao.getQuoteById(quoteId)
                quote?.let { createJobFromQuote(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
            if (status == QuoteStatus.ACCEPTED) {
                val quote = quoteDao.getQuoteById(quoteId)
                quote?.let {
                    createJobFromQuoteWithSchedule(it, scheduledDate, timeSlot)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ============ JOB OPERATIONS ============
    private suspend fun createJobFromQuote(quote: Quote) {
        try {
            val job = Job(
                quoteId = quote.id,
                requestId = quote.requestId,
                technicianId = quote.technicianId,   // Auto-assigned technician
                customerId = quote.customerId,
                buildingName = quote.buildingName,
                issueType = quote.issueType,
                description = quote.description,
                status = JobStatus.PENDING,
                scheduledDate = System.currentTimeMillis() + 24 * 60 * 60 * 1000
            )
            jobDao.insertJob(job)
            requestDao.updateRequestStatus(quote.requestId, RequestStatus.ACCEPTED)

            createNotification(
                userId = quote.technicianId,
                title = "New Job Created",
                message = "Quote #${quote.id} has been accepted for ${quote.buildingName}",
                type = NotificationType.JOB
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun createJobFromQuoteWithSchedule(
        quote: Quote,
        scheduledDate: Long?,
        timeSlot: String?
    ) {
        try {
            val job = Job(
                quoteId = quote.id,
                requestId = quote.requestId,
                technicianId = quote.technicianId,   // Auto-assigned technician
                customerId = quote.customerId,
                buildingName = quote.buildingName,
                issueType = quote.issueType,
                description = quote.description,
                status = JobStatus.SCHEDULED,
                scheduledDate = scheduledDate ?: System.currentTimeMillis() + 24 * 60 * 60 * 1000,
                notes = "Time Slot: ${timeSlot ?: "To be confirmed"}"
            )
            jobDao.insertJob(job)
            requestDao.updateRequestStatus(quote.requestId, RequestStatus.ACCEPTED)

            val dateStr = scheduledDate?.let { formatDate(it) } ?: "TBD"
            createNotification(
                userId = quote.technicianId,
                title = "New Job Scheduled",
                message = "Quote #${quote.id} accepted for ${quote.buildingName} on $dateStr at ${timeSlot ?: "TBD"}",
                type = NotificationType.JOB
            )

            createNotification(
                userId = quote.customerId,
                title = "Job Scheduled",
                message = "Your job for ${quote.buildingName} has been scheduled for $dateStr at ${timeSlot ?: "TBD"}",
                type = NotificationType.JOB
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getJobsForTechnician(technicianId: String): Flow<List<Job>> {
        return try {
            jobDao.getJobsByTechnician(technicianId)
        } catch (e: Exception) {
            e.printStackTrace()
            flow { emit(emptyList()) }
        }
    }

    fun getJobsForCustomer(customerId: String): Flow<List<Job>> {
        return try {
            jobDao.getJobsByCustomer(customerId)
        } catch (e: Exception) {
            e.printStackTrace()
            flow { emit(emptyList()) }
        }
    }

    suspend fun updateJobStatus(jobId: Int, status: JobStatus) {
        try {
            jobDao.updateJobStatus(jobId, status)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getJobById(jobId: Int): Job? {
        return try {
            jobDao.getJobById(jobId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ============ NOTIFICATION OPERATIONS ============
    private suspend fun createNotification(
        userId: String,
        title: String,
        message: String,
        type: NotificationType
    ) {
        try {
            val notification = Notification(
                title = title,
                message = message,
                type = type,
                userId = userId,
                timestamp = System.currentTimeMillis()
            )
            notificationDao.insertNotification(notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun formatDate(timestamp: Long): String {
        val format = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return format.format(Date(timestamp))
    }

    companion object {
        fun Factory(database: ArcticFlowDatabase): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
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
}