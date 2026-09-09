// app/src/main/java/com/prog7314/arcticflow/ui/screens/ServiceBookingsScreen.kt
package com.prog7314.arcticflow.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prog7314.arcticflow.navigation.NavManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceBookingsScreen(
    userId: String,
    navManager: NavManager
) {
    var selectedFilter by remember { mutableStateOf("Today") }
    val filters = listOf("Today", "This Week", "This Month", "All")

    // Sample bookings data
    val bookings = listOf(
        ServiceBooking(
            id = "1",
            buildingName = "Oakwood Medical Center",
            address = "92 Medical Way, Big 8",
            serviceType = "HVAC Inspection",
            dateTime = "Today, 11:30AM",
            status = BookingStatus.CONFIRMED
        ),
        ServiceBooking(
            id = "2",
            buildingName = "Riverview Apartments",
            address = "340 Riverview Dr, Unit 2B",
            serviceType = "Thermostat Calibration",
            dateTime = "Today, 02:30PM",
            status = BookingStatus.CONFIRMED
        ),
        ServiceBooking(
            id = "3",
            buildingName = "Apex Tech Plaza",
            address = "120 Innovation Way",
            serviceType = "Compressor Install",
            dateTime = "Tomorrow, 09:00 AM",
            status = BookingStatus.PENDING
        ),
        ServiceBooking(
            id = "4",
            buildingName = "Grand Hotel & Suites",
            address = "777 Broad St, Main Lobby",
            serviceType = "Heating System Check",
            dateTime = "Fri Oct 27, 04:00 PM",
            status = BookingStatus.CANCELLED
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Service Bookings") },
                navigationIcon = {
                    IconButton(onClick = { navManager.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Filter chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(bookings) { booking ->
                    BookingCard(
                        booking = booking,
                        onCallClick = { /* Call customer */ },
                        onNavigateClick = { /* Navigate to location */ }
                    )
                }
            }
        }
    }
}

data class ServiceBooking(
    val id: String,
    val buildingName: String,
    val address: String,
    val serviceType: String,
    val dateTime: String,
    val status: BookingStatus
)

enum class BookingStatus {
    CONFIRMED, PENDING, CANCELLED
}

@Composable
fun BookingCard(
    booking: ServiceBooking,
    onCallClick: () -> Unit,
    onNavigateClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = booking.buildingName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = booking.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = booking.serviceType,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = booking.dateTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Badge(
                    containerColor = when (booking.status) {
                        BookingStatus.CONFIRMED -> Color.Green
                        BookingStatus.PENDING -> Color(0xFFFF9800)
                        BookingStatus.CANCELLED -> Color.Red
                    }
                ) {
                    Text(booking.status.name)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCallClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Call")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call Customer")
                }
                OutlinedButton(
                    onClick = onNavigateClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = "Navigate")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Navigate")
                }
            }
        }
    }
}