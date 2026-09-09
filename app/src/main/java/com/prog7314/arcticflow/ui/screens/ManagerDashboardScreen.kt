// app/src/main/java/com/prog7314/arcticflow/ui/screens/ManagerDashboardScreen.kt
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
import androidx.compose.ui.unit.sp
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
        factory = NotificationViewModel.Factory(context, userId)  // <-- FIX THIS LINE
    )

    // Load sample notifications on first launch
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

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Manager Dashboard",
                navManager = navManager,
                notificationViewModel = notificationViewModel,
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
            // Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        StatsCard(
                            title = "Buildings",
                            value = stats.totalBuildings.toString(),
                            icon = Icons.Default.Business
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        StatsCard(
                            title = "Units",
                            value = stats.totalUnits.toString(),
                            icon = Icons.Default.Devices
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        StatsCard(
                            title = "Alerts",
                            value = stats.activeAlerts.toString(),
                            icon = Icons.Default.Warning,
                            color = if (stats.activeAlerts > 0) Color.Red else Color.Green
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        StatsCard(
                            title = "Pending Jobs",
                            value = stats.pendingMaintenance.toString(),
                            icon = Icons.Default.Construction,
                            color = if (stats.pendingMaintenance > 0) Color(0xFFFF9800) else Color.Green
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        StatsCard(
                            title = "Completed",
                            value = stats.completedJobs.toString(),
                            icon = Icons.Default.CheckCircle,
                            color = Color.Green
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        StatsCard(
                            title = "Technicians",
                            value = stats.technicianCount.toString(),
                            icon = Icons.Default.People
                        )
                    }
                }
            }

            if (alerts.isNotEmpty()) {
                item {
                    Text(
                        text = "Active Alerts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(alerts) { alert ->
                    AlertCard(
                        alert = alert,
                        onResolve = { viewModel.resolveAlert(alert.id) }
                    )
                }
            }

            if (jobs.isNotEmpty()) {
                item {
                    Text(
                        text = "Maintenance Jobs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(jobs.take(5)) { job ->
                    JobCard(
                        job = job,
                        onClick = { viewModel.selectJob(job.id) }
                    )
                }
            }

            // Quick Actions - ADDED MORE OPTIONS
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

                        // First row - 4 buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickActionButton(
                                icon = Icons.Default.Add,
                                label = "View Quotes",
                                onClick = { navManager.navigateToMyQuotes() }
                            )
                            QuickActionButton(
                                icon = Icons.Default.SupportAgent,
                                label = "Create Quote",
                                onClick = {
                                    navManager.navigateToPendingRequests()
                                }
                            )
                            QuickActionButton(
                                icon = Icons.Default.People,
                                label = "Manage Techs",
                                onClick = { /* Navigate to technicians */ }
                            )
                            QuickActionButton(
                                icon = Icons.Default.Analytics,
                                label = "Reports",
                                onClick = { /* Navigate to reports */ }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Second row - 3 buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickActionButton(
                                icon = Icons.Default.Business,
                                label = "Buildings",
                                onClick = { navManager.navigateToAddBuilding() }
                            )
                            QuickActionButton(
                                icon = Icons.Default.Build,
                                label = "Service Request",
                                onClick = { navManager.navigateToServiceRequest() }
                            )
                            QuickActionButton(
                                icon = Icons.Default.Receipt,
                                label = "My Jobs",
                                onClick = { /* Navigate to jobs */ }
                            )
                        }
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
fun StatsCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun AlertCard(
    alert: Alert,
    onResolve: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Show details */ },
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = alert.buildingName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = alert.issueType,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatTimestamp(alert.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Badge(
                    containerColor = when (alert.severity) {
                        AlertSeverity.CRITICAL -> Color.Red
                        AlertSeverity.HIGH -> Color(0xFFFF9800)
                        AlertSeverity.MEDIUM -> Color.Yellow
                        AlertSeverity.LOW -> Color.Green
                    }
                ) {
                    Text(alert.severity.name)
                }
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = onResolve) {
                    Text("Resolve")
                }
            }
        }
    }
}

@Composable
fun JobCard(
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
                        JobStatus.PENDING -> Color(0xFFFF9800)
                        JobStatus.ASSIGNED -> Color.Blue
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Column(horizontalAlignment = Alignment.End) {
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
                Text(
                    text = job.assignedTo?.let { "Assigned: #$it" } ?: "Unassigned",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(label, fontSize = 10.sp)
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
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun AlertDetailsDialog(
    alert: Alert,
    onResolve: () -> Unit,
    onDismiss: () -> Unit
) {
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
                TextButton(onClick = onResolve) {
                    Text("Resolve Alert")
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

private fun formatTimestamp(timestamp: Long): String {
    val format = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
    return format.format(Date(timestamp))
}