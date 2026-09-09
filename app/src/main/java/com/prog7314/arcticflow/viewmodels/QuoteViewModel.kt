package com.prog7314.arcticflow.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prog7314.arcticflow.data.ArcticFlowDatabase
import com.prog7314.arcticflow.data.entities.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class QuoteViewModel(
    private val database: ArcticFlowDatabase
) : ViewModel() {

    private val buildingDao = database.buildingDao()
    private val requestDao = database.serviceRequestDao()
    private val quoteDao = database.quoteDao()
    private val jobDao = database.jobDao()

    suspend fun addBuilding(userId: String, building: BuildingEntity): Long {
        return buildingDao.insertBuilding(building)
    }

    fun getBuildingsForUser(userId: String): Flow<List<BuildingEntity>> {
        return buildingDao.getBuildingsByUser(userId)
    }

    suspend fun getBuildingById(buildingId: Int): BuildingEntity? {
        return buildingDao.getBuildingById(buildingId)
    }

    suspend fun createServiceRequest(request: ServiceRequest): Long {
        return requestDao.insertRequest(request)
    }

    fun getRequestsForUser(userId: String): Flow<List<ServiceRequest>> {
        return requestDao.getRequestsByUser(userId)
    }

    suspend fun getRequestById(requestId: Int): ServiceRequest? {
        return requestDao.getRequestById(requestId)
    }

    suspend fun updateRequestStatus(requestId: Int, status: RequestStatus) {
        requestDao.updateRequestStatus(requestId, status)
    }

    suspend fun createQuote(quote: Quote): Long {
        return quoteDao.insertQuote(quote)
    }

    fun getQuotesForCustomer(customerId: String): Flow<List<Quote>> {
        return quoteDao.getQuotesByCustomer(customerId)
    }

    fun getQuotesForTechnician(technicianId: String): Flow<List<Quote>> {
        return quoteDao.getQuotesByTechnician(technicianId)
    }

    suspend fun getQuoteById(quoteId: Int): Quote? {
        return quoteDao.getQuoteById(quoteId)
    }

    suspend fun updateQuoteStatus(quoteId: Int, status: QuoteStatus) {
        quoteDao.updateQuoteStatus(quoteId, status)
        if (status == QuoteStatus.ACCEPTED) {
            val quote = quoteDao.getQuoteById(quoteId)
            quote?.let { createJobFromQuote(it) }
        }
    }

    private suspend fun createJobFromQuote(quote: Quote) {
        val job = Job(
            quoteId = quote.id,
            requestId = quote.requestId,
            technicianId = quote.technicianId,
            customerId = quote.customerId,
            buildingName = quote.buildingName,
            issueType = quote.issueType,
            description = quote.description,
            status = JobStatus.PENDING, // Changed from SCHEDULED to PENDING
            scheduledDate = System.currentTimeMillis() + 24 * 60 * 60 * 1000
        )
        jobDao.insertJob(job)
        requestDao.updateRequestStatus(quote.requestId, RequestStatus.ACCEPTED)
    }

    fun getJobsForTechnician(technicianId: String): Flow<List<Job>> {
        return jobDao.getJobsByTechnician(technicianId)
    }

    fun getJobsForCustomer(customerId: String): Flow<List<Job>> {
        return jobDao.getJobsByCustomer(customerId)
    }

    suspend fun updateJobStatus(jobId: Int, status: JobStatus) {
        jobDao.updateJobStatus(jobId, status)
    }

    suspend fun getJobById(jobId: Int): Job? {
        return jobDao.getJobById(jobId)
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