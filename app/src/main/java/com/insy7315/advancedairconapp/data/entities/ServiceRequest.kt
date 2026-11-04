package com.insy7315.advancedairconapp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "service_requests")
data class ServiceRequest(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val serverId: Int? = null,

    val userId: String = "",
    val buildingId: Int = 0,
    val serverBuildingId: Int? = null,
    val buildingName: String = "",
    val issueType: String = "",
    val description: String = "",
    val priority: RequestPriority = RequestPriority.MEDIUM,
    val preferredDate: Long? = null,
    val status: RequestStatus = RequestStatus.PENDING,
    val fullAddress: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // NEW — populated when a technician declines the request
    val declineReason: String? = null,
    val declinedBy: String? = null,   // "TECHNICIAN"
    val declinedAt: Long? = null
)

enum class RequestPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}

enum class RequestStatus {
    PENDING,
    QUOTED,
    ACCEPTED,
    DECLINED,
    DECLINED_BY_TECH,   // NEW — technician refused to quote
    CANCELLED,
    COMPLETED
}