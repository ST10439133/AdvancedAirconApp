package com.insy7315.advancedairconapp.data.sync

import android.content.Context
import android.util.Log
import com.insy7315.advancedairconapp.data.ArcticFlowDatabase
import com.insy7315.advancedairconapp.data.api.ApiClient
import com.insy7315.advancedairconapp.data.api.BuildingDto
import com.insy7315.advancedairconapp.data.api.JobDto
import com.insy7315.advancedairconapp.data.api.QuoteDto
import com.insy7315.advancedairconapp.data.api.ServiceRequestDto
import com.insy7315.advancedairconapp.data.api.safeApiCall
import com.insy7315.advancedairconapp.data.entities.BuildingEntity
import com.insy7315.advancedairconapp.data.entities.BuildingStatusEnum
import com.insy7315.advancedairconapp.data.entities.BuildingType
import com.insy7315.advancedairconapp.data.entities.Job
import com.insy7315.advancedairconapp.data.entities.JobStatus
import com.insy7315.advancedairconapp.data.entities.Quote
import com.insy7315.advancedairconapp.data.entities.QuoteStatus
import com.insy7315.advancedairconapp.data.entities.RequestPriority
import com.insy7315.advancedairconapp.data.entities.RequestStatus
import com.insy7315.advancedairconapp.data.entities.ServiceRequest
import com.insy7315.advancedairconapp.data.network.NetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pulls data from the REST API and mirrors it into Room.
 *
 * This is the PULL half of the sync loop. The SyncManager handles PUSH
 * (uploading local writes to the server). DataSyncManager handles PULL
 * (downloading remote writes to the local DB).
 *
 * Once downloaded, Room's Flow automatically updates every UI observing
 * the corresponding DAO. So a manager's new request becomes visible on
 * the technician's phone within seconds of opening the Requests tab.
 */
object DataSyncManager {

    private const val TAG = "DataSyncManager"

    // ============================================================
    // BUILDINGS
    // ============================================================
    suspend fun syncBuildings(context: Context, userId: String): Int =
        withContext(Dispatchers.IO) {
            if (!NetworkMonitor.isOnline(context)) return@withContext 0

            val api = ApiClient.get(context)
            val dtos: List<BuildingDto> = safeApiCall(TAG) {
                api.getBuildings()
            } ?: return@withContext 0

            val db = ArcticFlowDatabase.getDatabase(context)
            var inserted = 0

            for (dto in dtos) {
                val existing = db.buildingDao().getBuildingById(dto.id)
                if (existing == null) {
                    db.buildingDao().insertBuilding(dto.toEntity(userId))
                    inserted++
                }
            }
            Log.d(TAG, "syncBuildings: $inserted new building(s) from server")
            inserted
        }

    // ============================================================
    // SERVICE REQUESTS
    // ============================================================
    suspend fun syncPendingRequests(context: Context): Int =
        withContext(Dispatchers.IO) {
            if (!NetworkMonitor.isOnline(context)) return@withContext 0

            val api = ApiClient.get(context)
            val dtos: List<ServiceRequestDto> = safeApiCall(TAG) {
                api.getPendingRequests()
            } ?: return@withContext 0

            val db = ArcticFlowDatabase.getDatabase(context)
            var inserted = 0

            for (dto in dtos) {
                val existing = db.serviceRequestDao().getRequestById(dto.id)
                if (existing == null) {
                    db.serviceRequestDao().insertRequest(dto.toEntity())
                    inserted++
                }
            }
            Log.d(TAG, "syncPendingRequests: $inserted new request(s) from server")
            inserted
        }

