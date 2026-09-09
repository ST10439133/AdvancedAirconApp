package com.prog7314.arcticflow.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prog7314.arcticflow.data.entities.Quote
import com.prog7314.arcticflow.data.entities.QuoteStatus
import com.prog7314.arcticflow.navigation.NavManager
import com.prog7314.arcticflow.viewmodels.QuoteViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotesListScreen(
    viewModel: QuoteViewModel,
    userId: String,
    isCustomer: Boolean = true,
    navManager: NavManager
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val quotes by if (isCustomer) {
        viewModel.getQuotesForCustomer(userId)
    } else {
        viewModel.getQuotesForTechnician(userId)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedQuote by remember { mutableStateOf<Quote?>(null) }
    var showQuoteDetails by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isCustomer) "My Quotes" else "Pending Requests") },
                navigationIcon = {
                    IconButton(onClick = { navManager.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (quotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isCustomer) "No quotes yet" else "No pending requests",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isCustomer) "Quotes will appear here when created" else "Service requests will appear here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quotes) { quote ->
                    QuoteCard(
                        quote = quote,
                        isCustomer = isCustomer,
                        onAccept = {
                            coroutineScope.launch {
                                viewModel.updateQuoteStatus(quote.id, QuoteStatus.ACCEPTED)
                                Toast.makeText(context, "Quote accepted! A job has been scheduled.", Toast.LENGTH_LONG).show()
                            }
                        },
                        onDecline = {
                            coroutineScope.launch {
                                viewModel.updateQuoteStatus(quote.id, QuoteStatus.DECLINED)
                                Toast.makeText(context, "Quote declined.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onClick = {
                            selectedQuote = quote
                            showQuoteDetails = true
                        }
                    )
                }
            }
        }
    }

    if (showQuoteDetails && selectedQuote != null) {
        QuoteDetailsDialog(
            quote = selectedQuote!!,
            isCustomer = isCustomer,
            onDismiss = {
                showQuoteDetails = false
                selectedQuote = null
            }
        )
    }
}



@Composable
fun QuoteCard(
    quote: Quote,
    isCustomer: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = quote.buildingName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = quote.issueType,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Badge(
                    containerColor = when (quote.status) {
                        QuoteStatus.PENDING -> Color(0xFFFF9800)
                        QuoteStatus.ACCEPTED -> Color.Green
                        QuoteStatus.DECLINED -> Color.Red
                        QuoteStatus.EXPIRED -> Color.Gray
                    }
                ) {
                    Text(quote.status.name)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Total: R${String.format("%.2f", quote.grandTotal)}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            if (isCustomer && quote.status == QuoteStatus.PENDING) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Green
                        )
                    ) {
                        Text("Accept")
                    }
                    OutlinedButton(
                        onClick = onDecline,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Red
                        )
                    ) {
                        Text("Decline")
                    }
                }
            }
        }
    }
}

@Composable
fun QuoteDetailsDialog(
    quote: Quote,
    isCustomer: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quote Details") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Building: ${quote.buildingName}")
                Text("Issue: ${quote.issueType}")
                Text("Status: ${quote.status.name}")
                Divider()
                Text("Scope of Work:", style = MaterialTheme.typography.titleSmall)
                Text(quote.scopeOfWork)
                Text("Parts Required:", style = MaterialTheme.typography.titleSmall)
                Text(quote.partsRequired)
                Divider()
                Text("Cost Breakdown:", style = MaterialTheme.typography.titleSmall)
                Text("Labor: R${String.format("%.2f", quote.laborCost)}")
                Text("Parts: R${String.format("%.2f", quote.partsCost)}")
                Text("Tax (15%): R${String.format("%.2f", quote.taxAmount)}")
                Text(
                    "Total: R${String.format("%.2f", quote.grandTotal)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                quote.notes?.let {
                    Divider()
                    Text("Notes:", style = MaterialTheme.typography.titleSmall)
                    Text(it)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}