// app/src/main/java/com/insy7315/advancedairconapp/services/LocationTrackingCoordinator.kt
package com.insy7315.advancedairconapp.services

import android.content.Context
import android.util.Log
import com.insy7315.advancedairconapp.data.ArcticFlowDatabase
import com.insy7315.advancedairconapp.data.api.ApiRepository
import com.insy7315.advancedairconapp.data.api.TechLocationDto
import com.insy7315.advancedairconapp.data.entities.TechLocation
import com.insy7315.advancedairconapp.data.network.LocationTrackingManager
import com.insy7315.advancedairconapp.data.network.NetworkMonitor
import com.insy7315.advancedairconapp.utils.LocationHelper

/**
 * Central place that starts and stops technician tracking for a job.
 *
 * Start:
 *   - verifies permission
 *   - pushes one immediate location to the server
 *   - starts TechLocationService (10s cadence, survives background)
 *
 * Stop:
 *   - stops the service
 *   - DELETEs the server-side location row
 *   - marks the Firestore doc as not-on-my-way
 *   - flips the job's technicianOnWay flag locally
 */
object LocationTrackingCoordinator {

    private const val TAG = "LocationTrackingCoord"

    suspend fun startForJob(
        context: Context,
        technicianId: String,
        technicianName: String?,
        jobId: Int,
        customerId: String,
        buildingName: String
    ): Boolean {
        if (technicianId.isBlank()) {
            Log.e(TAG, "startForJob: blank technicianId")
            return false
        }
        if (!LocationHelper.hasLocationPermission(context)) {
            Log.w(TAG, "startForJob: no location permission")
            return false
        }

        // 1. One immediate push so the customer sees us right away.
        val location = LocationHelper.getCurrentLocation(context)
        if (location != null) {
            val dto = TechLocationDto(
                technicianId = technicianId,
                technicianName = technicianName ?: "Technician",
                latitude = location.latitude,
                longitude = location.longitude,
                jobId = jobId,
                customerId = customerId,
                buildingName = buildingName,
                isOnMyWay = true,
                lastUpdated = System.currentTimeMillis(),
                status = "ON_MY_WAY"
            )

            // Firestore (used by LocationTrackingManager.streamActiveLocations)
            try {
                LocationTrackingManager.updateLocation(
                    TechLocation(
                        technicianId = technicianId,
                        technicianName = technicianName ?: "Technician",
                        latitude = location.latitude,
                        longitude = location.longitude,
                        jobId = jobId,
                        customerId = customerId,
                        buildingName = buildingName,
                        onMyWay = true,
                        lastUpdated = System.currentTimeMillis(),
                        status = "ON_MY_WAY"
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "Firestore push failed", e)
            }

            // REST
            if (NetworkMonitor.isOnline(context)) {
                ApiRepository.pushLocation(context, technicianId, dto)
            }
        } else {
            Log.w(TAG, "startForJob: no location fix; service will keep retrying")
        }

        // 2. Kick off the foreground service. It re-pushes every 10s.
        TechLocationService.start(
            context = context,
            technicianId = technicianId,
            technicianName = technicianName,
            jobId = jobId,
            customerId = customerId,
            buildingName = buildingName
        )

        // 3. Flip the local job flag (Room + server)
        try {
            val db = ArcticFlowDatabase.getDatabase(context)
            db.jobDao().updateTechnicianOnWay(jobId, true)
        } catch (e: Exception) {
            Log.w(TAG, "Could not flip technicianOnWay locally", e)
        }

        return true
    }

    suspend fun stopForTechnician(context: Context, technicianId: String) {
        if (technicianId.isBlank()) return

        // 1. Kill the service
        TechLocationService.stop(context)

        // 2. Firestore clear
        try { LocationTrackingManager.stopTracking(technicianId) }
        catch (e: Exception) { Log.w(TAG, "Firestore stop failed", e) }

        // 3. REST delete
        if (NetworkMonitor.isOnline(context)) {
            try { ApiRepository.stopTracking(context, technicianId) }
            catch (e: Exception) { Log.w(TAG, "REST stop failed", e) }
        }
        Log.d(TAG, "Stopped tracking for $technicianId")
    }
}