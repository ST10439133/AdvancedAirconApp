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

// ============================================================
// Centralised wrapper around ArcticFlowApi. ViewModels call
// these helpers so they never touch Retrofit directly. Every
// call is wrapped in safeApiCall() so a network failure returns
// null instead of crashing — Room remains the offline cache.
// ============================================================
object ApiRepository {

    private const val TAG = "ApiRepository"

    // ---------- Users ----------
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

    // ---------- Buildings ----------
    // NOTE: The Room entity is BuildingEntity, not Building.
    suspend fun pushBuilding(context: Context, building: BuildingEntity): Boolean =
        withContext(Dispatchers.IO) {
            val api = ApiClient.get(context)
            val body: Map<String, Any?> = mapOf(
                "name"          to building.name,
                "address"       to building.address,
                "suburb"        to building.suburb,
                "city"          to building.city,
                "province"      to building.province,
                "postal_code"   to building.postalCode,
                "unit_count"    to building.unitCount,
                "floors"        to building.floors,
                "building_type" to building.buildingType.name
            )
            val result = safeApiCall(TAG) { api.createBuilding(body) }
            result != null
        }

    suspend fun deleteBuilding(context: Context, buildingId: Int): Boolean =
        withContext(Dispatchers.IO) {
            val api = ApiClient.get(context)
            val ok = safeApiCall(TAG) { api.deleteBuilding(buildingId); true }
            ok == true
        }

    // ---------- Service Requests ----------
    suspend fun pushServiceRequest(
        context: Context,
        request: ServiceRequest,
        buildingName: String?,
        fullAddress: String?
    ): Boolean = withContext(Dispatchers.IO) {
        val api = ApiClient.get(context)
        val body: Map<String, Any?> = mapOf(
            "building_id"    to request.buildingId,
            "issue_type"     to request.issueType,
            "description"    to request.description,
            "priority"       to request.priority.name,
            "preferred_date" to request.preferredDate,
            "status"         to request.status.name,
            "building_name"  to buildingName,
            "full_address"   to fullAddress
        )
        safeApiCall(TAG) { api.createRequest(body) } != null
    }

    suspend fun updateRequestStatus(context: Context, requestId: Int, status: String): Boolean =
        withContext(Dispatchers.IO) {
            val api = ApiClient.get(context)
            safeApiCall(TAG) { api.updateRequestStatus(requestId, StatusUpdate(status)) ; true } == true
        }

    // ---------- Quotes ----------
    suspend fun pushQuote(context: Context, quote: Quote): Boolean = withContext(Dispatchers.IO) {
        val api = ApiClient.get(context)
        val body: Map<String, Any?> = mapOf(
            "request_id"  to quote.requestId,
            "grand_total" to quote.grandTotal,
            "status"      to quote.status.name
        )
        safeApiCall(TAG) { api.createQuote(body) } != null
    }

    suspend fun updateQuoteStatus(context: Context, quoteId: Int, status: String): Boolean =
        withContext(Dispatchers.IO) {
            val api = ApiClient.get(context)
            safeApiCall(TAG) { api.updateQuoteStatus(quoteId, StatusUpdate(status)) ; true } == true
        }

    // ---------- Jobs ----------
    suspend fun pushJob(context: Context, job: Job): Boolean = withContext(Dispatchers.IO) {
        val api = ApiClient.get(context)
        val body: Map<String, Any?> = mapOf(
            "quote_id"       to job.quoteId,
            "request_id"     to job.requestId,
            "scheduled_date" to job.scheduledDate,
            "status"         to job.status.name
        )
        safeApiCall(TAG) { api.createJob(body) } != null
    }

    suspend fun updateJobStatus(context: Context, jobId: Int, status: String): Boolean =
        withContext(Dispatchers.IO) {
            val api = ApiClient.get(context)
            safeApiCall(TAG) { api.updateJobStatus(jobId, StatusUpdate(status)) ; true } == true
        }

    suspend fun setJobOnWay(context: Context, jobId: Int, onWay: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            val api = ApiClient.get(context)
            safeApiCall(TAG) { api.setOnWay(jobId, OnWayUpdate(onWay)) ; true } == true
        }

    // ---------- Live Locations ----------
    suspend fun pushLocation(context: Context, technicianId: String, dto: TechLocationDto): Boolean =
        withContext(Dispatchers.IO) {
            val api = ApiClient.get(context)
            safeApiCall(TAG) { api.updateLocation(technicianId, dto) ; true } == true
        }

    suspend fun fetchLocations(context: Context, customerId: String? = null): List<TechLocationDto> =
        withContext(Dispatchers.IO) {
            val api = ApiClient.get(context)
            safeApiCall(TAG) { api.getActiveLocations(customerId) } ?: emptyList()
        }

    suspend fun stopTracking(context: Context, technicianId: String): Boolean =
        withContext(Dispatchers.IO) {
            val api = ApiClient.get(context)
            safeApiCall(TAG) { api.stopTracking(technicianId) ; true } == true
        }
}