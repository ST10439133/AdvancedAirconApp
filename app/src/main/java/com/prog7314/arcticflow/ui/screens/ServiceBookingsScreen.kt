// app/src/main/java/com/prog7314/arcticflow/ui/screens/ServiceBookingsScreen.kt
package com.prog7314.arcticflow.ui.screens

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
import com.prog7314.arcticflow.data.api.ApiRepository             // 🔽 API SYNC
import com.prog7314.arcticflow.data.entities.Job
import com.prog7314.arcticflow.data.entities.JobStatus
import com.prog7314.arcticflow.data.entities.TechLocation
import com.prog7314.arcticflow.data.network.LocationTrackingManager
import com.prog7314.arcticflow.navigation.NavManager
import com.prog7314.arcticflow.ui.components.LocationPusher       // 🔽 API SYNC
import com.prog7314.arcticflow.utils.LocationHelper
import com.prog7314.arcticflow.viewmodels.QuoteViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceBookingsScreen(
    userId: String,
    navManager: NavManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = ArcticFlowDatabase.getDatabase(context)
    val viewModel: QuoteViewModel = viewModel(
        factory = QuoteViewModel.Factory(database, context)   // 🔽 API SYNC: pass context
    )

    val allJobs by viewModel.getJobsForTechnician(userId)
        .collectAsState(initial = emptyList())

    // 🔽 API SYNC: active tracking job (the one with technicianOnWay = true)
    val activeTrackingJob = remember(allJobs) {
        allJobs.firstOrNull { it.technicianOnWay }
    }

    // 🔽 API SYNC: push technician GPS to the REST API while any job is being tracked
    if (activeTrackingJob != null) {
        LocationPusher(
            enabled = true,
            context = context,
            technicianId = activeTrackingJob.technicianId,
            technicianName = "Technician",
            jobId = activeTrackingJob.id,
            customerId = activeTrackingJob.customerId,
            buildingName = activeTrackingJob.buildingName
        )
    }

    // ---------- Time filter ----------
    var selectedFilter by remember { mutableStateOf("Today") }
    val filters = listOf("Today", "This Week", "This Month", "All")

    val filteredJobs = remember(allJobs, selectedFilter) {
        val now = Calendar.getInstance()
        val startOfDay = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val endOfDay = startOfDay + 86_400_000L
        val startOfWeek = (now.clone() as Calendar).apply {
            set(Calendar.DAY_OF_WEEK, now.firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val startOfMonth = (now.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        allJobs.filter { job ->
            val d = job.scheduledDate
            if (selectedFilter == "All") {
                true
            } else if (d == null) {
                false
            } else {
                when (selectedFilter) {
                    "Today" -> d in startOfDay until endOfDay
                    "This Week" -> d >= startOfWeek
                    "This Month" -> d >= startOfMonth
                    else -> true
                }
            }
        }.sortedBy { it.scheduledDate ?: Long.MAX_VALUE }
    }

    // ---------- Address resolution map ----------
    var addressMap by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }

    LaunchedEffect(allJobs) {
        val resolved = mutableMapOf<Int, String>()
        for (job in allJobs) {
            resolved[job.id] = try {
                viewModel.resolveJobAddress(job)
            } catch (e: Exception) {
                Log.e("ServiceBookings", "resolveJobAddress failed for job ${job.id}", e)
                ""
            }
        }
        addressMap = resolved
        Log.d("ServiceBookings", "Resolved addresses: $resolved")
    }

    // ---------- Tracking permission + start ----------
    var pendingTrackingJob by remember { mutableStateOf<Job?>(null) }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { f ->
                    FilterChip(
                        selected = selectedFilter == f,
                        onClick = { selectedFilter = f },
                        label = { Text(f) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (filteredJobs.isEmpty()) {
                Box(
                    Modifier.fillMaxSize(),
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
                            "Jobs assigned to you will appear here",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filteredJobs, key = { it.id }) { job ->
                        val resolvedAddress = addressMap[job.id].orEmpty()
                        BookingCard(
                            job = job,
                            resolvedAddress = resolvedAddress,
                            onCall = {
                                Toast.makeText(context, "Calling customer...", Toast.LENGTH_SHORT).show()
                            },
                            onMap = {
                                scope.launch {
                                    openMapForJob(context, job, viewModel)
                                }
                            },
                            onOnMyWay = {
                                scope.launch {
                                    if (job.technicianOnWay) {
                                        // STOP tracking
                                        viewModel.setTechnicianOnWay(job.id, false)
                                        LocationTrackingManager.stopTracking(userId)

                                        // 🔽 API SYNC: remove the technician from the API's active list
                                        ApiRepository.stopTracking(context, job.technicianId)

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

    // Persist the "on my way" flag in Room — this also mirrors to the API
    // because we updated QuoteViewModel.setTechnicianOnWay()
    viewModel.setTechnicianOnWay(job.id, true)

    // Existing Firestore-based tracking
    val techLocation = TechLocation(
        technicianId = technicianId,
        technicianName = "Technician",
        latitude = location.latitude,
        longitude = location.longitude,
        jobId = job.id,
        customerId = job.customerId,
        buildingName = job.buildingName,
        onMyWay = true,
        lastUpdated = System.currentTimeMillis(),
        status = "on_the_way"
    )

    val ok = LocationTrackingManager.updateLocation(techLocation)

    // 🔽 API SYNC: push the initial GPS fix to the REST API immediately
    // (so the manager sees the tech right away, not after the first 10s poll)
    ApiRepository.pushLocation(
        context = context,
        technicianId = technicianId,
        dto = com.prog7314.arcticflow.data.api.TechLocationDto(
            technicianId = technicianId,
            technicianName = "Technician",
            latitude = location.latitude,
            longitude = location.longitude,
            jobId = job.id,
            customerId = job.customerId,
            buildingName = job.buildingName,
            isOnMyWay = true,
            lastUpdated = System.currentTimeMillis(),
            status = "ON_MY_WAY"
        )
    )

    Log.d(
        "ServiceBookings",
        "startTracking job=${job.id} tech=$technicianId cust=${job.customerId} → $ok"
    )
    onDone(ok)
}

// ============================================================
// Helper: open Google Maps with the SERVICE ADDRESS.
// ============================================================
private suspend fun openMapForJob(
    context: android.content.Context,
    job: Job,
    viewModel: QuoteViewModel
) {
    val target = viewModel.resolveJobAddress(job).trim()

    if (target.isBlank()) {
        Toast.makeText(
            context,
            "No address saved for this job",
            Toast.LENGTH_SHORT
        ).show()
        return
    }

    val encoded = Uri.encode(target)
    Log.d("ServiceBookings", "Opening map for address: '$target'")

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

    val anyGeoIntent = Intent(Intent.ACTION_VIEW, geoUri)
    if (anyGeoIntent.resolveActivity(context.packageManager) != null) {
        try {
            context.startActivity(anyGeoIntent)
            return
        } catch (e: Exception) {
            Log.w("ServiceBookings", "any geo intent failed, falling back", e)
        }
    }

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
// Booking Card UI — unchanged
// ============================================================
@Composable
fun BookingCard(
    job: Job,
    resolvedAddress: String,
    onCall: () -> Unit,
    onMap: () -> Unit,
    onOnMyWay: () -> Unit,
    onOpenJobCard: () -> Unit
) {
    val dateFormat = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())

    var isLocalToggleOn by remember { mutableStateOf(job.technicianOnWay) }
    var isProcessing by remember { mutableStateOf(false) }

    LaunchedEffect(job.technicianOnWay) {
        isLocalToggleOn = job.technicianOnWay
        isProcessing = false
    }

    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
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
                    if (resolvedAddress.isNotBlank()) {
                        Text(
                            resolvedAddress,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                        JobStatus.PENDING -> Color(0xFFFF9800)
                        else -> Color(0xFF9E9E9E)
                    }
                ) { Text(job.status.name, color = Color.White) }
            }

            Spacer(Modifier.height(8.dp))

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

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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