// app/src/main/java/com/prog7314/arcticflow/ui/screens/ServiceBookingsScreen.kt
package com.prog7314.arcticflow.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prog7314.arcticflow.data.ArcticFlowDatabase
import com.prog7314.arcticflow.data.entities.Job
import com.prog7314.arcticflow.data.entities.JobStatus
import com.prog7314.arcticflow.data.entities.TechLocation
import com.prog7314.arcticflow.data.network.LocationTrackingManager
import com.prog7314.arcticflow.navigation.NavManager
import com.prog7314.arcticflow.utils.LocationHelper
import com.prog7314.arcticflow.viewmodels.QuoteViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceBookingsScreen(userId: String, navManager: NavManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = ArcticFlowDatabase.getDatabase(context)
    val viewModel: QuoteViewModel = viewModel(
        factory = QuoteViewModel.Factory(database)
    )

    val jobs by viewModel.getJobsForTechnician(userId).collectAsState(initial = emptyList())

    // Tracks which job we're currently trying to start tracking
    var pendingTrackingJob by remember { mutableStateOf<Job?>(null) }

    // Location permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        val hasPermission = granted.values.any { it }
        val job = pendingTrackingJob
        pendingTrackingJob = null

        if (hasPermission && job != null) {
            scope.launch {
                startTrackingForJob(
                    context = context,
                    job = job,
                    viewModel = viewModel,
                    onDone = { ok ->
                        Toast.makeText(
                            context,
                            if (ok) "Customer can track you now"
                            else "Failed to start tracking",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        } else {
            Toast.makeText(
                context,
                "Location permission is required for tracking",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Service Bookings") },
                navigationIcon = {
                    IconButton(onClick = { navManager.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (jobs.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.EventBusy, null, Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No bookings",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Accepted jobs will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(jobs, key = { it.id }) { job ->
                    BookingCard(
                        job = job,
                        onCall = {
                            Toast.makeText(context, "Calling customer...", Toast.LENGTH_SHORT).show()
                        },
                        onMap = {
                            openMapForJob(context, job)
                        },
                        onOnMyWay = {
                            scope.launch {
                                if (job.technicianOnWay) {
                                    // STOP tracking
                                    viewModel.setTechnicianOnWay(job.id, false)
                                    LocationTrackingManager.stopTracking(userId)
                                    Toast.makeText(context, "Tracking stopped", Toast.LENGTH_SHORT).show()
                                } else {
                                    // START tracking — request permission first
                                    if (LocationHelper.hasLocationPermission(context)) {
                                        startTrackingForJob(
                                            context = context,
                                            job = job,
                                            viewModel = viewModel,
                                            onDone = { ok ->
                                                Toast.makeText(
                                                    context,
                                                    if (ok) "Customer can track you now"
                                                    else "Failed to start tracking",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        )
                                    } else {
                                        pendingTrackingJob = job
                                        permissionLauncher.launch(LocationHelper.PERMISSIONS)
                                    }
                                }
                            }
                        },
                        onOpenJobCard = { navManager.navigateToCreateJobCard(job.id) }
                    )
                }
            }
        }
    }
}

// ============================================================
// Helper: start tracking for a job
// ============================================================
private suspend fun startTrackingForJob(
    context: android.content.Context,
    job: Job,
    viewModel: QuoteViewModel,
    onDone: (Boolean) -> Unit
) {
    val technicianId = job.technicianId
    if (technicianId.isBlank()) {
        Log.e("ServiceBookings", "Job #${job.id} has blank technicianId — cannot track")
        onDone(false)
        return
    }

    val location = LocationHelper.getCurrentLocation(context)
    if (location == null) {
        onDone(false)
        return
    }

    viewModel.setTechnicianOnWay(job.id, true)

    val techLocation = TechLocation(
        technicianId = technicianId,
        technicianName = "Technician",
        latitude = location.latitude,
        longitude = location.longitude,
        jobId = job.id,
        customerId = job.customerId,
        buildingName = job.buildingName,
        onMyWay = true,                       // ← renamed parameter
        lastUpdated = System.currentTimeMillis(),
        status = "on_the_way"
    )

    val ok = LocationTrackingManager.updateLocation(techLocation)
    Log.d("ServiceBookings",
        "startTracking job=${job.id} tech=$technicianId cust=${job.customerId} → $ok")
    onDone(ok)
}

// ============================================================
// Helper: open Google Maps with the building's address.
// ============================================================
private fun openMapForJob(context: android.content.Context, job: Job) {
    val target = job.buildingName.ifBlank {
        job.issueType.ifBlank { "Service location" }
    }
    val encoded = Uri.encode(target)

    // --- Attempt 1: native Google Maps app via geo: URI ---
    val geoUri = Uri.parse("geo:0,0?q=$encoded")
    val geoIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
        setPackage("com.google.android.apps.maps")
    }
    if (geoIntent.resolveActivity(context.packageManager) != null) {
        try {
            context.startActivity(geoIntent)
            return
        } catch (e: Exception) {
            Log.w("ServiceBookings", "geo intent failed, falling back", e)
        }
    }

    // --- Attempt 2: any app that can handle geo: URIs ---
    val anyGeoIntent = Intent(Intent.ACTION_VIEW, geoUri)
    if (anyGeoIntent.resolveActivity(context.packageManager) != null) {
        try {
            context.startActivity(anyGeoIntent)
            return
        } catch (e: Exception) {
            Log.w("ServiceBookings", "any geo intent failed, falling back", e)
        }
    }

    // --- Attempt 3: HTTPS Google Maps URL — opens in browser ---
    val httpsUri = Uri.parse(
        "https://www.google.com/maps/search/?api=1&query=$encoded"
    )
    val webIntent = Intent(Intent.ACTION_VIEW, httpsUri)
    try {
        context.startActivity(webIntent)
    } catch (e: Exception) {
        Log.e("ServiceBookings", "All map intents failed", e)
        Toast.makeText(
            context,
            "No app available to open maps",
            Toast.LENGTH_SHORT
        ).show()
    }
}

// ============================================================
// Booking Card UI — WITH DEBOUNCED TOGGLE
// ============================================================
@Composable
fun BookingCard(
    job: Job,
    onCall: () -> Unit,
    onMap: () -> Unit,
    onOnMyWay: () -> Unit,
    onOpenJobCard: () -> Unit
) {
    val dateFormat = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())

    // ===== Local state for the toggle (prevents rapid ON/OFF firing) =====
    var isLocalToggleOn by remember { mutableStateOf(job.technicianOnWay) }
    var isProcessing by remember { mutableStateOf(false) }

    // Keep the local toggle state synced with the DB-backed job state
    LaunchedEffect(job.technicianOnWay) {
        isLocalToggleOn = job.technicianOnWay
        isProcessing = false
    }

    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            // ===== Top row: building + status =====
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        job.buildingName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        job.issueType,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    job.scheduledDate?.let {
                        Text(
                            dateFormat.format(Date(it)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Badge(
                    containerColor = when (job.status) {
                        JobStatus.COMPLETED -> Color(0xFF4CAF50)
                        JobStatus.IN_PROGRESS -> Color(0xFF2196F3)
                        JobStatus.SCHEDULED -> Color(0xFF03A9F4)
                        else -> Color(0xFFFF9800)
                    }
                ) { Text(job.status.name, color = Color.White) }
            }

            Spacer(Modifier.height(8.dp))

            // ===== Tracking toggle (DEBOUNCED) =====
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isLocalToggleOn)
                        Color(0xFF4CAF50).copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DirectionsCar, null,
                        tint = if (isLocalToggleOn) Color(0xFF4CAF50)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (isLocalToggleOn) "Customer can track you" else "Not tracking yet",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (isLocalToggleOn) "Tap to stop tracking" else "Tap when you depart",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isLocalToggleOn,
                        enabled = !isProcessing,
                        onCheckedChange = { checked ->
                            if (!isProcessing) {
                                isProcessing = true
                                isLocalToggleOn = checked
                                onOnMyWay()
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ===== Action buttons =====
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCall, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Phone, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Call", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(onClick = onMap, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Navigation, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Map", style = MaterialTheme.typography.labelSmall)
                }
                Button(onClick = onOpenJobCard, modifier = Modifier.weight(1f)) {
                    Text("Job Card", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}