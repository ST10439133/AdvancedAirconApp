// app/src/main/java/com/prog7314/arcticflow/ui/screens/FieldTrackingScreen.kt
package com.prog7314.arcticflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.prog7314.arcticflow.data.api.TechLocationDto          // 🔽 API SYNC
import com.prog7314.arcticflow.navigation.NavManager
import com.prog7314.arcticflow.viewmodels.ManagerTrackingViewModel // 🔽 API SYNC
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldTrackingScreen(
    navManager: NavManager,
    managerId: String               // kept for signature compatibility — API doesn't need it
) {
    val context = LocalContext.current

    // 🔽 API SYNC — new polling ViewModel (no Room, no Firestore)
    val viewModel: ManagerTrackingViewModel = viewModel()

    // 🔽 API SYNC — `technicians` comes from GET /api/locations via the ViewModel
    val technicians by viewModel.technicians.collectAsStateWithLifecycle()
    val isLoading by viewModel.isPolling.collectAsStateWithLifecycle()

    // Start/stop polling tied to this screen's lifecycle
    LaunchedEffect(Unit) {
        viewModel.startPolling(customerId = null)   // manager sees ALL technicians
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.stopPolling() }
    }

    // Default camera position (Johannesburg as fallback)
    val defaultPosition = LatLng(-26.2041, 28.0473)

    val initialPosition = remember(technicians) {
        if (technicians.isNotEmpty()) {
            LatLng(technicians.first().latitude, technicians.first().longitude)
        } else defaultPosition
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, 12f)
    }

    // Recenter when first tech appears
    LaunchedEffect(technicians.size) {
        if (technicians.isNotEmpty()) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                LatLng(technicians.first().latitude, technicians.first().longitude),
                12f
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Technician Tracking") },
                actions = {
                    IconButton(onClick = { /* ViewModel auto-refreshes every 5s */ }) {
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
            // ============ GOOGLE MAP ============
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = false),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    myLocationButtonEnabled = false
                )
            ) {
                // 🔽 API SYNC — iterate technicians from the REST API
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
                        },
                        icon = com.google.android.gms.maps.model.BitmapDescriptorFactory
                            .defaultMarker(
                                when (tech.status) {
                                    "on_the_way", "ON_MY_WAY" -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE
                                    "on_site"                 -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN
                                    "completed"               -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_VIOLET
                                    else                      -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_ORANGE
                                }
                            )
                    )
                }
            }

            // ============ LOADING OVERLAY ============
            if (isLoading && technicians.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }

            // ============ EMPTY STATE OVERLAY ============
            if (!isLoading && technicians.isEmpty()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    ),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
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
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            // ============ TECH COUNT BADGE (top-right) ============
            if (technicians.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.DirectionsCar, null,
                            tint = Color.White, modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${technicians.size} active",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // ============ BOTTOM SHEET WITH TECH LIST ============
            if (technicians.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "On-Duty Technicians",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))

                        technicians.take(3).forEach { tech ->
                            TechRow(tech)
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// Row item — takes TechLocationDto (API) instead of TechLocation (Firestore)
// ============================================================
@Composable
private fun TechRow(tech: TechLocationDto) {
    val df = SimpleDateFormat("h:mm a", Locale.getDefault())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(
                    when (tech.status) {
                        "on_the_way", "ON_MY_WAY" -> Color(0xFF03A9F4)
                        "on_site"                 -> Color(0xFF4CAF50)
                        "completed"               -> Color(0xFF9C27B0)
                        else                      -> Color(0xFFFF9800)
                    },
                    shape = RoundedCornerShape(50)
                )
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                tech.technicianName?.takeIf { it.isNotBlank() } ?: "Technician",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                tech.buildingName?.takeIf { it.isNotBlank() } ?: "Idle",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            df.format(Date(tech.lastUpdated)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}