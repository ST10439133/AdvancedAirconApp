// app/src/main/java/com/prog7314/arcticflow/ui/screens/ServicesScreen.kt
package com.prog7314.arcticflow.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prog7314.arcticflow.navigation.NavManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(
    navManager: NavManager
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
                .padding(16.dp)
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

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> RequestServiceTab(navManager)
                1 -> ServiceHistoryTab()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestServiceTab(
    navManager: NavManager
) {
    var selectedBuilding by remember { mutableStateOf("Apex Tech Plaza (Suite 401)") }
    var selectedServiceType by remember { mutableStateOf("HVAC Preventive Maintenance") }
    var selectedPriority by remember { mutableStateOf("Medium") }
    var description by remember { mutableStateOf("") }
    var selectedTechnician by remember { mutableStateOf("Marcus Vance (Lead HVAC)") }

    val buildings = listOf("Apex Tech Plaza (Suite 401)", "Oakwood Medical Center", "Riverview Apartments")
    val serviceTypes = listOf("HVAC Preventive Maintenance", "AC Repair", "Heating Repair", "Installation", "Emergency Service")
    val priorities = listOf("Low", "Medium", "High", "Emergency")
    val technicians = listOf("Marcus Vance (Lead HVAC)", "Sarah Jenkins", "David Miller", "Terry Smith")

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Building Selection
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedBuilding,
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
                            text = { Text(building) },
                            onClick = {
                                selectedBuilding = building
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        item {
            // Service Type
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedServiceType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Service Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    serviceTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                selectedServiceType = type
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        item {
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
                    priorities.forEach { priority ->
                        FilterChip(
                            selected = selectedPriority == priority,
                            onClick = { selectedPriority = priority },
                            label = { Text(priority) }
                        )
                    }
                }
            }
        }

        item {
            // Preferred Date
            OutlinedTextField(
                value = "July 24, 2026",
                onValueChange = {},
                label = { Text("Preferred Date") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                }
            )
        }

        item {
            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description of Issue") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 8,
                placeholder = {
                    Text("Vibration reported in standard compressor B, possible loose mounting or bearing wear. Advise diagnostic testing before scheduled shutdown.")
                }
            )
        }

        item {
            // Assign Lead Technician
            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedTechnician,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Assign Lead Technician") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    technicians.forEach { tech ->
                        DropdownMenuItem(
                            text = { Text(tech) },
                            onClick = {
                                selectedTechnician = tech
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { /* Submit request */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Submit Request")
            }
        }
    }
}

@Composable
fun ServiceHistoryTab() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Service History",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Your service history will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}