package com.insy7315.advancedairconapp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quotes")
data class Quote(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val serverId: Int? = null,

    val requestId: Int = 0,
    val serverRequestId: Int? = null,
    val technicianId: String = "",
    val customerId: String = "",
    val buildingName: String = "",
    val issueType: String = "",
    val description: String = "",
    val scopeOfWork: String = "",
    val partsRequired: String = "",
    val estimatedHours: Double = 0.0,
    val laborCost: Double = 0.0,
    val partsCost: Double = 0.0,
    val totalCost: Double = 0.0,
    val taxAmount: Double = 0.0,
    val grandTotal: Double = 0.0,

    val discountType: String = "NONE",
    val discountValue: Double = 0.0,
    val discountAmount: Double = 0.0,
    val finalTotal: Double = 0.0,

    val status: QuoteStatus = QuoteStatus.PENDING,
    val validUntil: Long = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000,
    val notes: String = "",

    // NEW: decline reason + who declined
    val declineReason: String? = null,
    val declinedBy: String? = null,   // "CUSTOMER" or "TECHNICIAN"

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // payment metadata
    val paymentReference: String? = null,
    val paidAt: Long? = null
)

enum class QuoteStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    EXPIRED,
    AWAITING_PAYMENT,
    PAID;

    /** "AWAITING_PAYMENT" → "Awaiting Payment". */
    val displayName: String
        get() = name.lowercase()
            .split('_', ' ')
            .joinToString(" ") { part ->
                part.replaceFirstChar { it.titlecase() }
            }
}