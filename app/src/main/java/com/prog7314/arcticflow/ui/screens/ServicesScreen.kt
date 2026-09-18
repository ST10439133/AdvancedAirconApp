// app/src/main/java/com/prog7314/arcticflow/ui/screens/ServicesScreen.kt
package com.prog7314.arcticflow.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prog7314.arcticflow.data.entities.BuildingEntity
import com.prog7314.arcticflow.data.entities.RequestPriority
import com.prog7314.arcticflow.data.entities.RequestStatus
import com.prog7314.arcticflow.data.entities.ServiceRequest
import com.prog7314.arcticflow.navigation.NavManager
import com.prog7314.arcticflow.viewmodels.QuoteViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ============================================================
// BRAND TOKENS
// ============================================================
private val BabyBlue     = Color(0xFF4FA8D8)
private val BabyBlueDeep = Color(0xFF2E7BA6)
private val BabyBlueSoft = Color(0xFFE1F1FB)
private val OrangeAccent = Color(0xFFF7941D)
private val OrangeSoft   = Color(0xFFFFEBD2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(
    navManager: NavManager,
    userId: String,
    viewModel: QuoteViewModel,
    onBackToDashboard: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Request Service", "Service History")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Services") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ---- TabRow with brand styling ----
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                divider = {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold
                                else FontWeight.Normal
                            )
                        },
                        selectedContentColor = BabyBlueDeep,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when (selectedTab) {
                0 -> RequestServiceTab(
                    navManager = navManager,
                    userId = userId,
                    viewModel = viewModel
                )
                1 -> ServiceRequestHistoryScreen(
                    viewModel = viewModel,
                    userId = userId
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestServiceTab(
    navManager: NavManager,
    userId: String,
    viewModel: QuoteViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedBuilding by remember { mutableStateOf<BuildingEntity?>(null) }
    var selectedServiceType by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(RequestPriority.MEDIUM) }
    var preferredDate by remember { mutableStateOf<Long?>(null) }
    var description by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val buildings by viewModel.getBuildingsForUser(userId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val serviceTypes = listOf(
        "HVAC Preventive Maintenance",
        "AC Repair",
        "Heating Repair",
        "Installation",
        "Emergency Service",
        "Ventilation Issue",
        "Filter Replacement",
        "Compressor Problem",
        "Refrigerant Leak",
        "Thermostat Issue"
    )

    val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, day ->
            val cal = Calendar.getInstance()
            cal.set(year, month, day, 9, 0)
            preferredDate = cal.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ---- Empty-buildings warning ----
        if (buildings.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = OrangeSoft)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = OrangeAccent.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = OrangeAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "No buildings registered",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = OrangeAccent
                        )
                        Text(
                            "Add a building before you request a service.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    TextButton(onClick = { navManager.navigateToAddBuilding() }) {
                        Text(
                            "Add",
                            color = OrangeAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // ============================================================
        // SECTION 1 — Where
        // ============================================================
        SectionHeader(icon = Icons.Default.Business, title = "Where do you need service?")

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (buildings.isNotEmpty()) {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedBuilding?.name ?: "Select a Building",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Building") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            buildings.forEach { b ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(b.name, fontWeight = FontWeight.Bold)
                                            Text(
                                                b.address,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedBuilding = b
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // ---- Service type ----
                var svcExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = svcExpanded,
                    onExpandedChange = { svcExpanded = !svcExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedServiceType.ifBlank { "Select Service Type" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Service Type") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(svcExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = svcExpanded,
                        onDismissRequest = { svcExpanded = false }
                    ) {
                        serviceTypes.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t) },
                                onClick = {
                                    selectedServiceType = t
                                    svcExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // ============================================================
        // SECTION 2 — When
        // ============================================================
        SectionHeader(icon = Icons.Default.CalendarMonth, title = "When should we come?")

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = preferredDate?.let { dateFormat.format(Date(it)) }
                        ?: "Select preferred date",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Preferred Date") },
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(Icons.Default.CalendarMonth, "Pick Date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PriorityHigh,
                            contentDescription = null,
                            tint = BabyBlueDeep,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Priority Level",
                            style = MaterialTheme.typography.labelMedium,
                            color = BabyBlueDeep,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RequestPriority.values().forEach { p ->
                            FilterChip(
                                selected = selectedPriority == p,
                                onClick = { selectedPriority = p },
                                label = { Text(p.name, maxLines = 1, softWrap = false) },
                                enabled = !isSubmitting
                            )
                        }
                    }
                }
            }
        }

        // ============================================================
        // SECTION 3 — Description
        // ============================================================
        SectionHeader(icon = Icons.Default.Description, title = "Tell us more")

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Describe the issue") },
                placeholder = { Text("Describe the problem in detail...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(12.dp),
                minLines = 4,
                maxLines = 8,
                enabled = !isSubmitting
            )
        }

        // ============================================================
        // SUBMIT
        // ============================================================
        Button(
            onClick = {
                val building = selectedBuilding
                if (building == null) {
                    Toast.makeText(context, "Select a building", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (selectedServiceType.isBlank()) {
                    Toast.makeText(context, "Select service type", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (description.isBlank()) {
                    Toast.makeText(context, "Enter a description", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                coroutineScope.launch {
                    isSubmitting = true
                    try {
                        val request = ServiceRequest(
                            userId = userId,
                            buildingId = building.id,
                            buildingName = building.name,
                            issueType = selectedServiceType,
                            description = description,
                            priority = selectedPriority,
                            preferredDate = preferredDate,
                            status = RequestStatus.PENDING
                        )
                        val id = viewModel.createServiceRequest(request)
                        if (id > 0L) {
                            Toast.makeText(
                                context,
                                "Request submitted! Technicians will see it now.",
                                Toast.LENGTH_LONG
                            ).show()
                            selectedBuilding = null
                            selectedServiceType = ""
                            selectedPriority = RequestPriority.MEDIUM
                            preferredDate = null
                            description = ""
                        } else {
                            Toast.makeText(context, "Failed to submit", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isSubmitting = false
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
            enabled = !isSubmitting &&
                    selectedBuilding != null &&
                    selectedServiceType.isNotBlank() &&
                    description.isNotBlank()
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    Modifier.size(20.dp),
                    color = Color.White
                )
            } else {
                Text(
                    "Submit Request",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

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