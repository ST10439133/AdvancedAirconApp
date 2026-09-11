// app/src/main/java/com/prog7314/arcticflow/ui/screens/ManagerDashboardScreen.kt
package com.prog7314.arcticflow.ui.screens

import androidx.compose.foundation.background
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
import com.prog7314.arcticflow.data.entities.Alert
import com.prog7314.arcticflow.data.entities.AlertSeverity
import com.prog7314.arcticflow.data.entities.DashboardStats
import com.prog7314.arcticflow.data.entities.JobPriority
import com.prog7314.arcticflow.data.entities.JobStatus
import com.prog7314.arcticflow.data.entities.MaintenanceJob
import com.prog7314.arcticflow.data.entities.Technician
import com.prog7314.arcticflow.navigation.NavManager
import com.prog7314.arcticflow.ui.components.AppTopBar
import com.prog7314.arcticflow.viewmodels.ManagerDashboardViewModel
import com.prog7314.arcticflow.viewmodels.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerDashboardScreen(
    viewModel: ManagerDashboardViewModel,
    navManager: NavManager,
    userId: String
) {
    val context = LocalContext.current

    val notificationViewModel: NotificationViewModel = viewModel(
        factory = NotificationViewModel.Factory(context, userId)
    )

    LaunchedEffect(Unit) {
        notificationViewModel.addSampleNotifications()
    }

    val stats by viewModel.stats.collectAsStateWithLifecycle(initialValue = DashboardStats())
    val alerts by viewModel.alerts.collectAsStateWithLifecycle(initialValue = emptyList())
    val jobs by viewModel.maintenanceJobs.collectAsStateWithLifecycle(initialValue = emptyList())
    val technicians by viewModel.technicians.collectAsStateWithLifecycle(initialValue = emptyList())
    val showJobDetails by viewModel.showJobDetails.collectAsStateWithLifecycle(initialValue = false)
    val showAlertDetails by viewModel.showAlertDetails.collectAsStateWithLifecycle(initialValue = false)

    val selectedJobId by viewModel.selectedJobId.collectAsStateWithLifecycle()
    val selectedAlertId by viewModel.selectedAlertId.collectAsStateWithLifecycle()

    val activeBuildings = 14
    val openQuotes = 9
    val scheduled = 28
    val techsOnline = 11

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Manager Dashboard",
                navManager = navManager,
                notificationViewModel = notificationViewModel,
                showBackButton = false,
                actions = {
                    IconButton(onClick = { navManager.navigateToSettings() }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
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
            // Welcome
            item {
                Column {
                    Text(
                        text = "System Administrator",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Jordan Vance",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Overview Stats
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "OVERVIEW",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            OverviewStatItem(activeBuildings.toString(), "Active Buildings")
                            OverviewStatItem(openQuotes.toString(), "Open Quotes")
                            OverviewStatItem(scheduled.toString(), "Scheduled")
                            OverviewStatItem("$techsOnline/15", "Techs Online")
                        }
                    }
                }
            }

            // Monthly Performance
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "MONTHLY PERFORMANCE",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Service Revenue",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold)
                                Text("R84,200",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary)
                                Text("+24% from last month",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Green)
                            }
                            Column {
                                Text("Service Breakdown",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(Modifier.size(40.dp)
                                        .background(Color(0xFF4CAF50),
                                            shape = MaterialTheme.shapes.small))
                                    Box(Modifier.size(40.dp)
                                        .background(Color(0xFF2196F3),
                                            shape = MaterialTheme.shapes.small))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("HVAC 65%", style = MaterialTheme.typography.labelSmall)
                                    Text("Electr. 35%", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            // Recent Operations
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "RECENT OPERATIONS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        RecentOperationItem(
                            icon = Icons.Default.Warning,
                            title = "Emergency HVAC Repair dispatched",
                            time = "9:02 AM"
                        )
                        RecentOperationItem(
                            icon = Icons.Default.Receipt,
                            title = "Quote #Q-1894 accepted by Apex",
                            time = "Yesterday"
                        )
                        RecentOperationItem(
                            icon = Icons.Default.RequestQuote,
                            title = "New quote requested: Thermostat",
                            time = "Yesterday"
                        )
                    }
                }
            }
        }
    }

    if (showJobDetails && selectedJobId != null) {
        val job = jobs.find { it.id == selectedJobId }
        if (job != null) {
            JobDetailsDialog(
                job = job,
                technicians = technicians,
                onAssignTechnician = { techId -> viewModel.assignJobToTechnician(job.id, techId) },
                onUpdateStatus = { status -> viewModel.updateJobStatus(job.id, status) },
                onDismiss = { viewModel.dismissDetails() }
            )
        }
    }

    if (showAlertDetails && selectedAlertId != null) {
        val alert = alerts.find { it.id == selectedAlertId }
        if (alert != null) {
            AlertDetailsDialog(
                alert = alert,
                onResolve = { viewModel.resolveAlert(alert.id) },
                onDismiss = { viewModel.dismissDetails() }
            )
        }
    }
}

