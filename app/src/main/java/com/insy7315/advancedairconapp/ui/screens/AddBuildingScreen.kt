// app/src/main/java/com/insy7315/advancedairconapp/ui/screens/AddBuildingScreen.kt
package com.insy7315.advancedairconapp.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.insy7315.advancedairconapp.data.SouthAfricaLocations
import com.insy7315.advancedairconapp.data.entities.BuildingEntity
import com.insy7315.advancedairconapp.data.entities.BuildingType
import com.insy7315.advancedairconapp.navigation.NavManager
import com.insy7315.advancedairconapp.viewmodels.QuoteViewModel
import kotlinx.coroutines.launch


// BRAND TOKENS

private val BabyBlueDeep = Color(0xFF152A47)
private val BabyBlueSoft = Color(0xFFE8EDF3)
private val OrangeAccent = Color(0xFFC8102E)

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

    var province by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var suburb by remember { mutableStateOf("") }

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

            // SECTION 1 - Building Details

            SectionHeader(
                icon = Icons.Default.Business,
                title = "Building Details"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Building Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        enabled = !isSaving
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = unitCount,
                            onValueChange = { unitCount = it },
                            label = { Text("Units") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            enabled = !isSaving
                        )
                        OutlinedTextField(
                            value = floors,
                            onValueChange = { floors = it },
                            label = { Text("Floors") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            enabled = !isSaving
                        )
                    }

                    // Building Type chips
                    Column {
                        Text(
                            text = "Building Type",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            BuildingType.values().take(3).forEach { type ->
                                FilterChip(
                                    selected = buildingType == type,
                                    onClick = { buildingType = type },
                                    label = {
                                        Text(
                                            type.name.lowercase()
                                                .replaceFirstChar { it.titlecase() },
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    enabled = !isSaving
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            BuildingType.values().drop(3).forEach { type ->
                                FilterChip(
                                    selected = buildingType == type,
                                    onClick = { buildingType = type },
                                    label = {
                                        Text(
                                            type.name.lowercase()
                                                .replaceFirstChar { it.titlecase() },
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    enabled = !isSaving
                                )
                            }
                        }
                    }
                }
            }


            // SECTION 2 - Location

            SectionHeader(
                icon = Icons.Default.LocationOn,
                title = "Location"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ---- Province ----
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
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isSaving
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
                                        city = ""
                                        suburb = ""
                                        provinceExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // ---- City ----
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
                            enabled = province.isNotBlank() && !isSaving,
                            label = { Text("City") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(cityExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
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

                    // ---- Suburb ----
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
                            enabled = city.isNotBlank() && !isSaving,
                            label = { Text("Suburb") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(suburbExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = suburbExpanded,
                            onDismissRequest = { suburbExpanded = false }
                        ) {
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
                }
            }


            // SECTION 3 - Address
            SectionHeader(
                icon = Icons.Default.Place,
                title = "Address"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = street,
                        onValueChange = { street = it },
                        label = { Text("Street Address *") },
                        placeholder = { Text("e.g. 48 Allamanda Rd") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        enabled = !isSaving
                    )

                    OutlinedTextField(
                        value = postalCode,
                        onValueChange = { input ->
                            if (input.length <= 4 && input.all { it.isDigit() }) {
                                postalCode = input
                            }
                        },
                        label = { Text("Postal Code *") },
                        placeholder = { Text("e.g. 2092") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        enabled = !isSaving
                    )

                    // ---- Address preview ----
                    if (previewAddress.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BabyBlueSoft,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Place,
                                        contentDescription = null,
                                        tint = BabyBlueDeep,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Full Address Preview",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BabyBlueDeep,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    previewAddress,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))


            // SUBMIT

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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeAccent,
                    contentColor = Color.White
                ),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                } else {
                    Text(
                        "Add Building",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


// SECTION HEADER

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