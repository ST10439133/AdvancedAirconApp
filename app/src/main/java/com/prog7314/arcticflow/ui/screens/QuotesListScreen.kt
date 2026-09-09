// app/src/main/java/com/prog7314/arcticflow/ui/screens/QuotesListScreen.kt
package com.prog7314.arcticflow.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.prog7314.arcticflow.data.entities.*
import com.prog7314.arcticflow.navigation.NavManager
import com.prog7314.arcticflow.viewmodels.QuoteViewModel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private val TAG = "QuotesListScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotesListScreen(
    viewModel: QuoteViewModel,
    userId: String,
    isCustomer: Boolean,
    navManager: NavManager
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Pending", "Accepted", "Declined")

    // For Customers: Show their quotes
    val quotes by if (isCustomer) {
        viewModel.getQuotesForCustomer(userId)
    } else {
        viewModel.getQuotesForTechnician(userId)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    // For Technicians: Get ALL pending service requests
    val pendingRequests by if (!isCustomer) {
        viewModel.getPendingServiceRequests()
    } else {
        flow { emit(emptyList<ServiceRequest>()) }
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedQuote by remember { mutableStateOf<Quote?>(null) }
    var showQuoteDetails by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var quoteToSchedule by remember { mutableStateOf<Quote?>(null) }

    // Log for debugging
    LaunchedEffect(pendingRequests) {
        Log.d(TAG, "Pending requests count: ${pendingRequests.size}")
        pendingRequests.forEach { request ->
            Log.d(TAG, "Request: #${request.id} - ${request.buildingName} - ${request.issueType} - ${request.status}")
        }
    }

    LaunchedEffect(quotes) {
        Log.d(TAG, "Quotes count: ${quotes.size}")
        quotes.forEach { quote ->
            Log.d(TAG, "Quote: #${quote.id} - ${quote.buildingName} - ${quote.status}")
        }
    }

    // Filter quotes based on selected filter
    val filteredQuotes = try {
        when (selectedFilter) {
            "Pending" -> quotes.filter { it.status == QuoteStatus.PENDING }
            "Accepted" -> quotes.filter { it.status == QuoteStatus.ACCEPTED }
            "Declined" -> quotes.filter { it.status == QuoteStatus.DECLINED }
            else -> quotes
        }
    } catch (e: Exception) {
        emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isCustomer) "My Quotes" else "Pending Requests")
                },
                navigationIcon = {
                    IconButton(onClick = { navManager.navigateToMain() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Dashboard")
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
            // Show status message for debugging
            Text(
                text = if (!isCustomer) {
                    "Pending Requests: ${pendingRequests.size}"
                } else {
                    "Your Quotes: ${quotes.size}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // For Technicians: Show pending service requests
            if (!isCustomer) {
                if (pendingRequests.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
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
                                text = "No Pending Requests",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Create a service request from the Manager side first",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { navManager.navigateToServiceRequest() }
                            ) {
                                Text("Create Service Request")
                            }
                        }
                    }
                } else {
                    // Filter chips for pending requests
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
                        items(pendingRequests) { request ->
                            PendingRequestCard(
                                request = request,
                                onClick = {
                                    Log.d(TAG, "Creating quote for request: ${request.id}")
                                    navManager.navigateToCreateQuote(request.id)
                                }
                            )
                        }
                    }
                }
            } else {
                // For Customers: Show their quotes
                if (quotes.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
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
                                text = "No quotes yet",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Quotes will appear here when created",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
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
                        items(filteredQuotes) { quote ->
                            QuoteCard(
                                quote = quote,
                                isCustomer = isCustomer,
                                onAccept = {
                                    quoteToSchedule = quote
                                    showScheduleDialog = true
                                },
                                onDecline = {
                                    try {
                                        coroutineScope.launch {
                                            viewModel.updateQuoteStatus(quote.id, QuoteStatus.DECLINED)
                                            Toast.makeText(context, "Quote declined.", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error declining quote: ${e.message}", Toast.LENGTH_SHORT).show()
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

    if (showScheduleDialog && quoteToSchedule != null) {
        JobSchedulingDialog(
            quoteId = quoteToSchedule!!.id,
            buildingName = quoteToSchedule!!.buildingName,
            onSchedule = { date, timeSlot ->
                coroutineScope.launch {
                    try {
                        viewModel.updateQuoteStatusWithSchedule(
                            quoteId = quoteToSchedule!!.id,
                            status = QuoteStatus.ACCEPTED,
                            scheduledDate = date,
                            timeSlot = timeSlot
                        )
                        Toast.makeText(context, "Quote accepted! Job scheduled.", Toast.LENGTH_LONG).show()
                        showScheduleDialog = false
                        quoteToSchedule = null
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = {
                showScheduleDialog = false
                quoteToSchedule = null
            }
        )
    }
}

// Pending Request Card for Technicians
@Composable
fun PendingRequestCard(
    request: ServiceRequest,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
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
                    text = "Request #${request.id}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = request.buildingName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = request.issueType,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Priority: ${request.priority.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = when (request.priority) {
                        RequestPriority.URGENT -> Color.Red
                        RequestPriority.HIGH -> Color(0xFFFF9800)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = "Created: ${formatDate(request.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Badge(
                    containerColor = Color(0xFFFF9800)
                ) {
                    Text("PENDING")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onClick,
                    modifier = Modifier.width(100.dp)
                ) {
                    Text("Create Quote")
                }
            }
        }
    }
}

// Quote Card for Customers
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
                        text = "Quote #${quote.id}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = quote.buildingName,
                        style = MaterialTheme.typography.bodyMedium,
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
                        else -> Color.Gray
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

            Text(
                text = "Created: ${formatDate(quote.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        Text("Accept & Schedule")
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

            if (quote.status == QuoteStatus.ACCEPTED) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "✅ Job Scheduled",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Green
                )
            }
        }
    }
}

// Quote Details Dialog
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
                Text("Quote #${quote.id}", style = MaterialTheme.typography.titleSmall)
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
                Text("Tax: R${String.format("%.2f", quote.taxAmount)}")
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

// Helper function to format date
private fun formatDate(timestamp: Long): String {
    return try {
        val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        format.format(Date(timestamp))
    } catch (e: Exception) {
        "Unknown date"
    }
}