// app/src/main/java/com/prog7314/arcticflow/ui/screens/QuoteHistoryScreen.kt
package com.prog7314.arcticflow.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prog7314.arcticflow.navigation.NavManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteHistoryScreen(
    navManager: NavManager
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Pending", "Accepted", "Declined")

    val quotes = listOf(
        QuoteHistoryItem("Q-1896 - Jul 18", "Apex Tech Plaza", "Boiler Installation", "$14,500", "Pending"),
        QuoteHistoryItem("Q-1895 - Jul 15", "Oakwood Medical", "Chiller Overhaul", "$8,200", "Accepted"),
        QuoteHistoryItem("Q-1894 - Jul 10", "Riverview Appts", "Thermostat Swap", "$1,850", "Declined")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quote History") },
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
                .padding(16.dp)
        ) {
            // Filter chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quotes) { quote ->
                    QuoteHistoryItemCard(quote = quote)
                }
            }
        }
    }
}

data class QuoteHistoryItem(
    val id: String,
    val client: String,
    val service: String,
    val amount: String,
    val status: String
)

@Composable
fun QuoteHistoryItemCard(
    quote: QuoteHistoryItem
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Navigate to quote details */ },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = quote.id,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = quote.client,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = quote.service,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = quote.amount,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Badge(
                containerColor = when (quote.status) {
                    "Pending" -> Color(0xFFFF9800)
                    "Accepted" -> Color.Green
                    "Declined" -> Color.Red
                    else -> Color.Gray
                }
            ) {
                Text(quote.status)
            }
        }
    }
}