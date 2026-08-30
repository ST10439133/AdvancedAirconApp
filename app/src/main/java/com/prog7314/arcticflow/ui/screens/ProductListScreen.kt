package com.prog7314.arcticflow.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prog7314.arcticflow.auth.AuthViewModel
import com.prog7314.arcticflow.data.entities.Product
import com.prog7314.arcticflow.data.entities.SortType
import com.prog7314.arcticflow.viewmodels.ProductViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    viewModel: ProductViewModel,
    authViewModel: AuthViewModel,
    onNavigateToSettings: () -> Unit
) {
    val products by viewModel.products.collectAsStateWithLifecycle(initialValue = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val brands by viewModel.brands.collectAsStateWithLifecycle()
    val sortType by viewModel.sortType.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ArcticFlow") },
                actions = {
                    // Settings Button
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings"
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
                .padding(16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchProducts(it) },
                label = { Text("Search products...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Brand Filter Dropdown
            if (brands.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }
                var selectedBrand by remember { mutableStateOf<String?>(null) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedBrand ?: "All Brands",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Filter by Brand") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
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

            // Sort Options
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

            // Product List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(products) { product ->
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
            }

            // Favorite button using text/emoji
            TextButton(
                onClick = onFavoriteClick,
                modifier = Modifier.width(60.dp)
            ) {
                Text(
                    text = if (product.isFavorite) "❤️" else "🤍",
                    fontSize = 24.sp
                )
            }
        }
    }
}