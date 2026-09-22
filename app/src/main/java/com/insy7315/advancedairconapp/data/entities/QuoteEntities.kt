package com.insy7315.advancedairconapp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey


// ROOM ENTITIES

@Entity(tableName = "buildings")
data class BuildingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String,
    val name: String,
    val address: String,
    val suburb: String = "",
    val city: String = "",
    val province: String = "",
    val postalCode: String = "",
    val fullAddress: String = "",
    val unitCount: Int = 1,
    val floors: Int = 1,
    val buildingType: BuildingType = BuildingType.RESIDENTIAL,
    val registeredDate: Long = System.currentTimeMillis(),
    val status: BuildingStatusEnum = BuildingStatusEnum.ACTIVE
)

@Entity(tableName = "service_requests")
data class ServiceRequest(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String,
    val buildingId: Int,
    val buildingName: String,
    val issueType: String,
    val description: String,
    val priority: RequestPriority = RequestPriority.MEDIUM,
    val preferredDate: Long? = null,
    val status: RequestStatus = RequestStatus.PENDING,
    val fullAddress: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "quotes")
data class Quote(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val requestId: Int,
    val technicianId: String,
    val customerId: String,
    val buildingName: String,
    val issueType: String,
    val description: String,
    val scopeOfWork: String,
    val partsRequired: String,
    val estimatedHours: Double = 0.0,
    val laborCost: Double = 0.0,
    val partsCost: Double = 0.0,
    val totalCost: Double = 0.0,
    val taxAmount: Double = 0.0,
    val grandTotal: Double = 0.0,
    val status: QuoteStatus = QuoteStatus.PENDING,
    val validUntil: Long = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val notes: String? = null
)

@Entity(tableName = "jobs")
data class Job(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val quoteId: Int = 0,
    val requestId: Int = 0,
    val technicianId: String = "",
    val customerId: String = "",
    val buildingName: String = "",
    val issueType: String = "",
    val description: String = "",
    val status: JobStatus = JobStatus.PENDING,
    val scheduledDate: Long? = null,
    val startDate: Long? = null,
    val completionDate: Long? = null,
    val notes: String? = null,
    val rating: Float? = null,
    val review: String? = null,
    val technicianOnWay: Boolean = false,
    val fullAddress: String = "",
    val createdAt: Long = System.currentTimeMillis()
)


// ROOM ENTITY ENUMS

enum class BuildingType {
    RESIDENTIAL,
    COMMERCIAL,
    INDUSTRIAL,
    OFFICE,
    RETAIL
}

enum class BuildingStatusEnum {
    ACTIVE,
    MAINTENANCE,
    INACTIVE
}

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
    CANCELLED
}

enum class QuoteStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    EXPIRED
}