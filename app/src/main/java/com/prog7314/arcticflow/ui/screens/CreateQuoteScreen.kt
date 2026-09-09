package com.prog7314.arcticflow.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prog7314.arcticflow.data.entities.Quote
import com.prog7314.arcticflow.navigation.NavManager
import com.prog7314.arcticflow.viewmodels.QuoteViewModel
import kotlinx.coroutines.launch
import java.util.Locale
import com.prog7314.arcticflow.data.entities.ServiceRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQuoteScreen(
    viewModel: QuoteViewModel,
    requestId: Int,
    technicianId: String,
    customerId: String,
    navManager: NavManager
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Use a separate state for the request to avoid delegate issues
    var currentRequest by remember { mutableStateOf<ServiceRequest?>(null) }

    // Load the request
    LaunchedEffect(requestId) {
        currentRequest = viewModel.getRequestById(requestId)
    }

    var scopeOfWork by remember { mutableStateOf("") }
    var partsRequired by remember { mutableStateOf("") }
    var estimatedHours by remember { mutableStateOf("") }
    var laborCost by remember { mutableStateOf("") }
    var partsCost by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }

    val taxRate = 0.15
    val labor = laborCost.toDoubleOrNull() ?: 0.0
    val parts = partsCost.toDoubleOrNull() ?: 0.0
    val subtotal = labor + parts
    val tax = subtotal * taxRate
    val total = subtotal + tax

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Quote") },
                navigationIcon = {
                    IconButton(onClick = { navManager.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Show request details if available
            if (currentRequest != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text("Service Request #${currentRequest!!.id}", style = MaterialTheme.typography.titleSmall)
                        Text("Building: ${currentRequest!!.buildingName}")
                        Text("Issue: ${currentRequest!!.issueType}")
                        Text("Priority: ${currentRequest!!.priority.name}")
                    }
                }
            }

            OutlinedTextField(
                value = scopeOfWork,
                onValueChange = { scopeOfWork = it },
                label = { Text("Scope of Work") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                enabled = !isCreating
            )

            OutlinedTextField(
                value = partsRequired,
                onValueChange = { partsRequired = it },
                label = { Text("Parts Required") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                enabled = !isCreating
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = estimatedHours,
                    onValueChange = { estimatedHours = it },
                    label = { Text("Est. Hours") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    enabled = !isCreating
                )
                OutlinedTextField(
                    value = laborCost,
                    onValueChange = { laborCost = it },
                    label = { Text("Labor Cost (R)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    enabled = !isCreating
                )
            }

            OutlinedTextField(
                value = partsCost,
                onValueChange = { partsCost = it },
                label = { Text("Parts Cost (R)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                enabled = !isCreating
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Cost Summary", style = MaterialTheme.typography.titleSmall)
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal:")
                        Text(String.format(Locale.US, "R%.2f", subtotal))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tax (15%):")
                        Text(String.format(Locale.US, "R%.2f", tax))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total:", style = MaterialTheme.typography.titleSmall)
                        Text(
                            String.format(Locale.US, "R%.2f", total),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Additional Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                enabled = !isCreating
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (scopeOfWork.isNotBlank() && laborCost.isNotBlank() && partsCost.isNotBlank()) {
                        coroutineScope.launch {
                            isCreating = true
                            val quote = Quote(
                                requestId = requestId,
                                technicianId = technicianId,
                                customerId = customerId,
                                buildingName = currentRequest?.buildingName ?: "",
                                issueType = currentRequest?.issueType ?: "",
                                description = currentRequest?.description ?: "",
                                scopeOfWork = scopeOfWork,
                                partsRequired = partsRequired,
                                estimatedHours = estimatedHours.toDoubleOrNull() ?: 0.0,
                                laborCost = labor,
                                partsCost = parts,
                                totalCost = subtotal,
                                taxAmount = tax,
                                grandTotal = total,
                                notes = notes
                            )
                            viewModel.createQuote(quote)
                            isCreating = false
                            Toast.makeText(context, "Quote created successfully!", Toast.LENGTH_SHORT).show()
                            navManager.navigateBack()
                        }
                    } else {
                        Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCreating
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Send Quote")
                }
            }
        }
    }
}