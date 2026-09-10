// app/src/main/java/com/prog7314/arcticflow/ui/screens/TechnicianDashboardScreen.kt
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prog7314.arcticflow.data.entities.DashboardStats
import com.prog7314.arcticflow.data.entities.JobPriority
import com.prog7314.arcticflow.data.entities.JobStatus
import com.prog7314.arcticflow.data.entities.MaintenanceJob
import com.prog7314.arcticflow.navigation.NavManager
import com.prog7314.arcticflow.ui.components.AppTopBar
import com.prog7314.arcticflow.viewmodels.NotificationViewModel
import com.prog7314.arcticflow.viewmodels.TechnicianDashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechnicianDashboardScreen(
    viewModel: TechnicianDashboardViewModel,
    navManager: NavManager,
    userId: String
) {
    val context = LocalContext.current

    val notificationViewModel: NotificationViewModel = viewModel(
        factory = NotificationViewModel.Factory(context, userId)
    )

    // Load sample notifications on first launch
    LaunchedEffect(Unit) {
        notificationViewModel.addSampleNotifications()
    }

    val myJobs by viewModel.myJobs.collectAsStateWithLifecycle(initialValue = emptyList())
    val myStats by viewModel.myStats.collectAsStateWithLifecycle(initialValue = DashboardStats())
    val isAvailable by viewModel.isAvailable.collectAsStateWithLifecycle(initialValue = true)
    val showJobDetails by viewModel.showJobDetails.collectAsStateWithLifecycle(initialValue = false)

    val selectedJobId by viewModel.selectedJobId.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "My Dashboard",
                navManager = navManager,
                notificationViewModel = notificationViewModel,
                actions = {
                    IconButton(onClick = { navManager.navigateToSettings() }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = { viewModel.toggleAvailability() }) {
                        Icon(
                            if (isAvailable) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = if (isAvailable) "Available" else "Unavailable",
                            tint = if (isAvailable) Color.Green else Color.Red
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Technician Status
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAvailable) Color.Green.copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Status",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isAvailable) "Available for Jobs" else "Unavailable",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isAvailable) Color.Green else Color.Red
                            )
                        }
                        Switch(
                            checked = isAvailable,
                            onCheckedChange = { viewModel.toggleAvailability() }
                        )
                    }
                }
            }

            // Stats
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatsCard(
                        title = "My Jobs",
                        value = myStats.totalUnits.toString(),
                        icon = Icons.Default.Work,
                        modifier = Modifier.weight(1f)
                    )
                    StatsCard(
                        title = "Pending",
                        value = myStats.pendingMaintenance.toString(),
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
                        value = myStats.completedJobs.toString(),
                        icon = Icons.Default.DoneAll,
                        modifier = Modifier.weight(1f),
                        color = Color.Green
                    )
                    StatsCard(
                        title = "Buildings",
                        value = myStats.totalBuildings.toString(),
                        icon = Icons.Default.Business,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // My Jobs
            item {
                Text(
                    text = "My Assigned Jobs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (myJobs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Work,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No jobs assigned",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "You're all caught up!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(myJobs) { job ->
                    TechnicianJobCard(
                        job = job,
                        onClick = { viewModel.selectJob(job.id) }
                    )
                }
            }

            // Quick Actions for Technician - UPDATED
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Quick Actions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickActionButton(
                                icon = Icons.Default.RequestQuote,
                                label = "View Quotes",
                                onClick = { navManager.navigateToMyQuotes() }
                            )
                            QuickActionButton(
                                icon = Icons.Default.Pending,
                                label = "Pending Requests",
                                onClick = { navManager.navigateToPendingRequests() }
                            )
                            QuickActionButton(
                                icon = Icons.Default.Build,
                                label = "BTU Calculator",
                                onClick = { navManager.navigateToBTUCalculator() }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showJobDetails && selectedJobId != null) {
        val job = myJobs.find { it.id == selectedJobId }
        if (job != null) {
            TechnicianJobDetailsDialog(
                job = job,
                onUpdateStatus = { status -> viewModel.updateJobStatus(job.id, status) },
                onDismiss = { viewModel.dismissDetails() }
            )
        }
    }
}

@Composable
fun TechnicianJobCard(
    job: MaintenanceJob,
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
                    text = job.buildingName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${job.unitNumber} - ${job.issueType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Status: ${job.status.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = when (job.status) {
                        JobStatus.COMPLETED -> Color.Green
                        JobStatus.IN_PROGRESS -> Color.Blue
                        else -> Color(0xFFFF9800)
                    }
                )
                job.scheduledDate?.let {
                    Text(
                        text = "Scheduled: ${formatDate(it)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Badge(
                containerColor = when (job.priority) {
                    JobPriority.URGENT -> Color.Red
                    JobPriority.HIGH -> Color(0xFFFF9800)
                    JobPriority.MEDIUM -> Color.Yellow
                    JobPriority.LOW -> Color.Green
                }
            ) {
                Text(job.priority.name)
            }
        }
    }
}

@Composable
fun TechnicianJobDetailsDialog(
    job: MaintenanceJob,
    onUpdateStatus: (JobStatus) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Job Details") },
        text = {
            Column {
                Text("Building: ${job.buildingName}")
                Text("Unit: ${job.unitNumber}")
                Text("Issue: ${job.issueType}")
                Text("Priority: ${job.priority.name}")
                Text("Status: ${job.status.name}")
                Text("Description: ${job.description}")
                Spacer(modifier = Modifier.height(8.dp))
                job.scheduledDate?.let {
                    Text("Scheduled: ${formatDate(it)}")
                }

                if (job.status != JobStatus.COMPLETED && job.status != JobStatus.CANCELLED) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Update Status:")
                    Row {
                        listOf(
                            JobStatus.IN_PROGRESS,
                            JobStatus.COMPLETED
                        ).filter { it != job.status }.forEach { status ->
                            TextButton(onClick = { onUpdateStatus(status) }) {
                                Text("Mark ${status.name}")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private fun formatDate(timestamp: Long): String {
    val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return format.format(Date(timestamp))
}