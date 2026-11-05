// IEEE Xplore. 2026. Building HIPAA-Compliant Offline Edge CRM Workflows with Secure ePHI Management and Resilient Synchronization. [Online]. Available at: https://ieeexplore.ieee.org/document/11476488/similar#similar [Accessed: 5 November 2026].
// Android Developers. 2026. Build an offline-first app | App architecture. [Online]. Available at: https://developer.android.com/topic/architecture/data-layer/offline-first [Accessed: 2 November 2026].
// Google for Developers. 2026. Use Kotlin coroutines in your Android app. [Online]. Available at: https://developer.android.com/codelabs/kotlin-coroutines [Accessed: 2 November 2026].
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