// app/src/main/java/com/insy7315/advancedairconapp/services/TechLocationService.kt
package com.insy7315.advancedairconapp.services

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

/**
 * Foreground service that pushes the technician's live location to the
 * backend every 10 seconds while the "on my way" toggle is active.
 *
 * Extras:
 *   EXTRA_TECH_ID       (String, required)
 *   EXTRA_TECH_NAME     (String, optional)
 *   EXTRA_JOB_ID        (Int, optional, 0 to omit)
 *   EXTRA_CUSTOMER_ID   (String, optional)
 *   EXTRA_BUILDING_NAME (String, optional)
 */
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

        fun start(
            context: Context,
            technicianId: String,
            technicianName: String?,
            jobId: Int?,
            customerId: String?,
            buildingName: String?
        ) {
            val intent = Intent(context, TechLocationService::class.java).apply {
                putExtra(EXTRA_TECH_ID, technicianId)
                putExtra(EXTRA_TECH_NAME, technicianName)
                putExtra(EXTRA_JOB_ID, jobId ?: 0)
                putExtra(EXTRA_CUSTOMER_ID, customerId)
                putExtra(EXTRA_BUILDING_NAME, buildingName)
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

        startForeground(NOTIFICATION_ID, buildNotification(buildingName))

        pushJob?.cancel()
        pushJob = scope.launch {
            val client = LocationServices.getFusedLocationProviderClient(this@TechLocationService)
            while (isActive) {
                try {
                    val loc: Location? = client.lastLocation.await()
                    if (loc != null) {
                        ApiRepository.pushLocation(
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
                                status = "ON_MY_WAY"
                            )
                        )
                        Log.d(TAG, "pushed lat=${loc.latitude} lon=${loc.longitude}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "push failed: ${e.message}")
                }
                delay(10_000L)
            }
        }

        return START_STICKY
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
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("ArcticFlow tracking active")
            .setContentText(content)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}