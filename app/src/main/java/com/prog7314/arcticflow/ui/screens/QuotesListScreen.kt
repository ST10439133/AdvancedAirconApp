// app/src/main/java/com/prog7314/arcticflow/ui/screens/QuotesListScreen.kt
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prog7314.arcticflow.data.entities.Quote
import com.prog7314.arcticflow.data.entities.QuoteStatus
import com.prog7314.arcticflow.navigation.NavManager
import com.prog7314.arcticflow.viewmodels.QuoteViewModel
import kotlinx.coroutines.launch
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val quotes by (if (isCustomer) viewModel.getQuotesForCustomer(userId)
    else viewModel.getQuotesForTechnician(userId))
        .collectAsState(initial = emptyList())

    // Which quote is currently being scheduled (opens the Accept dialog)
    var quoteToSchedule by remember { mutableStateOf<Quote?>(null) }

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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (isCustomer) "No quotes yet"
                        else "You haven't sent any quotes yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (isCustomer) "Quotes sent by technicians will appear here."
                        else "Quotes you create will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                    QuoteRow(
                        quote = quote,
                        showActions = isCustomer && quote.status == QuoteStatus.PENDING,
                        onClick = { /* TODO: open quote details */ },
                        onAccept = { quoteToSchedule = quote },
                        onDecline = {
                            scope.launch {
                                viewModel.updateQuoteStatusWithSchedule(
                                    quoteId = quote.id,
                                    status = QuoteStatus.DECLINED
                                )
                                Toast.makeText(
                                    context,
                                    "Quote #${quote.id} declined.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }
            }
        }
    }

    // ===== Accept & Schedule dialog (manager only) =====
    quoteToSchedule?.let { quote ->
        ScheduleQuoteDialog(
            quote = quote,
            onDismiss = { quoteToSchedule = null },
            onConfirm = { date, slot ->
                scope.launch {
                    viewModel.updateQuoteStatusWithSchedule(
                        quoteId = quote.id,
                        status = QuoteStatus.ACCEPTED,
                        scheduledDate = date,
                        timeSlot = slot
                    )
                    Toast.makeText(
                        context,
                        "Quote #${quote.id} accepted. Job scheduled.",
                        Toast.LENGTH_LONG
                    ).show()
                    quoteToSchedule = null
                }
            }
        )
    }
}

// ============================================================
// QUOTE ROW — optionally shows Accept / Decline
// ============================================================
@Composable
fun QuoteRow(
    quote: Quote,
    showActions: Boolean,
    onClick: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
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

            // ===== ACCEPT / DECLINE (manager only, pending only) =====
            if (showActions) {
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
}

// ============================================================
// SCHEDULE QUOTE DIALOG
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleQuoteDialog(
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