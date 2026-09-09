// app/src/main/java/com/prog7314/arcticflow/ui/screens/BTUCalculatorScreen.kt
package com.prog7314.arcticflow.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight  // <-- ADD THIS IMPORT
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.prog7314.arcticflow.navigation.NavManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BTUCalculatorScreen(
    navManager: NavManager
) {
    var length by remember { mutableStateOf("20") }
    var width by remember { mutableStateOf("15") }
    var ceiling by remember { mutableStateOf("10") }
    var windows by remember { mutableStateOf("4") }
    var occupants by remember { mutableStateOf("3") }
    var sunExposure by remember { mutableStateOf("Medium") }
    var insulation by remember { mutableStateOf("Good") }

    var calculatedBTU by remember { mutableStateOf<Double?>(null) }
    var calculatedTons by remember { mutableStateOf<Double?>(null) }

    val sunExposureOptions = listOf("Low", "Medium", "High")
    val insulationOptions = listOf("Poor", "Average", "Good", "Excellent")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BTU Calculator") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Estimate system load for HVAC deployment",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = length,
                    onValueChange = { length = it },
                    label = { Text("Length (ft)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = width,
                    onValueChange = { width = it },
                    label = { Text("Width (ft)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = ceiling,
                    onValueChange = { ceiling = it },
                    label = { Text("Ceiling (ft)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = windows,
                    onValueChange = { windows = it },
                    label = { Text("Windows") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = occupants,
                    onValueChange = { occupants = it },
                    label = { Text("Occupants") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            // Sun Exposure
            Column {
                Text(
                    text = "Sun Exposure",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    sunExposureOptions.forEach { option ->
                        FilterChip(
                            selected = sunExposure == option,
                            onClick = { sunExposure = option },
                            label = { Text(option) }
                        )
                    }
                }
            }

            // Insulation
            Column {
                Text(
                    text = "Insulation",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    insulationOptions.forEach { option ->
                        FilterChip(
                            selected = insulation == option,
                            onClick = { insulation = option },
                            label = { Text(option) }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val l = length.toDoubleOrNull() ?: 0.0
                    val w = width.toDoubleOrNull() ?: 0.0
                    val c = ceiling.toDoubleOrNull() ?: 0.0
                    val win = windows.toIntOrNull() ?: 0
                    val occ = occupants.toIntOrNull() ?: 0

                    // Simple BTU calculation
                    val baseBTU = l * w * c * 0.1 // Base load
                    val windowLoad = win * 1000.0 // 1000 BTU per window
                    val occupantLoad = occ * 600.0 // 600 BTU per person
                    val sunFactor = when (sunExposure) {
                        "High" -> 1.3
                        "Medium" -> 1.1
                        else -> 1.0
                    }
                    val insulationFactor = when (insulation) {
                        "Poor" -> 1.4
                        "Average" -> 1.2
                        "Good" -> 1.0
                        "Excellent" -> 0.8
                        else -> 1.0
                    }

                    val totalBTU = (baseBTU + windowLoad + occupantLoad) * sunFactor * insulationFactor
                    calculatedBTU = totalBTU
                    calculatedTons = totalBTU / 12000.0
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Calculate Load")
            }

            if (calculatedBTU != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "CALCULATED REQUIREMENT",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = String.format("%.0f BTU/hr", calculatedBTU),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold  // Now works with import
                        )
                        Text(
                            text = String.format("%.1f Tons", calculatedTons),
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "SUGGESTED PRODUCTS (2)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        SuggestedProductItem(
                            name = "Carrier Infinity 1.5 Ton AC",
                            specs = "18,000 BTU · \$3,100"
                        )
                        SuggestedProductItem(
                            name = "Trane CleanEffects 2.0 Ton",
                            specs = "24,000 BTU · \$3,850"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestedProductItem(
    name: String,
    specs: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(name, style = MaterialTheme.typography.bodyMedium)
            Text(specs, style = MaterialTheme.typography.bodySmall)
        }
        Button(
            onClick = { /* Select product */ },
            modifier = Modifier.height(32.dp)
        ) {
            Text("Select")
        }
    }
}