// app/src/main/java/com/insy7315/advancedairconapp/ui/screens/EditQuotePricingScreen.kt
package com.insy7315.advancedairconapp.ui.screens

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Discount
import androidx.compose.material.icons.filled.ReceiptLong
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
import com.insy7315.advancedairconapp.data.entities.Quote
import com.insy7315.advancedairconapp.data.entities.QuoteStatus
import com.insy7315.advancedairconapp.navigation.NavManager
import com.insy7315.advancedairconapp.viewmodels.QuoteViewModel
import kotlinx.coroutines.launch
import java.util.Locale

private val BabyBlue     = Color(0xFF1F3A5F)
private val BabyBlueDeep = Color(0xFF152A47)
private val BabyBlueSoft = Color(0xFFE8EDF3)
private val OrangeAccent = Color(0xFFC8102E)
private val OrangeSoft   = Color(0xFFFBE5E8)

private const val DISC_NONE       = "NONE"
private const val DISC_PERCENTAGE = "PERCENTAGE"
private const val DISC_FIXED      = "FIXED"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditQuotePricingScreen(
    viewModel: QuoteViewModel,
    quoteId: Int,
    navManager: NavManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var quote by remember { mutableStateOf<Quote?>(null) }
    var loadError by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    var discountType by remember { mutableStateOf(DISC_NONE) }
    var discountValueStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    LaunchedEffect(quoteId) {
        val q = viewModel.getQuoteById(quoteId)
        if (q == null) {
            loadError = true
        } else {
            quote = q
            discountType = q.discountType.ifBlank { DISC_NONE }
            discountValueStr = if (q.discountValue > 0.0) {
                if (discountType == DISC_PERCENTAGE) {
                    if (q.discountValue % 1.0 == 0.0) q.discountValue.toInt().toString()
                    else q.discountValue.toString()
                } else {
                    String.format(Locale.US, "%.2f", q.discountValue)
                }
            } else ""
            notes = q.notes
        }
    }

    // Allow editing for the full lifecycle except terminal-decline states.
    // FIXED: AWAITING_PAYMENT included so last-minute discounts before
    // the customer pays are still possible.
    val isEditable = when (quote?.status) {
        QuoteStatus.PENDING,
        QuoteStatus.ACCEPTED,
        QuoteStatus.AWAITING_PAYMENT,
        QuoteStatus.PAID -> true
        else -> false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Quote Pricing") },
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
    ) { padding ->
        if (loadError) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Could not load quote #$quoteId",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            return@Scaffold
        }

        val q = quote ?: return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(BabyBlue, BabyBlueDeep)))
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
                                    Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Quote #${q.id}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                q.buildingName,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                q.issueType,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        StatusPill(q.status)
                    }
                }
            }

            SectionHeader(icon = Icons.Default.ReceiptLong, title = "Original Breakdown")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BreakdownRow("Service Fee", q.laborCost)
                    BreakdownRow("Parts & Materials", q.partsCost)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    BreakdownRow("Grand Total", q.grandTotal, bold = true)
                }
            }

            SectionHeader(icon = Icons.Default.Discount, title = "Apply Discount")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DiscountChip(
                            selected = discountType == DISC_NONE,
                            label = "None",
                            enabled = isEditable && !isSaving,
                            onClick = {
                                discountType = DISC_NONE
                                discountValueStr = ""
                            }
                        )
                        DiscountChip(
                            selected = discountType == DISC_PERCENTAGE,
                            label = "% Off",
                            enabled = isEditable && !isSaving,
                            onClick = { discountType = DISC_PERCENTAGE }
                        )
                        DiscountChip(
                            selected = discountType == DISC_FIXED,
                            label = "R Off",
                            enabled = isEditable && !isSaving,
                            onClick = { discountType = DISC_FIXED }
                        )
                    }

                    if (discountType != DISC_NONE) {
                        OutlinedTextField(
                            value = discountValueStr,
                            onValueChange = { input ->
                                if (input.isEmpty() ||
                                    input.matches(Regex("^\\d*\\.?\\d*$"))
                                ) {
                                    discountValueStr = input
                                }
                            },
                            label = {
                                Text(
                                    if (discountType == DISC_PERCENTAGE)
                                        "Discount percentage (0–100)"
                                    else "Discount amount in Rands"
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            enabled = isEditable && !isSaving,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            )
                        )
                    }

                    val enteredValue = discountValueStr.toDoubleOrNull() ?: 0.0
                    val cappedValue = when (discountType) {
                        DISC_PERCENTAGE -> enteredValue.coerceIn(0.0, 100.0)
                        DISC_FIXED -> enteredValue.coerceIn(0.0, q.grandTotal)
                        else -> 0.0
                    }
                    val previewDiscount = when (discountType) {
                        DISC_PERCENTAGE -> q.grandTotal * (cappedValue / 100.0)
                        DISC_FIXED -> cappedValue
                        else -> 0.0
                    }.coerceIn(0.0, q.grandTotal)
                    val previewFinal = (q.grandTotal - previewDiscount).coerceAtLeast(0.0)

                    if (previewDiscount > 0) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = BabyBlueSoft,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                BreakdownRow(
                                    "Discount",
                                    previewDiscount,
                                    bold = false,
                                    isDiscount = true
                                )
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                                BreakdownRow(
                                    "New Total",
                                    previewFinal,
                                    bold = true
                                )
                            }
                        }
                    }
                }
            }

            SectionHeader(icon = Icons.Default.Description, title = "Notes")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Reason for adjustment / notes") },
                    placeholder = { Text("e.g. Loyalty discount, weekend promo, etc.") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(12.dp),
                    enabled = isEditable && !isSaving
                )
            }

            if (!isEditable) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = OrangeSoft)
                ) {
                    Text(
                        "This quote has already been ${q.status.name.lowercase()} and can no longer be edited.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OrangeAccent,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Button(
                onClick = {
                    val current = quote ?: return@Button
                    val entered = discountValueStr.toDoubleOrNull() ?: 0.0
                    val normalizedValue = when (discountType) {
                        DISC_PERCENTAGE -> entered.coerceIn(0.0, 100.0)
                        DISC_FIXED -> entered.coerceIn(0.0, current.grandTotal)
                        else -> 0.0
                    }

                    scope.launch {
                        isSaving = true
                        val ok = viewModel.updateQuotePricing(
                            quoteId = current.id,
                            discountType = discountType,
                            discountValue = normalizedValue,
                            notes = notes
                        )
                        isSaving = false

                        if (ok) {
                            Toast.makeText(
                                context,
                                "Quote pricing updated",
                                Toast.LENGTH_SHORT
                            ).show()
                            navManager.navigateBack()
                        } else {
                            Toast.makeText(
                                context,
                                "Failed to update quote pricing",
                                Toast.LENGTH_LONG
                            ).show()
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
                enabled = isEditable && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                } else {
                    Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Save Pricing",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
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

@Composable
private fun BreakdownRow(
    label: String,
    amount: Double,
    bold: Boolean = false,
    isDiscount: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = if (bold) MaterialTheme.typography.titleSmall
            else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            (if (isDiscount) "- " else "") +
                    "R${String.format(Locale.US, "%.2f", amount)}",
            style = if (bold) MaterialTheme.typography.titleSmall
            else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = if (isDiscount) OrangeAccent else BabyBlueDeep
        )
    }
}

@Composable
private fun DiscountChip(
    selected: Boolean,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        enabled = enabled
    )
}

@Composable
private fun StatusPill(status: QuoteStatus) {
    // AWAITING_PAYMENT branch added so `when` is exhaustive.
    val (bg, fg) = when (status) {
        QuoteStatus.PENDING          -> Color(0xFFFFE0B2) to Color(0xFF8B5A00)
        QuoteStatus.ACCEPTED         -> Color(0xFFE6F4EA) to Color(0xFF2E7D32)
        QuoteStatus.DECLINED         -> Color(0xFFF4DADD) to Color(0xFF8B1E20)
        QuoteStatus.EXPIRED          -> Color(0xFFE8EDF3) to Color(0xFF152A47)
        QuoteStatus.AWAITING_PAYMENT -> Color(0xFFFFE0B2) to Color(0xFF8B5A00)
        QuoteStatus.PAID             -> Color(0xFFE6F4EA) to Color(0xFF2E7D32)
    }
    Surface(shape = RoundedCornerShape(50), color = bg) {
        Text(
            status.name.lowercase().replaceFirstChar { it.titlecase() },
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}