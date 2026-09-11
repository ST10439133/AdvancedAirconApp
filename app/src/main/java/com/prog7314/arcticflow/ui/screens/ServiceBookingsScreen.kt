package com.prog7314.arcticflow.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Phone
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
import com.prog7314.arcticflow.viewmodels.QuoteViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceBookingsScreen(userId: String, navManager: NavManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: QuoteViewModel = viewModel(
        factory = QuoteViewModel.Factory(ArcticFlowDatabase.getDatabase(context))
    )

    val jobs by viewModel.getJobsForTechnician(userId).collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Service Bookings") },
                navigationIcon = {
                    IconButton(onClick = { navManager.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (jobs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.EventBusy, null, Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("No bookings",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Accepted jobs will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(jobs, key = { it.id }) { job ->
                    BookingCard(
                        job = job,
                        onCall = { Toast.makeText(context, "Calling customer...", Toast.LENGTH_SHORT).show() },
                        onMap = {
                            val uri = Uri.parse("geo:0,0?q=${Uri.encode(job.buildingName)}")
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        },
                        onOnMyWay = {
                            scope.launch {
                                viewModel.setTechnicianOnWay(job.id, !job.technicianOnWay)
                                Toast.makeText(context,
                                    if (!job.technicianOnWay) "Customer notified — on the way!"
                                    else "Tracking stopped",
                                    Toast.LENGTH_SHORT).show()
                            }
                        },
                        onOpenJobCard = { navManager.navigateToCreateJobCard(job.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun BookingCard(
    job: Job,
    onCall: () -> Unit,
    onMap: () -> Unit,
    onOnMyWay: () -> Unit,
    onOpenJobCard: () -> Unit
) {
    val dateFormat = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())
    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(job.buildingName, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Text(job.issueType, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary)
                    job.scheduledDate?.let {
                        Text(dateFormat.format(Date(it)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Badge(containerColor = when (job.status) {
                    JobStatus.COMPLETED -> Color(0xFF4CAF50)
                    JobStatus.IN_PROGRESS -> Color(0xFF2196F3)
                    JobStatus.SCHEDULED -> Color(0xFF03A9F4)
                    else -> Color(0xFFFF9800)
                }) { Text(job.status.name, color = Color.White) }
            }

            Spacer(Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (job.technicianOnWay)
                        Color(0xFF4CAF50).copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsCar, null,
                        tint = if (job.technicianOnWay) Color(0xFF4CAF50)
                        else MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (job.technicianOnWay) "Customer can track you" else "Not tracking yet",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold)
                        Text(if (job.technicianOnWay) "Tap to stop tracking" else "Tap when you depart",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = job.technicianOnWay, onCheckedChange = { onOnMyWay() })
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCall, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Phone, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Call", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(onClick = onMap, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Navigation, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Map", style = MaterialTheme.typography.labelSmall)
                }
                Button(onClick = onOpenJobCard, modifier = Modifier.weight(1f)) {
                    Text("Job Card", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}