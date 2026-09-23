package com.insy7315.advancedairconapp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jobs")
data class Job(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val serverId: Int? = null,

    val quoteId: Int = 0,
    val serverQuoteId: Int? = null,
    val requestId: Int = 0,
    val serverRequestId: Int? = null,

    val technicianId: String = "",
    val customerId: String = "",
    val buildingName: String = "",
    val issueType: String = "",
    val description: String = "",
    val status: JobStatus = JobStatus.PENDING,
    val scheduledDate: Long? = null,
    val technicianOnWay: Boolean = false,
    val notes: String? = null,
    val fullAddress: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

enum class JobStatus {
    PENDING,
    ASSIGNED,
    SCHEDULED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}