// app/src/main/java/com/prog7314/arcticflow/ui/screens/ServiceRequestHistoryScreen.kt
package com.prog7314.arcticflow.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prog7314.arcticflow.data.entities.RequestStatus
import com.prog7314.arcticflow.data.entities.ServiceRequest
import com.prog7314.arcticflow.viewmodels.QuoteViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceRequestHistoryScreen(
    viewModel: QuoteViewModel,
    userId: String
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val requests by viewModel.getRequestsForUser(userId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var editingRequest by remember { mutableStateOf<ServiceRequest?>(null) }
    var deleteTarget by remember { mutableStateOf<ServiceRequest?>(null) }

    if (editingRequest != null) {
        EditServiceRequestDialog(
            request = editingRequest!!,
            onSave = { updated ->
                coroutineScope.launch {
                    viewModel.updateServiceRequest(updated)
                    Toast.makeText(context, "Request updated", Toast.LENGTH_SHORT).show()
                    editingRequest = null
                }
            },
            onDismiss = { editingRequest = null }
        )
    }

    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Request") },
            text = { Text("Are you sure you want to delete this request for ${deleteTarget!!.buildingName}?") },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        viewModel.deleteServiceRequest(deleteTarget!!)
                        Toast.makeText(context, "Request deleted", Toast.LENGTH_SHORT).show()
                        deleteTarget = null
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }

    if (requests.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No service requests yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(requests) { request ->
                ServiceRequestHistoryCard(
                    request = request,
                    onEdit = { editingRequest = request },
                    onDelete = { deleteTarget = request }
                )
            }
        }
    }
}

@Composable
fun ServiceRequestHistoryCard(
    request: ServiceRequest,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(request.buildingName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(request.issueType, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Priority: ${request.priority.name}", style = MaterialTheme.typography.bodySmall)
                    request.preferredDate?.let {
                        Text("Preferred: ${dateFormat.format(Date(it))}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Badge(
                    containerColor = when (request.status) {
                        RequestStatus.PENDING -> Color(0xFFFF9800)
                        RequestStatus.QUOTED -> Color(0xFF2196F3)
                        RequestStatus.ACCEPTED -> Color.Green
                        RequestStatus.DECLINED -> Color.Red
                        else -> Color.Gray
                    }
                ) { Text(request.status.name) }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(request.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (request.status == RequestStatus.PENDING) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Edit")
                    }
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
fun EditServiceRequestDialog(
    request: ServiceRequest,
    onSave: (ServiceRequest) -> Unit,
    onDismiss: () -> Unit
) {
    var description by remember { mutableStateOf(request.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Request") },
        text = {
            Column {
                Text("Building: ${request.buildingName}")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(request.copy(description = description, updatedAt = System.currentTimeMillis()))
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}