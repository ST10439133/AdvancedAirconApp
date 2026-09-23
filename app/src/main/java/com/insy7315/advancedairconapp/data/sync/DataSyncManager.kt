// app/src/main/java/com/insy7315/advancedairconapp/data/sync/DataSyncManager.kt
package com.insy7315.advancedairconapp.data.sync

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.insy7315.advancedairconapp.data.ArcticFlowDatabase
import com.insy7315.advancedairconapp.data.api.ApiClient
import com.insy7315.advancedairconapp.data.api.BuildingDto
import com.insy7315.advancedairconapp.data.api.JobDto
import com.insy7315.advancedairconapp.data.api.QuoteDto
import com.insy7315.advancedairconapp.data.api.ServiceRequestDto
import com.insy7315.advancedairconapp.data.api.UserSyncRequest
import com.insy7315.advancedairconapp.data.api.safeApiCall
import com.insy7315.advancedairconapp.data.entities.*
import com.insy7315.advancedairconapp.data.network.NetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pulls data from the REST API and mirrors it into Room.
 *
 * Uses server-side IDs to match existing rows: `getXByServerId(dto.id)`
 * instead of `getXById(dto.id)`. This prevents the pull-sync from
 * inserting duplicates when local Room IDs differ from server IDs.
 *
 * Never lets a server-side PENDING overwrite a local ACCEPTED/DECLINED.
 *
 * Every entry point calls `ensureToken(context)` first: if we don't have a
 * JWT yet (fresh install, first launch while offline, previous syncUser call
 * failed) we transparently re-acquire one from the current Firebase user
 * before pulling.
 */
object DataSyncManager {

    private const val TAG = "DataSyncManager"

    /**
     * Ensures ApiClient has a JWT. If not, retries /api/users/sync using the
     * Firebase user + local Room user we already have.
     */
    private suspend fun ensureToken(context: Context): Boolean {
        if (ApiClient.hasToken(context)) return true

        if (!NetworkMonitor.isOnline(context)) {
            Log.d(TAG, "ensureToken: offline, cannot re-acquire JWT")
            return false
        }

        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser == null) {
            Log.d(TAG, "ensureToken: no Firebase user, cannot re-acquire JWT")
            return false
        }

        val db = ArcticFlowDatabase.getDatabase(context)
        val localUser = db.userDao().getUserById(firebaseUser.uid)
        if (localUser == null) {
            Log.d(TAG, "ensureToken: no Room user for uid=${firebaseUser.uid}")
            return false
        }

        val api = ApiClient.get(context)
        val response = safeApiCall(TAG) {
            api.syncUser(
                UserSyncRequest(
                    uid = localUser.uid,
                    email = localUser.email,
                    displayName = localUser.displayName,
                    role = localUser.role.name,
                    phoneNumber = localUser.phoneNumber
                )
            )
        }

        if (response != null) {
            ApiClient.saveToken(context, response.token)
            Log.d(TAG, "ensureToken: re-acquired JWT (len=${response.token.length})")
            return true
        }

