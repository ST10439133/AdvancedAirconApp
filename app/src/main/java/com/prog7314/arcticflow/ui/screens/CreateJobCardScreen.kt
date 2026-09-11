package com.prog7314.arcticflow.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.prog7314.arcticflow.data.ArcticFlowDatabase
import com.prog7314.arcticflow.data.entities.Job
import com.prog7314.arcticflow.data.entities.JobStatus
import com.prog7314.arcticflow.data.entities.PartItem
import com.prog7314.arcticflow.navigation.NavManager
import com.prog7314.arcticflow.viewmodels.JobCardViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateJobCardScreen(jobId: Int, navManager: NavManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: JobCardViewModel = viewModel(
        factory = JobCardViewModel.Factory(ArcticFlowDatabase.getDatabase(context))
    )

    var currentJob by remember { mutableStateOf<Job?>(null) }
    var workSummary by remember { mutableStateOf("") }
    var additionalNotes by remember { mutableStateOf("") }
    var parts by remember { mutableStateOf(listOf(PartItem())) }
    var startTime by remember { mutableStateOf<Long?>(null) }
    var endTime by remember { mutableStateOf<Long?>(null) }
    var photoPaths by remember { mutableStateOf(listOf<String>()) }
    var isSubmitting by remember { mutableStateOf(false) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingCameraFile?.let {
                photoPaths = photoPaths + it.absolutePath
                Toast.makeText(context, "Photo captured!", Toast.LENGTH_SHORT).show()
            }
        }
        pendingCameraFile = null
    }

    fun launchCamera() {
        try {
            val file = createImageFile(context)
            pendingCameraFile = file
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "Camera error: ${e.message}",
                Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(jobId) {
        currentJob = viewModel.getJobById(jobId)
        viewModel.getJobCardByJobId(jobId)?.let { jc ->
            workSummary = jc.workSummary
            additionalNotes = jc.additionalNotes
            parts = jc.partsUsed.ifEmpty { listOf(PartItem()) }
            startTime = jc.startTime
            endTime = jc.endTime
            photoPaths = jc.photoPaths
        }
    }

    DisposableEffect(Unit) { onDispose { viewModel.resetSubmitState() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Job Card") },
                navigationIcon = {
                    IconButton(onClick = { navManager.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            currentJob?.let { job ->
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Job #${job.id}", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold)
                        Text("Building: ${job.buildingName}",
                            style = MaterialTheme.typography.bodySmall)
                        Text("Issue: ${job.issueType}",
                            style = MaterialTheme.typography.bodySmall)
                        Text("Status: ${job.status.name}",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            OutlinedTextField(workSummary, { workSummary = it },
                label = { Text("Work Performed Summary") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4, maxLines = 8,
                placeholder = { Text("Describe work done, diagnoses, and actions taken...") },
                enabled = !isSubmitting)

            Card(Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Job Site Photos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        Text("${photoPaths.size} photo(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(photoPaths) { path ->
                            Box {
                                AsyncImage(
                                    model = File(path),
                                    contentDescription = "Photo",
                                    modifier = Modifier.size(100.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { photoPaths = photoPaths - path },
                                    modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                                        .background(Color.Black.copy(alpha = 0.5f),
                                            RoundedCornerShape(50))
                                ) {
                                    Icon(Icons.Default.Close, "Remove",
                                        tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        item {
                            Box(
                                modifier = Modifier.size(100.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable(enabled = !isSubmitting) { launchCamera() },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AddAPhoto, "Add",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Add",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            Card(Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Parts Used/Consumables",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    parts.forEachIndexed { i, part ->
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(part.name,
                                { parts = parts.toMutableList().apply {
                                    set(i, part.copy(name = it)) } },
                                label = { Text("Part") }, modifier = Modifier.weight(2f),
                                singleLine = true, enabled = !isSubmitting)
                            OutlinedTextField(part.quantity.toString(),
                                { parts = parts.toMutableList().apply {
                                    set(i, part.copy(quantity = it.toIntOrNull() ?: 1)) } },
                                label = { Text("Qty") }, modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true, enabled = !isSubmitting)
                            IconButton(onClick = {
                                if (parts.size > 1) parts = parts.filterIndexed { j, _ -> j != i }
                            }, enabled = !isSubmitting) {
                                Icon(Icons.Default.Close, "Remove")
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    TextButton(onClick = { parts = parts + PartItem() },
                        enabled = !isSubmitting) {
                        Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp))
                        Text("Add Part")
                    }
                }
            }

            Card(Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Time Logged", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { startTime = System.currentTimeMillis() },
                            modifier = Modifier.weight(1f), enabled = !isSubmitting
                        ) {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(4.dp))
                            Text(startTime?.let { formatTime(it) } ?: "Start")
                        }
                        OutlinedButton(
                            onClick = {
                                if (startTime != null) endTime = System.currentTimeMillis()
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isSubmitting && startTime != null
                        ) {
                            Icon(Icons.Default.Stop, null)
                            Spacer(Modifier.width(4.dp))
                            Text(endTime?.let { formatTime(it) } ?: "Stop")
                        }
                    }
                    if (startTime != null && endTime != null) {
                        Spacer(Modifier.height(8.dp))
                        val dur = endTime!! - startTime!!
                        Text("Duration: ${dur / 3600000}h ${(dur % 3600000) / 60000}m",
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            OutlinedTextField(additionalNotes, { additionalNotes = it },
                label = { Text("Additional Notes") },
                modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 6,
                enabled = !isSubmitting)

            Row(Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val fp = parts.filter { it.name.isNotBlank() }
                        if (workSummary.isNotBlank() && fp.isNotEmpty()) {
                            scope.launch {
                                isSubmitting = true
                                viewModel.saveJobCard(
                                    jobId, currentJob?.technicianId ?: "",
                                    currentJob?.buildingName ?: "",
                                    workSummary, fp, startTime, endTime,
                                    additionalNotes, photoPaths
                                )
                                isSubmitting = false
                                Toast.makeText(context, "Saved as draft",
                                    Toast.LENGTH_SHORT).show()
                                navManager.navigateBack()
                            }
                        } else Toast.makeText(context,
                            "Fill in work summary + 1 part",
                            Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f), enabled = !isSubmitting
                ) { Text("Save Draft") }

                Button(
                    onClick = {
                        val fp = parts.filter { it.name.isNotBlank() }
                        if (workSummary.isNotBlank() && fp.isNotEmpty()) {
                            scope.launch {
                                isSubmitting = true
                                val id = viewModel.saveJobCard(
                                    jobId, currentJob?.technicianId ?: "",
                                    currentJob?.buildingName ?: "",
                                    workSummary, fp, startTime, endTime,
                                    additionalNotes, photoPaths
                                )
                                viewModel.submitJobCard(id)
                                isSubmitting = false
                                Toast.makeText(context, "Job card submitted!",
                                    Toast.LENGTH_LONG).show()
                                navManager.navigateBack()
                            }
                        } else Toast.makeText(context,
                            "Fill in work summary + 1 part",
                            Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f), enabled = !isSubmitting
                ) {
                    if (isSubmitting) CircularProgressIndicator(
                        Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Submit")
                }
            }
        }
    }
}

private fun formatTime(t: Long): String =
    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(t))

private fun createImageFile(context: Context): File {
    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val dir = File(context.filesDir, "job_photos")
    if (!dir.exists()) dir.mkdirs()
    return File.createTempFile("JPEG_${stamp}_", ".jpg", dir)
}