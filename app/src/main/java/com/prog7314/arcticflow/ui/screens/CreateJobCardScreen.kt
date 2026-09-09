// app/src/main/java/com/prog7314/arcticflow/ui/screens/CreateJobCardScreen.kt
package com.prog7314.arcticflow.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prog7314.arcticflow.data.ArcticFlowDatabase
import com.prog7314.arcticflow.data.entities.Job
import com.prog7314.arcticflow.data.entities.PartItem
import com.prog7314.arcticflow.navigation.NavManager
import com.prog7314.arcticflow.viewmodels.JobCardViewModel
import androidx.compose.ui.text.font.FontWeight
import com.prog7314.arcticflow.data.entities.JobStatus
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateJobCardScreen(
    jobId: Int,
    navManager: NavManager
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val database = ArcticFlowDatabase.getDatabase(context)
    val viewModel: JobCardViewModel = viewModel(
        factory = JobCardViewModel.Factory(database)
    )

    var currentJob by remember { mutableStateOf<Job?>(null) }
    var workSummary by remember { mutableStateOf("") }
    var additionalNotes by remember { mutableStateOf("") }
    var parts by remember { mutableStateOf(listOf(PartItem())) }
    var startTime by remember { mutableStateOf<Long?>(null) }
    var endTime by remember { mutableStateOf<Long?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Load job details
    LaunchedEffect(jobId) {
        currentJob = viewModel.getJobById(jobId)

        // Check if job card already exists
        val existingJobCard = viewModel.getJobCardByJobId(jobId)
        existingJobCard?.let { jobCard ->
            workSummary = jobCard.workSummary
            additionalNotes = jobCard.additionalNotes
            parts = jobCard.partsUsed.ifEmpty { listOf(PartItem()) }
            startTime = jobCard.startTime
            endTime = jobCard.endTime
        }
    }

    // Reset submit state when leaving
    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetSubmitState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Job Card") },
                navigationIcon = {
                    IconButton(onClick = { navManager.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Job info card
            if (currentJob != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Job #${currentJob!!.id}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Building: ${currentJob!!.buildingName}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Issue: ${currentJob!!.issueType}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Status: ${currentJob!!.status.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = when (currentJob!!.status) {
                                JobStatus.COMPLETED -> Color.Green
                                JobStatus.IN_PROGRESS -> Color.Blue
                                else -> Color(0xFFFF9800)
                            }
                        )
                    }
                }
            }

            // Work Performed Summary
            OutlinedTextField(
                value = workSummary,
                onValueChange = { workSummary = it },
                label = { Text("Work Performed Summary") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 8,
                placeholder = { Text("Describe work done, diagnoses, and actions taken...") },
                enabled = !isSubmitting
            )

            // Parts Used
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Parts Used/Consumables",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    parts.forEachIndexed { index, part ->
                        PartItemRow(
                            part = part,
                            onPartChange = { updatedPart ->
                                parts = parts.toMutableList().apply {
                                    set(index, updatedPart)
                                }
                            },
                            onRemove = {
                                if (parts.size > 1) {
                                    parts = parts.filterIndexed { i, _ -> i != index }
                                } else {
                                    Toast.makeText(context, "At least one part is required", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isSubmitting
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    TextButton(
                        onClick = { parts = parts + PartItem() },
                        modifier = Modifier.align(Alignment.Start),
                        enabled = !isSubmitting
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Part")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Add Part")
                    }
                }
            }

            // Time Logged
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Time Logged",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Start Time
                        OutlinedButton(
                            onClick = {
                                startTime = System.currentTimeMillis()
                                Toast.makeText(context, "Start time recorded", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isSubmitting
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(startTime?.let { formatTime(it) } ?: "Start")
                        }

                        // End Time
                        OutlinedButton(
                            onClick = {
                                if (startTime != null) {
                                    endTime = System.currentTimeMillis()
                                    Toast.makeText(context, "End time recorded", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please record start time first", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isSubmitting && startTime != null
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(endTime?.let { formatTime(it) } ?: "Stop")
                        }
                    }

                    if (startTime != null && endTime != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val duration = endTime!! - startTime!!
                        val hours = duration / (1000 * 60 * 60)
                        val minutes = (duration % (1000 * 60 * 60)) / (1000 * 60)
                        Text(
                            text = "Duration: ${hours}h ${minutes}m",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Additional Notes
            OutlinedTextField(
                value = additionalNotes,
                onValueChange = { additionalNotes = it },
                label = { Text("Additional Technician Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                placeholder = { Text("Special requirements, follow-ups needed, or tenant feedback...") },
                enabled = !isSubmitting
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Save as Draft button
                OutlinedButton(
                    onClick = {
                        val filteredParts = parts.filter { it.name.isNotBlank() }
                        if (workSummary.isNotBlank() && filteredParts.isNotEmpty()) {
                            coroutineScope.launch {
                                isSubmitting = true
                                viewModel.saveJobCard(
                                    jobId = jobId,
                                    technicianId = currentJob?.technicianId ?: "",
                                    buildingName = currentJob?.buildingName ?: "",
                                    workSummary = workSummary,
                                    partsUsed = filteredParts,
                                    startTime = startTime,
                                    endTime = endTime,
                                    additionalNotes = additionalNotes,
                                    photoPaths = emptyList()
                                )
                                isSubmitting = false
                                Toast.makeText(context, "Job card saved as draft", Toast.LENGTH_SHORT).show()
                                navManager.navigateBack()
                            }
                        } else {
                            Toast.makeText(context, "Please fill in work summary and at least one part", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isSubmitting
                ) {
                    Text("Save Draft")
                }

                // Submit button
                Button(
                    onClick = {
                        val filteredParts = parts.filter { it.name.isNotBlank() }
                        if (workSummary.isNotBlank() && filteredParts.isNotEmpty()) {
                            coroutineScope.launch {
                                isSubmitting = true
                                // First save the job card
                                val jobCardId = viewModel.saveJobCard(
                                    jobId = jobId,
                                    technicianId = currentJob?.technicianId ?: "",
                                    buildingName = currentJob?.buildingName ?: "",
                                    workSummary = workSummary,
                                    partsUsed = filteredParts,
                                    startTime = startTime,
                                    endTime = endTime,
                                    additionalNotes = additionalNotes,
                                    photoPaths = emptyList()
                                )

                                // Then submit it (this will also mark job as COMPLETED)
                                viewModel.submitJobCard(jobCardId)
                                isSubmitting = false
                                Toast.makeText(context, "Job card submitted successfully!", Toast.LENGTH_LONG).show()
                                navManager.navigateBack()
                            }
                        } else {
                            Toast.makeText(context, "Please fill in work summary and at least one part", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Submit Job Card")
                    }
                }
            }
        }
    }
}

data class PartItem(
    val name: String = "",
    val quantity: Int = 1
)

@Composable
fun PartItemRow(
    part: PartItem,
    onPartChange: (PartItem) -> Unit,
    onRemove: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = part.name,
            onValueChange = { onPartChange(part.copy(name = it)) },
            label = { Text("Part Name") },
            modifier = Modifier.weight(2f),
            singleLine = true,
            enabled = enabled
        )
        OutlinedTextField(
            value = part.quantity.toString(),
            onValueChange = {
                val qty = it.toIntOrNull() ?: 1
                onPartChange(part.copy(quantity = qty))
            },
            label = { Text("Qty") },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            enabled = enabled
        )
        IconButton(
            onClick = onRemove,
            enabled = enabled
        ) {
            Icon(Icons.Default.Close, contentDescription = "Remove")
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val format = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return format.format(Date(timestamp))
}