package com.sheshabiz.quickquote.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.sheshabiz.quickquote.data.db.entity.BusinessProfile
import com.sheshabiz.quickquote.data.db.entity.DiscountType
import com.sheshabiz.quickquote.domain.model.QuoteWithItems
import java.io.File
import java.io.FileOutputStream

/**
 * Renders a quotation as a professional, print-ready A4 PDF using Android's native
 * PDF APIs only (no third-party library). Handles multi-page overflow when a quote
 * has many line items or a long description.
 */
class PdfGenerator(private val context: Context) {

    private val pageWidth = 595f
    private val pageHeight = 842f
    private val margin = 40f
    private val contentWidth = pageWidth - margin * 2
    private val footerReserve = 30f

    private val brandColor = Color.parseColor("#2E9E8B")
    private val mutedColor = Color.parseColor("#8C8C8C")
    private val textColor = Color.parseColor("#101012")
    private val dividerColor = Color.parseColor("#E7E7E3")
    private val tableHeaderBg = Color.parseColor("#EAF6F3")

    fun generate(profile: BusinessProfile, data: QuoteWithItems): File {
        val quote = data.quote
        val doc = PdfDocument()

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth.toInt(), pageHeight.toInt(), pageNumber).create()
        var page = doc.startPage(pageInfo)
        var canvas = page.canvas
        var y = margin

