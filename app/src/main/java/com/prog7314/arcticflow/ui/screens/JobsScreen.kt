// app/src/main/java/com/prog7314/arcticflow/ui/screens/JobsScreen.kt
package com.prog7314.arcticflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prog7314.arcticflow.data.ArcticFlowDatabase
import com.prog7314.arcticflow.data.entities.Job
import com.prog7314.arcticflow.data.entities.JobStatus
import com.prog7314.arcticflow.navigation.NavManager
import com.prog7314.arcticflow.viewmodels.QuoteViewModel
import java.text.SimpleDateFormat
import java.util.*

// ============================================================
// Filter options for the jobs list
// ============================================================
private enum class JobFilter(val label: String) {
    ALL("All"),
    TODAY("Today"),
    UPCOMING("Upcoming"),
    COMPLETED("Done")
}

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

    // ---- Time anchors ----
    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val todayEnd = todayStart + 24L * 60 * 60 * 1000

    // ---- Stats ----
    val jobsToday = jobs.count { (it.scheduledDate ?: 0L) in todayStart until todayEnd }
    val pendingJobs = jobs.count {
        it.status == JobStatus.SCHEDULED ||
                it.status == JobStatus.PENDING ||
                it.status == JobStatus.IN_PROGRESS ||
                it.status == JobStatus.ASSIGNED
    }
    val completedJobs = jobs.count { it.status == JobStatus.COMPLETED }

    // ---- Filter ----
    var filter by remember { mutableStateOf(JobFilter.ALL) }

    val filteredJobs = remember(jobs, filter) {
        jobs
            .filter { job ->
                when (filter) {
                    JobFilter.ALL       -> true
                    JobFilter.TODAY     ->
                        (job.scheduledDate ?: 0L) in todayStart until todayEnd
                    JobFilter.UPCOMING  ->
                        (job.scheduledDate ?: 0L) >= todayEnd &&
                                job.status != JobStatus.COMPLETED &&
                                job.status != JobStatus.CANCELLED
                    JobFilter.COMPLETED -> job.status == JobStatus.COMPLETED
                }
            }
            .sortedWith(
                compareBy(
                    { it.status == JobStatus.COMPLETED },
                    { it.scheduledDate ?: Long.MAX_VALUE }
                )
            )
    }

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---- Hero summary card ----
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF1B9AAA),
                                    Color(0xFF147D8A)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            "Today at a glance",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (jobsToday > 0) "$jobsToday job${if (jobsToday == 1) "" else "s"} scheduled"
                            else "No jobs scheduled today",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HeroStat("Today", jobsToday.toString(), Modifier.weight(1f))
                            HeroStat("Pending", pendingJobs.toString(), Modifier.weight(1f))
                            HeroStat("Done", completedJobs.toString(), Modifier.weight(1f))
                        }
                    }
                }
            }

            // ---- Filter chips (horizontally scrollable) ----
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    JobFilter.values().forEach { f ->
                        val selected = filter == f
                        FilterChip(
                            selected = selected,
                            onClick = { filter = f },
                            label = {
                                Text(
                                    f.label,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        null,
                                        Modifier.size(16.dp)
                                    )
                                }
                            } else null
                        )
                    }
                }
            }

            // ---- Section header ----
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Jobs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${filteredJobs.size} result${if (filteredJobs.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ---- List or empty state ----
            if (filteredJobs.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.WorkOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                when (filter) {
                                    JobFilter.ALL       -> "No jobs yet"
                                    JobFilter.TODAY     -> "No jobs today"
                                    JobFilter.UPCOMING  -> "No upcoming jobs"
                                    JobFilter.COMPLETED -> "No completed jobs"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                when (filter) {
                                    JobFilter.ALL       -> "Jobs will appear here once a quote is accepted."
                                    JobFilter.TODAY     -> "Enjoy the quiet — nothing scheduled."
                                    JobFilter.UPCOMING  -> "You're all caught up for now."
                                    JobFilter.COMPLETED -> "Completed jobs will be archived here."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredJobs, key = { it.id }) { job ->
                    JobCard(
                        job = job,
                        onClick = { navManager.navigateToCreateJobCard(job.id) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

// ============================================================
// Hero stat cell inside the summary card
// ============================================================
@Composable
private fun HeroStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}

// ============================================================
// Job card with status stripe + details
// ============================================================
@Composable
fun JobCard(
    job: Job,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = job.status.color()
    val dateFormat = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left status stripe
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(statusColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Title + badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        job.buildingName.ifBlank { "Unknown Building" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    StatusBadge(job.status)
                }

                // Issue type
                if (job.issueType.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        job.issueType,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Address
                if (job.fullAddress.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            job.fullAddress,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Scheduled date (relative)
                job.scheduledDate?.let { ts ->
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            relativeDateLabel(ts, dateFormat),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // CTA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (job.status == JobStatus.COMPLETED) "View job card"
                        else "Open job card",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ============================================================
// Status badge with soft-colored background
// ============================================================
@Composable
private fun StatusBadge(status: JobStatus) {
    val c = status.color()
    Surface(
        color = c.copy(alpha = 0.15f),
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(c)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                status.displayName(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = c
            )
        }
    }
}

// ============================================================
// Helpers
// ============================================================

private fun JobStatus.color(): Color = when (this) {
    JobStatus.COMPLETED   -> Color(0xFF4CAF50)
    JobStatus.IN_PROGRESS -> Color(0xFF2196F3)
    JobStatus.SCHEDULED   -> Color(0xFF03A9F4)
    JobStatus.PENDING     -> Color(0xFFFF9800)
    JobStatus.ASSIGNED    -> Color(0xFF9C27B0)
    JobStatus.CANCELLED   -> Color(0xFF9E9E9E)
}

private fun JobStatus.displayName(): String = when (this) {
    JobStatus.SCHEDULED   -> "Scheduled"
    JobStatus.IN_PROGRESS -> "In Progress"
    JobStatus.COMPLETED   -> "Completed"
    JobStatus.PENDING     -> "Pending"
    JobStatus.ASSIGNED    -> "Assigned"
    JobStatus.CANCELLED   -> "Cancelled"
}

/**
 * Renders "Today at 3:00 PM", "Tomorrow at 9:00 AM", "Yesterday at …",
 * or falls back to the absolute date for anything further away.
 */
private fun relativeDateLabel(ts: Long, fallbackFormat: SimpleDateFormat): String {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = ts }

    val sameYear = now.get(Calendar.YEAR) == target.get(Calendar.YEAR)
    val sameDay = sameYear &&
            now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

    val tomorrow = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
    val isTomorrow = sameYear &&
            tomorrow.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

    val yesterday = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    val isYesterday = sameYear &&
            yesterday.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

    val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
    val timeStr = timeFmt.format(Date(ts))

    return when {
        sameDay     -> "Today at $timeStr"
        isTomorrow  -> "Tomorrow at $timeStr"
        isYesterday -> "Yesterday at $timeStr"
        else        -> fallbackFormat.format(Date(ts))
    }
}