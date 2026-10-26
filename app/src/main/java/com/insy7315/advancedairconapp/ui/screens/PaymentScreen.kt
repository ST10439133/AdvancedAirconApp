// app/src/main/java/com/insy7315/advancedairconapp/ui/screens/PaymentScreen.kt
package com.insy7315.advancedairconapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.insy7315.advancedairconapp.data.entities.Job
import com.insy7315.advancedairconapp.data.entities.Quote
import com.insy7315.advancedairconapp.navigation.NavManager
import com.insy7315.advancedairconapp.utils.ReceiptGenerator
import com.insy7315.advancedairconapp.viewmodels.PaymentViewModel
import java.util.Locale

private val BabyBlue     = Color(0xFF1F3A5F)
private val BabyBlueDeep = Color(0xFF152A47)
private val BabyBlueSoft = Color(0xFFE8EDF3)
private val OrangeAccent = Color(0xFFC8102E)
private val SuccessGreen = Color(0xFF2E7D32)
private val SuccessSoft  = Color(0xFFE6F4EA)
private val ErrorRed     = Color(0xFF8B1E20)
private val ErrorSoft    = Color(0xFFF4DADD)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    quote: Quote,
    job: Job,
    customerEmail: String,
    customerName: String,
    technicianName: String,
    navManager: NavManager,
    onPaymentComplete: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: PaymentViewModel = viewModel(
        factory = PaymentViewModel.Factory(
            context.applicationContext as android.app.Application
        )
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val amount = if (quote.finalTotal > 0) quote.finalTotal else quote.grandTotal

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isSuccess) "Payment Complete" else "Checkout") },
                navigationIcon = {
                    if (!uiState.isProcessing) {
                        IconButton(
                            onClick = {
                                if (uiState.isSuccess) onPaymentComplete()
                                else navManager.navigateBack()
                            }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // =========================================================
            // SUCCESS SCREEN
            // =========================================================
            if (uiState.isSuccess) {
                SuccessContent(
                    quote = quote,
                    job = job,
                    amount = amount,
                    reference = uiState.reference,
                    receiptFile = uiState.receiptFile,
                    customerName = customerName,
                    technicianName = technicianName,
                    onDone = onPaymentComplete
                )
                return@Column
            }

            // =========================================================
            // CHECKOUT FLOW
            // =========================================================

            // Hero — order summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(listOf(BabyBlue, BabyBlueDeep))
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.22f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Payment for",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Text(
                                    job.buildingName.ifBlank { "Service" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Amount due",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            "R${String.format(Locale.US, "%.2f", amount)}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Order summary card
            SectionTitle("Order Summary")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BreakdownLine("Service Fee", quote.laborCost)
                    BreakdownLine("Parts & Materials", quote.partsCost)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    BreakdownLine("Subtotal", quote.grandTotal, bold = true)
                    if (quote.discountAmount > 0) {
                        BreakdownLine(
                            "Discount",
                            -quote.discountAmount,
                            isDiscount = true
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Total",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = BabyBlueDeep
                        )
                        Text(
                            "R${String.format(Locale.US, "%.2f", amount)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = BabyBlueDeep
                        )
                    }
                }
            }

            // Job details
            SectionTitle("Job Details")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailRow("Quote", "#${quote.id}")
                    DetailRow("Job", "#${job.id}")
                    DetailRow("Building", job.buildingName.ifBlank { "—" })
                    DetailRow("Service", job.issueType.ifBlank { "—" })
                    DetailRow("Technician", technicianName)
                    DetailRow("Customer", customerName)
                }
            }

            // Payment method (mock)
            SectionTitle("Payment Method")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = BabyBlueSoft,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = BabyBlueDeep,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Secure Card Payment",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Charged to $customerEmail",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = SuccessSoft
                    ) {
                        Text(
                            "PCI-DSS",
                            style = MaterialTheme.typography.labelSmall,
                            color = SuccessGreen,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(
                                horizontal = 8.dp, vertical = 3.dp
                            )
                        )
                    }
                }
            }

            // Error banner
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ErrorSoft)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            error,
                            style = MaterialTheme.typography.bodySmall,
                            color = ErrorRed
                        )
                    }
                }
            }

            // Pay button
            Button(
                onClick = {
                    viewModel.payNow(
                        quote = quote,
                        job = job,
                        customerEmail = customerEmail,
                        customerName = customerName,
                        technicianName = technicianName
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeAccent,
                    contentColor = Color.White
                ),
                enabled = !uiState.isProcessing
            ) {
                if (uiState.isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Processing…",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Icon(Icons.Default.Lock, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Pay R${String.format(Locale.US, "%.2f", amount)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                "Payments are processed securely. A receipt is generated automatically after payment.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ================================================================
// SUCCESS CONTENT
// ================================================================

@Composable
private fun SuccessContent(
    quote: Quote,
    job: Job,
    amount: Double,
    reference: String?,
    receiptFile: java.io.File?,
    customerName: String,
    technicianName: String,
    onDone: () -> Unit
) {
    val context = LocalContext.current

    // Header — success badge
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = SuccessSoft,
            modifier = Modifier.size(96.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(56.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Payment Successful",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = BabyBlueDeep
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "R${String.format(Locale.US, "%.2f", amount)} paid",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    // Confirmation card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DetailRow("Reference", reference ?: "—")
            DetailRow("Amount", "R${String.format(Locale.US, "%.2f", amount)}")
            DetailRow("Building", job.buildingName.ifBlank { "—" })
            DetailRow("Technician", technicianName)
            DetailRow("Customer", customerName)
            DetailRow("Status", "PAID", valueColor = SuccessGreen)
        }
    }

    // Receipt actions
    if (receiptFile != null) {
        SectionTitle("Receipt")

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BabyBlueSoft,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Receipt,
                            contentDescription = null,
                            tint = BabyBlueDeep,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Payment Receipt",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "PDF · ${receiptFile.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        try {
                            ReceiptGenerator.open(context, receiptFile)
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                e.message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Download, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("View", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = {
                        try {
                            val saved = ReceiptGenerator.downloadToDevice(context, receiptFile)
                            if (saved != null) {
                                Toast.makeText(
                                    context,
                                    "Saved to Downloads",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Download failed",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                e.message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Download, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Download", style = MaterialTheme.typography.labelLarge)
                }
                Button(
                    onClick = {
                        try {
                            ReceiptGenerator.share(context, receiptFile)
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                e.message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BabyBlueDeep,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Share, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    // Done
    Button(
        onClick = onDone,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = OrangeAccent,
            contentColor = Color.White
        )
    ) {
        Text(
            "Done",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
    }

    Spacer(Modifier.height(16.dp))
}

// ================================================================
// SMALL HELPERS
// ================================================================

@Composable
private fun SectionTitle(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = BabyBlueDeep
        )
    }
}

@Composable
private fun BreakdownLine(
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
                    "R${String.format(Locale.US, "%.2f", kotlin.math.abs(amount))}",
            style = if (bold) MaterialTheme.typography.titleSmall
            else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = if (isDiscount) OrangeAccent else BabyBlueDeep
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}