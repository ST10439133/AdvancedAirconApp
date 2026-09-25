// app/src/main/java/com/insy7315/advancedairconapp/ui/screens/QuoteHistoryScreen.kt
package com.insy7315.advancedairconapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.insy7315.advancedairconapp.navigation.NavManager

// BRAND TOKENS
private val BabyBlueDeep = Color(0xFF152A47)
private val BabyBlueSoft = Color(0xFFE8EDF3)
private val OrangeAccent = Color(0xFFC8102E)
private val OrangeSoft   = Color(0xFFFBE5E8)
private val SuccessGreen = Color(0xFF2E7D32)
private val SuccessSoft  = Color(0xFFE6F4EA)
private val ErrorRed     = Color(0xFF8B1E20)
private val ErrorSoft    = Color(0xFFF4DADD)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteHistoryScreen(
    navManager: NavManager
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Pending", "Accepted", "Declined")

    val quotes = listOf(
        QuoteHistoryItem("Q-1896 - Jul 18", "Apex Tech Plaza", "Boiler Installation", "$14,500", "Pending"),
        QuoteHistoryItem("Q-1895 - Jul 15", "Oakwood Medical", "Chiller Overhaul", "$8,200", "Accepted"),
        QuoteHistoryItem("Q-1894 - Jul 10", "Riverview Appts", "Thermostat Swap", "$1,850", "Declined")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quote History") },
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
        ) {
            // ===== Filter chips =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                filters.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, maxLines = 1, softWrap = false) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(quotes) { quote ->
                    QuoteHistoryItemCard(quote = quote)
                }
            }
        }
    }
}

data class QuoteHistoryItem(
    val id: String,
    val client: String,
    val service: String,
    val amount: String,
    val status: String
)

@Composable
fun QuoteHistoryItemCard(
    quote: QuoteHistoryItem
) {
    val (statusBg, statusFg, statusIcon) = when (quote.status) {
        "Pending"  -> Triple(OrangeSoft, OrangeAccent, Icons.Default.Schedule)
        "Accepted" -> Triple(SuccessSoft, SuccessGreen, Icons.Default.Check)
        "Declined" -> Triple(ErrorSoft, ErrorRed, Icons.Default.Close)
        else       -> Triple(BabyBlueSoft, BabyBlueDeep, Icons.Default.Schedule)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Navigate to quote details */ },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = BabyBlueSoft,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = BabyBlueDeep,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = quote.id,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = quote.client,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = quote.service,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = quote.amount,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BabyBlueDeep
                    )
                    Spacer(Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = statusBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                statusIcon,
                                contentDescription = null,
                                tint = statusFg,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                quote.status,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusFg,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}