    suspend fun syncMyRequests(context: Context): Int =
        withContext(Dispatchers.IO) {
            if (!NetworkMonitor.isOnline(context)) return@withContext 0

            val api = ApiClient.get(context)
            val dtos: List<ServiceRequestDto> = safeApiCall(TAG) {
                api.getMyRequests()
            } ?: return@withContext 0

            val db = ArcticFlowDatabase.getDatabase(context)
            var updated = 0

            for (dto in dtos) {
                val existing = db.serviceRequestDao().getRequestById(dto.id)
                if (existing == null) {
                    db.serviceRequestDao().insertRequest(dto.toEntity())
                    updated++
                } else if (existing.status != dto.status.toRequestStatus()) {
                    db.serviceRequestDao().updateRequestStatus(
                        dto.id, dto.status.toRequestStatus()
                    )
                    updated++
                }
            }
            Log.d(TAG, "syncMyRequests: $updated updated/new request(s)")
            updated
        }

    // ============================================================
    // QUOTES
    // ============================================================
    suspend fun syncCustomerQuotes(context: Context): Int =
        withContext(Dispatchers.IO) {
            if (!NetworkMonitor.isOnline(context)) return@withContext 0

            val api = ApiClient.get(context)
            val dtos: List<QuoteDto> = safeApiCall(TAG) {
                api.getCustomerQuotes()
            } ?: return@withContext 0

            val db = ArcticFlowDatabase.getDatabase(context)
            var inserted = 0

            for (dto in dtos) {
                val existing = db.quoteDao().getQuoteById(dto.id)
                if (existing == null) {
                    db.quoteDao().insertQuote(dto.toEntity())
                    inserted++
                } else if (existing.status != dto.status.toQuoteStatus()) {
                    db.quoteDao().updateQuoteStatus(dto.id, dto.status.toQuoteStatus())
                    inserted++
                }
            }
            Log.d(TAG, "syncCustomerQuotes: $inserted new/updated quote(s)")
            inserted
        }

    suspend fun syncTechnicianQuotes(context: Context): Int =
        withContext(Dispatchers.IO) {
            if (!NetworkMonitor.isOnline(context)) return@withContext 0

            val api = ApiClient.get(context)
            val dtos: List<QuoteDto> = safeApiCall(TAG) {
                api.getTechnicianQuotes()
            } ?: return@withContext 0

            val db = ArcticFlowDatabase.getDatabase(context)
            var inserted = 0

            for (dto in dtos) {
                val existing = db.quoteDao().getQuoteById(dto.id)
                if (existing == null) {
                    db.quoteDao().insertQuote(dto.toEntity())
                    inserted++
                }
            }
            Log.d(TAG, "syncTechnicianQuotes: $inserted new quote(s)")
            inserted
        }

    // ============================================================
    // JOBS
    // ============================================================
    suspend fun syncCustomerJobs(context: Context): Int =
        withContext(Dispatchers.IO) {
            if (!NetworkMonitor.isOnline(context)) return@withContext 0

            val api = ApiClient.get(context)
            val dtos: List<JobDto> = safeApiCall(TAG) {
                api.getCustomerJobs()
            } ?: return@withContext 0

            val db = ArcticFlowDatabase.getDatabase(context)
            var inserted = 0

            for (dto in dtos) {
                val existing = db.jobDao().getJobById(dto.id)
                if (existing == null) {
                    db.jobDao().insertJob(dto.toEntity())
                    inserted++
                } else {
                    if (existing.status != dto.status.toJobStatus()) {
                        db.jobDao().updateJobStatus(dto.id, dto.status.toJobStatus())
                    }
                    if (existing.technicianOnWay != dto.technicianOnWay) {
                        db.jobDao().updateTechnicianOnWay(dto.id, dto.technicianOnWay)
                    }
                    inserted++
                }
            }
            Log.d(TAG, "syncCustomerJobs: $inserted new/updated job(s)")
            inserted
        }

    suspend fun syncTechnicianJobs(context: Context): Int =
        withContext(Dispatchers.IO) {
            if (!NetworkMonitor.isOnline(context)) return@withContext 0

            val api = ApiClient.get(context)
            val dtos: List<JobDto> = safeApiCall(TAG) {
                api.getTechnicianJobs()
            } ?: return@withContext 0

            val db = ArcticFlowDatabase.getDatabase(context)
            var inserted = 0

            for (dto in dtos) {
                val existing = db.jobDao().getJobById(dto.id)
                if (existing == null) {
                    db.jobDao().insertJob(dto.toEntity())
                    inserted++
                } else {
                    if (existing.status != dto.status.toJobStatus()) {
                        db.jobDao().updateJobStatus(dto.id, dto.status.toJobStatus())
                    }
                    inserted++
                }
            }
            Log.d(TAG, "syncTechnicianJobs: $inserted new/updated job(s)")
            inserted
        }

