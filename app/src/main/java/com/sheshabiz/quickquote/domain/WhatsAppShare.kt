package com.sheshabiz.quickquote.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.sheshabiz.quickquote.data.db.entity.Quote

object WhatsAppShare {
    private val WHATSAPP_PACKAGES = listOf("com.whatsapp", "com.whatsapp.w4b")

    fun buildMessage(businessName: String, quote: Quote): String {
        val firstName = quote.customerName.trim().substringBefore(" ").ifBlank { quote.customerName }
        return buildString {
            append("Hi $firstName,\n\n")
            append("Please find your quotation from $businessName.\n\n")
            append("Quote: ${quote.quoteNumber}\n")
            append("Total: ${CurrencyFormat.format(quote.total)}\n\n")
            append("Thank you.")
        }
    }

    /** Shares the PDF via WhatsApp specifically if installed, otherwise the system share sheet. */
    fun sendViaWhatsApp(context: Context, pdfUri: Uri, message: String): Boolean {
        val whatsappPackage = resolvedWhatsAppPackage(context)
        if (whatsappPackage != null) {
            val intent = buildSendIntent(pdfUri, message).apply { setPackage(whatsappPackage) }
            context.startActivity(intent)
            return true
        }
        shareGeneric(context, pdfUri, message)
        return false
    }

    /** Opens the system share sheet so the PDF can be sent through any installed app. */
    fun shareGeneric(context: Context, pdfUri: Uri, message: String) {
        val intent = buildSendIntent(pdfUri, message)
        context.startActivity(Intent.createChooser(intent, "Share quotation"))
    }

    private fun buildSendIntent(pdfUri: Uri, message: String): Intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            putExtra(Intent.EXTRA_TEXT, message)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    private fun resolvedWhatsAppPackage(context: Context): String? {
        val pm = context.packageManager
        return WHATSAPP_PACKAGES.firstOrNull { pkg ->
            runCatching { pm.getPackageInfo(pkg, 0) }.isSuccess
        }
    }
}
