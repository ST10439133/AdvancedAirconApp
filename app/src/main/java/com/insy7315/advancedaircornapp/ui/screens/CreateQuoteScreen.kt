// app/src/main/java/com/insy7315/advancedaircornapp/ui/screens/CreateQuoteScreen.kt
package com.insy7315.advancedaircornapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.insy7315.advancedaircornapp.data.CatalogPart
import com.insy7315.advancedaircornapp.data.PartCatalog
import com.insy7315.advancedaircornapp.data.entities.ServiceRequest
import com.insy7315.advancedaircornapp.navigation.NavManager
import com.insy7315.advancedaircornapp.viewmodels.QuoteViewModel
import kotlinx.coroutines.launch
import java.util.Locale


// BRAND TOKENS

private val BabyBlue        = Color(0xFF4FA8D8)
private val BabyBlueDeep    = Color(0xFF2E7BA6)
private val BabyBlueSoft    = Color(0xFFE1F1FB)
private val OrangeAccent    = Color(0xFFF7941D)
private val OrangeSoft      = Color(0xFFFFEBD2)

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
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // HERO - Request summary
            currentRequest?.let { req ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(listOf(BabyBlue, BabyBlueDeep))
                            )
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.22f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.RequestQuote,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Request #${req.id}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    req.buildingName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Text(
                                    req.issueType,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            PriorityPill(req.priority.name)
                        }
                    }
                }
            }

            // SECTION 1 - Service Type & Fee
            SectionHeader(icon = Icons.Default.Work, title = "Service Type & Fee")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = serviceFeeExpanded,
                        onExpandedChange = { serviceFeeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedServiceFee?.name ?: "Select Service Type",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Service Type") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = serviceFeeExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp),
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
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = BabyBlueSoft,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Service Fee",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BabyBlueDeep
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    "R${String.format(Locale.US, "%.2f", selectedServiceFee!!.fee)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BabyBlueDeep
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 2 - Parts & Materials
            SectionHeader(icon = Icons.Default.Inventory2, title = "Parts & Materials")

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

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (showCustomPartInput) {
                        OutlinedTextField(
                            value = customPartName,
                            onValueChange = { customPartName = it },
                            label = { Text("Part Name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
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
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                enabled = !isCreating
                            )
                            OutlinedTextField(
                                value = customPartQuantity,
                                onValueChange = { customPartQuantity = it },
                                label = { Text("Qty") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isCreating && customPartName.isNotBlank() && customPartPrice.isNotBlank()
                        ) { Text("Add Custom Part") }
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = partDropdownExpanded,
                            onExpandedChange = { partDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedPart?.name ?: "Select a part...",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("From Catalog") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = partDropdownExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(12.dp),
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = qtyStr,
                                    onValueChange = { qtyStr = it },
                                    label = { Text("Quantity") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
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
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = !isCreating
                                ) { Text("Add") }
                            }
                        }
                    }
                }
            }

            // SECTION 3 - Line Items
            if (lineItems.isNotEmpty()) {
                SectionHeader(
                    icon = Icons.AutoMirrored.Filled.List,
                    title = "Line Items (${lineItems.size})"
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        lineItems.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        if (item.isCustom) "${item.name}*" else item.name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        "${item.quantity} × R${
                                            String.format(Locale.US, "%.2f", item.price)
                                        }",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    "R${String.format(Locale.US, "%.2f", item.price * item.quantity)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { lineItems = lineItems.filterIndexed { i, _ -> i != index } },
                                    enabled = !isCreating
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        "Remove",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            if (index < lineItems.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }

            // SECTION 4 - Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(listOf(BabyBlue, BabyBlueDeep))
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Text(
                            "Quote Summary",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(10.dp))

                        SummaryRow("Service Fee", "R${String.format(Locale.US, "%.2f", serviceFee)}")
                        SummaryRow("Parts", "R${String.format(Locale.US, "%.2f", partsSubtotal)}")

                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "TOTAL",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "R${String.format(Locale.US, "%.2f", total)}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // SECTION 5 -Notes
            SectionHeader(icon = Icons.Default.Description, title = "Notes")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                OutlinedTextField(
                    value = additionalNotes,
                    onValueChange = { additionalNotes = it },
                    label = { Text("Additional notes") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isCreating
                )
            }

            // SEND
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeAccent,
                    contentColor = Color.White
                ),
                enabled = !isCreating && selectedServiceFee != null && lineItems.isNotEmpty()
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                } else {
                    Icon(Icons.Default.Send, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Send Quote",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// HELPERS

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = BabyBlueDeep,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = BabyBlueDeep
        )
    }
}

@Composable
private fun PriorityPill(priority: String) {
    val (bg, fg) = when (priority.uppercase()) {
        "URGENT" -> Color(0xFFFFDAD6) to Color(0xFFBA1A1A)
        "HIGH"   -> OrangeSoft to OrangeAccent
        "MEDIUM" -> BabyBlueSoft to BabyBlueDeep
        else     -> Color(0xFFE6F4EA) to Color(0xFF2E7D32)
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.PriorityHigh,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(12.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                priority.lowercase().replaceFirstChar { it.titlecase() },
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
    }
}