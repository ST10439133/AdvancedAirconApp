// app/src/main/java/com/insy7315/advancedairconapp/ui/screens/JobsScreen.kt
package com.insy7315.advancedairconapp.ui.screens

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
import com.insy7315.advancedairconapp.data.ArcticFlowDatabase
import com.insy7315.advancedairconapp.data.entities.Job
import com.insy7315.advancedairconapp.data.entities.JobStatus
import com.insy7315.advancedairconapp.navigation.NavManager
import com.insy7315.advancedairconapp.viewmodels.QuoteViewModel
import java.text.SimpleDateFormat
import java.util.*

// BRAND TOKENS
private val BabyBlue     = Color(0xFF1F3A5F)
private val BabyBlueDeep = Color(0xFF152A47)
private val BabyBlueSoft = Color(0xFFE8EDF3)
private val OrangeAccent = Color(0xFFC8102E)

// Filter options
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

    val jobs: List<Job> by viewModel
        .getJobsForTechnician(userId)
        .collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        viewModel.refreshFromServer("TECHNICIAN")
    }

    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val todayEnd = todayStart + 24L * 60 * 60 * 1000

    val jobsToday = jobs.count { (it.scheduledDate ?: 0L) in todayStart until todayEnd }
    val pendingJobs = jobs.count {
        it.status == JobStatus.SCHEDULED ||
                it.status == JobStatus.PENDING ||
                it.status == JobStatus.IN_PROGRESS ||
                it.status == JobStatus.ASSIGNED
    }
    val completedJobs = jobs.count { it.status == JobStatus.COMPLETED }

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
            // ---- Hero summary ----
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(BabyBlue, BabyBlueDeep)))
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

            // Filter chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    JobFilter.entries.forEach { f ->
                        val selected = filter == f
                        FilterChip(
                            selected = selected,
                            onClick = { filter = f },
                            label = { Text(f.label, maxLines = 1, softWrap = false) },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
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

            // ---- List / empty ----
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
                                color = BabyBlueSoft,
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.WorkOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp),
                                        tint = BabyBlueDeep
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

// Hero stat cell
@Composable
private fun HeroStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.18f))
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

// Job card
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
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Status stripe
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

                if (job.issueType.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        job.issueType,
                        style = MaterialTheme.typography.bodyMedium,
                        color = BabyBlueDeep,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (job.status == JobStatus.COMPLETED) "View job card"
                        else "Open job card",
                        style = MaterialTheme.typography.labelLarge,
                        color = BabyBlueDeep,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = BabyBlueDeep,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// Status badge
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

// Helpers

private fun JobStatus.color(): Color = when (this) {
    JobStatus.COMPLETED   -> Color(0xFF2E7D32)
    JobStatus.IN_PROGRESS -> Color(0xFF1F3A5F)
    JobStatus.SCHEDULED   -> Color(0xFF152A47)
    JobStatus.PENDING     -> Color(0xFFC8102E)
    JobStatus.ASSIGNED    -> Color(0xFF8B1E20)
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