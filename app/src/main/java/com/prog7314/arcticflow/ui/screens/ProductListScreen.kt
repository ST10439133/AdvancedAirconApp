// app/src/main/java/com/prog7314/arcticflow/ui/screens/ProductListScreen.kt
package com.prog7314.arcticflow.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.prog7314.arcticflow.data.entities.Product
import com.prog7314.arcticflow.data.entities.SortType
import com.prog7314.arcticflow.data.network.SupabaseManager
import com.prog7314.arcticflow.navigation.NavManager
import com.prog7314.arcticflow.viewmodels.ProductViewModel
import java.util.Locale

// ============================================================
// BRAND TOKENS
// ============================================================
private val BabyBlue     = Color(0xFF4FA8D8)
private val BabyBlueDeep = Color(0xFF2E7BA6)
private val BabyBlueSoft = Color(0xFFE1F1FB)
private val OrangeAccent = Color(0xFFF7941D)

// ============================================================
// SUPABASE URL HELPERS
// ============================================================

fun Product.fullAssetUrl(relativePath: String): String {
    if (relativePath.isBlank()) return ""
    if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) {
        return relativePath
    }
    return SupabaseManager.productBaseUrl + relativePath.trimStart('/')
}

fun Product.fullImageUrl(): String = fullAssetUrl(imagePath)

fun Product.fullBrochureUrl(): String = fullAssetUrl(brochurePath)

// ============================================================
// PRODUCT LIST SCREEN
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    viewModel: ProductViewModel,
    navManager: NavManager,
    onBackToDashboard: () -> Unit
) {
    val products by viewModel.products.collectAsStateWithLifecycle(initialValue = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val brands by viewModel.brands.collectAsStateWithLifecycle()
    val sortType by viewModel.sortType.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    var selectedBrand by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Products") },
                navigationIcon = {
                    IconButton(onClick = onBackToDashboard) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Dashboard"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navManager.navigateToSettings() }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
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
        ) {
            // ============================================================
            // BTU CALCULATOR CTA
            // ============================================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { navManager.navigateToBTUCalculator() },
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(BabyBlue, BabyBlueDeep)))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.22f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Calculate,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "BTU Calculator",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "Estimate cooling load for a room",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ===== Search =====
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchProducts(it) },
                label = { Text("Search products...") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ===== Brand filter =====
            if (brands.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedBrand ?: "All Brands",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Filter by Brand") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Brands") },
                            onClick = {
                                selectedBrand = null
                                viewModel.filterByBrand(null)
                                expanded = false
                            }
                        )
                        brands.forEach { brand ->
                            DropdownMenuItem(
                                text = { Text(brand) },
                                onClick = {
                                    selectedBrand = brand
                                    viewModel.filterByBrand(brand)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // ===== Sort chips =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = sortType == SortType.NAME_ASC,
                    onClick = { viewModel.sortProducts(SortType.NAME_ASC) },
                    label = { Text("A-Z", maxLines = 1, softWrap = false) }
                )
                FilterChip(
                    selected = sortType == SortType.PRICE_LOW_TO_HIGH,
                    onClick = { viewModel.sortProducts(SortType.PRICE_LOW_TO_HIGH) },
                    label = { Text("Price ↑", maxLines = 1, softWrap = false) }
                )
                FilterChip(
                    selected = sortType == SortType.PRICE_HIGH_TO_LOW,
                    onClick = { viewModel.sortProducts(SortType.PRICE_HIGH_TO_LOW) },
                    label = { Text("Price ↓", maxLines = 1, softWrap = false) }
                )
                FilterChip(
                    selected = sortType == SortType.RATING_HIGH_TO_LOW,
                    onClick = { viewModel.sortProducts(SortType.RATING_HIGH_TO_LOW) },
                    label = { Text("Rating", maxLines = 1, softWrap = false) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ===== Results count =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Catalogue",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${products.size} result${if (products.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ===== Products List =====
            if (products.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = BabyBlueSoft,
                            modifier = Modifier.size(88.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AcUnit,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = BabyBlueDeep
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No products match",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Try adjusting your filters or search.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        ProductCard(
                            product = product,
                            onFavoriteClick = {
                                viewModel.toggleFavorite(product.id, !product.isFavorite)
                            }
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// PRODUCT CARD
// ============================================================

@Composable
fun ProductCard(
    product: Product,
    onFavoriteClick: () -> Unit
) {
    val context = LocalContext.current
    val imageUrl = product.fullImageUrl()
    var imageFailed by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ===== PRODUCT IMAGE =====
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(14.dp)),
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
                            modifier = Modifier.size(38.dp),
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
                                    Log.d("ImgDebug", "⏳ Start: $imageUrl")
                                },
                                onSuccess = { _, _ ->
                                    Log.d("ImgDebug", "✅ Loaded: $imageUrl")
                                },
                                onError = { _, result ->
                                    Log.e(
                                        "ImgDebug",
                                        "❌ Failed: $imageUrl | ${result.throwable.message}",
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

            // ===== DETAILS =====
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${product.brand} · ${product.model}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "R${String.format(Locale.US, "%.2f", product.price)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = BabyBlueDeep
                    )
                    Spacer(Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = BabyBlueSoft
                    ) {
                        Text(
                            "${product.btu} BTU",
                            style = MaterialTheme.typography.labelSmall,
                            color = BabyBlueDeep,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "⭐ ${String.format(Locale.US, "%.1f", product.rating)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}