@Composable
fun OverviewStatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun RecentOperationItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    time: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp))
            Text(text = title, style = MaterialTheme.typography.bodySmall)
        }
        Text(text = time, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AlertCard(alert: Alert, onResolve: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (alert.severity) {
                AlertSeverity.CRITICAL -> Color.Red.copy(alpha = 0.1f)
                AlertSeverity.HIGH -> Color(0xFFFF9800).copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(alert.buildingName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold)
                Text(alert.issueType,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatTimestamp(alert.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Badge(containerColor = when (alert.severity) {
                    AlertSeverity.CRITICAL -> Color.Red
                    AlertSeverity.HIGH -> Color(0xFFFF9800)
                    AlertSeverity.MEDIUM -> Color.Yellow
                    AlertSeverity.LOW -> Color.Green
                }) { Text(alert.severity.name) }
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = onResolve) { Text("Resolve") }
            }
        }
    }
}

@Composable
fun JobCard(job: MaintenanceJob, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(job.buildingName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold)
                Text("${job.unitNumber} - ${job.issueType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Status: ${job.status.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = when (job.status) {
                        JobStatus.COMPLETED -> Color.Green
                        JobStatus.PENDING -> Color(0xFFFF9800)
                        JobStatus.ASSIGNED -> Color.Blue
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    })
            }
            Column(horizontalAlignment = Alignment.End) {
                Badge(containerColor = when (job.priority) {
                    JobPriority.URGENT -> Color.Red
                    JobPriority.HIGH -> Color(0xFFFF9800)
                    JobPriority.MEDIUM -> Color.Yellow
                    JobPriority.LOW -> Color.Green
                }) { Text(job.priority.name) }
                Text(job.assignedTo?.let { "Assigned: #$it" } ?: "Unassigned",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun JobDetailsDialog(
    job: MaintenanceJob,
    technicians: List<Technician>,
    onAssignTechnician: (String) -> Unit,
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
                if (job.assignedTo == null) {
                    Text("Assign Technician:")
                    technicians.forEach { tech ->
                        TextButton(onClick = { onAssignTechnician(tech.id) }) {
                            Text("${tech.name} (${tech.specialization})")
                        }
                    }
                }
                if (job.status != JobStatus.COMPLETED && job.status != JobStatus.CANCELLED) {
                    Row {
                        listOf(JobStatus.IN_PROGRESS, JobStatus.COMPLETED).forEach { status ->
                            TextButton(onClick = { onUpdateStatus(status) }) {
                                Text("Mark ${status.name}")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun AlertDetailsDialog(alert: Alert, onResolve: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alert Details") },
        text = {
            Column {
                Text("Building: ${alert.buildingName}")
                Text("Issue: ${alert.issueType}")
                Text("Severity: ${alert.severity.name}")
                Text("Status: ${alert.status.name}")
                Text("Time: ${formatTimestamp(alert.timestamp)}")
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onResolve) { Text("Resolve Alert") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

private fun formatTimestamp(timestamp: Long): String {
    val format = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
    return format.format(Date(timestamp))
}