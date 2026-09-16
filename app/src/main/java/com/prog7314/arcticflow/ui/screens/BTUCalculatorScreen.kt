// app/src/main/java/com/prog7314/arcticflow/ui/screens/BTUCalculatorScreen.kt
package com.prog7314.arcticflow.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.prog7314.arcticflow.data.entities.Product
import com.prog7314.arcticflow.navigation.NavManager
import com.prog7314.arcticflow.viewmodels.ProductViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BTUCalculatorScreen(
    navManager: NavManager
) {
    // ── Product data (read-only, side-effect-free) ──
    val productViewModel: ProductViewModel = viewModel()
    val allProducts by productViewModel.products.collectAsStateWithLifecycle(initialValue = emptyList())

    // ── Inputs (metric system) ──
    var lengthM by remember { mutableStateOf("6.0") }        // metres
    var widthM by remember { mutableStateOf("4.5") }         // metres
    var heightM by remember { mutableStateOf("2.7") }        // metres
    var windows by remember { mutableStateOf("2") }
    var occupants by remember { mutableStateOf("3") }
    var sunExposure by remember { mutableStateOf("Medium") }
    var insulation by remember { mutableStateOf("Good") }
    var roomType by remember { mutableStateOf("Bedroom") }

    // ── Results ──
    var calculatedBTU by remember { mutableStateOf<Double?>(null) }
    var calculatedKW by remember { mutableStateOf<Double?>(null) }
    var calculatedTons by remember { mutableStateOf<Double?>(null) }
    var suggestedProducts by remember { mutableStateOf<List<Product>>(emptyList()) }

    val sunOptions = listOf("Low", "Medium", "High")
    val insulationOptions = listOf("Poor", "Average", "Good", "Excellent")
    val roomTypeOptions = listOf("Bedroom", "Lounge", "Office", "Server Room", "Commercial")

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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Estimate cooling load using South African metric units",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ============ ROOM DIMENSIONS (metres) ============
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = lengthM,
                    onValueChange = { lengthM = it },
                    label = { Text("Length (m)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = widthM,
                    onValueChange = { widthM = it },
                    label = { Text("Width (m)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = heightM,
                    onValueChange = { heightM = it },
                    label = { Text("Height (m)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }

            // ============ WINDOWS & OCCUPANTS ============
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = windows,
                    onValueChange = { windows = it },
                    label = { Text("Windows") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = occupants,
                    onValueChange = { occupants = it },
                    label = { Text("Occupants") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            // ============ ROOM TYPE ============
            Column {
                Text(
                    text = "Room Type",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // First 3 fit nicely; wrap the last two manually
                    roomTypeOptions.take(3).forEach { opt ->
                        FilterChip(
                            selected = roomType == opt,
                            onClick = { roomType = opt },
                            label = { Text(opt, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    roomTypeOptions.drop(3).forEach { opt ->
                        FilterChip(
                            selected = roomType == opt,
                            onClick = { roomType = opt },
                            label = { Text(opt, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // ============ SUN EXPOSURE ============
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
                    sunOptions.forEach { opt ->
                        FilterChip(
                            selected = sunExposure == opt,
                            onClick = { sunExposure = opt },
                            label = { Text(opt) }
                        )
                    }
                }
            }

            // ============ INSULATION ============
            Column {
                Text(
                    text = "Insulation Quality",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    insulationOptions.forEach { opt ->
                        FilterChip(
                            selected = insulation == opt,
                            onClick = { insulation = opt },
                            label = { Text(opt, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // ============ CALCULATE BUTTON ============
            Button(
                onClick = {
                    val l = lengthM.toDoubleOrNull() ?: 0.0
                    val w = widthM.toDoubleOrNull() ?: 0.0
                    val h = heightM.toDoubleOrNull() ?: 0.0
                    val win = windows.toIntOrNull() ?: 0
                    val occ = occupants.toIntOrNull() ?: 0

                    // 1. Volume in m³
                    val volume = l * w * h

                    // 2. Base cooling load (metric): ~180 BTU per m³ (SA climate)
                    val baseBTU = volume * 180.0

                    // 3. Window heat gain: ~1000 BTU per window
                    val windowBTU = win * 1000.0

                    // 4. Occupant heat gain: ~600 BTU per person
                    val occupantBTU = occ * 600.0

                    // 5. Multipliers
                    val sunFactor = when (sunExposure) {
                        "High" -> 1.30
                        "Medium" -> 1.10
                        else -> 1.00
                    }
                    val insulationFactor = when (insulation) {
                        "Poor" -> 1.40
                        "Average" -> 1.20
                        "Good" -> 1.00
                        "Excellent" -> 0.85
                        else -> 1.00
                    }
                    val roomFactor = when (roomType) {
                        "Server Room" -> 1.60
                        "Commercial" -> 1.30
                        "Office" -> 1.15
                        "Lounge" -> 1.10
                        else -> 1.00   // Bedroom
                    }

                    // 6. Total BTU
                    val totalBTU =
                        (baseBTU + windowBTU + occupantBTU) * sunFactor * insulationFactor * roomFactor

                    calculatedBTU = totalBTU
                    calculatedKW = totalBTU / 3412.0        // kW
                    calculatedTons = totalBTU / 12000.0     // Refrigeration tons

                    // 7. Find matching products (±20% of calculated BTU)
                    val lower = totalBTU * 0.8
                    val upper = totalBTU * 1.2
                    suggestedProducts = allProducts
                        .filter { it.btu in lower.toInt()..upper.toInt() }
                        .sortedBy { it.price }
                        .take(3)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Calculate Load")
            }

            // ============ RESULT ============
            if (calculatedBTU != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "CALCULATED REQUIREMENT",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.US, "%.0f BTU/hr", calculatedBTU),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = String.format(Locale.US, "%.2f kW", calculatedKW ?: 0.0),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = String.format(Locale.US, "%.2f Tons", calculatedTons ?: 0.0),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ============ SUGGESTED PRODUCTS ============
                if (suggestedProducts.isNotEmpty()) {
                    Text(
                        text = "SUGGESTED PRODUCTS (${suggestedProducts.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    suggestedProducts.forEach { product ->
                        SuggestedProductCard(product = product)
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "ℹ️ Suggestions are for reference only and do not affect your projects or quotes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "No matching units in stock",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "No products in your catalogue fall within ±20% of ${"%.0f".format(calculatedBTU)} BTU. Try adjusting inputs or check the Products list.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ============================================================
// SUGGESTED PRODUCT CARD
// ============================================================
@Composable
fun SuggestedProductCard(product: Product) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = product.imagePath,
                contentDescription = product.name,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${product.brand} · ${product.model}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${product.btu} BTU · ${String.format(Locale.US, "%.1f", product.btu / 3412.0)} kW",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "R${String.format(Locale.US, "%.2f", product.price)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}