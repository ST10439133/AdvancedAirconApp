package com.insy7315.advancedaircornapp.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.google.android.gms.location.LocationServices
import com.insy7315.advancedaircornapp.data.api.ApiRepository
import com.insy7315.advancedaircornapp.data.api.TechLocationDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.tasks.await

@SuppressLint("MissingPermission")
@Composable
fun LocationPusher(
    enabled: Boolean,
    context: Context,
    technicianId: String,
    technicianName: String?,
    jobId: Int?,
    customerId: String?,
    buildingName: String?
) {
    LaunchedEffect(enabled, technicianId, jobId) {
        if (!enabled || technicianId.isBlank()) return@LaunchedEffect
        val client = LocationServices.getFusedLocationProviderClient(context)

        while (isActive) {
            try {
                val loc: Location? = client.lastLocation.await()
                if (loc != null) {
                    ApiRepository.pushLocation(
                        context = context,
                        technicianId = technicianId,
                        dto = TechLocationDto(
                            technicianId = technicianId,
                            technicianName = technicianName,
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            jobId = jobId,
                            customerId = customerId,
                            buildingName = buildingName,
                            isOnMyWay = true,
                            lastUpdated = System.currentTimeMillis(),
                            status = "ON_MY_WAY"
                        )
                    )
                    Log.d("LocationPusher", "pushed lat=${loc.latitude} lon=${loc.longitude}")
                }
            } catch (e: Exception) {
                Log.w("LocationPusher", "push failed: ${e.message}")
            }
            delay(10_000L)
        }
    }
}