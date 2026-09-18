// app/src/main/java/com/prog7314/arcticflow/ui/screens/CreateQuoteScreen.kt
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
import com.prog7314.arcticflow.data.CatalogPart
import com.prog7314.arcticflow.data.PartCatalog
import com.prog7314.arcticflow.data.entities.ServiceRequest
import com.prog7314.arcticflow.navigation.NavManager
import com.prog7314.arcticflow.viewmodels.QuoteViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQuoteScreen(
    viewModel: QuoteViewModel,
    requestId: Int,
    technicianId: String,
    navManager: NavManager
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentRequest by remember { mutableStateOf<ServiceRequest?>(null) }
    LaunchedEffect(requestId) {
        currentRequest = viewModel.getRequestById(requestId)
    }

    data class ServiceFeeOption(val name: String, val fee: Double, val description: String = "")
    val serviceFeeOptions = listOf(
        ServiceFeeOption("Standard Inspection", 350.00, "Basic inspection and diagnostic"),
        ServiceFeeOption("AC Repair", 550.00, "AC repair service"),
        ServiceFeeOption("Heating Repair", 550.00, "Heating repair service"),
        ServiceFeeOption("Installation", 750.00, "New system installation"),
        ServiceFeeOption("Emergency Service", 950.00, "24/7 emergency service"),
        ServiceFeeOption("Preventive Maintenance", 450.00, "Regular maintenance"),
        ServiceFeeOption("Compressor Replacement", 1200.00, "Compressor replacement"),
        ServiceFeeOption("Refrigerant Recharge", 650.00, "Refrigerant recharge"),
        ServiceFeeOption("Duct Cleaning", 500.00, "Air duct cleaning"),
        ServiceFeeOption("Thermostat Installation", 400.00, "Thermostat install")
    )

    // NOTE: The parts catalogue now lives in PartCatalog.kt so both
    // CreateQuoteScreen and CreateJobCardScreen share the same list.

    data class LineItem(
        val name: String = "",
        val quantity: Int = 1,
        val price: Double = 0.0,
        val isCustom: Boolean = false
    )

    var selectedServiceFee by remember { mutableStateOf<ServiceFeeOption?>(null) }
    var selectedPart by remember { mutableStateOf<CatalogPart?>(null) }
    var customPartName by remember { mutableStateOf("") }
    var customPartPrice by remember { mutableStateOf("") }
    var customPartQuantity by remember { mutableStateOf("1") }
    var showCustomPartInput by remember { mutableStateOf(false) }
    var lineItems by remember { mutableStateOf(listOf<LineItem>()) }
    var serviceFeeExpanded by remember { mutableStateOf(false) }
    var partDropdownExpanded by remember { mutableStateOf(false) }
    var additionalNotes by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }

    val serviceFee = selectedServiceFee?.fee ?: 0.0
    val partsSubtotal = lineItems.sumOf { it.price * it.quantity }
    val total = serviceFee + partsSubtotal

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Quote") },
                navigationIcon = {
                    IconButton(onClick = { navManager.navigateBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (currentRequest != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Request #${currentRequest!!.id}", style = MaterialTheme.typography.titleSmall)
                        Text("Building: ${currentRequest!!.buildingName}")
                        Text("Issue: ${currentRequest!!.issueType}")
                        Text("Priority: ${currentRequest!!.priority.name}")
                    }
                }
            }

            Text("Service Type & Fee", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            ExposedDropdownMenuBox(
                expanded = serviceFeeExpanded,
                onExpandedChange = { serviceFeeExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedServiceFee?.name ?: "Select Service Type",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Service Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = serviceFeeExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    enabled = !isCreating
                )
                ExposedDropdownMenu(
                    expanded = serviceFeeExpanded,
                    onDismissRequest = { serviceFeeExpanded = false }
                ) {
                    serviceFeeOptions.forEach { opt ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(opt.name)
                                    Text(
                                        "R${String.format(Locale.US, "%.2f", opt.fee)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                selectedServiceFee = opt
                                serviceFeeExpanded = false
                            }
                        )
                    }
                }
            }

            if (selectedServiceFee != null) {
                Text(
                    "Service Fee: R${String.format(Locale.US, "%.2f", selectedServiceFee!!.fee)}",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Text("Parts & Materials", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !showCustomPartInput,
                    onClick = { showCustomPartInput = false },
                    label = { Text("From Catalog") }
                )
                FilterChip(
                    selected = showCustomPartInput,
                    onClick = { showCustomPartInput = true },
                    label = { Text("Custom Part") }
                )
            }

            if (showCustomPartInput) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customPartName,
                            onValueChange = { customPartName = it },
                            label = { Text("Part Name") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isCreating
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customPartPrice,
                                onValueChange = { customPartPrice = it },
                                label = { Text("Price (R)") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                enabled = !isCreating
                            )
                            OutlinedTextField(
                                value = customPartQuantity,
                                onValueChange = { customPartQuantity = it },
                                label = { Text("Qty") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                enabled = !isCreating
                            )
                        }
                        Button(
                            onClick = {
                                val p = customPartPrice.toDoubleOrNull()
                                val q = customPartQuantity.toIntOrNull() ?: 1
                                if (customPartName.isNotBlank() && p != null && p > 0) {
                                    lineItems = lineItems + LineItem(customPartName, q, p, true)
                                    customPartName = ""
                                    customPartPrice = ""
                                    customPartQuantity = "1"
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isCreating && customPartName.isNotBlank() && customPartPrice.isNotBlank()
                        ) { Text("Add Custom Part") }
                    }
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = partDropdownExpanded,
                            onExpandedChange = { partDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedPart?.name ?: "Select a part...",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select from Catalog") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = partDropdownExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                enabled = !isCreating
                            )
                            ExposedDropdownMenu(
                                expanded = partDropdownExpanded,
                                onDismissRequest = { partDropdownExpanded = false }
                            ) {
                                PartCatalog.availableParts.forEach { p ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(p.name)
                                                Text(
                                                    "R${String.format(Locale.US, "%.2f", p.price)} · ${p.category}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedPart = p
                                            partDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        var qtyStr by remember { mutableStateOf("1") }
                        if (selectedPart != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = qtyStr,
                                    onValueChange = { qtyStr = it },
                                    label = { Text("Quantity") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    enabled = !isCreating
                                )
                                Button(
                                    onClick = {
                                        val q = qtyStr.toIntOrNull() ?: 1
                                        if (q > 0) {
                                            lineItems = lineItems + LineItem(selectedPart!!.name, q, selectedPart!!.price, false)
                                            selectedPart = null
                                            qtyStr = "1"
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = !isCreating
                                ) { Text("Add Part") }
                            }
                        }
                    }
                }
            }

            if (lineItems.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Line Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        lineItems.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (item.isCustom) "${item.name}*" else item.name,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text("${item.quantity}x", style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "R${String.format(Locale.US, "%.2f", item.price * item.quantity)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { lineItems = lineItems.filterIndexed { i, _ -> i != index } },
                                    enabled = !isCreating
                                ) {
                                    Icon(Icons.Default.Close, "Remove", Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Quote Summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Service Fee:")
                        Text("R${String.format(Locale.US, "%.2f", serviceFee)}")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Parts:")
                        Text("R${String.format(Locale.US, "%.2f", partsSubtotal)}")
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("TOTAL:", fontWeight = FontWeight.Bold)
                        Text(
                            "R${String.format(Locale.US, "%.2f", total)}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            OutlinedTextField(
                value = additionalNotes,
                onValueChange = { additionalNotes = it },
                label = { Text("Additional Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                enabled = !isCreating
            )

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
                    coroutineScope.launch {
                        isCreating = true
                        try {
                            val id = viewModel.createQuoteForRequest(
                                requestId = requestId,
                                technicianId = technicianId,
                                customerId = currentRequest?.userId ?: "",
                                serviceName = selectedServiceFee!!.name,
                                serviceFee = selectedServiceFee!!.fee,
                                lineItems = lineItems.map { it.name to (it.quantity to it.price) },
                                notes = additionalNotes
                            )
                            if (id > 0) {
                                Toast.makeText(context, "Quote sent!", Toast.LENGTH_SHORT).show()
                                navManager.navigateBack()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isCreating = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCreating && selectedServiceFee != null && lineItems.isNotEmpty()
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