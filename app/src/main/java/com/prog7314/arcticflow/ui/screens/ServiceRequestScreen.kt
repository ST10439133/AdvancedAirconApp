// app/src/main/java/com/prog7314/arcticflow/ui/screens/ServiceRequestScreen.kt
package com.prog7314.arcticflow.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prog7314.arcticflow.data.entities.BuildingEntity
import com.prog7314.arcticflow.data.entities.RequestPriority
import com.prog7314.arcticflow.data.entities.ServiceRequest
import com.prog7314.arcticflow.navigation.NavManager
import com.prog7314.arcticflow.viewmodels.QuoteViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceRequestScreen(
    viewModel: QuoteViewModel,
    userId: String,
    navManager: NavManager
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedBuildingId by remember { mutableStateOf<Int?>(null) }
    var issueType by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(RequestPriority.MEDIUM) }
    var showAddBuilding by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val buildings by viewModel.getBuildingsForUser(userId).collectAsStateWithLifecycle(initialValue = emptyList())

    if (showAddBuilding) {
        AddBuildingScreen(
            viewModel = viewModel,
            userId = userId,
            navManager = navManager
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request Service") },
                navigationIcon = {
                    IconButton(onClick = { navManager.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showAddBuilding = true }) {
                        Text("Add Building")
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
            // Building Selection
            if (buildings.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No buildings registered yet.")
                        TextButton(onClick = { showAddBuilding = true }) {
                            Text("Add a Building")
                        }
                    }
                }
            } else {
                var expanded by remember { mutableStateOf(false) }
                var selectedBuilding by remember { mutableStateOf<BuildingEntity?>(null) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedBuilding?.name ?: "Select a Building",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Building") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        buildings.forEach { building ->
                            DropdownMenuItem(
                                text = { Text(building.name) },
                                onClick = {
                                    selectedBuilding = building
                                    selectedBuildingId = building.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Issue Type
            var issueExpanded by remember { mutableStateOf(false) }
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

            ExposedDropdownMenuBox(
                expanded = issueExpanded,
                onExpandedChange = { issueExpanded = !issueExpanded }
            ) {
                OutlinedTextField(
                    value = issueType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Service Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = issueExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = issueExpanded,
                    onDismissRequest = { issueExpanded = false }
                ) {
                    issueTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                issueType = type
                                issueExpanded = false
                            }
                        )
                    }
                }
            }

            // Description
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

            // Priority
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

            // Submit Button
            Button(
                onClick = {
                    if (selectedBuildingId != null && issueType.isNotBlank() && description.isNotBlank()) {
                        coroutineScope.launch {
                            isSubmitting = true
                            try {
                                val buildingId = selectedBuildingId ?: 0
                                val buildingName = buildings.find { it.id == buildingId }?.name ?: ""

                                val request = ServiceRequest(
                                    userId = userId,
                                    buildingId = buildingId,
                                    buildingName = buildingName,
                                    issueType = issueType,
                                    description = description,
                                    priority = priority
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
                        Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting && selectedBuildingId != null && issueType.isNotBlank() && description.isNotBlank()
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