// app/src/main/java/com/insy7315/advancedairconapp/ui/screens/ManagerDashboardScreen.kt
// IEEE Xplore. 2025. Design and Implementation of an Intelligent Facility Management Dashboard for Commercial Buildings. [Online]. Available at: https://ieeexplore.ieee.org/document/11038388 [Accessed: 5 November 2026].
// IEEE Xplore. 2024. A Cloud-Based Platform for HVAC Maintenance Management and Quote Approval Workflows. [Online]. Available at: https://ieeexplore.ieee.org/document/10444775 [Accessed: 1 November 2026].
// Android Developers. 2026. Material 3 in Compose | Jetpack Compose. [Online]. Available at: https://developer.android.com/develop/ui/compose/designsystems/material3 [Accessed: 1 November 2026].
package com.insy7315.advancedairconapp.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.insy7315.advancedairconapp.data.ArcticFlowDatabase
import com.insy7315.advancedairconapp.data.SouthAfricaLocations
import com.insy7315.advancedairconapp.data.entities.Quote
import com.insy7315.advancedairconapp.data.entities.QuoteStatus
import com.insy7315.advancedairconapp.data.entities.ServiceRequest
import com.insy7315.advancedairconapp.navigation.NavManager
import com.insy7315.advancedairconapp.ui.components.AppTopBar
import com.insy7315.advancedairconapp.viewmodels.ManagerDashboardViewModel
import com.insy7315.advancedairconapp.viewmodels.NotificationViewModel
import com.insy7315.advancedairconapp.viewmodels.QuoteViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// BRAND TOKENS
private val BabyBlue     = Color(0xFF1F3A5F)
private val BabyBlueDeep = Color(0xFF152A47)
private val BabyBlueSoft = Color(0xFFE8EDF3)
private val OrangeAccent = Color(0xFFC8102E)
private val OrangeSoft   = Color(0xFFFBE5E8)
private val SuccessGreen = Color(0xFF2E7D32)
private val ErrorRed     = Color(0xFF8B1E20)
private val ErrorSoft    = Color(0xFFF4DADD)

