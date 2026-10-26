// app/src/main/java/com/insy7315/advancedairconapp/utils/ReceiptGenerator.kt
package com.insy7315.advancedairconapp.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.insy7315.advancedairconapp.data.entities.Job
import com.insy7315.advancedairconapp.data.entities.Quote
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptGenerator {

    private const val PAGE_WIDTH = 595   // A4 portrait
    private const val PAGE_HEIGHT = 842

    fun generate(
        context: Context,
        quote: Quote,
        job: Job,
        customerName: String,
        technicianName: String,
        paymentReference: String,
        paidAt: Long = quote.paidAt ?: System.currentTimeMillis()
    ): File? {
        val doc = PdfDocument()
        val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = doc.startPage(info)
        val canvas = page.canvas

        drawReceipt(
            canvas,
            quote = quote,
            job = job,
            customerName = customerName,
            technicianName = technicianName,
            paymentReference = paymentReference,
            paidAt = paidAt
        )

        doc.finishPage(page)

        val dir = File(context.filesDir, "receipts").apply { if (!exists()) mkdirs() }
        val file = File(dir, "receipt_${quote.id}_${paidAt}.pdf")

        return try {
            FileOutputStream(file).use { doc.writeTo(it) }
            doc.close()
            file
        } catch (e: Exception) {
            doc.close()
            null
        }
    }

    private fun drawReceipt(
        canvas: Canvas,
        quote: Quote,
        job: Job,
        customerName: String,
        technicianName: String,
        paymentReference: String,
        paidAt: Long
    ) {
        val navy = Color.rgb(0x15, 0x2A, 0x47)
        val red = Color.rgb(0xC8, 0x10, 0x2E)
        val gray = Color.rgb(0x88, 0x88, 0x88)
        val lightGray = Color.rgb(0xE0, 0xE6, 0xEE)

        val left = 50f
        val right = PAGE_WIDTH - 50f
        var y = 60f

        // ---- Header bar ----
        val headerPaint = Paint().apply { color = navy }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 130f, headerPaint)

        val logoPaint = Paint().apply {
            color = Color.WHITE
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("ArcticFlow", left, 60f, logoPaint)

        val subtitlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 12f
        }
        canvas.drawText("HVAC Service Management", left, 82f, subtitlePaint)

        val receiptLabelPaint = Paint().apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("RECEIPT", right, 60f, receiptLabelPaint)

        val refLabelPaint = Paint().apply {
            color = Color.WHITE
            textSize = 11f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(paymentReference, right, 82f, refLabelPaint)

        y = 165f

        // ---- Section: Payment details ----
        val headingPaint = Paint().apply {
            color = navy
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("PAYMENT DETAILS", left, y, headingPaint)
        y += 22f

        val bodyPaint = Paint().apply { color = Color.BLACK; textSize = 11f }
        val bodyBoldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyRightPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            textAlign = Paint.Align.RIGHT
        }
        val bodyRightBoldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        val paidDateStr = dateFormat.format(Date(paidAt))

        canvas.drawText("Paid on", left, y, bodyPaint)
        canvas.drawText(paidDateStr, right, y, bodyRightPaint)
        y += 16f

        canvas.drawText("Customer", left, y, bodyPaint)
        canvas.drawText(customerName, right, y, bodyRightPaint)
        y += 16f

        canvas.drawText("Technician", left, y, bodyPaint)
        canvas.drawText(technicianName, right, y, bodyRightPaint)
        y += 16f

        canvas.drawText("Payment reference", left, y, bodyPaint)
        canvas.drawText(paymentReference, right, y, bodyRightPaint)
        y += 16f

        canvas.drawText("Status", left, y, bodyPaint)
        val statusPaint = Paint().apply {
            color = Color.rgb(0x2E, 0x7D, 0x32)
            textSize = 11f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("PAID", right, y, statusPaint)
        y += 30f

        // ---- Divider ----
        val dividerPaint = Paint().apply { color = lightGray; strokeWidth = 1f }
        canvas.drawLine(left, y, right, y, dividerPaint)
        y += 24f

        // ---- Section: Job details ----
        canvas.drawText("JOB DETAILS", left, y, headingPaint)
        y += 22f

        canvas.drawText("Building", left, y, bodyPaint)
        canvas.drawText(
            job.buildingName.ifBlank { "—" },
            right, y, bodyRightPaint
        )
        y += 16f

        canvas.drawText("Issue type", left, y, bodyPaint)
        canvas.drawText(
            job.issueType.ifBlank { "—" },
            right, y, bodyRightPaint
        )
        y += 16f

        canvas.drawText("Job ID", left, y, bodyPaint)
        canvas.drawText("#${job.id}", right, y, bodyRightPaint)
        y += 16f

        job.scheduledDate?.let { ts ->
            canvas.drawText("Scheduled", left, y, bodyPaint)
            canvas.drawText(
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(ts)),
                right, y, bodyRightPaint
            )
            y += 16f
        }

        if (job.fullAddress.isNotBlank()) {
            canvas.drawText("Address", left, y, bodyPaint)
            // Right-wrap long addresses
            val addrPaint = Paint().apply {
                color = Color.BLACK
                textSize = 10f
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(job.fullAddress, right, y, addrPaint)
            y += 16f
        }

        y += 14f

        // ---- Divider ----
        canvas.drawLine(left, y, right, y, dividerPaint)
        y += 24f

        // ---- Section: Breakdown ----
        canvas.drawText("PAYMENT BREAKDOWN", left, y, headingPaint)
        y += 22f

        canvas.drawText("Service Fee", left, y, bodyPaint)
        canvas.drawText(
            "R${"%.2f".format(Locale.US, quote.laborCost)}",
            right, y, bodyRightPaint
        )
        y += 16f

        canvas.drawText("Parts & Materials", left, y, bodyPaint)
        canvas.drawText(
            "R${"%.2f".format(Locale.US, quote.partsCost)}",
            right, y, bodyRightPaint
        )
        y += 16f

        canvas.drawText("Subtotal", left, y, bodyBoldPaint)
        canvas.drawText(
            "R${"%.2f".format(Locale.US, quote.grandTotal)}",
            right, y, bodyRightBoldPaint
        )
        y += 16f

        if (quote.discountAmount > 0) {
            val discountPaint = Paint().apply {
                color = red
                textSize = 11f
            }
            val discountRightPaint = Paint().apply {
                color = red
                textSize = 11f
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("Discount", left, y, discountPaint)
            canvas.drawText(
                "- R${"%.2f".format(Locale.US, quote.discountAmount)}",
                right, y, discountRightPaint
            )
            y += 16f
        }

        y += 4f
        canvas.drawLine(left, y, right, y, dividerPaint)
        y += 20f

        // ---- Total ----
        val totalPaint = Paint().apply {
            color = navy
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val totalRightPaint = Paint().apply {
            color = navy
            textSize = 20f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("TOTAL PAID", left, y, totalPaint)
        canvas.drawText(
            "R${"%.2f".format(Locale.US, quote.finalTotal)}",
            right, y, totalRightPaint
        )
        y += 40f

        // ---- Footer ----
        val footerPaint = Paint().apply {
            color = gray
            textSize = 9f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "Thank you for choosing ArcticFlow. This is a system-generated receipt.",
            PAGE_WIDTH / 2f, PAGE_HEIGHT - 60f, footerPaint
        )
        canvas.drawText(
            "For queries, contact support@arcticflow.co.za",
            PAGE_WIDTH / 2f, PAGE_HEIGHT - 45f, footerPaint
        )
    }

    fun share(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(intent, "Share receipt")
            )
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Could not share receipt: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun open(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "No PDF viewer found",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}