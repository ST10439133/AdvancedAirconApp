// app/src/main/java/com/prog7314/arcticflow/ui/screens/ServiceRequestScreen.kt
package com.prog7314.arcticflow.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prog7314.arcticflow.data.entities.BuildingEntity
import com.prog7314.arcticflow.data.entities.RequestPriority
import com.prog7314.arcticflow.data.entities.ServiceRequest
import com.prog7314.arcticflow.navigation.NavManager
import com.prog7314.arcticflow.viewmodels.QuoteViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceRequestScreen(
    viewModel: QuoteViewModel,
    userId: String,
    navManager: NavManager
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedBuilding by remember { mutableStateOf<BuildingEntity?>(null) }
    var issueType by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(RequestPriority.MEDIUM) }
    var preferredDate by remember { mutableStateOf<Long?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var buildingDropdownExpanded by remember { mutableStateOf(false) }
    var issueDropdownExpanded by remember { mutableStateOf(false) }

    // LIVE buildings list — auto refreshes when a new building is added
    val buildings by viewModel.getBuildingsForUser(userId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val issueTypes = listOf(
        "Air Conditioning Repair",
        "Heating System Repair",
        "Ventilation Issue",
        "Filter Replacement",
        "Compressor Problem",
        "Refrigerant Leak",
        "Thermostat Issue",
        "Preventive Maintenance",
        "Emergency Service",
        "Other"
    )

    // DATE PICKER
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth, 9, 0)
            preferredDate = cal.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request Service") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ===== BUILDING SELECTOR =====
            if (buildings.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No buildings registered yet.")
                        TextButton(onClick = { navManager.navigateToAddBuilding() }) {
                            Text("Add a Building First")
                        }
                    }
                }
            } else {
                ExposedDropdownMenuBox(
                    expanded = buildingDropdownExpanded,
                    onExpandedChange = { buildingDropdownExpanded = !buildingDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedBuilding?.name ?: "Select a Building",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Building") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = buildingDropdownExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = buildingDropdownExpanded,
                        onDismissRequest = { buildingDropdownExpanded = false }
                    ) {
                        buildings.forEach { building ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(building.name, fontWeight = FontWeight.Bold)
                                        Text(
                                            building.address,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedBuilding = building
                                    buildingDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // ===== ISSUE TYPE =====
            ExposedDropdownMenuBox(
                expanded = issueDropdownExpanded,
                onExpandedChange = { issueDropdownExpanded = !issueDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = issueType.ifBlank { "Select Service Type" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Service Type") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = issueDropdownExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = issueDropdownExpanded,
                    onDismissRequest = { issueDropdownExpanded = false }
                ) {
                    issueTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                issueType = type
                                issueDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // ===== PREFERRED DATE (with real picker) =====
            OutlinedTextField(
                value = preferredDate?.let { dateFormat.format(Date(it)) } ?: "Select preferred date",
                onValueChange = {},
                readOnly = true,
                label = { Text("Preferred Date") },
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Date")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { datePickerDialog.show() }
            )

            // ===== DESCRIPTION =====
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description of Issue") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 8,
                enabled = !isSubmitting,
                placeholder = { Text("Describe the problem in detail...") }
            )

            // ===== PRIORITY =====
            Column {
                Text(
                    text = "Priority Level",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RequestPriority.values().forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.name) },
                            enabled = !isSubmitting
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ===== SUBMIT =====
            Button(
                onClick = {
                    val building = selectedBuilding
                    if (building != null && issueType.isNotBlank() && description.isNotBlank()) {
                        coroutineScope.launch {
                            isSubmitting = true
                            try {
                                val request = ServiceRequest(
                                    userId = userId,
                                    buildingId = building.id,
                                    buildingName = building.name,
                                    issueType = issueType,
                                    description = description,
                                    priority = priority,
                                    preferredDate = preferredDate,
                                    status = com.prog7314.arcticflow.data.entities.RequestStatus.PENDING
                                )
                                viewModel.createServiceRequest(request)
                                Toast.makeText(context, "Service request submitted!", Toast.LENGTH_SHORT).show()
                                navManager.navigateBack()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isSubmitting = false
                            }
                        }
                    } else {
                        Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting && selectedBuilding != null &&
                        issueType.isNotBlank() && description.isNotBlank()
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Submit Request")
                }
            }
        }
    }
}