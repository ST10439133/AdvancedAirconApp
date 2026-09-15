// app/src/main/java/com/prog7314/arcticflow/data/entities/TechLocation.kt
package com.prog7314.arcticflow.data.entities

/**
 * Represents a technician's live location.
 * Stored in Firestore under: tech_locations/{technicianId}
 */
data class TechLocation(
    val technicianId: String = "",
    val technicianName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val jobId: Int = 0,
    val buildingName: String = "",
    val isOnMyWay: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis(),
    val status: String = "idle" // "idle" | "on_the_way" | "on_site" | "completed"
) {
    /** Empty constructor for Firestore deserialization */
    constructor() : this("", "", 0.0, 0.0, 0, "", false, 0L, "idle")
}