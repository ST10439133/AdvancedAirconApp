// app/src/main/java/com/prog7314/arcticflow/ui/screens/ManagerDashboardScreen.kt
package com.prog7314.arcticflow.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prog7314.arcticflow.data.ArcticFlowDatabase
import com.prog7314.arcticflow.data.entities.Quote
import com.prog7314.arcticflow.navigation.NavManager
import com.prog7314.arcticflow.ui.components.AppTopBar
import com.prog7314.arcticflow.viewmodels.ManagerDashboardViewModel
import com.prog7314.arcticflow.viewmodels.NotificationViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerDashboardScreen(
    navManager: NavManager,
    userId: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = ArcticFlowDatabase.getDatabase(context)

    val viewModel: ManagerDashboardViewModel = viewModel(
        factory = ManagerDashboardViewModel.Factory(
            context.applicationContext as android.app.Application,
            database,
            userId
        )
    )
    val notificationViewModel: NotificationViewModel = viewModel(
        factory = NotificationViewModel.Factory(context, userId)
    )

    LaunchedEffect(Unit) { notificationViewModel.addSampleNotifications() }

    val buildings by viewModel.buildings.collectAsStateWithLifecycle(initialValue = emptyList())
    val requests by viewModel.requests.collectAsStateWithLifecycle(initialValue = emptyList())
    val pendingQuotes by viewModel.pendingQuotes.collectAsStateWithLifecycle(initialValue = emptyList())
    val acceptedQuotes by viewModel.acceptedQuotes.collectAsStateWithLifecycle(initialValue = emptyList())

    var quoteToSchedule by remember { mutableStateOf<Quote?>(null) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Manager Dashboard",
                navManager = navManager,
                notificationViewModel = notificationViewModel,
                actions = {
                    IconButton(onClick = { navManager.navigateToSettings() }) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ===== WELCOME =====
            item {
                Column {
                    Text(
                        "Welcome back,",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Manager",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ===== OVERVIEW =====
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "OVERVIEW",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            OverviewStatItem(buildings.size.toString(), "Buildings")
                            OverviewStatItem(requests.size.toString(), "Requests")
                            OverviewStatItem(pendingQuotes.size.toString(), "Pending")
                            OverviewStatItem(acceptedQuotes.size.toString(), "Accepted")
                        }
                    }
                }
            }

            // ===== QUICK ACTIONS =====
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Quick Actions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))

                        // Row 1
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickActionButton(
                                icon = Icons.Default.Add,
                                label = "Request",
                                onClick = { navManager.navigateToServiceRequest() },
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionButton(
                                icon = Icons.Default.RequestQuote,
                                label = "Quotes",
                                onClick = { navManager.navigateToMyQuotes() },
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionButton(
                                icon = Icons.Default.Business,
                                label = "Add Building",
                                onClick = { navManager.navigateToAddBuilding() },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        // Row 2
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickActionButton(
                                icon = Icons.Default.Calculate,
                                label = "BTU Calc",
                                onClick = { navManager.navigateToBTUCalculator() },
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionButton(
                                icon = Icons.Default.Store,
                                label = "Products",
                                onClick = { navManager.navigateToProducts() },
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionButton(
                                icon = Icons.Default.People,
                                label = "Technicians",
                                onClick = { /* TODO: navigate to technicians list */ },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ===== PENDING QUOTES HEADER =====
            item {
                Text(
                    "Quotes Awaiting Your Approval",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (pendingQuotes.isEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                null,
                                Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No pending quotes",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(pendingQuotes, key = { it.id }) { quote ->
                    PendingQuoteCard(
                        quote = quote,
                        onAccept = { quoteToSchedule = quote },
                        onDecline = {
                            scope.launch {
                                viewModel.declineQuote(quote.id)
                                Toast.makeText(
                                    context,
                                    "Quote declined.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }
            }
        }
    }

    // ===== SCHEDULING DIALOG =====
    quoteToSchedule?.let { quote ->
        ScheduleAcceptDialog(
            quote = quote,
            onDismiss = { quoteToSchedule = null },
            onConfirm = { date, slot ->
                scope.launch {
                    viewModel.acceptQuote(quote.id, date, slot)
                    Toast.makeText(
                        context,
                        "Quote accepted. Job scheduled.",
                        Toast.LENGTH_LONG
                    ).show()
                    quoteToSchedule = null
                }
            }
        )
    }
}

// ============================================================
// OVERVIEW STAT ITEM
// ============================================================
@Composable
fun OverviewStatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ============================================================
// QUICK ACTION BUTTON
// ============================================================
@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

// ============================================================
// PENDING QUOTE CARD
// ============================================================
@Composable
fun PendingQuoteCard(
    quote: Quote,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val dateFmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    Card(
        Modifier.fillMaxWidth(),
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
                        "Submitted: ${dateFmt.format(Date(quote.createdAt))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "R${String.format(Locale.US, "%.2f", quote.grandTotal)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            Text(
                "Scope of Work:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                quote.scopeOfWork,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4
            )

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Decline")
                }
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Accept")
                }
            }
        }
    }
}

// ============================================================
// SCHEDULE ACCEPT DIALOG — fixed layout
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleAcceptDialog(
    quote: Quote,
    onDismiss: () -> Unit,
    onConfirm: (Long, String) -> Unit
) {
    val context = LocalContext.current
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    var selectedSlot by remember { mutableStateOf("") }

    val slots = listOf(
        "08:00 AM - 09:30 AM",
        "09:30 AM - 11:00 AM",
        "11:00 AM - 12:30 PM",
        "12:30 PM - 02:00 PM",
        "02:00 PM - 03:30 PM",
        "03:30 PM - 05:00 PM"
    )

    val dateFmt = SimpleDateFormat("EEE, MMM d yyyy", Locale.getDefault())
    val cal = Calendar.getInstance()
    val picker = DatePickerDialog(
        context,
        { _, y, m, d ->
            val c = Calendar.getInstance()
            c.set(y, m, d, 9, 0)
            selectedDate = c.timeInMillis
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Accept & Schedule Job",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quote summary
                Text(
                    "Quote #${quote.id} for ${quote.buildingName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Total: R${String.format(Locale.US, "%.2f", quote.grandTotal)}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider()

                // Date
                OutlinedTextField(
                    value = selectedDate?.let { dateFmt.format(Date(it)) } ?: "Select date",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Scheduled Date") },
                    trailingIcon = {
                        IconButton(onClick = { picker.show() }) {
                            Icon(Icons.Default.CalendarMonth, "Pick date")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { picker.show() }
                )

                // Time slots
                Text(
                    "Select Time Slot:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )

                slots.forEach { slot ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedSlot = slot }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedSlot == slot,
                            onClick = { selectedSlot = slot },
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = slot,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedDate != null && selectedSlot.isNotBlank()) {
                        onConfirm(selectedDate!!, selectedSlot)
                    }
                },
                enabled = selectedDate != null && selectedSlot.isNotBlank()
            ) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}