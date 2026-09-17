// app/src/main/java/com/prog7314/arcticflow/ui/screens/TechnicianDashboardScreen.kt
package com.prog7314.arcticflow.ui.screens

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prog7314.arcticflow.data.ArcticFlowDatabase
import com.prog7314.arcticflow.data.entities.JobStatus
import com.prog7314.arcticflow.data.entities.QuoteStatus
import com.prog7314.arcticflow.navigation.NavManager
import com.prog7314.arcticflow.ui.components.AppTopBar
import com.prog7314.arcticflow.ui.components.StatsCard
import com.prog7314.arcticflow.viewmodels.NotificationViewModel
import com.prog7314.arcticflow.viewmodels.QuoteViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechnicianDashboardScreen(
    navManager: NavManager,
    userId: String
) {
    val context = LocalContext.current
    val viewModel: QuoteViewModel = viewModel(
        factory = QuoteViewModel.Factory(ArcticFlowDatabase.getDatabase(context), context)
    )
    val notificationViewModel: NotificationViewModel = viewModel(
        factory = NotificationViewModel.Factory(context, userId)
    )

    LaunchedEffect(Unit) { notificationViewModel.addSampleNotifications() }

    val myJobs by viewModel.getJobsForTechnician(userId).collectAsState(initial = emptyList())
    val myQuotes by viewModel.getQuotesForTechnician(userId).collectAsState(initial = emptyList())
    val pendingRequests by viewModel.getPendingServiceRequests().collectAsState(initial = emptyList())

    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val todayEnd = todayStart + 24L * 60 * 60 * 1000

    val jobsToday = myJobs.count { (it.scheduledDate ?: 0L) in todayStart until todayEnd }
    val pendingJobs = myJobs.count {
        it.status == JobStatus.SCHEDULED ||
                it.status == JobStatus.PENDING ||
                it.status == JobStatus.IN_PROGRESS
    }
    val completedJobs = myJobs.count { it.status == JobStatus.COMPLETED }
    val pendingQuotes = myQuotes.count { it.status == QuoteStatus.PENDING }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Technician Dashboard",
                navManager = navManager,
                notificationViewModel = notificationViewModel,
                actions = {
                    IconButton(onClick = { navManager.navigateToSettings() }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(
                        "Welcome back,",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Technician",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatsCard(
                        "Today",
                        jobsToday.toString(),
                        Icons.Default.Today,
                        Modifier.weight(1f)
                    )
                    StatsCard(
                        "Pending Jobs",
                        pendingJobs.toString(),
                        Icons.Default.Pending,
                        Modifier.weight(1f),
                        Color(0xFFFF9800)
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatsCard(
                        "Completed",
                        completedJobs.toString(),
                        Icons.Default.DoneAll,
                        Modifier.weight(1f),
                        Color(0xFF4CAF50)
                    )
                    StatsCard(
                        "Pending Quotes",
                        pendingQuotes.toString(),
                        Icons.Default.Receipt,
                        Modifier.weight(1f),
                        Color(0xFF2196F3)
                    )
                }
            }

            if (pendingRequests.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.NotificationsActive, null)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${pendingRequests.size} new job request(s)",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Tap to review and send quotes",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Button(onClick = { navManager.navigateToPendingRequests() }) {
                                Text("View")
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "Today's Schedule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            val todayJobs = myJobs.filter {
                (it.scheduledDate ?: 0L) in todayStart until todayEnd
            }

            if (todayJobs.isEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.EventAvailable,
                                null,
                                Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No jobs today",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(todayJobs, key = { it.id }) { job ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                job.buildingName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                job.issueType,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            job.scheduledDate?.let {
                                Text(
                                    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(it)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}