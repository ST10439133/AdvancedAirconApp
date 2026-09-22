// app/src/main/java/com/prog7314/arcticflow/ui/screens/ManagerDashboardScreen.kt
//References (Material Design)
//Google (2024) Material Design 3. Available at: https://m3.material.io/ (Accessed: 22 September 2026).
package com.insy7315.advancedairconapp.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.insy7315.advancedairconapp.data.ArcticFlowDatabase
import com.insy7315.advancedairconapp.data.entities.Quote
import com.insy7315.advancedairconapp.navigation.NavManager
import com.insy7315.advancedairconapp.ui.components.AppTopBar
import com.insy7315.advancedairconapp.viewmodels.ManagerDashboardViewModel
import com.insy7315.advancedairconapp.viewmodels.NotificationViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


// ============================================================
// BRAND TOKENS
// ============================================================
private val BabyBlue     = Color(0xFF4FA8D8)
private val BabyBlueDeep = Color(0xFF2E7BA6)
private val BabyBlueSoft = Color(0xFFE1F1FB)
private val OrangeAccent = Color(0xFFF7941D)
private val OrangeSoft   = Color(0xFFFFEBD2)
private val SuccessGreen = Color(0xFF2E7D32)

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



    LaunchedEffect(Unit) {
        notificationViewModel.addSampleNotifications()
        viewModel.refreshFromServer("MANAGER")
    }

    val buildings by viewModel.buildings.collectAsStateWithLifecycle(initialValue = emptyList())
    val requests by viewModel.requests.collectAsStateWithLifecycle(initialValue = emptyList())
    val pendingQuotes by viewModel.pendingQuotes.collectAsStateWithLifecycle(initialValue = emptyList())
    val acceptedQuotes by viewModel.acceptedQuotes.collectAsStateWithLifecycle(initialValue = emptyList())
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle(initialValue = null)

    var quoteToSchedule by remember { mutableStateOf<Quote?>(null) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Dashboard",
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ===== WELCOME =====
            item {
                val firstName = currentUser
                    ?.displayName
                    ?.substringBefore("|")
                    ?.trim()
                    ?.substringBefore(" ")
                    .orEmpty()
                    .ifBlank { "Manager" }

                Column {
                    Text(
                        "Welcome back,",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        firstName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // =========================================================
            // OVERVIEW
            // =========================================================
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Overview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "All time",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Brush.linearGradient(listOf(BabyBlue, BabyBlueDeep)))
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.22f),
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ReceiptLong,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Pending quotes",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                    Text(
                                        pendingQuotes.size.toString(),
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        if (pendingQuotes.isEmpty()) "You're all caught up"
                                        else "Awaiting your approval",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }

                    // Supporting tiles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OverviewTile(
                            value = buildings.size.toString(),
                            label = "Buildings",
                            icon = Icons.Default.Business,
                            tint = BabyBlueDeep,
                            modifier = Modifier.weight(1f)
                        )
                        OverviewTile(
                            value = requests.size.toString(),
                            label = "Requests",
                            icon = Icons.AutoMirrored.Filled.Assignment,
                            tint = OrangeAccent,
                            modifier = Modifier.weight(1f)
                        )
                        OverviewTile(
                            value = acceptedQuotes.size.toString(),
                            label = "Accepted",
                            icon = Icons.Default.CheckCircle,
                            tint = SuccessGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // =========================================================
            // QUOTES AWAITING APPROVAL
            // =========================================================
            item {
                Text(
                    "Quotes Awaiting Your Approval",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (pendingQuotes.isEmpty()) {
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = BabyBlueSoft,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ReceiptLong,
                                        contentDescription = null,
                                        modifier = Modifier.size(30.dp),
                                        tint = BabyBlueDeep
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No pending quotes",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "New quotes will appear here for approval.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
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

            // =========================================================
            //  QUICK ACTIONS
            // =========================================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Quick Actions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(14.dp))

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
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
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
// OVERVIEW TILE
// ============================================================
@Composable
fun OverviewTile(
    value: String,
    label: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = tint.copy(alpha = 0.14f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
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
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = BabyBlueSoft,
            modifier = Modifier.size(50.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = BabyBlueDeep,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            color = MaterialTheme.colorScheme.onSurface
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
        shape = RoundedCornerShape(16.dp),
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
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = BabyBlueDeep
                    )
                    Text(
                        quote.buildingName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        quote.issueType,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Submitted ${dateFmt.format(Date(quote.createdAt))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "R${String.format(Locale.US, "%.2f", quote.grandTotal)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = BabyBlueDeep
                )
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(10.dp))

            Text(
                "Scope of Work",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                quote.scopeOfWork,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4
            )

            Spacer(Modifier.height(14.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
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
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeAccent,
                        contentColor = Color.White
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
// SCHEDULE ACCEPT DIALOG
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
        shape = RoundedCornerShape(20.dp),
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
                    color = BabyBlueDeep
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
                        .clickable { picker.show() },
                    shape = RoundedCornerShape(12.dp)
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
                            modifier = Modifier.size(20.dp),
                            colors = RadioButtonDefaults.colors(selectedColor = BabyBlueDeep)
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
                enabled = selectedDate != null && selectedSlot.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
            ) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}