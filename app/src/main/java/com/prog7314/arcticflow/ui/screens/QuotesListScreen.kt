// app/src/main/java/com/prog7314/arcticflow/ui/screens/QuotesListScreen.kt
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
import com.prog7314.arcticflow.data.entities.Quote
import com.prog7314.arcticflow.data.entities.QuoteStatus
import com.prog7314.arcticflow.navigation.NavManager
import com.prog7314.arcticflow.viewmodels.QuoteViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotesListScreen(
    viewModel: QuoteViewModel,
    userId: String,
    isCustomer: Boolean,
    navManager: NavManager,
    onBackToDashboard: () -> Unit
) {
    val quotes by (if (isCustomer) viewModel.getQuotesForCustomer(userId)
    else viewModel.getQuotesForTechnician(userId))
        .collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isCustomer) "My Quotes" else "Sent Quotes") },
                navigationIcon = {
                    IconButton(onClick = onBackToDashboard) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Dashboard"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (quotes.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No quotes yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quotes, key = { it.id }) { quote ->
                    QuoteRow(quote) { }
                }
            }
        }
    }
}

@Composable
fun QuoteRow(quote: Quote, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Quote #${quote.id}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        quote.buildingName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        quote.issueType,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Created: ${dateFormat.format(Date(quote.createdAt))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Badge(
                    containerColor = when (quote.status) {
                        QuoteStatus.PENDING  -> Color(0xFFFF9800)
                        QuoteStatus.ACCEPTED -> Color(0xFF4CAF50)
                        QuoteStatus.DECLINED -> Color(0xFFF44336)
                        QuoteStatus.EXPIRED  -> Color.Gray
                    }
                ) {
                    Text(quote.status.name, color = Color.White)
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                "Total: R${String.format(Locale.US, "%.2f", quote.grandTotal)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}