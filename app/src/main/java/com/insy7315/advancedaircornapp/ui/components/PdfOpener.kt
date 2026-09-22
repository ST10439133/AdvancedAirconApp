// app/src/main/java/com/insy7315/advancedaircornapp/ui/components/PdfOpener.kt
package com.insy7315.advancedaircornapp.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

fun openPdfInViewer(context: Context, pdfUrl: String) {
    if (pdfUrl.isBlank()) {
        Toast.makeText(context, "No catalogue available", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(pdfUrl), "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {

        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl))
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(browserIntent)
        } catch (e2: Exception) {
            Toast.makeText(context, "Could not open catalogue", Toast.LENGTH_SHORT).show()
        }
    }
}