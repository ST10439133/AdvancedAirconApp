// app/src/main/java/com/insy7315/advancedairconapp/ui/screens/ServiceBookingsScreen.kt
package com.insy7315.advancedairconapp.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.insy7315.advancedairconapp.data.ArcticFlowDatabase
import com.insy7315.advancedairconapp.data.api.ApiRepository
import com.insy7315.advancedairconapp.data.entities.Job
import com.insy7315.advancedairconapp.data.entities.JobStatus
import com.insy7315.advancedairconapp.data.entities.TechLocation
import com.insy7315.advancedairconapp.data.network.LocationTrackingManager
import com.insy7315.advancedairconapp.navigation.NavManager
import com.insy7315.advancedairconapp.ui.components.LocationPusher
import com.insy7315.advancedairconapp.utils.LocationHelper
import com.insy7315.advancedairconapp.viewmodels.QuoteViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ============================================================
// BRAND TOKENS
// ============================================================
private val BabyBlue     = Color(0xFF4FA8D8)
private val BabyBlueDeep = Color(0xFF2E7BA6)
private val BabyBlueSoft = Color(0xFFE1F1FB)
private val OrangeAccent = Color(0xFFF7941D)
private val OrangeSoft   = Color(0xFFFFEBD2)
private val SuccessGreen = Color(0xFF2E7D32)
private val SuccessSoft  = Color(0xFFE6F4EA)
private val ErrorRed     = Color(0xFFBA1A1A)
private val ErrorSoft    = Color(0xFFFFDAD6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceBookingsScreen(
    userId: String,
    navManager: NavManager,
    onBackToDashboard: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = ArcticFlowDatabase.getDatabase(context)
    val viewModel: QuoteViewModel = viewModel(
        factory = QuoteViewModel.Factory(database, context)
    )

    val allJobs by viewModel.getJobsForTechnician(userId)
        .collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        viewModel.refreshFromServer("TECHNICIAN")
    }

    val activeTrackingJob = remember(allJobs) {
        allJobs.firstOrNull { it.technicianOnWay }
    }

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

    var selectedFilter by remember { mutableStateOf("Today") }
    val filters = listOf("Today", "This Week", "This Month", "All")

    val ranges = remember {
        val now = Calendar.getInstance()

        val startOfDay = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDayMs = startOfDay.timeInMillis
        val endOfDayMs   = startOfDayMs + 86_400_000L

        val startOfWeekMs = (startOfDay.clone() as Calendar).apply {
            val offset = (get(Calendar.DAY_OF_WEEK) - firstDayOfWeek + 7) % 7
            add(Calendar.DAY_OF_MONTH, -offset)
        }.timeInMillis
        val endOfWeekMs = startOfWeekMs + 7L * 86_400_000L

        val startOfMonthMs = (startOfDay.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis
        val endOfMonthMs = (startOfDay.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, 1)
        }.timeInMillis

        mapOf(
            "Today"      to (startOfDayMs until endOfDayMs),
            "This Week"  to (startOfWeekMs until endOfWeekMs),
            "This Month" to (startOfMonthMs until endOfMonthMs)
        )
    }

    val counts = remember(allJobs, ranges) {
        mapOf(
            "Today"      to allJobs.count { it.scheduledDate?.let { d -> d in ranges.getValue("Today") } == true },
            "This Week"  to allJobs.count { it.scheduledDate?.let { d -> d in ranges.getValue("This Week") } == true },
            "This Month" to allJobs.count { it.scheduledDate?.let { d -> d in ranges.getValue("This Month") } == true },
            "All"        to allJobs.size
        )
    }

    val filteredJobs = remember(allJobs, selectedFilter, ranges) {
        allJobs
            .filter { job ->
                val d = job.scheduledDate
                when (selectedFilter) {
                    "All"        -> true
                    "Today"      -> d != null && d in ranges.getValue("Today")
                    "This Week"  -> d != null && d in ranges.getValue("This Week")
                    "This Month" -> d != null && d in ranges.getValue("This Month")
                    else         -> true
                }
            }
            .sortedBy { it.scheduledDate ?: Long.MAX_VALUE }
    }

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
                    IconButton(onClick = onBackToDashboard) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Dashboard"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // ---- Filter chips ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                filters.forEach { f ->
                    val n = counts[f] ?: 0
                    FilterChip(
                        selected = selectedFilter == f,
                        onClick = { selectedFilter = f },
                        label = {
                            Text(
                                if (n > 0) "$f ($n)" else f,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            if (filteredJobs.isEmpty()) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = BabyBlueSoft,
                            modifier = Modifier.size(88.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.EventBusy,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = BabyBlueDeep
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No bookings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "No jobs match the \"$selectedFilter\" filter",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredJobs, key = { it.id }) { job ->
                        val resolvedAddress = addressMap[job.id].orEmpty()
                        BookingCard(
                            job = job,
                            resolvedAddress = resolvedAddress,
                            onMap = {
                                scope.launch { openMapForJob(context, job, viewModel) }
                            },
                            onOnMyWay = {
                                scope.launch {
                                    if (job.technicianOnWay) {
                                        viewModel.setTechnicianOnWay(job.id, false)
                                        LocationTrackingManager.stopTracking(userId)
                                        ApiRepository.stopTracking(context, job.technicianId)
                                        Toast.makeText(context, "Tracking stopped", Toast.LENGTH_SHORT).show()
                                    } else {
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
// Helpers — unchanged
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
        onMyWay = true,
        lastUpdated = System.currentTimeMillis(),
        status = "on_the_way"
    )

    val ok = LocationTrackingManager.updateLocation(techLocation)

    ApiRepository.pushLocation(
        context = context,
        technicianId = technicianId,
        dto = com.insy7315.advancedairconapp.data.api.TechLocationDto(
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
// BOOKING CARD — restyled
// ============================================================
@Composable
fun BookingCard(
    job: Job,
    resolvedAddress: String,
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

    val (statusBg, statusFg) = when (job.status) {
        JobStatus.COMPLETED   -> SuccessSoft  to SuccessGreen
        JobStatus.IN_PROGRESS -> BabyBlueSoft to BabyBlueDeep
        JobStatus.SCHEDULED   -> BabyBlueSoft to BabyBlueDeep
        JobStatus.PENDING     -> OrangeSoft   to OrangeAccent
        else                  -> Color(0xFFEEEEEE) to Color(0xFF616161)
    }

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            // ---- Header ----
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        job.buildingName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        job.issueType,
                        style = MaterialTheme.typography.bodySmall,
                        color = BabyBlueDeep,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusBg
                ) {
                    Text(
                        job.status.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = statusFg,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // ---- Meta ----
            if (resolvedAddress.isNotBlank()) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        resolvedAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
            job.scheduledDate?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        dateFormat.format(Date(it)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ---- Tracking toggle panel ----
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isLocalToggleOn) SuccessSoft else BabyBlueSoft,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isLocalToggleOn) SuccessGreen.copy(alpha = 0.15f)
                        else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = if (isLocalToggleOn) SuccessGreen else BabyBlueDeep,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (isLocalToggleOn) "Customer can track you"
                            else "Not tracking yet",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (isLocalToggleOn) "Tap to stop tracking"
                            else "Tap when you depart",
                            style = MaterialTheme.typography.labelSmall,
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

            Spacer(Modifier.height(12.dp))

            // ---- Action row ----
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onMap,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Navigation, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Map", style = MaterialTheme.typography.labelLarge)
                }
                Button(
                    onClick = onOpenJobCard,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeAccent,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        "Job Card",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}