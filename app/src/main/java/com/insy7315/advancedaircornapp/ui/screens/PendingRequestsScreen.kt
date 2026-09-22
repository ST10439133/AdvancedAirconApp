// app/src/main/java/com/insy7315/advancedaircornapp/ui/screens/PendingRequestsScreen.kt
package com.insy7315.advancedaircornapp.ui.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.RequestQuote
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
import com.insy7315.advancedaircornapp.data.ArcticFlowDatabase
import com.insy7315.advancedaircornapp.data.entities.RequestPriority
import com.insy7315.advancedaircornapp.data.entities.ServiceRequest
import com.insy7315.advancedaircornapp.navigation.NavManager
import com.insy7315.advancedaircornapp.viewmodels.QuoteViewModel
import java.text.SimpleDateFormat
import java.util.*

// BRAND TOKENS
private val BabyBlue     = Color(0xFF4FA8D8)
private val BabyBlueDeep = Color(0xFF2E7BA6)
private val BabyBlueSoft = Color(0xFFE1F1FB)
private val OrangeAccent = Color(0xFFF7941D)
private val OrangeSoft   = Color(0xFFFFEBD2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingRequestsScreen(
    navManager: NavManager,
    onBackToDashboard: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: QuoteViewModel = viewModel(
        factory = QuoteViewModel.Factory(ArcticFlowDatabase.getDatabase(context), context)
    )

    val requests by viewModel.getPendingServiceRequests()
        .collectAsState(initial = emptyList())

    LaunchedEffect(requests) {
        Log.d("PendingRequestsScreen",
            "Received ${requests.size} pending requests")
        requests.forEach {
            Log.d("PendingRequestsScreen",
                "  - #${it.id} ${it.buildingName} status=${it.status}")
        }
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
                        "New manager requests will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
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
                    RequestCard(request) {
                        navManager.navigateToCreateQuote(request.id)
                    }
                }
            }
        }
    }
}

@Composable
fun RequestCard(request: ServiceRequest, onCreateQuote: () -> Unit) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCreateQuote() },
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

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = onCreateQuote,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeAccent,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.RequestQuote, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Create Quote",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// PRIORITY PILL
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