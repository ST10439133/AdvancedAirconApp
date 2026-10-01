package com.insy7315.advancedairconapp.services

import android.content.Context
import android.util.Log
import com.insy7315.advancedairconapp.data.ArcticFlowDatabase
import com.insy7315.advancedairconapp.data.api.ApiClient
import com.insy7315.advancedairconapp.data.api.ApiRepository
import com.insy7315.advancedairconapp.data.api.TechLocationDto
import com.insy7315.advancedairconapp.data.entities.TechLocation
import com.insy7315.advancedairconapp.data.network.LocationTrackingManager
import com.insy7315.advancedairconapp.data.network.NetworkMonitor
import com.insy7315.advancedairconapp.utils.LocationHelper

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

        // Look up destination coords from the local building row
        val db = ArcticFlowDatabase.getDatabase(context)
        var destLat = 0.0
        var destLng = 0.0
        try {
            val job = db.jobDao().getJobById(jobId)
            val request = job?.let { db.serviceRequestDao().getRequestById(it.requestId) }
            val building = request?.let { db.buildingDao().getBuildingById(it.buildingId) }
                ?: db.buildingDao().findFirstByName(buildingName)
            if (building != null) {
                destLat = building.latitude
                destLng = building.longitude
                Log.d(TAG, "Resolved destination: $destLat, $destLng for $buildingName")
            } else {
                Log.w(TAG, "No building found for '$buildingName' — dest coords default to 0")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not resolve destination coords for job $jobId", e)
        }

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
                status = "ON_MY_WAY",
                destinationLatitude = destLat,
                destinationLongitude = destLng
            )

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
                        destinationLatitude = destLat,
                        destinationLongitude = destLng,
                        onMyWay = true,
                        lastUpdated = System.currentTimeMillis(),
                        status = "ON_MY_WAY"
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "Firestore push failed", e)
            }

            if (NetworkMonitor.isOnline(context)) {
                ApiRepository.pushLocation(context, technicianId, dto)
            }
        } else {
            Log.w(TAG, "startForJob: no location fix; service will keep retrying")
        }

        TechLocationService.start(
            context = context,
            technicianId = technicianId,
            technicianName = technicianName,
            jobId = jobId,
            customerId = customerId,
            buildingName = buildingName,
            destinationLatitude = destLat,
            destinationLongitude = destLng
        )

        try {
            db.jobDao().updateTechnicianOnWay(jobId, true)
        } catch (e: Exception) {
            Log.w(TAG, "Could not flip technicianOnWay locally", e)
        }

        return true
    }

    suspend fun stopForTechnician(context: Context, technicianId: String) {
        if (technicianId.isBlank()) return

        TechLocationService.stop(context)

        try { LocationTrackingManager.stopTracking(technicianId) }
        catch (e: Exception) { Log.w(TAG, "Firestore stop failed", e) }

        if (NetworkMonitor.isOnline(context) && ApiClient.hasToken(context)) {
            try { ApiRepository.stopTracking(context, technicianId) }
            catch (e: Exception) { Log.w(TAG, "REST stop failed", e) }
        }
        Log.d(TAG, "Stopped tracking for $technicianId")
    }
}