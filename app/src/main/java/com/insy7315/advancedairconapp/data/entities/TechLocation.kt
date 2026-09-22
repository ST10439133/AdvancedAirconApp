package com.insy7315.advancedairconapp.data.entities

import com.google.firebase.firestore.PropertyName


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
    val status: String = "idle"
) {

    constructor() : this("", "", 0.0, 0.0, 0, "", "", false, 0L, "idle")
}