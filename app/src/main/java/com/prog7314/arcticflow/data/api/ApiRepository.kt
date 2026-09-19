package com.prog7314.arcticflow.data.api

import android.content.Context
import android.util.Log
import com.prog7314.arcticflow.data.entities.BuildingEntity
import com.prog7314.arcticflow.data.entities.Job
import com.prog7314.arcticflow.data.entities.Quote
import com.prog7314.arcticflow.data.entities.ServiceRequest
import com.prog7314.arcticflow.data.entities.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


// Centralised wrapper around ArcticFlowApi.
// Requests use camelCase keys - that's what the server reads
// (req.body.buildingId, req.body.issueType, etc.).
// Responses use @SerializedName snake_case - handled in ApiModels.
//
// Foreign-key strategy: Room ids and Postgres ids are different
// (auto-increment is per-database). Every push saves the server's
// id into IdMap so subsequent child rows can reference the right
// foreign key. Status-update methods translate local → server id
// before calling the PATCH endpoint.

object ApiRepository {

    private const val TAG = "ApiRepository"

    // User
    suspend fun syncUser(context: Context, user: User): String? = withContext(Dispatchers.IO) {
        val api = ApiClient.get(context)
        val response = safeApiCall(TAG) {
            api.syncUser(
                UserSyncRequest(
                    uid = user.uid,
                    email = user.email,
                    displayName = user.displayName,
                    role = user.role.name,
                    phoneNumber = user.phoneNumber
                )
            )
        }
        response?.token?.also { ApiClient.saveToken(context, it) }
    }

    // Buildings
    suspend fun pushBuilding(context: Context, building: BuildingEntity): BuildingDto? =
        withContext(Dispatchers.IO) {
            val api = ApiClient.get(context)
            val body: Map<String, Any?> = mapOf(
                "name"           to building.name,
                "address"        to building.address,
                "suburb"         to building.suburb,
                "city"           to building.city,
                "province"       to building.province,
                "postalCode"     to building.postalCode,
                "fullAddress"    to building.fullAddress,
                "unitCount"      to building.unitCount,
                "floors"         to building.floors,
                "buildingType"   to building.buildingType.name,
                "registeredDate" to building.registeredDate,
                "status"         to building.status.name
            )
            safeApiCall(TAG) { api.createBuilding(body) }
        }

    suspend fun deleteBuilding(context: Context, buildingId: Int): Boolean =
        withContext(Dispatchers.IO) {
            val api = ApiClient.get(context)
            val ok = safeApiCall(TAG) { api.deleteBuilding(buildingId); true }
            ok == true
        }

    // Service Requests
    suspend fun pushServiceRequest(
        context: Context,
        request: ServiceRequest,
        buildingName: String?,
        fullAddress: String?,
        serverBuildingId: Int?
    ): ServiceRequestDto? = withContext(Dispatchers.IO) {
        val api = ApiClient.get(context)
        val body: Map<String, Any?> = mapOf(
            "buildingId"    to (serverBuildingId ?: request.buildingId),
            "buildingName"  to (buildingName ?: request.buildingName),
            "issueType"     to request.issueType,
            "description"   to request.description,
            "priority"      to request.priority.name,
            "preferredDate" to request.preferredDate,
            "status"        to request.status.name,
            "fullAddress"   to (fullAddress ?: request.fullAddress)
        )
        safeApiCall(TAG) { api.createRequest(body) }
    }

    // Quotes
    suspend fun pushQuote(
        context: Context,
        quote: Quote,
        serverRequestId: Int?
    ): QuoteDto? = withContext(Dispatchers.IO) {
        val api = ApiClient.get(context)
        val body: Map<String, Any?> = mapOf(
            "requestId"      to (serverRequestId ?: quote.requestId),
            "technicianId"   to quote.technicianId,
            "customerId"     to quote.customerId,
            "buildingName"   to quote.buildingName,
            "issueType"      to quote.issueType,
            "description"    to quote.description,
            "scopeOfWork"    to quote.scopeOfWork,
            "partsRequired"  to quote.partsRequired,
            "estimatedHours" to quote.estimatedHours,
            "laborCost"      to quote.laborCost,
            "partsCost"      to quote.partsCost,
            "totalCost"      to quote.totalCost,
            "taxAmount"      to quote.taxAmount,
            "grandTotal"     to quote.grandTotal,
            "status"         to quote.status.name,
            "validUntil"     to quote.validUntil,
            "notes"          to quote.notes
        )
        safeApiCall(TAG) { api.createQuote(body) }
    }

