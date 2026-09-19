// app/src/main/java/com/prog7314/arcticflow/ui/screens/CreateJobCardScreen.kt
package com.prog7314.arcticflow.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

// ============================================================
// BRAND TOKENS
// ============================================================
private val BabyBlue        = Color(0xFF4FA8D8)
private val BabyBlueDeep    = Color(0xFF2E7BA6)
private val BabyBlueSoft    = Color(0xFFE1F1FB)
private val OrangeAccent    = Color(0xFFF7941D)
private val OrangeSoft      = Color(0xFFFFEBD2)
private val SuccessGreen    = Color(0xFF2E7D32)

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

    var selectedCatalogPart by remember { mutableStateOf<CatalogPart?>(null) }
    var partQtyStr by remember { mutableStateOf("1") }
    var partDropdownExpanded by remember { mutableStateOf(false) }

    // ---- Camera launcher ----
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingCameraFile
        pendingCameraFile = null
        if (success && file != null) {
            photoPaths = photoPaths + file.absolutePath
            Toast.makeText(context, "Photo captured!", Toast.LENGTH_SHORT).show()
        } else {
            file?.delete()
        }
    }

    // ---- Camera permission ----
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = createImageFile(context)
            pendingCameraFile = file
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(
                context,
                "Camera permission denied. Enable it in Settings to take photos.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ---- Gallery picker ----
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val saved = copyUriToJobPhotos(context, uri)
            if (saved != null) {
                photoPaths = photoPaths + saved.absolutePath
                Toast.makeText(context, "Image added from gallery!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to import image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun onCameraClick() {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            val file = createImageFile(context)
            pendingCameraFile = file
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
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
                title = { Text("Job Card") },
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ============================================================
            // HERO - job summary
            // ============================================================
            currentJob?.let { job ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(listOf(BabyBlue, BabyBlueDeep))
                            )
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.22f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Build,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(
                                    "Job #${job.id}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (job.buildingName.isNotBlank()) {
                                    Text(
                                        job.buildingName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                                if (job.issueType.isNotBlank()) {
                                    Text(
                                        job.issueType,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ============================================================
            // SECTION 1 - Work Summary
            // ============================================================
            SectionHeader(icon = Icons.Default.Description, title = "Work Performed")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                OutlinedTextField(
                    value = workSummary,
                    onValueChange = { workSummary = it },
                    label = { Text("Summary of work *") },
                    placeholder = { Text("Describe what you did...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    minLines = 4,
                    maxLines = 8,
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSubmitting
                )
            }

            // ============================================================
            // SECTION 2 - Job Site Photos
            // ============================================================
            SectionHeader(icon = Icons.Default.AddAPhoto, title = "Job Site Photos")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(photoPaths) { path ->
                            Box {
                                AsyncImage(
                                    model = File(path),
                                    contentDescription = "Job photo",
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { photoPaths = photoPaths - path },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(26.dp)
                                        .background(
                                            Color.Black.copy(alpha = 0.55f),
                                            CircleShape
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

                        // Camera tile
                        item {
                            PhotoTile(
                                icon = Icons.Default.AddAPhoto,
                                label = "Camera",
                                enabled = !isSubmitting,
                                onClick = { onCameraClick() }
                            )
                        }

                        // Gallery tile
                        item {
                            PhotoTile(
                                icon = Icons.Default.PhotoLibrary,
                                label = "Gallery",
                                enabled = !isSubmitting,
                                onClick = { galleryLauncher.launch("image/*") }
                            )
                        }
                    }
                }
            }

            // ============================================================
            // SECTION 3 - Parts Used
            // ============================================================
            SectionHeader(icon = Icons.Default.Build, title = "Parts Used")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                            label = { Text("From Catalog") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = partDropdownExpanded
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp),
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
                                shape = RoundedCornerShape(12.dp),
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
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isSubmitting
                            ) {
                                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add")
                            }
                        }
                    }

                    if (parts.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Added Parts (${parts.size})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = BabyBlueDeep
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
                                    Icon(
                                        Icons.Default.Close,
                                        "Remove",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Subtotal",
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
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = BabyBlueDeep
                            )
                        }
                    }
                }
            }

            // ============================================================
            // SECTION 4 - Time Logged
            // ============================================================
            SectionHeader(icon = Icons.Default.Timer, title = "Time Logged")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { startTime = System.currentTimeMillis() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isSubmitting
                        ) {
                            Icon(Icons.Default.PlayArrow, "Start", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(startTime?.let { formatTime(it) } ?: "Start")
                        }
                        OutlinedButton(
                            onClick = {
                                if (startTime != null) endTime = System.currentTimeMillis()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isSubmitting && startTime != null
                        ) {
                            Icon(Icons.Default.Stop, "Stop", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(endTime?.let { formatTime(it) } ?: "Stop")
                        }
                    }
                    if (startTime != null && endTime != null) {
                        val dur = endTime!! - startTime!!
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = BabyBlueSoft,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = BabyBlueDeep,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Duration: ${dur / (1000 * 60 * 60)}h ${
                                        (dur % (1000 * 60 * 60)) / (1000 * 60)
                                    }m",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = BabyBlueDeep,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // ============================================================
            // SECTION 5 - Additional Notes
            // ============================================================
            SectionHeader(icon = Icons.Default.Description, title = "Notes")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                OutlinedTextField(
                    value = additionalNotes,
                    onValueChange = { additionalNotes = it },
                    label = { Text("Additional notes") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSubmitting
                )
            }

            // ============================================================
            // ACTIONS
            // ============================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
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
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeAccent,
                        contentColor = Color.White
                    ),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White
                        )
                    } else {
                        Text(
                            "Submit",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ============================================================
// HELPERS
// ============================================================

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = BabyBlueDeep,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = BabyBlueDeep
        )
    }
}

@Composable
private fun PhotoTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(BabyBlueSoft)
            .border(
                width = 1.dp,
                color = BabyBlue.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                label,
                tint = BabyBlueDeep,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = BabyBlueDeep,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private suspend fun copyUriToJobPhotos(
    context: Context,
    uri: Uri
): File? = withContext(Dispatchers.IO) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return@withContext null

        val file = createImageFile(context)
        inputStream.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
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