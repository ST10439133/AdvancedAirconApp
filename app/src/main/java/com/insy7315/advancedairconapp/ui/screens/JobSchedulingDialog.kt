// app/src/main/java/com/insy7315/advancedairconapp/ui/screens/JobSchedulingDialog.kt
package com.insy7315.advancedairconapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

// BRAND TOKENS
private val BabyBlueDeep = Color(0xFF152A47)
private val BabyBlueSoft = Color(0xFFE8EDF3)
private val OrangeAccent = Color(0xFFC8102E)

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
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Text(
                    "Schedule Job",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Quote #$quoteId · $buildingName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // DATE SECTION
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BabyBlueSoft)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "Select Date",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = BabyBlueDeep
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableDates.take(5).forEach { (timestamp, display) ->
                                FilterChip(
                                    selected = selectedDate == timestamp,
                                    onClick = { selectedDate = timestamp },
                                    label = {
                                        Text(display, maxLines = 1, softWrap = false)
                                    }
                                )
                            }
                        }
                    }
                }

                // TIME SLOT SECTION
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BabyBlueSoft)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "Select Time Slot",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = BabyBlueDeep
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        timeSlots.forEach { slot ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedTimeSlot = slot }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedTimeSlot == slot,
                                    onClick = { selectedTimeSlot = slot },
                                    modifier = Modifier.size(20.dp),
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = BabyBlueDeep
                                    )
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = slot,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
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
                enabled = selectedDate != null && selectedTimeSlot.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeAccent,
                    contentColor = Color.White
                )
            ) {
                Text("Schedule Job", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}