    // Jobs
    suspend fun pushJob(
        context: Context,
        job: Job,
        serverQuoteId: Int?,
        serverRequestId: Int?
    ): JobDto? = withContext(Dispatchers.IO) {
        val api = ApiClient.get(context)
        val body: Map<String, Any?> = mapOf(
            "quoteId"        to (serverQuoteId ?: job.quoteId),
            "requestId"      to (serverRequestId ?: job.requestId),
            "technicianId"   to job.technicianId,
            "customerId"     to job.customerId,
            "buildingName"   to job.buildingName,
            "issueType"      to job.issueType,
            "description"    to job.description,
            "status"         to job.status.name,
            "scheduledDate"  to job.scheduledDate,
            "notes"          to job.notes,
            "fullAddress"    to job.fullAddress
        )
        safeApiCall(TAG) { api.createJob(body) }
    }

    // Status updates
    suspend fun updateRequestStatus(context: Context, localRequestId: Int, status: String): Boolean =
        withContext(Dispatchers.IO) {
            val serverId = IdMap.getRequest(context, localRequestId)
            if (serverId == null) {
                Log.w(TAG, "updateRequestStatus: no server id for local=$localRequestId — skipping")
                return@withContext false
            }
            val api = ApiClient.get(context)
            safeApiCall(TAG) { api.updateRequestStatus(serverId, StatusUpdate(status)); true } == true
        }

    suspend fun updateQuoteStatus(context: Context, localQuoteId: Int, status: String): Boolean =
        withContext(Dispatchers.IO) {
            val serverId = IdMap.getQuote(context, localQuoteId)
            if (serverId == null) {
                Log.w(TAG, "updateQuoteStatus: no server id for local=$localQuoteId — skipping")
                return@withContext false
            }
            val api = ApiClient.get(context)
            safeApiCall(TAG) { api.updateQuoteStatus(serverId, StatusUpdate(status)); true } == true
        }

    suspend fun updateJobStatus(context: Context, localJobId: Int, status: String): Boolean =
        withContext(Dispatchers.IO) {
            val serverId = IdMap.getJob(context, localJobId)
            if (serverId == null) {
                Log.w(TAG, "updateJobStatus: no server id for local=$localJobId — skipping")
                return@withContext false
            }
            val api = ApiClient.get(context)
            safeApiCall(TAG) { api.updateJobStatus(serverId, StatusUpdate(status)); true } == true
        }

    suspend fun setJobOnWay(context: Context, localJobId: Int, onWay: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            val serverId = IdMap.getJob(context, localJobId)
            if (serverId == null) {
                Log.w(TAG, "setJobOnWay: no server id for local=$localJobId — skipping")
                return@withContext false
            }
            val api = ApiClient.get(context)
            safeApiCall(TAG) { api.setOnWay(serverId, OnWayUpdate(onWay)); true } == true
        }

    //  Live Locations

    suspend fun pushLocation(context: Context, technicianId: String, dto: TechLocationDto): Boolean =
        withContext(Dispatchers.IO) {
            val api = ApiClient.get(context)
            val body: Map<String, Any?> = mapOf(
                "technicianName" to dto.technicianName,
                "latitude"       to dto.latitude,
                "longitude"      to dto.longitude,
                "jobId"          to dto.jobId,
                "customerId"     to dto.customerId,
                "buildingName"   to dto.buildingName,
                "isOnMyWay"      to dto.isOnMyWay,
                "status"         to dto.status
            )
            safeApiCall(TAG) { api.updateLocationRaw(technicianId, body); true } == true
        }

    suspend fun fetchLocations(context: Context, customerId: String? = null): List<TechLocationDto> =
        withContext(Dispatchers.IO) {
            val api = ApiClient.get(context)
            safeApiCall(TAG) { api.getActiveLocations(customerId) } ?: emptyList()
        }

    suspend fun stopTracking(context: Context, technicianId: String): Boolean =
        withContext(Dispatchers.IO) {
            val api = ApiClient.get(context)
            safeApiCall(TAG) { api.stopTracking(technicianId); true } == true
        }
}