package com.insy7315.advancedairconapp.data.api

import android.content.Context
import android.util.Log
import com.insy7315.advancedairconapp.data.entities.BuildingEntity
import com.insy7315.advancedairconapp.data.entities.Job
import com.insy7315.advancedairconapp.data.entities.Quote
import com.insy7315.advancedairconapp.data.entities.ServiceRequest
import com.insy7315.advancedairconapp.data.entities.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ApiRepository {

    private const val TAG = "ApiRepository"

    // ---------- USER ----------
    suspend fun syncUser(context: Context, user: User): String? =
        withContext(Dispatchers.IO) {
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

    // ---------- BUILDINGS ----------
    suspend fun pushBuilding(
        context: Context,
        building: BuildingEntity
    ): BuildingDto? = withContext(Dispatchers.IO) {
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

    suspend fun deleteBuilding(context: Context, serverId: Int): Boolean =
        withContext(Dispatchers.IO) {
            val api = ApiClient.get(context)
            safeApiCall(TAG) { api.deleteBuilding(serverId); true } == true
        }

    // ---------- SERVICE REQUESTS ----------
    suspend fun pushServiceRequest(
        context: Context,
        request: ServiceRequest,
        buildingName: String?,
        fullAddress: String?,
        serverBuildingId: Int?
    ): ServiceRequestDto? = withContext(Dispatchers.IO) {
        val api = ApiClient.get(context)
        val body: Map<String, Any?> = mapOf(
            "buildingId"    to serverBuildingId,
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

    suspend fun updateRequestStatus(
        context: Context,
        serverId: Int,
        status: String
    ): Boolean = withContext(Dispatchers.IO) {
        val api = ApiClient.get(context)
        safeApiCall(TAG) {
            api.updateRequestStatus(serverId, StatusUpdate(status)); true
        } == true
    }

    // ---------- QUOTES ----------
    suspend fun pushQuote(
        context: Context,
        quote: Quote,
        serverRequestId: Int?
    ): QuoteDto? = withContext(Dispatchers.IO) {
        val api = ApiClient.get(context)
        val body: Map<String, Any?> = mapOf(
            "requestId"      to serverRequestId,
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

    suspend fun updateQuoteStatus(
        context: Context,
        serverId: Int,
        status: String
    ): Boolean = withContext(Dispatchers.IO) {
        val api = ApiClient.get(context)
        safeApiCall(TAG) {
            api.updateQuoteStatus(serverId, StatusUpdate(status)); true
        } == true
    }

    // ---------- JOBS ----------
    suspend fun pushJob(
        context: Context,
        job: Job,
        serverQuoteId: Int?,
        serverRequestId: Int?
    ): JobDto? = withContext(Dispatchers.IO) {
        val api = ApiClient.get(context)
        val body: Map<String, Any?> = mapOf(
            "quoteId"        to serverQuoteId,
            "requestId"      to serverRequestId,
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

    suspend fun updateJobStatus(
        context: Context,
        serverId: Int,
        status: String
    ): Boolean = withContext(Dispatchers.IO) {
        val api = ApiClient.get(context)
        safeApiCall(TAG) {
            api.updateJobStatus(serverId, StatusUpdate(status)); true
        } == true
    }

    suspend fun setJobOnWay(
        context: Context,
        serverId: Int,
        onWay: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        val api = ApiClient.get(context)
        safeApiCall(TAG) {
            api.setOnWay(serverId, OnWayUpdate(onWay)); true
        } == true
    }

    // ---------- LIVE LOCATIONS ----------
    suspend fun pushLocation(
        context: Context,
        technicianId: String,
        dto: TechLocationDto
    ): Boolean = withContext(Dispatchers.IO) {
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

    suspend fun fetchLocations(
        context: Context,
        customerId: String? = null
    ): List<TechLocationDto> = withContext(Dispatchers.IO) {
        val api = ApiClient.get(context)
        safeApiCall(TAG) { api.getActiveLocations(customerId) } ?: emptyList()
    }

    suspend fun stopTracking(context: Context, technicianId: String): Boolean =
        withContext(Dispatchers.IO) {
            val api = ApiClient.get(context)
            safeApiCall(TAG) { api.stopTracking(technicianId); true } == true
        }
}