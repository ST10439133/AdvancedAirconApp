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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.prog7314.arcticflow.data.ArcticFlowDatabase
import com.prog7314.arcticflow.data.entities.TechLocation
import com.prog7314.arcticflow.navigation.NavManager
import com.prog7314.arcticflow.viewmodels.ManagerTrackingViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldTrackingScreen(
    navManager: NavManager,
    managerId: String
) {
    val context = LocalContext.current
    val database = ArcticFlowDatabase.getDatabase(context)

    val viewModel: ManagerTrackingViewModel = viewModel(
        factory = ManagerTrackingViewModel.Factory(database, managerId)
    )

    val activeLocations by viewModel.activeLocations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Default camera position (Johannesburg as fallback)
    val defaultPosition = LatLng(-26.2041, 28.0473)

    // If we have techs, center the map on the first one
    val initialPosition = remember(activeLocations) {
        if (activeLocations.isNotEmpty()) {
            LatLng(
                activeLocations.first().latitude,
                activeLocations.first().longitude
            )
        } else defaultPosition
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, 12f)
    }

    // Recenter when first tech appears
    LaunchedEffect(activeLocations) {
        if (activeLocations.isNotEmpty()) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                LatLng(
                    activeLocations.first().latitude,
                    activeLocations.first().longitude
                ),
                12f
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Technician Tracking") },
                actions = {
                    IconButton(onClick = { /* ViewModel auto-refreshes */ }) {
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
                // Add a marker for each tracking technician
                activeLocations.forEach { location ->
                    Marker(
                        state = MarkerState(
                            position = LatLng(location.latitude, location.longitude)
                        ),
                        title = location.technicianName.ifBlank { "Technician" },
                        snippet = "${location.buildingName} • ${location.status}",
                        icon = com.google.android.gms.maps.model.BitmapDescriptorFactory
                            .defaultMarker(
                                when (location.status) {
                                    "on_the_way" -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE
                                    "on_site"     -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN
                                    "completed"   -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_VIOLET
                                    else          -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_ORANGE
                                }
                            )
                    )
                }
            }

            // ============ LOADING OVERLAY ============
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }

            // ============ DEBUG STATUS (temporary — remove in production) ============
            val debugMsg by viewModel.debugMessage.collectAsState()
            if (debugMsg.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.7f)
                    )
                ) {
                    Text(
                        debugMsg,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // ============ EMPTY STATE OVERLAY ============
            if (!isLoading && activeLocations.isEmpty()) {
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
            if (activeLocations.isNotEmpty()) {
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
                        Icon(Icons.Default.DirectionsCar, null,
                            tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${activeLocations.size} active",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // ============ BOTTOM SHEET WITH TECH LIST ============
            if (activeLocations.isNotEmpty()) {
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

                        activeLocations.take(3).forEach { location ->
                            TechRow(location)
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TechRow(location: TechLocation) {
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
                    when (location.status) {
                        "on_the_way" -> Color(0xFF03A9F4)
                        "on_site"     -> Color(0xFF4CAF50)
                        "completed"   -> Color(0xFF9C27B0)
                        else          -> Color(0xFFFF9800)
                    },
                    shape = RoundedCornerShape(50)
                )
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                location.technicianName.ifBlank { "Technician" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                location.buildingName.ifBlank { "Idle" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            df.format(Date(location.lastUpdated)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}