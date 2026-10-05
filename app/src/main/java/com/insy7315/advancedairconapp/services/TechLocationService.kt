// Android Developers. 2026. Foreground service types are required. [Online]. Available at: https://developer.android.com/about/versions/14/changes/fgs-types-required [Accessed: 5 October 2026].
//Stack Overflow. 2021. Coroutine doesn't stop when the service containing it stops. [Online]. Available at: https://stackoverflow.com/questions/68353369/coroutine-doesnt-stop-when-the-service-containing-it-stops [Accessed: 5 October 2026].
//Android Developers. 2026. About background location and battery life. [Online]. Available at: https://developer.android.com/develop/sensors-and-location/location/battery [Accessed: 5 October 2026].

package com.insy7315.advancedairconapp.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import com.insy7315.advancedairconapp.R
import com.insy7315.advancedairconapp.data.api.ApiClient
import com.insy7315.advancedairconapp.data.api.ApiRepository
import com.insy7315.advancedairconapp.data.api.TechLocationDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.time.Duration.Companion.seconds

class TechLocationService : Service() {

    companion object {
        private const val TAG = "TechLocationService"
        private const val CHANNEL_ID = "arcticflow_tracking"
        private const val NOTIFICATION_ID = 9001

        const val EXTRA_TECH_ID = "tech_id"
        const val EXTRA_TECH_NAME = "tech_name"
        const val EXTRA_JOB_ID = "job_id"
        const val EXTRA_CUSTOMER_ID = "customer_id"
        const val EXTRA_BUILDING_NAME = "building_name"
        const val EXTRA_DEST_LAT = "dest_lat"
        const val EXTRA_DEST_LNG = "dest_lng"

        fun start(
            context: Context,
            technicianId: String,
            technicianName: String?,
            jobId: Int?,
            customerId: String?,
            buildingName: String?,
            destinationLatitude: Double = 0.0,
            destinationLongitude: Double = 0.0
        ) {
            val intent = Intent(context, TechLocationService::class.java).apply {
                putExtra(EXTRA_TECH_ID, technicianId)
                putExtra(EXTRA_TECH_NAME, technicianName)
                putExtra(EXTRA_JOB_ID, jobId ?: 0)
                putExtra(EXTRA_CUSTOMER_ID, customerId)
                putExtra(EXTRA_BUILDING_NAME, buildingName)
                putExtra(EXTRA_DEST_LAT, destinationLatitude)
                putExtra(EXTRA_DEST_LNG, destinationLongitude)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TechLocationService::class.java))
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pushJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val techId = intent?.getStringExtra(EXTRA_TECH_ID).orEmpty()
        if (techId.isBlank()) {
            Log.w(TAG, "Started without technicianId — stopping")
            stopSelf()
            return START_NOT_STICKY
        }

        val techName = intent?.getStringExtra(EXTRA_TECH_NAME)
        val jobId = intent?.getIntExtra(EXTRA_JOB_ID, 0)?.takeIf { it != 0 }
        val customerId = intent?.getStringExtra(EXTRA_CUSTOMER_ID)
        val buildingName = intent?.getStringExtra(EXTRA_BUILDING_NAME)
        val destLat = intent?.getDoubleExtra(EXTRA_DEST_LAT, 0.0) ?: 0.0
        val destLng = intent?.getDoubleExtra(EXTRA_DEST_LNG, 0.0) ?: 0.0

        startForeground(NOTIFICATION_ID, buildNotification(buildingName))

        pushJob?.cancel()
        pushJob = scope.launch {
            val client = LocationServices.getFusedLocationProviderClient(this@TechLocationService)

            if (!ApiClient.hasToken(this@TechLocationService)) {
                Log.w(TAG, "No JWT at start — stopping service")
                stopSelfSafely()
                return@launch
            }

            while (isActive) {
                try {
                    if (!ApiClient.hasToken(this@TechLocationService)) {
                        Log.w(TAG, "JWT cleared mid-run — stopping service")
                        stopSelfSafely()
                        return@launch
                    }

                    val loc: Location? = try {
                        client.lastLocation.await()
                    } catch (e: SecurityException) {
                        Log.e(TAG, "Location permission revoked mid-run", e)
                        stopSelfSafely()
                        return@launch
                    }

                    if (loc != null) {
                        val ok = ApiRepository.pushLocation(
                            context = this@TechLocationService,
                            technicianId = techId,
                            dto = TechLocationDto(
                                technicianId = techId,
                                technicianName = techName ?: "Technician",
                                latitude = loc.latitude,
                                longitude = loc.longitude,
                                jobId = jobId,
                                customerId = customerId,
                                buildingName = buildingName,
                                isOnMyWay = true,
                                lastUpdated = System.currentTimeMillis(),
                                status = "ON_MY_WAY",
                                destinationLatitude = destLat,
                                destinationLongitude = destLng
                            )
                        )
                        if (ok) {
                            Log.d(TAG, "pushed lat=${loc.latitude} lon=${loc.longitude}")
                        } else {
                            if (!ApiClient.hasToken(this@TechLocationService)) {
                                Log.w(TAG, "Push failed and JWT is gone — stopping")
                                stopSelfSafely()
                                return@launch
                            }
                            Log.w(TAG, "push failed — will retry")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "push threw: ${e.message}")
                }
                delay(10.seconds)
            }
        }

        return START_STICKY
    }

    private fun stopSelfSafely() {
        try {
            pushJob?.cancel()
            stopSelf()
        } catch (e: Exception) {
            Log.w(TAG, "stopSelfSafely failed", e)
        }
    }

    override fun onDestroy() {
        pushJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(buildingName: String?): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val chan = NotificationChannel(
                    CHANNEL_ID,
                    "Technician tracking",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shows while you are on the way to a job"
                }
                nm.createNotificationChannel(chan)
            }
        }

        val content = if (buildingName.isNullOrBlank())
            "Sharing your live location with the customer"
        else
            "On the way to $buildingName"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_aircon)
            .setContentTitle("ArcticFlow Tracking Active")
            .setContentText(content)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}