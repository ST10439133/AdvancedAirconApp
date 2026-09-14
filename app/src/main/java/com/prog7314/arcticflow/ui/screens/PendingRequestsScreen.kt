// app/src/main/java/com/prog7314/arcticflow/ui/screens/PendingRequestsScreen.kt
package com.prog7314.arcticflow.ui.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prog7314.arcticflow.data.ArcticFlowDatabase
import com.prog7314.arcticflow.data.entities.RequestPriority
import com.prog7314.arcticflow.data.entities.ServiceRequest
import com.prog7314.arcticflow.navigation.NavManager
import com.prog7314.arcticflow.viewmodels.QuoteViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingRequestsScreen(navManager: NavManager) {
    val context = LocalContext.current
    val viewModel: QuoteViewModel = viewModel(
        factory = QuoteViewModel.Factory(ArcticFlowDatabase.getDatabase(context))
    )

    val requests by viewModel.getPendingServiceRequests()
        .collectAsState(initial = emptyList())

    // 🔍 Debug: log every emission
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
            TopAppBar(title = { Text("Job Requests") })
        }
    ) { padding ->
        if (requests.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Inbox, null, Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("No pending requests",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("New manager requests will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
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
        modifier = Modifier.fillMaxWidth().clickable { onCreateQuote() },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(request.buildingName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Text(request.issueType,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary)
                    Text(request.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2)
                    request.preferredDate?.let {
                        Text("Preferred: ${dateFormat.format(Date(it))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Badge(containerColor = when (request.priority) {
                    RequestPriority.URGENT -> Color.Red
                    RequestPriority.HIGH -> Color(0xFFFF9800)
                    RequestPriority.MEDIUM -> Color(0xFF2196F3)
                    RequestPriority.LOW -> Color(0xFF4CAF50)
                }) { Text(request.priority.name, color = Color.White) }
            }

            Spacer(Modifier.height(12.dp))

            Button(onClick = onCreateQuote, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.RequestQuote, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Create Quote")
            }
        }
    }
}