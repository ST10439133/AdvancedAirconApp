// app/src/main/java/com/insy7315/advancedairconapp/ui/screens/FieldTrackingScreen.kt
package com.insy7315.advancedairconapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.insy7315.advancedairconapp.data.api.TechLocationDto
import com.insy7315.advancedairconapp.navigation.NavManager
import com.insy7315.advancedairconapp.viewmodels.ManagerTrackingViewModel
import java.text.SimpleDateFormat
import java.util.*

// BRAND TOKENS
private val BabyBlue     = Color(0xFF4FA8D8)
private val BabyBlueDeep = Color(0xFF2E7BA6)
private val BabyBlueSoft = Color(0xFFE1F1FB)
private val OrangeAccent = Color(0xFFF7941D)
private val SuccessGreen = Color(0xFF2E7D32)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldTrackingScreen(
    navManager: NavManager,
    onBackToDashboard: () -> Unit
) {
    val viewModel: ManagerTrackingViewModel = viewModel()

    val technicians by viewModel.technicians.collectAsStateWithLifecycle()
    val isLoading by viewModel.isPolling.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.startPolling(customerId = null)
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.stopPolling() }
    }

    val defaultPosition = LatLng(-26.2041, 28.0473)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPosition, 12f)
    }

    // Re-center every time the list updates (position, not just count).
    // This makes the camera follow the technician in real time.
    LaunchedEffect(technicians) {
        val first = technicians.firstOrNull() ?: return@LaunchedEffect
        cameraPositionState.animate(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.fromLatLngZoom(
                    LatLng(first.latitude, first.longitude),
                    14f
                )
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Technician Tracking") },
                navigationIcon = {
                    IconButton(onClick = onBackToDashboard) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Dashboard"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* polling handles refresh */ }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = false),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    myLocationButtonEnabled = false
                )
            ) {
                technicians.forEach { tech ->
                    Marker(
                        state = MarkerState(
                            position = LatLng(tech.latitude, tech.longitude)
                        ),
                        title = tech.technicianName?.takeIf { it.isNotBlank() } ?: "Technician",
                        snippet = buildString {
                            if (!tech.buildingName.isNullOrBlank()) {
                                append(tech.buildingName)
                                append(" • ")
                            }
                            append(tech.status)
                            append(" • ")
                            append(relativeAge(tech.lastUpdated))
                        },
                        icon = com.google.android.gms.maps.model.BitmapDescriptorFactory
                            .defaultMarker(
                                when (tech.status) {
                                    "on_the_way", "ON_MY_WAY" ->
                                        com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE
                                    "on_site" ->
                                        com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN
                                    "completed" ->
                                        com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_VIOLET
                                    else ->
                                        com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_ORANGE
                                }
                            )
                    )
                }
            }

            if (isLoading && technicians.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = BabyBlueDeep)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Locating technicians…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (!isLoading && technicians.isEmpty()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = BabyBlueSoft,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(34.dp),
                                    tint = BabyBlueDeep
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "No Active Technicians",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Technicians will appear here when they tap\n\"On My Way\" on a booking.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (technicians.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    shape = RoundedCornerShape(50),
                    color = SuccessGreen,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${technicians.size} active",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            if (technicians.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = BabyBlueSoft,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.DirectionsCar,
                                        contentDescription = null,
                                        tint = BabyBlueDeep,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "On-Duty Technicians",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${technicians.size} tracked",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(6.dp))

                        technicians.take(3).forEach { tech ->
                            TechRow(tech)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TechRow(tech: TechLocationDto) {
    val statusColor = when (tech.status) {
        "on_the_way", "ON_MY_WAY" -> Color(0xFF03A9F4)
        "on_site"                 -> SuccessGreen
        "completed"               -> Color(0xFF9C27B0)
        else                      -> OrangeAccent
    }

    val statusLabel = when (tech.status) {
        "on_the_way", "ON_MY_WAY" -> "On the way"
        "on_site"                 -> "On site"
        "completed"               -> "Completed"
        else                      -> "Idle"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = statusColor.copy(alpha = 0.15f),
            modifier = Modifier.size(34.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    shape = CircleShape,
                    color = statusColor,
                    modifier = Modifier.size(10.dp)
                ) {}
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                tech.technicianName?.takeIf { it.isNotBlank() } ?: "Technician",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    tech.buildingName?.takeIf { it.isNotBlank() } ?: "Idle",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(Modifier.width(10.dp))

        Text(
            relativeAge(tech.lastUpdated),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun relativeAge(ts: Long): String {
    val diff = System.currentTimeMillis() - ts
    return when {
        diff < 15_000    -> "just now"
        diff < 60_000    -> "${diff / 1000}s ago"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        else -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ts))
    }
}