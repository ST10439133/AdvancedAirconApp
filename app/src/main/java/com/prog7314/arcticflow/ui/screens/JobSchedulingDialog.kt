// app/src/main/java/com/prog7314/arcticflow/ui/screens/JobSchedulingDialog.kt
package com.prog7314.arcticflow.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobSchedulingDialog(
    quoteId: Int,
    buildingName: String,
    onSchedule: (Long, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    var selectedTimeSlot by remember { mutableStateOf("") }

    val timeSlots = listOf(
        "08:00 AM - 09:30 AM",
        "09:30 AM - 11:00 AM",
        "11:00 AM - 12:30 PM",
        "12:30 PM - 02:00 PM",
        "02:00 PM - 03:30 PM",
        "03:30 PM - 05:00 PM"
    )

    // Calculate available dates (next 14 days)
    val availableDates = remember {
        val dates = mutableListOf<Pair<Long, String>>()
        val calendar = Calendar.getInstance()
        for (i in 1..14) {
            calendar.add(Calendar.DAY_OF_YEAR, if (i == 1) 0 else 1)
            val date = calendar.timeInMillis
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY) {
                val format = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                dates.add(date to format.format(Date(date)))
            }
        }
        dates
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Schedule Job",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Quote #$quoteId for $buildingName",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Date Selection
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Select Date",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Date chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableDates.take(5).forEach { (timestamp, display) ->
                                FilterChip(
                                    selected = selectedDate == timestamp,
                                    onClick = { selectedDate = timestamp },
                                    label = { Text(display) }
                                )
                            }
                        }
                    }
                }

                // Time Slot Selection
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Select Time Slot",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        timeSlots.forEach { slot ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedTimeSlot == slot,
                                    onClick = { selectedTimeSlot = slot }
                                )
                                Text(
                                    text = slot,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedDate != null && selectedTimeSlot.isNotEmpty()) {
                        onSchedule(selectedDate!!, selectedTimeSlot)
                    }
                },
                enabled = selectedDate != null && selectedTimeSlot.isNotEmpty()
            ) {
                Text("Schedule Job")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}