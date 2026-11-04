package com.insy7315.advancedairconapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.insy7315.advancedairconapp.data.ArcticFlowDatabase
import com.insy7315.advancedairconapp.data.entities.RequestPriority
import com.insy7315.advancedairconapp.data.entities.ServiceRequest
import com.insy7315.advancedairconapp.navigation.NavManager
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
private val ErrorRed     = Color(0xFF8B1E20)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingRequestsScreen(
    navManager: NavManager,
    onBackToDashboard: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: QuoteViewModel = viewModel(
        factory = QuoteViewModel.Factory(ArcticFlowDatabase.getDatabase(context), context)
    )

    val requests by viewModel.getPendingServiceRequests()
        .collectAsState(initial = emptyList())

    var isRefreshing by remember { mutableStateOf(false) }
    var requestToDecline by remember { mutableStateOf<ServiceRequest?>(null) }

    val currentTechId: String = remember {
        FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    }

    LaunchedEffect(Unit) {
        isRefreshing = true
        viewModel.refreshFromServer("TECHNICIAN")
        isRefreshing = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Job Requests") },
                navigationIcon = {
                    IconButton(onClick = onBackToDashboard) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Dashboard"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                isRefreshing = true
                                viewModel.refreshFromServer("TECHNICIAN")
                                isRefreshing = false
                            }
                        }
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = BabyBlueDeep
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (requests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = BabyBlueSoft,
                        modifier = Modifier.size(88.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Inbox,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = BabyBlueDeep
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No pending requests",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Pull to refresh or tap the refresh icon.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                isRefreshing = true
                                viewModel.refreshFromServer("TECHNICIAN")
                                isRefreshing = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangeAccent,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Refresh Now")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(requests, key = { it.id }) { request ->
                    RequestCard(
                        request = request,
                        onCreateQuote = { navManager.navigateToCreateQuote(request.id) },
                        onDecline = { requestToDecline = request }
                    )
                }
            }
        }
    }

    // ---- Decline reason dialog ----
    requestToDecline?.let { request ->
        RequestDeclineReasonDialog(
            request = request,
            onDismiss = { requestToDecline = null },
            onConfirm = { reason ->
                scope.launch {
                    val ok = viewModel.declineRequestAsTechnician(
                        requestId = request.id,
                        reason = reason,
                        technicianId = currentTechId
                    )
                    if (ok) {
                        Toast.makeText(
                            context,
                            "Request declined. The manager has been notified.",
                            Toast.LENGTH_LONG
                        ).show()
                        requestToDecline = null
                    } else {
                        Toast.makeText(
                            context,
                            "Could not decline request — please try again.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }
}

@Composable
fun RequestCard(
    request: ServiceRequest,
    onCreateQuote: () -> Unit,
    onDecline: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        request.buildingName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        request.issueType,
                        style = MaterialTheme.typography.bodyMedium,
                        color = BabyBlueDeep,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        request.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                    request.preferredDate?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Preferred: ${dateFormat.format(Date(it))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                PriorityPill(request.priority)
            }

            Spacer(Modifier.height(16.dp))

            // --- BUTTONS FIX ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // DECLINE BUTTON
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier
                        .weight(0.35f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ErrorRed
                    ),
                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Decline",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                // CREATE QUOTE BUTTON
                Button(
                    onClick = onCreateQuote,
                    modifier = Modifier
                        .weight(0.65f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeAccent,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Create Quote",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ============================================================
// DECLINE REASON DIALOG
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDeclineReasonDialog(
    request: ServiceRequest,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                "Decline Request",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Request for ${request.buildingName} — ${request.issueType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Please explain why you cannot take this job. Your reason " +
                            "will be sent to the manager so they can reassign it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = {
                        reason = it
                        error = null
                    },
                    label = { Text("Reason for declining *") },
                    placeholder = { Text("e.g. I'm fully booked next week, or the site is out of my coverage area.") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(12.dp),
                    isError = error != null
                )
                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = reason.trim()
                    if (trimmed.isBlank()) {
                        error = "A reason is required."
                    } else if (trimmed.length < 5) {
                        error = "Please write at least 5 characters."
                    } else {
                        onConfirm(trimmed)
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ErrorRed,
                    contentColor = Color.White
                )
            ) { Text("Decline Request") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PriorityPill(priority: RequestPriority) {
    val (bg, fg) = when (priority) {
        RequestPriority.URGENT -> Color(0xFFFFDAD6) to Color(0xFFBA1A1A)
        RequestPriority.HIGH   -> OrangeSoft       to OrangeAccent
        RequestPriority.MEDIUM -> BabyBlueSoft     to BabyBlueDeep
        RequestPriority.LOW    -> Color(0xFFE6F4EA) to Color(0xFF2E7D32)
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = bg
    ) {
        Text(
            priority.name.lowercase().replaceFirstChar { it.titlecase() },
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}