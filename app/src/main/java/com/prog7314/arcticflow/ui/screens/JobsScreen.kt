// app/src/main/java/com/prog7314/arcticflow/ui/screens/JobsScreen.kt
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
import com.prog7314.arcticflow.data.entities.JobStatus
import com.prog7314.arcticflow.navigation.NavManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobsScreen(
    userId: String,
    navManager: NavManager
) {
    // Sample jobs data
    val jobs = listOf(
        SampleJob("JC-1002", "Apex Tech Plaza", "AC Compressor Repair", JobStatus.COMPLETED, "09:00 AM", "1.5 hrs"),
        SampleJob("JC-1003", "Oakwood Medical", "Scheduled HVAC Maintenance", JobStatus.IN_PROGRESS, "11:00 AM", "2.0 hrs"),
        SampleJob("JC-1004", "Riverview Apartments", "Thermostat Calibration", JobStatus.PENDING, "02:30 PM", "1.0 hrs"),
        SampleJob("JC-1005", "Grand Hotel & Suites", "Emergency Repair", JobStatus.PENDING, "04:00 PM", "3.0 hrs")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Jobs") },
                actions = {
                    IconButton(onClick = { /* Refresh */ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatsCard(
                    title = "Today",
                    value = "4",
                    icon = Icons.Default.Today,
                    modifier = Modifier.weight(1f)
                )
                StatsCard(
                    title = "Pending",
                    value = "3",
                    icon = Icons.Default.Pending,
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFFF9800)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatsCard(
                    title = "Completed",
                    value = "12",
                    icon = Icons.Default.DoneAll,
                    modifier = Modifier.weight(1f),
                    color = Color.Green
                )
                StatsCard(
                    title = "Revenue",
                    value = "R84.2k",
                    icon = Icons.Default.AttachMoney,
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF2196F3)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Recent Jobs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(jobs) { job ->
                    JobItem(job = job) {
                        // Navigate to job details or create job card
                        navManager.navigateToCreateJobCard(job.id.toInt())
                    }
                }
            }
        }
    }
}

data class SampleJob(
    val id: String,
    val buildingName: String,
    val title: String,
    val status: JobStatus,
    val time: String,
    val duration: String
)

@Composable
fun JobItem(
    job: SampleJob,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = job.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = job.buildingName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = job.time,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = job.duration,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Badge(
                containerColor = when (job.status) {
                    JobStatus.COMPLETED -> Color.Green
                    JobStatus.IN_PROGRESS -> Color.Blue
                    JobStatus.PENDING -> Color(0xFFFF9800)
                    else -> Color.Gray
                }
            ) {
                Text(job.status.name)
            }
        }
    }
}