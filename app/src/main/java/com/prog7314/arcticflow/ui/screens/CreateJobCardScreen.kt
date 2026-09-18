// app/src/main/java/com/prog7314/arcticflow/ui/screens/CreateJobCardScreen.kt
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
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
import com.prog7314.arcticflow.data.CatalogPart
import com.prog7314.arcticflow.data.PartCatalog
import com.prog7314.arcticflow.data.entities.Job
import com.prog7314.arcticflow.data.entities.PartItem
import com.prog7314.arcticflow.navigation.NavManager
import com.prog7314.arcticflow.viewmodels.JobCardViewModel
import kotlinx.coroutines.launch
import java.io.File
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
    var parts by remember { mutableStateOf(listOf<PartItem>()) }
    var startTime by remember { mutableStateOf<Long?>(null) }
    var endTime by remember { mutableStateOf<Long?>(null) }
    var photoPaths by remember { mutableStateOf(listOf<String>()) }
    var isSubmitting by remember { mutableStateOf(false) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    // ---- Catalog part picker (mirrors CreateQuoteScreen) ----
    var selectedCatalogPart by remember { mutableStateOf<CatalogPart?>(null) }
    var partQtyStr by remember { mutableStateOf("1") }
    var partDropdownExpanded by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingCameraFile?.let { file ->
                photoPaths = photoPaths + file.absolutePath
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
                context,
                "${context.packageName}.fileprovider",
                file
            )
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "Camera error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(jobId) {
        currentJob = viewModel.getJobById(jobId)
        val existing = viewModel.getJobCardByJobId(jobId)
        existing?.let {
            workSummary = it.workSummary
            additionalNotes = it.additionalNotes
            parts = it.partsUsed
            startTime = it.startTime
            endTime = it.endTime
            photoPaths = it.photoPaths
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.resetSubmitState() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Job Card") },
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Job summary card
            currentJob?.let { job ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Job #${job.id}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Building: ${job.buildingName}", style = MaterialTheme.typography.bodySmall)
                        Text("Issue: ${job.issueType}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Work summary
            OutlinedTextField(
                value = workSummary,
                onValueChange = { workSummary = it },
                label = { Text("Work Performed Summary") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 8,
                enabled = !isSubmitting
            )

            // Photos
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Job Site Photos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(photoPaths) { path ->
                            Box {
                                AsyncImage(
                                    model = File(path),
                                    contentDescription = "Job photo",
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { photoPaths = photoPaths - path },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(24.dp)
                                        .background(
                                            Color.Black.copy(alpha = 0.5f),
                                            RoundedCornerShape(50)
                                        )
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        "Remove",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                        item {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable(enabled = !isSubmitting) { launchCamera() },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.AddAPhoto,
                                        "Add photo",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text("Add", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            // ============================================================
            // PARTS USED — same dropdown pattern as CreateQuoteScreen
            // ============================================================
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Parts Used",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Dropdown
                    ExposedDropdownMenuBox(
                        expanded = partDropdownExpanded,
                        onExpandedChange = {
                            if (!isSubmitting) {
                                partDropdownExpanded = !partDropdownExpanded
                            }
                        }
                    ) {
                        OutlinedTextField(
                            value = selectedCatalogPart?.name ?: "Select a part...",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select from Catalog") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = partDropdownExpanded
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            enabled = !isSubmitting
                        )
                        ExposedDropdownMenu(
                            expanded = partDropdownExpanded,
                            onDismissRequest = { partDropdownExpanded = false }
                        ) {
                            PartCatalog.availableParts.forEach { p ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(p.name)
                                            Text(
                                                "R${String.format(Locale.US, "%.2f", p.price)} · ${p.category}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedCatalogPart = p
                                        partDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Quantity + Add Part (only when a part is selected)
                    if (selectedCatalogPart != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = partQtyStr,
                                onValueChange = { input ->
                                    if (input.isEmpty() || input.all { it.isDigit() }) {
                                        partQtyStr = input
                                    }
                                },
                                label = { Text("Quantity") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number
                                ),
                                singleLine = true,
                                enabled = !isSubmitting
                            )
                            Button(
                                onClick = {
                                    val q = partQtyStr.toIntOrNull() ?: 1
                                    if (q > 0) {
                                        val p = selectedCatalogPart!!
                                        parts = parts + PartItem(
                                            name = p.name,
                                            quantity = q,
                                            price = p.price
                                        )
                                        selectedCatalogPart = null
                                        partQtyStr = "1"
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Quantity must be at least 1",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !isSubmitting
                            ) {
                                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add Part")
                            }
                        }
                    }

                    // Added parts list
                    if (parts.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Added Parts (${parts.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        parts.forEachIndexed { index, part ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        part.name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        "${part.quantity} × R${
                                            String.format(Locale.US, "%.2f", part.price)
                                        } = R${
                                            String.format(
                                                Locale.US,
                                                "%.2f",
                                                part.price * part.quantity
                                            )
                                        }",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        parts = parts.filterIndexed { i, _ -> i != index }
                                    },
                                    enabled = !isSubmitting
                                ) {
                                    Icon(Icons.Default.Close, "Remove")
                                }
                            }
                        }

                        // Subtotal
                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Parts Subtotal",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "R${
                                    String.format(
                                        Locale.US,
                                        "%.2f",
                                        parts.sumOf { it.price * it.quantity }
                                    )
                                }",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Time Logged
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Time Logged",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { startTime = System.currentTimeMillis() },
                            modifier = Modifier.weight(1f),
                            enabled = !isSubmitting
                        ) {
                            Icon(Icons.Default.PlayArrow, "Start")
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
                            Icon(Icons.Default.Stop, "Stop")
                            Spacer(Modifier.width(4.dp))
                            Text(endTime?.let { formatTime(it) } ?: "Stop")
                        }
                    }
                    if (startTime != null && endTime != null) {
                        Spacer(Modifier.height(8.dp))
                        val dur = endTime!! - startTime!!
                        Text(
                            "Duration: ${dur / (1000 * 60 * 60)}h ${
                                (dur % (1000 * 60 * 60)) / (1000 * 60)
                            }m",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            OutlinedTextField(
                value = additionalNotes,
                onValueChange = { additionalNotes = it },
                label = { Text("Additional Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                enabled = !isSubmitting
            )

            // Save / Submit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (workSummary.isNotBlank()) {
                            coroutineScope.launch {
                                isSubmitting = true
                                viewModel.saveJobCard(
                                    jobId = jobId,
                                    technicianId = currentJob?.technicianId ?: "",
                                    buildingName = currentJob?.buildingName ?: "",
                                    workSummary = workSummary,
                                    partsUsed = parts,
                                    startTime = startTime,
                                    endTime = endTime,
                                    additionalNotes = additionalNotes,
                                    photoPaths = photoPaths
                                )
                                isSubmitting = false
                                Toast.makeText(context, "Saved as draft", Toast.LENGTH_SHORT).show()
                                navManager.navigateBack()
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "Please fill in the work summary",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isSubmitting
                ) { Text("Save Draft") }

                Button(
                    onClick = {
                        if (workSummary.isNotBlank()) {
                            coroutineScope.launch {
                                isSubmitting = true
                                val id = viewModel.saveJobCard(
                                    jobId = jobId,
                                    technicianId = currentJob?.technicianId ?: "",
                                    buildingName = currentJob?.buildingName ?: "",
                                    workSummary = workSummary,
                                    partsUsed = parts,
                                    startTime = startTime,
                                    endTime = endTime,
                                    additionalNotes = additionalNotes,
                                    photoPaths = photoPaths
                                )
                                viewModel.submitJobCard(id)
                                isSubmitting = false
                                Toast.makeText(
                                    context,
                                    "Job card submitted!",
                                    Toast.LENGTH_LONG
                                ).show()
                                navManager.navigateBack()
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "Please fill in the work summary",
                                Toast.LENGTH_SHORT
                            ).show()
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
                        Text("Submit")
                    }
                }
            }
        }
    }
}

private fun formatTime(ts: Long): String =
    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(ts))

private fun createImageFile(context: Context): File {
    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val dir = File(context.filesDir, "job_photos")
    if (!dir.exists()) dir.mkdirs()
    return File.createTempFile("JPEG_${stamp}_", ".jpg", dir)
}