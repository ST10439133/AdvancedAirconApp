// app/src/main/java/com/prog7314/arcticflow/utils/LocationHelper.kt
package com.prog7314.arcticflow.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

object LocationHelper {

    private const val TAG = "LocationHelper"

    // Fallback location if the device can't provide one (useful for emulators/tests)
    private const val FALLBACK_LAT = -26.2041    // Johannesburg
    private const val FALLBACK_LNG = 28.0473

    /**
     * Returns the current device location using FusedLocationProvider.
     * Falls back to a default location (Johannesburg) if:
     *   - the location service returns null
     *   - the request times out
     *   - we're on an emulator without a location fix
     *
     * NOTE: We explicitly check for location permission at the very start of
     *       this function. The @SuppressLint("MissingPermission") annotation
     *       below just tells Android Studio to trust our check.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? {
        // ⚠️ Explicit permission check — required before accessing location
        if (!hasLocationPermission(context)) {
            Log.w(TAG, "Location permission not granted")
            return null
        }

        val client = LocationServices.getFusedLocationProviderClient(context)

        // Try to get a fresh location
        try {
            val fresh = client.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).await()

            if (fresh != null) {
                Log.d(TAG, "Fresh location: ${fresh.latitude}, ${fresh.longitude}")
                return fresh
            }
        } catch (e: SecurityException) {
            // Very rare — user revoked permission between our check and this call
            Log.e(TAG, "Permission revoked during location fetch", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get current location", e)
        }

        // Try last known location
        try {
            val last = client.lastLocation.await()
            if (last != null) {
                Log.d(TAG, "Last known location: ${last.latitude}, ${last.longitude}")
                return last
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission revoked during last-location fetch", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get last known location", e)
        }

        // Fallback for emulator / testing
        Log.w(TAG, "No location from device — using fallback")
        val fallback = Location("fallback").apply {
            latitude = FALLBACK_LAT
            longitude = FALLBACK_LNG
        }
        return fallback
    }

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    val PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
}