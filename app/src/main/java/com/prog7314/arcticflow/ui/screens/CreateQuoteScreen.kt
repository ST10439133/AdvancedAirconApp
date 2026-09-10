package com.prog7314.arcticflow.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.prog7314.arcticflow.data.entities.ServiceRequest
import com.prog7314.arcticflow.navigation.NavManager
import com.prog7314.arcticflow.viewmodels.QuoteViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQuoteScreen(
    viewModel: QuoteViewModel,
    requestId: Int,
    technicianId: String,
    navManager: NavManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentRequest by remember { mutableStateOf<ServiceRequest?>(null) }

    LaunchedEffect(requestId) {
        currentRequest = viewModel.getRequestById(requestId)
    }

    data class ServiceFeeOption(val name: String, val fee: Double, val description: String = "")
    data class CatalogPart(val name: String, val price: Double, val category: String = "General")
    data class LineItem(val name: String, val quantity: Int = 1, val price: Double = 0.0)

    val serviceFeeOptions = listOf(
        ServiceFeeOption("Standard Inspection", 350.0, "Basic diagnostic"),
        ServiceFeeOption("AC Repair", 550.0, "Air conditioning repair"),
        ServiceFeeOption("Heating Repair", 550.0, "Heating system repair"),
        ServiceFeeOption("Installation", 750.0, "New system installation"),
        ServiceFeeOption("Emergency Service", 950.0, "24/7 emergency"),
        ServiceFeeOption("Preventive Maintenance", 450.0, "Routine maintenance"),
        ServiceFeeOption("Compressor Replacement", 1200.0, "Compressor swap"),
        ServiceFeeOption("Refrigerant Recharge", 650.0, "Refrigerant top-up")
    )

    val availableParts = listOf(
        CatalogPart("R410A Refrigerant (1kg)", 180.0, "Refrigerants"),
        CatalogPart("R134A Refrigerant (1kg)", 220.0, "Refrigerants"),
        CatalogPart("Copper Tubing (15m)", 350.0, "Tubing"),
        CatalogPart("Contactor (30-Amp)", 85.0, "Electrical"),
        CatalogPart("Run Capacitor (45 uF)", 65.0, "Electrical"),
        CatalogPart("Thermostat (Smart/WiFi)", 280.0, "Electrical"),
        CatalogPart("Condenser Fan Motor", 250.0, "Motors"),
        CatalogPart("Blower Motor (1/2 HP)", 280.0, "Motors"),
        CatalogPart("Air Filter (16x25x1)", 30.0, "Filters"),
        CatalogPart("Compressor (2 Ton)", 520.0, "Compressor"),
        CatalogPart("Coil Cleaner (1L)", 35.0, "Maintenance"),
        CatalogPart("Flex Duct (6\" x 25')", 180.0, "Duct Work")
    )

    var selectedServiceFee by remember { mutableStateOf<ServiceFeeOption?>(null) }
    var selectedPart by remember { mutableStateOf<CatalogPart?>(null) }
    var partQuantity by remember { mutableStateOf("1") }
    var customName by remember { mutableStateOf("") }
    var customPrice by remember { mutableStateOf("") }
    var customQty by remember { mutableStateOf("1") }
    var showCustom by remember { mutableStateOf(false) }
    var lineItems by remember { mutableStateOf(listOf<LineItem>()) }
    var serviceExpanded by remember { mutableStateOf(false) }
    var partExpanded by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    val serviceFee = selectedServiceFee?.fee ?: 0.0
    val partsSubtotal = lineItems.sumOf { it.price * it.quantity }
    val total = serviceFee + partsSubtotal

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Quote") },
                navigationIcon = {
                    IconButton(onClick = { navManager.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            currentRequest?.let { req ->
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Request #${req.id}", style = MaterialTheme.typography.titleSmall)
                        Text("Building: ${req.buildingName}")
                        Text("Issue: ${req.issueType}")
                        Text("Priority: ${req.priority.name}")
                    }
                }
            }

            Text("Service Type & Fee", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)

            ExposedDropdownMenuBox(expanded = serviceExpanded,
                onExpandedChange = { serviceExpanded = it }) {
                OutlinedTextField(
                    value = selectedServiceFee?.name ?: "Select Service Type",
                    onValueChange = {}, readOnly = true,
                    label = { Text("Service Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(serviceExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = serviceExpanded,
                    onDismissRequest = { serviceExpanded = false }) {
                    serviceFeeOptions.forEach { opt ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(opt.name, fontWeight = FontWeight.Medium)
                                    Text("R${String.format("%.2f", opt.fee)} • ${opt.description}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            onClick = { selectedServiceFee = opt; serviceExpanded = false }
                        )
                    }
                }
            }

            Text("Parts & Materials", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !showCustom, onClick = { showCustom = false },
                    label = { Text("Catalog") })
                FilterChip(selected = showCustom, onClick = { showCustom = true },
                    label = { Text("Custom") })
            }

            if (showCustom) {
                Card { Column(Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(customName, { customName = it },
                        label = { Text("Part Name") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(customPrice, { customPrice = it },
                            label = { Text("Price (R)") }, modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        OutlinedTextField(customQty, { customQty = it },
                            label = { Text("Qty") }, modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                    Button(
                        onClick = {
                            val p = customPrice.toDoubleOrNull()
                            val q = customQty.toIntOrNull() ?: 1
                            if (customName.isNotBlank() && p != null && p > 0) {
                                lineItems = lineItems + LineItem(customName, q, p)
                                customName = ""; customPrice = ""; customQty = "1"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Add Custom Part") }
                }}
            } else {
                Card { Column(Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(expanded = partExpanded,
                        onExpandedChange = { partExpanded = it }) {
                        OutlinedTextField(
                            value = selectedPart?.name ?: "Select part...",
                            onValueChange = {}, readOnly = true,
                            label = { Text("Catalog Part") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(partExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = partExpanded,
                            onDismissRequest = { partExpanded = false }) {
                            availableParts.forEach { p ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(p.name)
                                            Text("R${String.format("%.2f", p.price)} • ${p.category}",
                                                style = MaterialTheme.typography.bodySmall)
                                        }
                                    },
                                    onClick = { selectedPart = p; partExpanded = false }
                                )
                            }
                        }
                    }
                    if (selectedPart != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(partQuantity, { partQuantity = it },
                                label = { Text("Qty") }, modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            Button(
                                onClick = {
                                    val q = partQuantity.toIntOrNull() ?: 1
                                    if (q > 0) {
                                        lineItems = lineItems + LineItem(selectedPart!!.name, q, selectedPart!!.price)
                                        selectedPart = null; partQuantity = "1"
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Add") }
                        }
                    }
                }}
            }

            if (lineItems.isNotEmpty()) {
                Card { Column(Modifier.padding(16.dp)) {
                    Text("Line Items", fontWeight = FontWeight.Bold)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    lineItems.forEachIndexed { i, item ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("${item.name} x${item.quantity}",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall)
                            Text("R${String.format("%.2f", item.price * item.quantity)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold)
                            IconButton(onClick = {
                                lineItems = lineItems.filterIndexed { idx, _ -> idx != i }
                            }) { Icon(Icons.Default.Close, "Remove", Modifier.size(16.dp)) }
                        }
                    }
                }}
            }

            Card(colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Service Fee:"); Text("R${String.format("%.2f", serviceFee)}")
                    }
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Parts:"); Text("R${String.format("%.2f", partsSubtotal)}")
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total:", fontWeight = FontWeight.Bold)
                        Text("R${String.format("%.2f", total)}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            OutlinedTextField(notes, { notes = it },
                label = { Text("Additional Notes") },
                modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4)

            Button(
                onClick = {
                    if (selectedServiceFee == null) {
                        Toast.makeText(context, "Select a service type", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (lineItems.isEmpty()) {
                        Toast.makeText(context, "Add at least one part", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    scope.launch {
                        sending = true
                        val id = viewModel.createQuoteForRequest(
                            requestId = requestId,
                            technicianId = technicianId,
                            serviceName = selectedServiceFee!!.name,
                            serviceFee = selectedServiceFee!!.fee,
                            lineItems = lineItems.map { it.name to (it.quantity to it.price) },
                            notes = notes
                        )
                        sending = false
                        if (id > 0L) {
                            Toast.makeText(context, "Quote sent!", Toast.LENGTH_SHORT).show()
                            navManager.navigateBack()
                        } else {
                            Toast.makeText(context, "Failed to send quote", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !sending && selectedServiceFee != null && lineItems.isNotEmpty()
            ) {
                if (sending) CircularProgressIndicator(Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary)
                else Text("Send Quote")
            }
        }
    }
}