// app/src/main/java/com/prog7314/arcticflow/ui/screens/ServicesScreen.kt
package com.prog7314.arcticflow.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
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
import com.prog7314.arcticflow.data.entities.RequestStatus
import com.prog7314.arcticflow.data.entities.ServiceRequest
import com.prog7314.arcticflow.navigation.NavManager
import com.prog7314.arcticflow.viewmodels.QuoteViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(
    navManager: NavManager,
    userId: String,
    viewModel: QuoteViewModel
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Request Service", "Service History")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Services") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> RequestServiceTab(
                    navManager = navManager,
                    userId = userId,
                    viewModel = viewModel
                )
                1 -> ServiceRequestHistoryScreen(
                    viewModel = viewModel,
                    userId = userId
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestServiceTab(
    navManager: NavManager,
    userId: String,
    viewModel: QuoteViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedBuilding by remember { mutableStateOf<BuildingEntity?>(null) }
    var selectedServiceType by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(RequestPriority.MEDIUM) }
    var preferredDate by remember { mutableStateOf<Long?>(null) }
    var description by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var buildingDropdownExpanded by remember { mutableStateOf(false) }
    var serviceTypeDropdownExpanded by remember { mutableStateOf(false) }

    // Live list of buildings — refreshes automatically
    val buildings by viewModel.getBuildingsForUser(userId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val serviceTypes = listOf(
        "HVAC Preventive Maintenance",
        "AC Repair",
        "Heating Repair",
        "Installation",
        "Emergency Service",
        "Ventilation Issue",
        "Filter Replacement",
        "Compressor Problem",
        "Refrigerant Leak",
        "Thermostat Issue"
    )

    val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ===== BUILDING SELECTION =====
        if (buildings.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No buildings registered yet.")
                    TextButton(onClick = { navManager.navigateToAddBuilding() }) {
                        Text("Add a Building")
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
                    label = { Text("Select Building") },
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

        // ===== SERVICE TYPE =====
        ExposedDropdownMenuBox(
            expanded = serviceTypeDropdownExpanded,
            onExpandedChange = { serviceTypeDropdownExpanded = !serviceTypeDropdownExpanded }
        ) {
            OutlinedTextField(
                value = selectedServiceType.ifBlank { "Select Service Type" },
                onValueChange = {},
                readOnly = true,
                label = { Text("Service Type") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = serviceTypeDropdownExpanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = serviceTypeDropdownExpanded,
                onDismissRequest = { serviceTypeDropdownExpanded = false }
            ) {
                serviceTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type) },
                        onClick = {
                            selectedServiceType = type
                            serviceTypeDropdownExpanded = false
                        }
                    )
                }
            }
        }

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
                RequestPriority.values().forEach { priority ->
                    FilterChip(
                        selected = selectedPriority == priority,
                        onClick = { selectedPriority = priority },
                        label = { Text(priority.name) },
                        enabled = !isSubmitting
                    )
                }
            }
        }

        // ===== PREFERRED DATE (real date picker) =====
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
            modifier = Modifier.fillMaxWidth()
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
            placeholder = {
                Text("Describe the issue in detail...")
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ===== SUBMIT BUTTON =====
        Button(
            onClick = {
                val building = selectedBuilding
                if (building == null) {
                    Toast.makeText(context, "Please select a building", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (selectedServiceType.isBlank()) {
                    Toast.makeText(context, "Please select a service type", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (description.isBlank()) {
                    Toast.makeText(context, "Please enter a description", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                coroutineScope.launch {
                    isSubmitting = true
                    try {
                        val request = ServiceRequest(
                            userId = userId,
                            buildingId = building.id,
                            buildingName = building.name,
                            issueType = selectedServiceType,
                            description = description,
                            priority = selectedPriority,
                            preferredDate = preferredDate,
                            status = RequestStatus.PENDING
                        )
                        val id = viewModel.createServiceRequest(request)
                        if (id > 0L) {
                            Toast.makeText(context, "Service request submitted!", Toast.LENGTH_SHORT).show()
                            // Reset form
                            selectedBuilding = null
                            selectedServiceType = ""
                            selectedPriority = RequestPriority.MEDIUM
                            preferredDate = null
                            description = ""
                        } else {
                            Toast.makeText(context, "Failed to submit request", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isSubmitting = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSubmitting && selectedBuilding != null &&
                    selectedServiceType.isNotBlank() && description.isNotBlank()
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