    // ============================================================
    // CONVENIENCE — pull everything at once
    // ============================================================
    suspend fun syncEverything(context: Context, role: String) {
        try {
            when (role.uppercase()) {
                "MANAGER" -> {
                    syncMyRequests(context)
                    syncCustomerQuotes(context)
                    syncCustomerJobs(context)
                    syncBuildings(context, "")
                }
                else -> {
                    syncPendingRequests(context)
                    syncTechnicianQuotes(context)
                    syncTechnicianJobs(context)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncEverything failed", e)
        }
    }

    // ============================================================
    // DTO → ENTITY MAPPERS
    // ============================================================

    private fun BuildingDto.toEntity(userId: String) = BuildingEntity(
        id = id,
        userId = userId,
        name = name,
        address = address.orEmpty(),
        suburb = suburb.orEmpty(),
        city = city.orEmpty(),
        province = province.orEmpty(),
        postalCode = postalCode.orEmpty(),
        fullAddress = fullAddress.orEmpty(),
        unitCount = unitCount,
        floors = floors,
        buildingType = runCatching { BuildingType.valueOf(buildingType) }
            .getOrDefault(BuildingType.RESIDENTIAL),
        registeredDate = registeredDate,
        status = runCatching { BuildingStatusEnum.valueOf(status) }
            .getOrDefault(BuildingStatusEnum.ACTIVE)
    )

    private fun ServiceRequestDto.toEntity() = ServiceRequest(
        id = id,
        userId = userId,
        buildingId = buildingId,
        buildingName = buildingName.orEmpty(),
        issueType = issueType.orEmpty(),
        description = description.orEmpty(),
        priority = priority.toRequestPriority(),
        preferredDate = preferredDate,
        status = status.toRequestStatus(),
        fullAddress = fullAddress.orEmpty(),
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun QuoteDto.toEntity() = Quote(
        id = id,
        requestId = requestId,
        technicianId = technicianId,
        customerId = customerId,
        buildingName = buildingName.orEmpty(),
        issueType = issueType.orEmpty(),
        description = "",
        scopeOfWork = "",
        partsRequired = "",
        laborCost = 0.0,
        partsCost = 0.0,
        totalCost = grandTotal,
        taxAmount = 0.0,
        grandTotal = grandTotal,
        status = status.toQuoteStatus(),
        validUntil = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000,
        createdAt = createdAt,
        updatedAt = createdAt
    )

    private fun JobDto.toEntity() = Job(
        id = id,
        quoteId = quoteId,
        requestId = requestId,
        technicianId = technicianId,
        customerId = customerId,
        buildingName = buildingName.orEmpty(),
        issueType = issueType.orEmpty(),
        description = "",
        status = status.toJobStatus(),
        scheduledDate = scheduledDate,
        technicianOnWay = technicianOnWay,
        fullAddress = fullAddress.orEmpty(),
        createdAt = createdAt
    )

    private fun String.toRequestStatus(): RequestStatus =
        runCatching { RequestStatus.valueOf(uppercase()) }
            .getOrDefault(RequestStatus.PENDING)

    private fun String.toRequestPriority(): RequestPriority =
        runCatching { RequestPriority.valueOf(uppercase()) }
            .getOrDefault(RequestPriority.MEDIUM)

    private fun String.toQuoteStatus(): QuoteStatus =
        runCatching { QuoteStatus.valueOf(uppercase()) }
            .getOrDefault(QuoteStatus.PENDING)

    private fun String.toJobStatus(): JobStatus =
        runCatching { JobStatus.valueOf(uppercase()) }
            .getOrDefault(JobStatus.PENDING)
}