        fun startNewPage() {
            doc.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth.toInt(), pageHeight.toInt(), pageNumber).create()
            page = doc.startPage(pageInfo)
            canvas = page.canvas
            y = margin
        }

        fun ensureSpace(needed: Float) {
            if (y + needed > pageHeight - margin - footerReserve) {
                startNewPage()
            }
        }

        // ---- Header (business identity) ----
        val logoBitmap = decodeLogo(profile.logoUri)
        val headerTextX: Float
        if (logoBitmap != null) {
            val box = 56f
            val scaled = scaleBitmapToBox(logoBitmap, box)
            canvas.drawBitmap(scaled, margin, y, null)
            headerTextX = margin + box + 14f
        } else {
            headerTextX = margin
        }
        val nameP = textPaint(20f, true, textColor)
        canvas.drawText(profile.businessName, headerTextX, y + 20f, nameP)
        var contactY = y + 38f
        val smallMuted = textPaint(10.5f, false, mutedColor)
        val contactLines = listOfNotNull(
            profile.phone.takeIf { it.isNotBlank() },
            profile.email.takeIf { it.isNotBlank() },
            profile.address.takeIf { it.isNotBlank() },
            profile.vatNumber?.takeIf { it.isNotBlank() }?.let { "VAT: $it" }
        )
        for (line in contactLines) {
            canvas.drawText(line, headerTextX, contactY, smallMuted)
            contactY += 13f
        }
        y = maxOf(contactY, y + 56f) + 10f

        drawDivider(canvas, y)
        y += 20f

        // ---- Quotation meta ----
        val titleP = textPaint(18f, true, brandColor)
        canvas.drawText("QUOTATION", margin, y + 14f, titleP)
        val metaLabel = textPaint(9.5f, false, mutedColor)
        val metaValue = textPaint(11f, true, textColor)
        val col2 = margin + 220f
        val col3 = margin + 360f
        canvas.drawText("QUOTE NUMBER", margin, y + 34f, metaLabel)
        canvas.drawText(quote.quoteNumber, margin, y + 48f, metaValue)
        canvas.drawText("DATE", col2, y + 34f, metaLabel)
        canvas.drawText(CurrencyFormat.formatDate(quote.quoteDate), col2, y + 48f, metaValue)
        canvas.drawText("VALID UNTIL", col3, y + 34f, metaLabel)
        canvas.drawText(CurrencyFormat.formatDate(quote.validUntil), col3, y + 48f, metaValue)
        y += 62f

        drawDivider(canvas, y)
        y += 18f

        // ---- Customer ----
        val sectionLabel = textPaint(10f, true, mutedColor)
        canvas.drawText("CUSTOMER", margin, y, sectionLabel)
        y += 16f
        val custName = textPaint(13f, true, textColor)
        canvas.drawText(quote.customerName, margin, y, custName)
        y += 16f
        val custDetail = textPaint(11f, false, textColor)
        val customerLines = listOfNotNull(
            quote.customerPhone.takeIf { it.isNotBlank() },
            quote.customerEmail?.takeIf { it.isNotBlank() },
            quote.customerAddress?.takeIf { it.isNotBlank() }
        )
        for (line in customerLines) {
            canvas.drawText(line, margin, y, custDetail)
            y += 14f
        }
        y += 14f

        // ---- Items table ----
        val colDescX = margin
        val colQtyX = margin + 330f
        val colPriceX = margin + 390f
        val colTotalX = margin + 470f
        val descColWidth = colQtyX - colDescX - 10f

        fun drawTableHeader() {
            val headerH = 24f
            val bgPaint = Paint().apply { color = tableHeaderBg; style = Paint.Style.FILL }
            canvas.drawRect(margin, y, margin + contentWidth, y + headerH, bgPaint)
            val h = textPaint(9.5f, true, textColor)
            canvas.drawText("DESCRIPTION", colDescX + 8f, y + 16f, h)
            canvas.drawText("QTY", colQtyX, y + 16f, h)
            canvas.drawText("PRICE", colPriceX, y + 16f, h)
            canvas.drawText("TOTAL", colTotalX, y + 16f, h)
            y += headerH + 6f
        }

        ensureSpace(60f)
        drawTableHeader()

        val itemDescPaint = TextPaint().apply {
            isAntiAlias = true
            textSize = 11f
            color = textColor
        }
        val rowValuePaint = textPaint(11f, false, textColor)

        for (item in data.items.sortedBy { it.sortOrder }) {
            val layout = StaticLayout.Builder
                .obtain(item.description, 0, item.description.length, itemDescPaint, descColWidth.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .build()
            val rowHeight = maxOf(layout.height.toFloat(), 16f) + 14f

            ensureSpace(rowHeight)
            if (y == margin) {
                // We just started a fresh continuation page mid-table: redraw header.
                val cont = textPaint(11f, true, mutedColor)
                canvas.drawText("${quote.quoteNumber} (continued)", margin, y, cont)
                y += 18f
                drawTableHeader()
                ensureSpace(rowHeight)
            }

            canvas.save()
            canvas.translate(colDescX + 8f, y)
            layout.draw(canvas)
            canvas.restore()

            val qtyText = formatQuantity(item.quantity)
            canvas.drawText(qtyText, colQtyX, y + 12f, rowValuePaint)
            canvas.drawText(CurrencyFormat.format(item.unitPrice), colPriceX, y + 12f, rowValuePaint)
            canvas.drawText(CurrencyFormat.format(item.lineTotal), colTotalX, y + 12f, rowValuePaint)

            y += rowHeight
            val rowDivider = Paint().apply { color = dividerColor; strokeWidth = 1f }
            canvas.drawLine(margin, y - 6f, margin + contentWidth, y - 6f, rowDivider)
        }

        y += 12f

        // ---- Totals ----
        val totalsHeight = 24f * (2 + (if (quote.discountAmount > 0) 1 else 0) + (if (quote.vatEnabled) 1 else 0)) + 20f
        ensureSpace(totalsHeight)

        val totalsX = margin + contentWidth - 200f
        val totalsValueX = margin + contentWidth
        val totalLabel = textPaint(11f, false, mutedColor)
        val totalValue = textPaint(11f, false, textColor)

        canvas.drawText("Subtotal", totalsX, y, totalLabel)
        canvas.drawTextRightAligned(CurrencyFormat.format(quote.subtotal), totalsValueX, y, totalValue)
        y += 20f

        if (quote.discountAmount > 0) {
            val label = if (quote.discountType == DiscountType.PERCENT) {
                "Discount (${trimNumber(quote.discountValue)}%)"
            } else "Discount"
            canvas.drawText(label, totalsX, y, totalLabel)
            canvas.drawTextRightAligned("-${CurrencyFormat.format(quote.discountAmount)}", totalsValueX, y, totalValue)
            y += 20f
        }

        if (quote.vatEnabled) {
            canvas.drawText("VAT (${trimNumber(quote.vatRate)}%)", totalsX, y, totalLabel)
            canvas.drawTextRightAligned(CurrencyFormat.format(quote.vatAmount), totalsValueX, y, totalValue)
            y += 20f
        }

        val totalBarPaint = Paint().apply { color = brandColor; style = Paint.Style.FILL }
        val barRect = RectF(totalsX - 12f, y - 4f, totalsValueX, y + 24f)
        canvas.drawRoundRect(barRect, 6f, 6f, totalBarPaint)
        val totalLabelWhite = textPaint(12f, true, Color.WHITE)
        val totalValueWhite = textPaint(15f, true, Color.WHITE)
        canvas.drawText("TOTAL", totalsX, y + 15f, totalLabelWhite)
        canvas.drawTextRightAligned(CurrencyFormat.format(quote.total), totalsValueX - 6f, y + 16f, totalValueWhite)
        y += 42f

        // ---- Notes / payment terms ----
        val notesAndTerms = listOfNotNull(
            quote.paymentTerms?.takeIf { it.isNotBlank() }?.let { "Payment terms" to it },
            quote.notes?.takeIf { it.isNotBlank() }?.let { "Notes" to it }
        )
        for ((label, text) in notesAndTerms) {
            val bodyPaint = TextPaint().apply { isAntiAlias = true; textSize = 10.5f; color = textColor }
            val layout = StaticLayout.Builder
                .obtain(text, 0, text.length, bodyPaint, contentWidth.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()
            ensureSpace(30f + layout.height)
            canvas.drawText(label.uppercase(), margin, y, sectionLabel)
            y += 14f
            canvas.save()
            canvas.translate(margin, y)
            layout.draw(canvas)
            canvas.restore()
            y += layout.height + 16f
        }

        // ---- Footer ----
        ensureSpace(24f)
        val footerPaint = textPaint(11f, false, mutedColor)
        canvas.drawCenteredText("Thank you for your business.", pageWidth / 2f, y + 10f, footerPaint)

        doc.finishPage(page)

        val outFile = outputFile(quote.quoteNumber)
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        return outFile
    }

    fun fileForQuote(quoteNumber: String): File = outputFile(quoteNumber)

    private fun outputFile(quoteNumber: String): File {
        val dir = File(context.cacheDir, "quotes").apply { mkdirs() }
        val safeName = quoteNumber.replace(Regex("[^A-Za-z0-9-]"), "_")
        return File(dir, "Quote-$safeName.pdf")
    }

    private fun decodeLogo(logoUri: String?): Bitmap? {
        if (logoUri.isNullOrBlank()) return null
        return runCatching {
            val file = File(logoUri)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                context.contentResolver.openInputStream(Uri.parse(logoUri))?.use {
                    BitmapFactory.decodeStream(it)
                }
            }
        }.getOrNull()
    }

    private fun scaleBitmapToBox(bitmap: Bitmap, box: Float): Bitmap {
        val ratio = minOf(box / bitmap.width, box / bitmap.height)
        val w = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val h = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }

    private fun drawDivider(canvas: Canvas, y: Float) {
        val p = Paint().apply { color = dividerColor; strokeWidth = 1f }
        canvas.drawLine(margin, y, margin + contentWidth, y, p)
    }

    private fun textPaint(size: Float, bold: Boolean, colorInt: Int): Paint = Paint().apply {
        isAntiAlias = true
        textSize = size
        color = colorInt
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT,
            if (bold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL
        )
    }

    private fun Canvas.drawTextRightAligned(text: String, xRight: Float, y: Float, paint: Paint) {
        val width = paint.measureText(text)
        drawText(text, xRight - width, y, paint)
    }

    private fun Canvas.drawCenteredText(text: String, xCenter: Float, y: Float, paint: Paint) {
        val width = paint.measureText(text)
        drawText(text, xCenter - width / 2f, y, paint)
    }

    private fun formatQuantity(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

    private fun trimNumber(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
}
