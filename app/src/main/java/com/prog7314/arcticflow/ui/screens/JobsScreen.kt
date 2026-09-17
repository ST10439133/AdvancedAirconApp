// app/src/main/java/com/prog7314/arcticflow/ui/screens/JobsScreen.kt
package com.prog7314.arcticflow.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.prog7314.arcticflow.data.entities.Job
import com.prog7314.arcticflow.data.entities.JobStatus
import com.prog7314.arcticflow.navigation.NavManager
import com.prog7314.arcticflow.ui.components.StatsCard
import com.prog7314.arcticflow.viewmodels.QuoteViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobsScreen(
    userId: String,
    navManager: NavManager,
    onBackToDashboard: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: QuoteViewModel = viewModel(
        factory = QuoteViewModel.Factory(ArcticFlowDatabase.getDatabase(context), context)
    )

    val jobs by viewModel.getJobsForTechnician(userId).collectAsState(initial = emptyList())

    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val todayEnd = todayStart + 24L * 60 * 60 * 1000

    val jobsToday = jobs.count { (it.scheduledDate ?: 0L) in todayStart until todayEnd }
    val pending = jobs.count {
        it.status == JobStatus.SCHEDULED || it.status == JobStatus.PENDING ||
                it.status == JobStatus.IN_PROGRESS
    }
    val completed = jobs.count { it.status == JobStatus.COMPLETED }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Jobs") },
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
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatsCard(
                    "Today",
                    jobsToday.toString(),
                    Icons.Default.Today,
                    Modifier.weight(1f)
                )
                StatsCard(
                    "Pending",
                    pending.toString(),
                    Icons.Default.Pending,
                    Modifier.weight(1f),
                    Color(0xFFFF9800)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatsCard(
                    "Completed",
                    completed.toString(),
                    Icons.Default.DoneAll,
                    Modifier.weight(1f),
                    Color(0xFF4CAF50)
                )
                StatsCard(
                    "Total",
                    jobs.size.toString(),
                    Icons.Default.Work,
                    Modifier.weight(1f),
                    Color(0xFF2196F3)
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "All Jobs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            if (jobs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.WorkOff,
                            null,
                            Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No jobs assigned yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Jobs appear here once a quote is accepted.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(jobs, key = { it.id }) { job ->
                        JobItem(job) { navManager.navigateToCreateJobCard(job.id) }
                    }
                }
            }
        }
    }
}

@Composable
fun JobItem(job: Job, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())
    Card(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    job.buildingName.ifBlank { "Unknown Building" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    job.issueType.ifBlank { "No issue specified" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                job.scheduledDate?.let {
                    Text(
                        dateFormat.format(Date(it)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Badge(
                containerColor = when (job.status) {
                    JobStatus.COMPLETED   -> Color(0xFF4CAF50)
                    JobStatus.IN_PROGRESS -> Color(0xFF2196F3)
                    JobStatus.SCHEDULED   -> Color(0xFF03A9F4)
                    JobStatus.PENDING     -> Color(0xFFFF9800)
                    JobStatus.ASSIGNED    -> Color(0xFF9C27B0)
                    JobStatus.CANCELLED   -> Color(0xFF9E9E9E)
                }
            ) {
                Text(
                    job.status.name,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}