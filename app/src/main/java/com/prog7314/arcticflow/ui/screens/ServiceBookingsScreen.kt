// app/src/main/java/com/prog7314/arcticflow/ui/screens/ServiceBookingsScreen.kt
package com.prog7314.arcticflow.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceBookingsScreen(
    userId: String,
    navManager: NavManager
) {
    val context = LocalContext.current
    val database = ArcticFlowDatabase.getDatabase(context)
    val viewModel: QuoteViewModel = viewModel(
        factory = QuoteViewModel.Factory(database)
    )

    var selectedFilter by remember { mutableStateOf("Today") }
    val filters = listOf("Today", "This Week", "This Month", "All")

    val allJobs by viewModel.getJobsForTechnician(userId)
        .collectAsState(initial = emptyList())

    val filteredJobs = remember(allJobs, selectedFilter) {
        val now = Calendar.getInstance()
        val startOfDay = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val endOfDay = startOfDay + 86_400_000L
        val startOfWeek = (now.clone() as Calendar).apply {
            set(Calendar.DAY_OF_WEEK, now.firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val startOfMonth = (now.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        allJobs.filter { job ->
            val d = job.scheduledDate ?: return@filter false
            when (selectedFilter) {
                "Today" -> d in startOfDay until endOfDay
                "This Week" -> d >= startOfWeek
                "This Month" -> d >= startOfMonth
                else -> true
            }
        }.sortedBy { it.scheduledDate }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Service Bookings") },
                navigationIcon = {
                    IconButton(onClick = { navManager.navigateBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { f ->
                    FilterChip(
                        selected = selectedFilter == f,
                        onClick = { selectedFilter = f },
                        label = { Text(f) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (filteredJobs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No bookings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Jobs assigned to you will appear here", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filteredJobs) { job ->
                        BookingCard(
                            job = job,
                            onCallClick = {
                                Toast.makeText(context, "Calling customer...", Toast.LENGTH_SHORT).show()
                            },
                            onNavigateClick = {
                                val uri = Uri.parse("geo:0,0?q=${Uri.encode(job.buildingName)}")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            },
                            onOpenJobCard = { navManager.navigateToCreateJobCard(job.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BookingCard(
    job: Job,
    onCallClick: () -> Unit,
    onNavigateClick: () -> Unit,
    onOpenJobCard: () -> Unit
) {
    val dateFmt = SimpleDateFormat("EEE, MMM d, h:mm a", Locale.getDefault())
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(job.buildingName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(job.issueType, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    job.scheduledDate?.let {
                        Text(dateFmt.format(Date(it)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Badge(
                    containerColor = when (job.status) {
                        JobStatus.COMPLETED -> Color.Green
                        JobStatus.IN_PROGRESS -> Color.Blue
                        JobStatus.SCHEDULED -> Color(0xFF2196F3)
                        JobStatus.PENDING -> Color(0xFFFF9800)
                        else -> Color.Gray
                    }
                ) { Text(job.status.name) }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onCallClick, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Phone, "Call", Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Call", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(onClick = onNavigateClick, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Navigation, "Navigate", Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Navigate", style = MaterialTheme.typography.labelSmall)
                }
                Button(onClick = onOpenJobCard, modifier = Modifier.weight(1f)) {
                    Text("Job Card", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}