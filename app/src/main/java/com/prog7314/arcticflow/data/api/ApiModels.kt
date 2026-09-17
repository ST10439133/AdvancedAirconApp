package com.prog7314.arcticflow.data.api

import com.google.gson.annotations.SerializedName

// ============================================================
// Data classes mirroring the JSON shapes returned by the
// ArcticFlow REST API. Gson maps snake_case JSON keys to
// camelCase Kotlin properties via @SerializedName.
// ============================================================

data class UserSyncRequest(
    val uid: String,
    val email: String,
    val displayName: String?,
    val role: String,
    val phoneNumber: String?
)

data class UserSyncResponse(val token: String)

data class UserDto(
    val uid: String,
    val email: String,
    @SerializedName("display_name") val displayName: String?,
    val role: String,
    @SerializedName("phone_number") val phoneNumber: String?,
    @SerializedName("created_at") val createdAt: Long
)

data class BuildingDto(
    val id: Int,
    @SerializedName("user_id") val userId: String,
    val name: String,
    val address: String?,
    val suburb: String?,
    val city: String?,
    val province: String?,
    @SerializedName("postal_code") val postalCode: String?,
    @SerializedName("full_address") val fullAddress: String?,
    @SerializedName("unit_count") val unitCount: Int,
    val floors: Int,
    @SerializedName("building_type") val buildingType: String,
    @SerializedName("registered_date") val registeredDate: Long,
    val status: String
)

data class ServiceRequestDto(
    val id: Int,
    @SerializedName("user_id") val userId: String,
    @SerializedName("building_id") val buildingId: Int,
    @SerializedName("building_name") val buildingName: String?,
    @SerializedName("issue_type") val issueType: String?,
    val description: String?,
    val priority: String,
    @SerializedName("preferred_date") val preferredDate: Long?,
    val status: String,
    @SerializedName("full_address") val fullAddress: String?,
    @SerializedName("created_at") val createdAt: Long,
    @SerializedName("updated_at") val updatedAt: Long
)

data class QuoteDto(
    val id: Int,
    @SerializedName("request_id") val requestId: Int,
    @SerializedName("technician_id") val technicianId: String,
    @SerializedName("customer_id") val customerId: String,
    @SerializedName("building_name") val buildingName: String?,
    @SerializedName("issue_type") val issueType: String?,
    @SerializedName("grand_total") val grandTotal: Double,
    val status: String,
    @SerializedName("created_at") val createdAt: Long
)

data class JobDto(
    val id: Int,
    @SerializedName("quote_id") val quoteId: Int,
    @SerializedName("request_id") val requestId: Int,
    @SerializedName("technician_id") val technicianId: String,
    @SerializedName("customer_id") val customerId: String,
    @SerializedName("building_name") val buildingName: String?,
    @SerializedName("issue_type") val issueType: String?,
    val status: String,
    @SerializedName("scheduled_date") val scheduledDate: Long?,
    @SerializedName("technician_on_way") val technicianOnWay: Boolean,
    @SerializedName("full_address") val fullAddress: String?,
    @SerializedName("created_at") val createdAt: Long
)

data class TechLocationDto(
    @SerializedName("technician_id") val technicianId: String,
    @SerializedName("technician_name") val technicianName: String?,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("job_id") val jobId: Int?,
    @SerializedName("customer_id") val customerId: String?,
    @SerializedName("building_name") val buildingName: String?,
    @SerializedName("is_on_my_way") val isOnMyWay: Boolean,
    @SerializedName("last_updated") val lastUpdated: Long,
    val status: String
)

// Small helper DTOs for PATCH requests
data class StatusUpdate(val status: String)
data class OnWayUpdate(val onWay: Boolean)