        Log.w(TAG, "ensureToken: could not re-acquire JWT")
        return false
    }

    // ============================================================
    // BUILDINGS
    // ============================================================
    suspend fun syncBuildings(context: Context, userId: String): Int =
        withContext(Dispatchers.IO) {
            if (!NetworkMonitor.isOnline(context)) return@withContext 0
            if (!ensureToken(context)) {
                Log.d(TAG, "syncBuildings skipped: no JWT yet and couldn't get one")
                return@withContext 0
            }

            val api = ApiClient.get(context)
            val dtos: List<BuildingDto> = safeApiCall(TAG) {
                api.getBuildings()
            } ?: return@withContext 0

            val db = ArcticFlowDatabase.getDatabase(context)
            var inserted = 0

            for (dto in dtos) {
                val existing = db.buildingDao().getBuildingByServerId(dto.id)
                if (existing == null) {
                    db.buildingDao().insertBuilding(dto.toEntity(userId))
                    inserted++
                }
            }
            Log.d(TAG, "syncBuildings: $inserted new")
            inserted
        }

    // ============================================================
    // SERVICE REQUESTS
    // ============================================================
    suspend fun syncPendingRequests(context: Context): Int =
        withContext(Dispatchers.IO) {
            if (!NetworkMonitor.isOnline(context)) return@withContext 0
            if (!ensureToken(context)) {
                Log.d(TAG, "syncPendingRequests skipped: no JWT yet and couldn't get one")
                return@withContext 0
            }

            val api = ApiClient.get(context)
            val dtos: List<ServiceRequestDto> = safeApiCall(TAG) {
                api.getPendingRequests()
            } ?: return@withContext 0

            val db = ArcticFlowDatabase.getDatabase(context)
            var changed = 0

            for (dto in dtos) {
                val existing = db.serviceRequestDao().getRequestByServerId(dto.id)
                if (existing == null) {
                    db.serviceRequestDao().insertRequest(dto.toEntity(db))
                    changed++
                } else {
                    val incoming = dto.status.toRequestStatus()
                    val shouldUpdate =
                        !(existing.status == RequestStatus.ACCEPTED && incoming == RequestStatus.PENDING)
                                && !(existing.status == RequestStatus.DECLINED && incoming == RequestStatus.PENDING)
                    if (shouldUpdate && existing.status != incoming) {
                        db.serviceRequestDao().updateRequestStatus(existing.id, incoming)
                        changed++
                    }
                }
            }
            Log.d(TAG, "syncPendingRequests: $changed")
            changed
        }

    suspend fun syncMyRequests(context: Context): Int =
        withContext(Dispatchers.IO) {
            if (!NetworkMonitor.isOnline(context)) return@withContext 0
            if (!ensureToken(context)) {
                Log.d(TAG, "syncMyRequests skipped: no JWT yet and couldn't get one")
                return@withContext 0
            }

            val api = ApiClient.get(context)
            val dtos: List<ServiceRequestDto> = safeApiCall(TAG) {
                api.getMyRequests()
            } ?: return@withContext 0

            val db = ArcticFlowDatabase.getDatabase(context)
            var changed = 0

            for (dto in dtos) {
                val existing = db.serviceRequestDao().getRequestByServerId(dto.id)
                if (existing == null) {
                    db.serviceRequestDao().insertRequest(dto.toEntity(db))
                    changed++
                } else {
                    val incoming = dto.status.toRequestStatus()
                    val shouldUpdate =
                        !(existing.status == RequestStatus.ACCEPTED && incoming == RequestStatus.PENDING)
                                && !(existing.status == RequestStatus.DECLINED && incoming == RequestStatus.PENDING)
                    if (shouldUpdate && existing.status != incoming) {
                        db.serviceRequestDao().updateRequestStatus(existing.id, incoming)
                        changed++
                    }
                }
            }
            Log.d(TAG, "syncMyRequests: $changed")
            changed
        }

    // ============================================================
    // QUOTES
    // ============================================================
    suspend fun syncCustomerQuotes(context: Context): Int =
        withContext(Dispatchers.IO) {
            if (!NetworkMonitor.isOnline(context)) return@withContext 0
            if (!ensureToken(context)) {
                Log.d(TAG, "syncCustomerQuotes skipped: no JWT yet and couldn't get one")
                return@withContext 0
            }

            val api = ApiClient.get(context)
            val dtos: List<QuoteDto> = safeApiCall(TAG) {
                api.getCustomerQuotes()
            } ?: return@withContext 0

            val db = ArcticFlowDatabase.getDatabase(context)
            var changed = 0

            for (dto in dtos) {
                val existing = db.quoteDao().getQuoteByServerId(dto.id)
                if (existing == null) {
                    db.quoteDao().insertQuote(dto.toEntity(db))
                    changed++
                } else {
                    val incoming = dto.status.toQuoteStatus()
                    val shouldUpdate =
                        !(existing.status == QuoteStatus.ACCEPTED && incoming == QuoteStatus.PENDING)
                                && !(existing.status == QuoteStatus.DECLINED && incoming == QuoteStatus.PENDING)
                    if (shouldUpdate && existing.status != incoming) {
                        db.quoteDao().updateQuoteStatus(existing.id, incoming)
                        changed++
                    }
                }
            }
            Log.d(TAG, "syncCustomerQuotes: $changed")
            changed
        }

    suspend fun syncTechnicianQuotes(context: Context): Int =
        withContext(Dispatchers.IO) {
            if (!NetworkMonitor.isOnline(context)) return@withContext 0
            if (!ensureToken(context)) {
                Log.d(TAG, "syncTechnicianQuotes skipped: no JWT yet and couldn't get one")
                return@withContext 0
            }

            val api = ApiClient.get(context)
            val dtos: List<QuoteDto> = safeApiCall(TAG) {
                api.getTechnicianQuotes()
            } ?: return@withContext 0

            val db = ArcticFlowDatabase.getDatabase(context)
            var changed = 0

            for (dto in dtos) {
                val existing = db.quoteDao().getQuoteByServerId(dto.id)
                if (existing == null) {
                    db.quoteDao().insertQuote(dto.toEntity(db))
                    changed++
                } else {
                    val incoming = dto.status.toQuoteStatus()
                    val shouldUpdate =
                        !(existing.status == QuoteStatus.ACCEPTED && incoming == QuoteStatus.PENDING)
                                && !(existing.status == QuoteStatus.DECLINED && incoming == QuoteStatus.PENDING)
                    if (shouldUpdate && existing.status != incoming) {
                        db.quoteDao().updateQuoteStatus(existing.id, incoming)
                        changed++
                    }
                }
            }
            Log.d(TAG, "syncTechnicianQuotes: $changed")
            changed
        }

    // ============================================================
    // JOBS
    // ============================================================
    suspend fun syncCustomerJobs(context: Context): Int =
        withContext(Dispatchers.IO) {
            if (!NetworkMonitor.isOnline(context)) return@withContext 0
            if (!ensureToken(context)) {
                Log.d(TAG, "syncCustomerJobs skipped: no JWT yet and couldn't get one")
                return@withContext 0
            }

            val api = ApiClient.get(context)
            val dtos: List<JobDto> = safeApiCall(TAG) {
                api.getCustomerJobs()
            } ?: return@withContext 0

            val db = ArcticFlowDatabase.getDatabase(context)
            var changed = 0

            for (dto in dtos) {
                val existing = db.jobDao().getJobByServerId(dto.id)
                if (existing == null) {
                    db.jobDao().insertJob(dto.toEntity(db))
                    changed++
                } else {
                    if (existing.status != dto.status.toJobStatus()) {
                        db.jobDao().updateJobStatus(existing.id, dto.status.toJobStatus())
                    }
                    if (existing.technicianOnWay != dto.technicianOnWay) {
                        db.jobDao().updateTechnicianOnWay(existing.id, dto.technicianOnWay)
                    }
                    changed++
                }
            }
            Log.d(TAG, "syncCustomerJobs: $changed")
            changed
        }

    suspend fun syncTechnicianJobs(context: Context): Int =
        withContext(Dispatchers.IO) {
            if (!NetworkMonitor.isOnline(context)) return@withContext 0
            if (!ensureToken(context)) {
                Log.d(TAG, "syncTechnicianJobs skipped: no JWT yet and couldn't get one")
                return@withContext 0
            }

            val api = ApiClient.get(context)
            val dtos: List<JobDto> = safeApiCall(TAG) {
                api.getTechnicianJobs()
            } ?: return@withContext 0

            val db = ArcticFlowDatabase.getDatabase(context)
            var changed = 0

            for (dto in dtos) {
                val existing = db.jobDao().getJobByServerId(dto.id)
                if (existing == null) {
                    db.jobDao().insertJob(dto.toEntity(db))
                    changed++
                } else if (existing.status != dto.status.toJobStatus()) {
                    db.jobDao().updateJobStatus(existing.id, dto.status.toJobStatus())
                    changed++
                }
            }
            Log.d(TAG, "syncTechnicianJobs: $changed")
            changed
        }

    // ============================================================
    // CONVENIENCE
    // ============================================================
    suspend fun syncEverything(context: Context, role: String) {
        try {
            if (!ensureToken(context)) {
                Log.d(TAG, "syncEverything skipped: no JWT yet and couldn't get one")
                return
            }

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
    // DTO -> ENTITY MAPPERS
    // ============================================================

    private fun BuildingDto.toEntity(userId: String) = BuildingEntity(
        id = 0,
        serverId = id,
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

    private suspend fun ServiceRequestDto.toEntity(db: ArcticFlowDatabase): ServiceRequest {
        val localBuilding = db.buildingDao().getBuildingByServerId(buildingId)
        return ServiceRequest(
            id = 0,
            serverId = id,
            userId = userId,
            buildingId = localBuilding?.id ?: 0,
            serverBuildingId = buildingId,
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
    }

    private suspend fun QuoteDto.toEntity(db: ArcticFlowDatabase): Quote {
        val localRequest = db.serviceRequestDao().getRequestByServerId(requestId)
        return Quote(
            id = 0,
            serverId = id,
            requestId = localRequest?.id ?: 0,
            serverRequestId = requestId,
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
    }

    private suspend fun JobDto.toEntity(db: ArcticFlowDatabase): Job {
        val localQuote = db.quoteDao().getQuoteByServerId(quoteId)
        val localRequest = db.serviceRequestDao().getRequestByServerId(requestId)
        return Job(
            id = 0,
            serverId = id,
            quoteId = localQuote?.id ?: 0,
            serverQuoteId = quoteId,
            requestId = localRequest?.id ?: 0,
            serverRequestId = requestId,
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
    }

    private fun String.toRequestStatus(): RequestStatus =
        runCatching { RequestStatus.valueOf(uppercase()) }.getOrDefault(RequestStatus.PENDING)

    private fun String.toRequestPriority(): RequestPriority =
        runCatching { RequestPriority.valueOf(uppercase()) }.getOrDefault(RequestPriority.MEDIUM)

    private fun String.toQuoteStatus(): QuoteStatus =
        runCatching { QuoteStatus.valueOf(uppercase()) }.getOrDefault(QuoteStatus.PENDING)

    private fun String.toJobStatus(): JobStatus =
        runCatching { JobStatus.valueOf(uppercase()) }.getOrDefault(JobStatus.PENDING)
}