internal fun prettyLabel(raw: String): String =
    raw.lowercase().split('_', ' ').joinToString(" ") { part ->
        part.replaceFirstChar { it.titlecase() }
    }

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
    val quoteViewModel: QuoteViewModel = viewModel(
        factory = QuoteViewModel.Factory(database, context)
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

    val awaitingPayment by quoteViewModel
        .getAwaitingPaymentQuotesForCustomer(userId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val declinedRequests by quoteViewModel
        .getDeclinedRequestsForManager(userId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var quoteToSchedule by remember { mutableStateOf<Quote?>(null) }
    var quoteToDecline by remember { mutableStateOf<Quote?>(null) }
    var declinedRequestForReason by remember { mutableStateOf<ServiceRequest?>(null) }

    val pendingOnlyCount = pendingQuotes.count { it.status == QuoteStatus.PENDING }

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
            // WELCOME
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

            // OVERVIEW
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
                                        "Your quotes",
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
                                        when {
                                            pendingQuotes.isEmpty() -> "No quotes yet"
                                            pendingOnlyCount == 0 -> "All quotes are up to date"
                                            else -> "$pendingOnlyCount awaiting your approval"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }

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

            // YOUR QUOTES
            item {
                Text(
                    "Your Quotes",
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
                                "No quotes yet",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Quotes you receive will appear here.",
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
                        onDecline = { quoteToDecline = quote }
                    )
                }
            }

            // AWAITING PAYMENT
            if (awaitingPayment.isNotEmpty()) {
                item {
                    Text(
                        "Awaiting Payment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(awaitingPayment, key = { "awaiting_${it.id}" }) { quote ->
                    var jobId by remember(quote.id) { mutableStateOf<Int?>(null) }

                    LaunchedEffect(quote.id) {
                        jobId = quoteViewModel.getJobForQuote(quote.id)?.id
                    }

                    AwaitingPaymentCard(
                        quote = quote,
                        jobId = jobId,
                        onPayNow = { jId -> navManager.navigateToPayment(quote.id, jId) }
                    )
                }
            }

            // DECLINED REQUESTS (by technician)
            if (declinedRequests.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Declined Requests",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = ErrorSoft
                        ) {
                            Text(
                                "${declinedRequests.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ErrorRed,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                items(declinedRequests, key = { "declined_${it.id}" }) { request ->
                    DeclinedRequestCard(
                        request = request,
                        onViewReason = { declinedRequestForReason = request },
                        onReopen = {
                            scope.launch {
                                val ok = quoteViewModel.reopenDeclinedRequest(
                                    requestId = request.id,
                                    managerId = userId
                                )
                                if (ok) {
                                    Toast.makeText(
                                        context,
                                        "Request re-opened and returned to the technician queue.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Could not re-open request.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    )
                }
            }

            // QUICK ACTIONS
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

    // Accept quote
    quoteToSchedule?.let { quote ->
        ScheduleAcceptDialog(
            quote = quote,
            onDismiss = { quoteToSchedule = null },
            onConfirm = { date, slot ->
                scope.launch {
                    val error = viewModel.acceptQuote(quote.id, date, slot)
                    if (error != null) {
                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(
                            context,
                            "Quote accepted. Job scheduled.",
                            Toast.LENGTH_LONG
                        ).show()
                        quoteToSchedule = null
                    }
                }
            }
        )
    }

    // Decline quote
    quoteToDecline?.let { quote ->
        DeclineReasonDialog(
            title = "Decline Quote",
            subtitle = "Quote #${quote.id} — ${quote.buildingName}",
            confirmLabel = "Decline",
            onDismiss = { quoteToDecline = null },
            onConfirm = { reason ->
                scope.launch {
                    viewModel.declineQuoteWithReason(quote.id, reason)
                    Toast.makeText(context, "Quote declined.", Toast.LENGTH_SHORT).show()
                    quoteToDecline = null
                }
            }
        )
    }

    // Decline reason viewer for manager
    declinedRequestForReason?.let { request ->
        AlertDialog(
            onDismissRequest = { declinedRequestForReason = null },
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "Decline Reason",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${request.buildingName} — ${request.issueType}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        "Reason",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ErrorRed
                    )
                    Text(
                        request.declineReason ?: "No reason provided.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    request.declinedAt?.let { ts ->
                        Text(
                            "Declined on ${
                                SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
                                    .format(Date(ts))
                            }",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { declinedRequestForReason = null }) {
                    Text("Close", fontWeight = FontWeight.SemiBold, color = BabyBlueDeep)
                }
            }
        )
    }
}


// DECLINED REQUEST CARD


@Composable
private fun DeclinedRequestCard(
    request: ServiceRequest,
    onViewReason: () -> Unit,
    onReopen: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
    val declinedWhen = request.declinedAt?.let { dateFormat.format(Date(it)) } ?: "—"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.35f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = ErrorSoft,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Block,
                                contentDescription = null,
                                tint = ErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "Declined by Technician",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ErrorRed
                        )
                        Text(
                            request.buildingName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            request.issueType,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ErrorSoft,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Reason for declining",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ErrorRed
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        request.declineReason
                            ?.takeIf { it.isNotBlank() }
                            ?: "No reason provided.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ErrorRed
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Declined $declinedWhen",
                        style = MaterialTheme.typography.labelSmall,
                        color = ErrorRed.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewReason,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Info, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Details", style = MaterialTheme.typography.labelLarge)
                }
                Button(
                    onClick = onReopen,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeAccent,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Restore, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Re-open",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}


// OVERVIEW TILE


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


// QUICK ACTION BUTTON


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


// PENDING QUOTE CARD

@Composable
fun PendingQuoteCard(
    quote: Quote,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val dateFmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    val (statusBg, statusFg) = when (quote.status) {
        QuoteStatus.PENDING          -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        QuoteStatus.ACCEPTED         -> Color(0xFFE6F4EA) to Color(0xFF2E7D32)
        QuoteStatus.DECLINED         -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        QuoteStatus.EXPIRED          -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        QuoteStatus.AWAITING_PAYMENT -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        QuoteStatus.PAID             -> Color(0xFFE6F4EA) to Color(0xFF2E7D32)
    }

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Quote #${quote.id}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(shape = RoundedCornerShape(50), color = statusBg) {
                            Text(
                                prettyLabel(quote.status.name),
                                style = MaterialTheme.typography.labelSmall,
                                color = statusFg,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        quote.buildingName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
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
                val displayTotal = if (quote.finalTotal > 0) quote.finalTotal else quote.grandTotal
                Column(horizontalAlignment = Alignment.End) {
                    if (quote.discountAmount > 0) {
                        Text(
                            "R${String.format(Locale.US, "%.2f", quote.grandTotal)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                    Text(
                        "R${String.format(Locale.US, "%.2f", displayTotal)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
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
                color = MaterialTheme.colorScheme.onSurface,
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
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
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


// AWAITING PAYMENT CARD


@Composable
fun AwaitingPaymentCard(
    quote: Quote,
    jobId: Int?,
    onPayNow: (Int) -> Unit
) {
    val amount = if (quote.finalTotal > 0) quote.finalTotal else quote.grandTotal

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Payment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "Payment Due",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Quote #${quote.id}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    "R${String.format(Locale.US, "%.2f", amount)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                quote.buildingName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                quote.issueType,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Completed — awaiting your payment",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = {
                    val jId = jobId
                    if (jId != null && jId > 0) onPayNow(jId)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeAccent,
                    contentColor = Color.White
                ),
                enabled = jobId != null && jobId > 0
            ) {
                Icon(Icons.Default.Payment, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Pay Now",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}


// SCHEDULE ACCEPT DIALOG


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
            c.set(y, m, d, 0, 0, 0)
            c.set(Calendar.MILLISECOND, 0)
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
                val finalAmount = if (quote.finalTotal > 0) quote.finalTotal else quote.grandTotal
                Text(
                    "Total: R${String.format(Locale.US, "%.2f", finalAmount)}",
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
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
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
                    val d = selectedDate
                    if (d != null && selectedSlot.isNotBlank()) {
                        val combined = SouthAfricaLocations.combineDateAndSlot(d, selectedSlot)
                        onConfirm(combined, selectedSlot)
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