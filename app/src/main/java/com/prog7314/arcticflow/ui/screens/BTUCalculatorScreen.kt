// app/src/main/java/com/prog7314/arcticflow/ui/screens/BTUCalculatorScreen.kt
package com.prog7314.arcticflow.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Window
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.prog7314.arcticflow.data.entities.Product
import com.prog7314.arcticflow.navigation.NavManager
import com.prog7314.arcticflow.viewmodels.ProductViewModel
import java.util.Locale

// ============================================================
// BRAND TOKENS
// ============================================================
private val BabyBlue        = Color(0xFF4FA8D8)
private val BabyBlueDeep    = Color(0xFF2E7BA6)
private val BabyBlueSoft    = Color(0xFFE1F1FB)
private val OrangeAccent    = Color(0xFFF7941D)
private val OrangeSoft      = Color(0xFFFFEBD2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BTUCalculatorScreen(
    navManager: NavManager
) {
    val productViewModel: ProductViewModel = viewModel()
    val allProducts by productViewModel.products
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var lengthM by remember { mutableStateOf("6.0") }
    var widthM by remember { mutableStateOf("4.5") }
    var heightM by remember { mutableStateOf("2.7") }
    var windows by remember { mutableStateOf("2") }
    var occupants by remember { mutableStateOf("3") }
    var sunExposure by remember { mutableStateOf("Medium") }
    var insulation by remember { mutableStateOf("Good") }
    var roomType by remember { mutableStateOf("Bedroom") }

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
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ============================================================
            // HERO HEADER
            // ============================================================
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
                                    Icons.Default.Calculate,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                "Cooling Load Estimator",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "South African metric units",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // ============================================================
            // SECTION 1 — Room Dimensions
            // ============================================================
            SectionHeader(icon = Icons.Default.CropSquare, title = "Room Dimensions (metres)")

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
                        OutlinedTextField(
                            value = lengthM,
                            onValueChange = { lengthM = it },
                            label = { Text("Length") },
                            suffix = { Text("m") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = widthM,
                            onValueChange = { widthM = it },
                            label = { Text("Width") },
                            suffix = { Text("m") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = heightM,
                            onValueChange = { heightM = it },
                            label = { Text("Height") },
                            suffix = { Text("m") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
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
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Window,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = occupants,
                            onValueChange = { occupants = it },
                            label = { Text("Occupants") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.People,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }
            }

            // ============================================================
            // SECTION 2 — Environment
            // ============================================================
            SectionHeader(icon = Icons.Default.WbSunny, title = "Environment")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Room type
                    Column {
                        Text(
                            "Room Type",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            roomTypeOptions.forEach { opt ->
                                FilterChip(
                                    selected = roomType == opt,
                                    onClick = { roomType = opt },
                                    label = { Text(opt, maxLines = 1, softWrap = false) }
                                )
                            }
                        }
                    }

                    // Sun exposure
                    Column {
                        Text(
                            "Sun Exposure",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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

                    // Insulation
                    Column {
                        Text(
                            "Insulation Quality",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            insulationOptions.forEach { opt ->
                                FilterChip(
                                    selected = insulation == opt,
                                    onClick = { insulation = opt },
                                    label = { Text(opt, maxLines = 1, softWrap = false) }
                                )
                            }
                        }
                    }
                }
            }

            // ============================================================
            // CTA
            // ============================================================
            Button(
                onClick = {
                    val l = lengthM.toDoubleOrNull() ?: 0.0
                    val w = widthM.toDoubleOrNull() ?: 0.0
                    val h = heightM.toDoubleOrNull() ?: 0.0
                    val win = windows.toIntOrNull() ?: 0
                    val occ = occupants.toIntOrNull() ?: 0

                    val volume = l * w * h
                    val baseBTU = volume * 180.0
                    val windowBTU = win * 1000.0
                    val occupantBTU = occ * 600.0

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
                        else -> 1.00
                    }

                    val totalBTU =
                        (baseBTU + windowBTU + occupantBTU) *
                                sunFactor * insulationFactor * roomFactor

                    calculatedBTU = totalBTU
                    calculatedKW = totalBTU / 3412.0
                    calculatedTons = totalBTU / 12000.0

                    val lower = totalBTU * 0.8
                    val upper = totalBTU * 1.2
                    suggestedProducts = allProducts
                        .filter { it.btu in lower.toInt()..upper.toInt() }
                        .sortedBy { it.price }
                        .take(3)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeAccent,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Calculate Load",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // ============================================================
            // RESULT
            // ============================================================
            if (calculatedBTU != null) {
                SectionHeader(icon = Icons.Default.LocalFireDepartment, title = "Result")

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
                            .padding(20.dp)
                    ) {
                        Column {
                            Text(
                                "Calculated Requirement",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                String.format(Locale.US, "%.0f", calculatedBTU) + " BTU/hr",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ResultPill(
                                    label = "kW",
                                    value = String.format(Locale.US, "%.2f", calculatedKW ?: 0.0)
                                )
                                ResultPill(
                                    label = "Tons",
                                    value = String.format(Locale.US, "%.2f", calculatedTons ?: 0.0)
                                )
                            }
                        }
                    }
                }

                // ============ SUGGESTED PRODUCTS ============
                if (suggestedProducts.isNotEmpty()) {
                    SectionHeader(
                        icon = Icons.Default.AcUnit,
                        title = "Suggested Units (${suggestedProducts.size})"
                    )

                    suggestedProducts.forEach { product ->
                        SuggestedProductCard(product = product)
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "ℹ️ Suggestions are for reference only and do not affect your projects or quotes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = OrangeSoft
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "No matching units in stock",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = OrangeAccent
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "No products in your catalogue fall within ±20% of ${
                                    "%.0f".format(calculatedBTU)
                                } BTU. Try adjusting inputs or check the Products list.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ============================================================
// SECTION HEADER
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
private fun ResultPill(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

// ============================================================
// SUGGESTED PRODUCT CARD
// ============================================================
@Composable
fun SuggestedProductCard(product: Product) {
    val context = LocalContext.current
    val imageUrl = product.fullImageUrl()
    var imageFailed by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (imageFailed) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BabyBlueSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AcUnit,
                            contentDescription = product.name,
                            modifier = Modifier.size(32.dp),
                            tint = BabyBlueDeep
                        )
                    }
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageUrl)
                            .crossfade(true)
                            .listener(
                                onStart = { _ ->
                                    Log.d("ImgDebug", "BTU  Start: $imageUrl")
                                },
                                onSuccess = { _, _ ->
                                    Log.d("ImgDebug", "BTU  Loaded: $imageUrl")
                                },
                                onError = { _, result ->
                                    Log.e(
                                        "ImgDebug",
                                        "BTU  Failed: $imageUrl | ${result.throwable.message}",
                                        result.throwable
                                    )
                                    imageFailed = true
                                }
                            )
                            .build(),
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${product.brand} · ${product.model}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${product.btu} BTU · ${
                        String.format(Locale.US, "%.1f", product.btu / 3412.0)
                    } kW",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "R${String.format(Locale.US, "%.2f", product.price)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = BabyBlueDeep,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}