package com.prog7314.arcticflow.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.prog7314.arcticflow.data.SouthAfricaLocations
import com.prog7314.arcticflow.data.entities.BuildingEntity
import com.prog7314.arcticflow.data.entities.BuildingType
import com.prog7314.arcticflow.navigation.NavManager
import com.prog7314.arcticflow.viewmodels.QuoteViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBuildingScreen(
    viewModel: QuoteViewModel,
    userId: String,
    navManager: NavManager
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var unitCount by remember { mutableStateOf("1") }
    var floors by remember { mutableStateOf("1") }
    var buildingType by remember { mutableStateOf(BuildingType.RESIDENTIAL) }
    var isSaving by remember { mutableStateOf(false) }

    // Location cascade
    var province by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var suburb by remember { mutableStateOf("") }

    // Dropdown open state
    var provinceExpanded by remember { mutableStateOf(false) }
    var cityExpanded by remember { mutableStateOf(false) }
    var suburbExpanded by remember { mutableStateOf(false) }

    val cities = remember(province) { SouthAfricaLocations.citiesFor(province) }
    val suburbs = remember(province, city) {
        SouthAfricaLocations.suburbsFor(province, city)
    }

    val previewAddress = SouthAfricaLocations.composeFullAddress(
        street = street,
        suburb = suburb,
        city = city,
        province = province,
        postalCode = postalCode
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Building") },
                navigationIcon = {
                    IconButton(onClick = { navManager.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Building Name *") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            )

            // ===== LOCATION CASCADE =====
            Text(
                "Location",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            // ---- Province dropdown ----
            ExposedDropdownMenuBox(
                expanded = provinceExpanded,
                onExpandedChange = {
                    if (!isSaving) provinceExpanded = !provinceExpanded
                }
            ) {
                OutlinedTextField(
                    value = province.ifBlank { "Select Province *" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Province") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(provinceExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = provinceExpanded,
                    onDismissRequest = { provinceExpanded = false }
                ) {
                    SouthAfricaLocations.provinces.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p) },
                            onClick = {
                                province = p
                                // Reset downstream selections
                                city = ""
                                suburb = ""
                                provinceExpanded = false
                            }
                        )
                    }
                }
            }

            // ---- City dropdown (enabled once province chosen) ----
            ExposedDropdownMenuBox(
                expanded = cityExpanded,
                onExpandedChange = {
                    if (!isSaving && province.isNotBlank()) {
                        cityExpanded = !cityExpanded
                    }
                }
            ) {
                OutlinedTextField(
                    value = city.ifBlank {
                        if (province.isBlank()) "Select Province first" else "Select City *"
                    },
                    onValueChange = {},
                    readOnly = true,
                    enabled = province.isNotBlank(),
                    label = { Text("City") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(cityExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = cityExpanded,
                    onDismissRequest = { cityExpanded = false }
                ) {
                    cities.forEach { c ->
                        DropdownMenuItem(
                            text = { Text(c) },
                            onClick = {
                                city = c
                                suburb = ""
                                cityExpanded = false
                            }
                        )
                    }
                }
            }

            // ---- Suburb dropdown (enabled once city chosen) ----
            ExposedDropdownMenuBox(
                expanded = suburbExpanded,
                onExpandedChange = {
                    if (!isSaving && city.isNotBlank()) {
                        suburbExpanded = !suburbExpanded
                    }
                }
            ) {
                OutlinedTextField(
                    value = suburb.ifBlank {
                        if (city.isBlank()) "Select City first" else "Select Suburb *"
                    },
                    onValueChange = {},
                    readOnly = true,
                    enabled = city.isNotBlank(),
                    label = { Text("Suburb") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(suburbExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = suburbExpanded,
                    onDismissRequest = { suburbExpanded = false }
                ) {
                    // Allow the user to type a suburb that isn't in our list
                    if (suburbs.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No suburbs listed for this city") },
                            onClick = { suburbExpanded = false }
                        )
                    } else {
                        suburbs.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s) },
                                onClick = {
                                    suburb = s
                                    suburbExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // ---- Street address (free text) ----
            OutlinedTextField(
                value = street,
                onValueChange = { street = it },
                label = { Text("Street Address *") },
                placeholder = { Text("e.g. 48 Allamanda Rd") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            )

            // ---- Postal code (free text, numeric) ----
            OutlinedTextField(
                value = postalCode,
                onValueChange = { input ->
                    // SA postal codes are exactly 4 digits
                    if (input.length <= 4 && input.all { it.isDigit() }) {
                        postalCode = input
                    }
                },
                label = { Text("Postal Code *") },
                placeholder = { Text("e.g. 2092") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = !isSaving
            )

            // ---- Address preview ----
            if (previewAddress.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "Full address preview",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            previewAddress,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // ===== BUILDING DETAILS =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = unitCount,
                    onValueChange = { unitCount = it },
                    label = { Text("Units") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !isSaving
                )
                OutlinedTextField(
                    value = floors,
                    onValueChange = { floors = it },
                    label = { Text("Floors") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !isSaving
                )
            }

            Column {
                Text(
                    text = "Building Type",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BuildingType.values().forEach { type ->
                        FilterChip(
                            selected = buildingType == type,
                            onClick = { buildingType = type },
                            label = { Text(type.name) },
                            enabled = !isSaving
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (name.isBlank() || street.isBlank() ||
                        province.isBlank() || city.isBlank() ||
                        suburb.isBlank() || postalCode.isBlank()
                    ) {
                        Toast.makeText(
                            context,
                            "Please fill in all required fields",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }
                    if (postalCode.length != 4) {
                        Toast.makeText(
                            context,
                            "Postal code must be 4 digits",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }
                    coroutineScope.launch {
                        isSaving = true

                        // ⚠️ DIAGNOSTIC — verify what gets saved to the DB
                        Log.d(
                            "AddBuildingScreen",
                            "Saving building:\n" +
                                    "  address='$street'\n" +
                                    "  suburb='$suburb'\n" +
                                    "  city='$city'\n" +
                                    "  province='$province'\n" +
                                    "  postalCode='$postalCode'\n" +
                                    "  fullAddress='$previewAddress'"
                        )

                        val building = BuildingEntity(
                            userId = userId,
                            name = name.trim(),
                            address = street.trim(),
                            suburb = suburb,
                            city = city,
                            province = province,
                            postalCode = postalCode,
                            fullAddress = previewAddress,
                            unitCount = unitCount.toIntOrNull() ?: 1,
                            floors = floors.toIntOrNull() ?: 1,
                            buildingType = buildingType
                        )
                        viewModel.addBuilding(userId, building)
                        isSaving = false
                        Toast.makeText(
                            context,
                            "Building added successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        navManager.navigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Add Building")
                }
            }
        }
    }
}