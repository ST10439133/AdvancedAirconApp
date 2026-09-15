package com.prog7314.arcticflow.data.entities

import com.google.firebase.firestore.PropertyName

/**
 * Represents a technician's live location.
 * Stored in Firestore under: tech_locations/{technicianId}
 *
 * IMPORTANT: Kotlin properties starting with `is` (e.g. `isOnMyWay`) break
 * Firestore's reflection-based deserializer because Kotlin generates the
 * getter as `isOnMyWay()` instead of `getIsOnMyWay()`. Firestore's
 * CustomClassMapper then looks for a field called `onMyWay`, fails, and
 * silently drops the value.
 *
 * To avoid this trap we name the Kotlin property `onMyWay` (safe), but
 * annotate it with @PropertyName("isOnMyWay") so the Firestore document
 * field is still called `isOnMyWay` — this keeps existing docs and any
 * server-side queries working.
 */
data class TechLocation(
    val technicianId: String = "",
    val technicianName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val jobId: Int = 0,
    val customerId: String = "",
    val buildingName: String = "",

    @get:PropertyName("isOnMyWay")
    @set:PropertyName("isOnMyWay")
    var onMyWay: Boolean = false,

    val lastUpdated: Long = System.currentTimeMillis(),
    val status: String = "idle" // "idle" | "on_the_way" | "on_site" | "completed"
) {
    /** No-arg constructor required by Firestore */
    constructor() : this("", "", 0.0, 0.0, 0, "", "", false, 0L, "idle")
}