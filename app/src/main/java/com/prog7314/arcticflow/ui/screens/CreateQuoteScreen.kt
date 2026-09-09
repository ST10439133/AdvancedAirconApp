// app/src/main/java/com/prog7314/arcticflow/ui/screens/CreateQuoteScreen.kt
package com.prog7314.arcticflow.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prog7314.arcticflow.data.entities.Quote
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
    customerId: String,
    navManager: NavManager
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentRequest by remember { mutableStateOf<ServiceRequest?>(null) }

    // Load the request
    LaunchedEffect(requestId) {
        currentRequest = viewModel.getRequestById(requestId)
    }

    // ============ SERVICE FEE CONFIGURATION ============
    data class ServiceFeeOption(
        val name: String,
        val fee: Double,
        val description: String = ""
    )

    val serviceFeeOptions = listOf(
        ServiceFeeOption("Standard Inspection", 350.00, "Basic system inspection and diagnostic"),
        ServiceFeeOption("AC Repair", 550.00, "Air conditioning repair service"),
        ServiceFeeOption("Heating Repair", 550.00, "Heating system repair service"),
        ServiceFeeOption("Installation", 750.00, "New system installation"),
        ServiceFeeOption("Emergency Service", 950.00, "24/7 emergency repair service"),
        ServiceFeeOption("Preventive Maintenance", 450.00, "Regular maintenance and service"),
        ServiceFeeOption("Compressor Replacement", 1200.00, "Compressor replacement service"),
        ServiceFeeOption("Refrigerant Recharge", 650.00, "Refrigerant recharge service"),
        ServiceFeeOption("Duct Cleaning", 500.00, "Air duct cleaning service"),
        ServiceFeeOption("Thermostat Installation", 400.00, "Thermostat installation and setup")
    )

    // ============ AVAILABLE PARTS CATALOG ============
    data class CatalogPart(
        val name: String,
        val price: Double,
        val category: String = "General"
    )

    val availableParts = listOf(
        // Refrigerants
        CatalogPart("R410A Refrigerant (1kg)", 180.00, "Refrigerants"),
        CatalogPart("R134A Refrigerant (1kg)", 220.00, "Refrigerants"),
        CatalogPart("R32 Refrigerant (1kg)", 200.00, "Refrigerants"),

        // Copper Tubing
        CatalogPart("Copper Tubing (15m)", 350.00, "Tubing"),
        CatalogPart("Copper Tubing (50ft)", 120.00, "Tubing"),
        CatalogPart("Copper Tubing (100ft)", 220.00, "Tubing"),

        // Electrical Components
        CatalogPart("Contactor (30-Amp)", 85.00, "Electrical"),
        CatalogPart("Run Capacitor (45 uF)", 65.00, "Electrical"),
        CatalogPart("Start Capacitor (88-108 uF)", 75.00, "Electrical"),
        CatalogPart("Thermostat (Digital)", 150.00, "Electrical"),
        CatalogPart("Thermostat (Smart/WiFi)", 280.00, "Electrical"),
        CatalogPart("Circuit Breaker (20A)", 45.00, "Electrical"),

        // Motors & Fans
        CatalogPart("Condenser Fan Motor", 250.00, "Motors"),
        CatalogPart("Blower Motor (1/2 HP)", 280.00, "Motors"),
        CatalogPart("Blower Motor (1 HP)", 350.00, "Motors"),
        CatalogPart("Draft Inducer Motor", 200.00, "Motors"),

        // Filters
        CatalogPart("Air Filter (14x20x1)", 25.00, "Filters"),
        CatalogPart("Air Filter (16x25x1)", 30.00, "Filters"),
        CatalogPart("Air Filter (20x20x1)", 28.00, "Filters"),
        CatalogPart("HEPA Filter", 65.00, "Filters"),

        // Compressor Parts
        CatalogPart("Compressor (1.5 Ton)", 450.00, "Compressor"),
        CatalogPart("Compressor (2 Ton)", 520.00, "Compressor"),
        CatalogPart("Compressor (3 Ton)", 620.00, "Compressor"),
        CatalogPart("Compressor Overload Kit", 45.00, "Compressor"),

        // Maintenance
        CatalogPart("AC Maintenance Kit", 340.00, "Maintenance"),
        CatalogPart("Coil Cleaner (1L)", 35.00, "Maintenance"),
        CatalogPart("Condenser Coil Cleaner", 40.00, "Maintenance"),
        CatalogPart("Flush Kit", 55.00, "Maintenance"),
        CatalogPart("Leak Detection Kit", 120.00, "Maintenance"),
        CatalogPart("Vacuum Pump Oil", 30.00, "Maintenance"),

        // Duct Work
        CatalogPart("Flex Duct (6\" x 25')", 180.00, "Duct Work"),
        CatalogPart("Flex Duct (8\" x 25')", 220.00, "Duct Work"),
        CatalogPart("Sheet Metal (24x24)", 75.00, "Duct Work"),
        CatalogPart("Duct Tape (Heavy Duty)", 15.00, "Duct Work"),
        CatalogPart("Insulated Duct (6\")", 200.00, "Duct Work"),

        // Tools & Consumables
        CatalogPart("Manifold Gauge Set", 180.00, "Tools"),
        CatalogPart("Digital Thermometer", 65.00, "Tools"),
        CatalogPart("Vacuum Pump (5 CFM)", 350.00, "Tools"),
        CatalogPart("Pipe Insulation (10ft)", 45.00, "Consumables"),
        CatalogPart("Electrical Tape", 5.00, "Consumables"),
        CatalogPart("Wire Nuts (100-pack)", 12.00, "Consumables"),

        // Specialty Items
        CatalogPart("UV Light Kit", 250.00, "Specialty"),
        CatalogPart("Air Purifier Kit", 320.00, "Specialty"),
        CatalogPart("Humidifier Kit", 280.00, "Specialty"),
        CatalogPart("Dehumidifier Kit", 350.00, "Specialty")
    )

    // ============ LINE ITEMS ============
    data class LineItem(
        val name: String = "",
        val quantity: Int = 1,
        val price: Double = 0.0,
        val isCustom: Boolean = false
    )

    // State variables
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

    // ============ CALCULATIONS ============
    val serviceFee = selectedServiceFee?.fee ?: 0.0
    val partsSubtotal = lineItems.sumOf { it.price * it.quantity }
    val total = serviceFee + partsSubtotal

    // ============ UI ============
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Service Request Info
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

            // ============ SERVICE FEE SELECTION ============
            Text(
                text = "Service Type & Fee",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            ExposedDropdownMenuBox(
                expanded = serviceFeeExpanded,
                onExpandedChange = { serviceFeeExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedServiceFee?.name ?: "Select Service Type",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Service Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = serviceFeeExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    enabled = !isCreating
                )
                ExposedDropdownMenu(
                    expanded = serviceFeeExpanded,
                    onDismissRequest = { serviceFeeExpanded = false }
                ) {
                    serviceFeeOptions.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(option.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "R${String.format("%.2f", option.fee)} - ${option.description}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                selectedServiceFee = option
                                serviceFeeExpanded = false
                            }
                        )
                    }
                }
            }

            if (selectedServiceFee != null) {
                Text(
                    text = "Service Fee: R${String.format("%.2f", selectedServiceFee!!.fee)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // ============ ADD PARTS SECTION ============
            Text(
                text = "Parts & Materials",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Toggle between catalog and custom part
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
                // Custom Part Input
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
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
                                val price = customPartPrice.toDoubleOrNull()
                                val qty = customPartQuantity.toIntOrNull() ?: 1
                                if (customPartName.isNotBlank() && price != null && price > 0) {
                                    lineItems = lineItems + LineItem(
                                        name = customPartName,
                                        quantity = qty,
                                        price = price,
                                        isCustom = true
                                    )
                                    customPartName = ""
                                    customPartPrice = ""
                                    customPartQuantity = "1"
                                    Toast.makeText(context, "Custom part added!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please enter valid part details", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isCreating && customPartName.isNotBlank() && customPartPrice.isNotBlank()
                        ) {
                            Text("Add Custom Part")
                        }
                    }
                }
            } else {
                // Catalog Part Selection
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                enabled = !isCreating
                            )
                            ExposedDropdownMenu(
                                expanded = partDropdownExpanded,
                                onDismissRequest = { partDropdownExpanded = false }
                            ) {
                                // Group parts by category
                                availableParts.groupBy { it.category }.forEach { (category, parts) ->
                                    // Category header
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                category,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        onClick = {}
                                    )
                                    parts.forEach { part ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(part.name, style = MaterialTheme.typography.bodyMedium)
                                                    Text(
                                                        "R${String.format("%.2f", part.price)}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            onClick = {
                                                selectedPart = part
                                                partDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Quantity selector for selected part
                        var selectedPartQuantity by remember { mutableStateOf("1") }

                        if (selectedPart != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = selectedPartQuantity,
                                    onValueChange = { selectedPartQuantity = it },
                                    label = { Text("Quantity") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    enabled = !isCreating
                                )
                                Button(
                                    onClick = {
                                        val qty = selectedPartQuantity.toIntOrNull() ?: 1
                                        if (qty > 0) {
                                            lineItems = lineItems + LineItem(
                                                name = selectedPart!!.name,
                                                quantity = qty,
                                                price = selectedPart!!.price,
                                                isCustom = false
                                            )
                                            selectedPart = null
                                            selectedPartQuantity = "1"
                                            Toast.makeText(context, "Part added!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = !isCreating
                                ) {
                                    Text("Add Part")
                                }
                            }
                        }
                    }
                }
            }

            // ============ LINE ITEMS LIST ============
            if (lineItems.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Line Items",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ITEM", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(3f))
                            Text("QTY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("PRICE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                            Text("TOTAL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                            Text("", modifier = Modifier.weight(0.5f))
                        }

                        HorizontalDivider()

                        // Line items
                        lineItems.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (item.isCustom) "${item.name}*" else item.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(3f)
                                )
                                Text(
                                    text = item.quantity.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "R${String.format("%.2f", item.price)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1.5f)
                                )
                                Text(
                                    text = "R${String.format("%.2f", item.price * item.quantity)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1.5f)
                                )
                                IconButton(
                                    onClick = {
                                        lineItems = lineItems.filterIndexed { i, _ -> i != index }
                                    },
                                    modifier = Modifier.weight(0.5f),
                                    enabled = !isCreating
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // ============ SUMMARY ============
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Quote Summary",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider()

                    // Service Fee
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Service Fee:")
                        Text("R${String.format("%.2f", serviceFee)}")
                    }

                    // Parts Subtotal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Parts & Materials:")
                        Text("R${String.format("%.2f", partsSubtotal)}")
                    }

                    HorizontalDivider()

                    // Total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Estimated Total:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "R${String.format("%.2f", total)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // ============ ADDITIONAL NOTES ============
            OutlinedTextField(
                value = additionalNotes,
                onValueChange = { additionalNotes = it },
                label = { Text("Additional Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                enabled = !isCreating
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ============ SEND QUOTE BUTTON ============
            Button(
                onClick = {
                    if (selectedServiceFee == null) {
                        Toast.makeText(context, "Please select a service type", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (lineItems.isEmpty()) {
                        Toast.makeText(context, "Please add at least one part", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    coroutineScope.launch {
                        isCreating = true
                        try {
                            val partsDescription = lineItems.joinToString("\n") {
                                "${it.name} x${it.quantity} - R${String.format("%.2f", it.price * it.quantity)}"
                            }

                            val quote = Quote(
                                requestId = requestId,
                                technicianId = technicianId,
                                customerId = customerId,
                                buildingName = currentRequest?.buildingName ?: "",
                                issueType = currentRequest?.issueType ?: "",
                                description = currentRequest?.description ?: "",
                                scopeOfWork = "${selectedServiceFee!!.name}: ${selectedServiceFee!!.description}\n\nParts Used:\n$partsDescription",
                                partsRequired = partsDescription,
                                estimatedHours = 0.0,
                                laborCost = serviceFee,
                                partsCost = partsSubtotal,
                                totalCost = total,
                                taxAmount = 0.0,  // No separate tax
                                grandTotal = total,
                                notes = additionalNotes
                            )
                            viewModel.createQuote(quote)
                            Toast.makeText(context, "Quote created successfully!", Toast.LENGTH_SHORT).show()
                            navManager.navigateBack()
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