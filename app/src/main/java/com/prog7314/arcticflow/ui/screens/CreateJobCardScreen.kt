// app/src/main/java/com/prog7314/arcticflow/ui/screens/CreateJobCardScreen.kt
package com.prog7314.arcticflow.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.prog7314.arcticflow.navigation.NavManager
import androidx.compose.ui.text.font.FontWeight
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateJobCardScreen(
    jobId: Int,
    navManager: NavManager
) {
    var workSummary by remember { mutableStateOf("") }
    var additionalNotes by remember { mutableStateOf("") }
    var parts by remember { mutableStateOf(listOf(PartItem())) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Job Card") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Work Performed Summary
            OutlinedTextField(
                value = workSummary,
                onValueChange = { workSummary = it },
                label = { Text("Work Performed Summary") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 8,
                placeholder = { Text("Describe work done, diagnoses, and actions taken...") }
            )

            // Parts Used
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Parts Used/Consumables",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    parts.forEachIndexed { index, part ->
                        PartItemRow(
                            part = part,
                            onPartChange = { updatedPart ->
                                parts = parts.toMutableList().apply {
                                    set(index, updatedPart)
                                }
                            },
                            onRemove = {
                                parts = parts.filterIndexed { i, _ -> i != index }
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    TextButton(
                        onClick = { parts = parts + PartItem() },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Part")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Add Part")
                    }
                }
            }

            // Additional Notes
            OutlinedTextField(
                value = additionalNotes,
                onValueChange = { additionalNotes = it },
                label = { Text("Additional Technician Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                placeholder = { Text("Special requirements, follow-ups needed, or tenant feedback...") }
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { /* Submit job card */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Submit Job Card")
            }
        }
    }
}

data class PartItem(
    val name: String = "",
    val quantity: Int = 1
)

@Composable
fun PartItemRow(
    part: PartItem,
    onPartChange: (PartItem) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = part.name,
            onValueChange = { onPartChange(part.copy(name = it)) },
            label = { Text("Part Name") },
            modifier = Modifier.weight(2f),
            singleLine = true
        )
        OutlinedTextField(
            value = part.quantity.toString(),
            onValueChange = {
                val qty = it.toIntOrNull() ?: 1
                onPartChange(part.copy(quantity = qty))
            },
            label = { Text("Qty") },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, contentDescription = "Remove")
        }
    }
}