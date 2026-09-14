// app/src/main/java/com/prog7314/arcticflow/ui/screens/ProductListScreen.kt
package com.prog7314.arcticflow.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.prog7314.arcticflow.data.entities.Product
import com.prog7314.arcticflow.data.entities.SortType
import com.prog7314.arcticflow.data.network.SupabaseManager
import com.prog7314.arcticflow.navigation.NavManager
import com.prog7314.arcticflow.ui.components.openPdfInViewer
import com.prog7314.arcticflow.viewmodels.ProductViewModel
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    viewModel: ProductViewModel,
    navManager: NavManager
) {
    val products by viewModel.products.collectAsStateWithLifecycle(initialValue = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val brands by viewModel.brands.collectAsStateWithLifecycle()
    val sortType by viewModel.sortType.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    var selectedBrand by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ArcticFlow") },
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
                .padding(16.dp)
        ) {
            // ============================================================
            // 🧪 TEMPORARY — SUPABASE CONNECTION TEST BUTTON
            // Remove once you've confirmed the connection works.
            // ============================================================
            Button(
                onClick = {
                    scope.launch {
                        Log.d("SupabaseTest", "=====================================")
                        Log.d("SupabaseTest", "Testing Supabase connection...")
                        try {
                            // 1. Log the URL we're using
                            Log.d("SupabaseTest",
                                "Client URL: ${SupabaseManager.client.supabaseUrl}")

                            // 2. Make a REAL network call — list files in "products"
                            val files = SupabaseManager.client
                                .storage
                                .from("products")
                                .list()

                            Log.d("SupabaseTest",
                                "SUCCESS! Found ${files.size} file(s) in 'products' bucket")
                            files.forEach { file ->
                                Log.d("SupabaseTest",
                                    "  - ${file.name}")
                            }
                            Log.d("SupabaseTest", "CONNECTION VERIFIED ✅")
                        } catch (e: Exception) {
                            Log.e("SupabaseTest", "CONNECTION FAILED ❌", e)
                            Log.e("SupabaseTest", "Error: ${e.message}")
                        }
                        Log.d("SupabaseTest", "=====================================")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🧪 Test Supabase Connection")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ============================================================
            // Search Bar
            // ============================================================
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchProducts(it) },
                label = { Text("Search products...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ============================================================
            // Brand Filter
            // ============================================================
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
                            .menuAnchor()
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
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ============================================================
            // Sort Chips
            // ============================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = sortType == SortType.NAME_ASC,
                    onClick = { viewModel.sortProducts(SortType.NAME_ASC) },
                    label = { Text("A-Z") }
                )
                FilterChip(
                    selected = sortType == SortType.PRICE_LOW_TO_HIGH,
                    onClick = { viewModel.sortProducts(SortType.PRICE_LOW_TO_HIGH) },
                    label = { Text("Price: Low") }
                )
                FilterChip(
                    selected = sortType == SortType.PRICE_HIGH_TO_LOW,
                    onClick = { viewModel.sortProducts(SortType.PRICE_HIGH_TO_LOW) },
                    label = { Text("Price: High") }
                )
                FilterChip(
                    selected = sortType == SortType.RATING_HIGH_TO_LOW,
                    onClick = { viewModel.sortProducts(SortType.RATING_HIGH_TO_LOW) },
                    label = { Text("Rating") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ============================================================
            // Products List
            // ============================================================
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
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

@Composable
fun ProductCard(
    product: Product,
    onFavoriteClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ===== PRODUCT IMAGE (from Supabase) =====
            AsyncImage(
                model = product.imagePath,
                contentDescription = product.name,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            // ===== PRODUCT DETAILS =====
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${product.brand} - ${product.model}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "R${String.format(Locale.US, "%.2f", product.price)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${product.btu} BTU | ⭐ ${String.format(Locale.US, "%.1f", product.rating)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // ===== CATALOGUE BUTTON =====
                if (product.brochurePath.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = {
                            openPdfInViewer(context, product.brochurePath)
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = "View Catalogue",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "View Catalogue",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            // ===== FAVORITE BUTTON =====
            TextButton(
                onClick = onFavoriteClick,
                modifier = Modifier.width(52.dp)
            ) {
                Text(
                    text = if (product.isFavorite) "❤️" else "🤍",
                    fontSize = 22.sp
                )
            }
        }
    }
}