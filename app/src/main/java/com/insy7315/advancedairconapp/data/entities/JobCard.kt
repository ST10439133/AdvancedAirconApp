// app/src/main/java/com/insy7315/advancedaircornapp/data/entities/JobCard.kt
package com.insy7315.advancedairconapp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "job_cards")
data class JobCard(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val jobId: Int,
    val technicianId: String,
    val buildingName: String = "",
    val workSummary: String = "",
    val partsUsed: List<PartItem> = emptyList(),  // Needs converter
    val startTime: Long? = null,
    val endTime: Long? = null,
    val additionalNotes: String = "",
    val photoPaths: List<String> = emptyList(),  // Needs converter
    val status: JobCardStatus = JobCardStatus.DRAFT,
    val createdAt: Long = System.currentTimeMillis(),
    val submittedAt: Long? = null
)

data class PartItem(
    val name: String = "",
    val quantity: Int = 1,
    val price: Double = 0.0
)

enum class JobCardStatus {
    DRAFT,
    SUBMITTED,
    APPROVED,
    REJECTED
}