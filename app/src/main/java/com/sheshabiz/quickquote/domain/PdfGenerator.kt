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
import com.sheshabiz.quickquote.data.db.entity.InvoiceStatus
import com.sheshabiz.quickquote.data.db.entity.QuoteStatus
import com.sheshabiz.quickquote.domain.model.DocumentKind
import com.sheshabiz.quickquote.domain.model.InvoiceWithItems
import com.sheshabiz.quickquote.domain.model.PrintableDocument
import com.sheshabiz.quickquote.domain.model.PrintableItem
import com.sheshabiz.quickquote.domain.model.QuoteWithItems
import com.sheshabiz.quickquote.domain.model.ReportPdfData
import com.sheshabiz.quickquote.domain.model.SaleWithItems
import java.io.File
import java.io.FileOutputStream

/**
 * Renders a quotation or invoice as a professional, print-ready A4 PDF using Android's
 * native PDF APIs only (no third-party library). Handles multi-page overflow when a
 * document has many line items or a long description.
 */
class PdfGenerator(private val context: Context) {

    private val pageWidth = 595f
    private val pageHeight = 842f
    private val margin = 40f
    private val contentWidth = pageWidth - margin * 2
    private val footerReserve = 30f

    private val brandColor = Color.parseColor("#0BA84A")
    private val mutedColor = Color.parseColor("#8C8C8C")
    private val textColor = Color.parseColor("#101012")
    private val dividerColor = Color.parseColor("#E7E7E3")
    private val tableHeaderBg = Color.parseColor("#EAF6F3")
    private val errorColor = Color.parseColor("#C4453B")

    fun generateForQuote(profile: BusinessProfile, data: QuoteWithItems): File {
        val quote = data.quote
        val document = PrintableDocument(
            kind = DocumentKind.QUOTATION,
            number = quote.quoteNumber,
            primaryDate = quote.quoteDate,
            secondaryDateLabel = "VALID UNTIL",
            secondaryDate = quote.validUntil,
            statusLabel = if (quote.status == QuoteStatus.DRAFT) null else quote.status.name,
            customerName = quote.customerName,
            customerPhone = quote.customerPhone,
            customerEmail = quote.customerEmail,
            customerAddress = quote.customerAddress,
            items = data.items.sortedBy { it.sortOrder }.map {
                PrintableItem(it.description, it.quantity, it.unitPrice, it.lineTotal, it.sortOrder)
            },
            vatEnabled = quote.vatEnabled,
            vatRate = quote.vatRate,
            discountType = quote.discountType,
            discountValue = quote.discountValue,
            subtotal = quote.subtotal,
            discountAmount = quote.discountAmount,
            vatAmount = quote.vatAmount,
            total = quote.total,
            notes = quote.notes,
            paymentTerms = quote.paymentTerms
        )
        return render(profile, document)
    }

    fun generateForInvoice(profile: BusinessProfile, data: InvoiceWithItems): File {
        val invoice = data.invoice
        val document = PrintableDocument(
            kind = DocumentKind.INVOICE,
            number = invoice.invoiceNumber,
            primaryDate = invoice.invoiceDate,
            secondaryDateLabel = "DUE DATE",
            secondaryDate = invoice.dueDate,
            statusLabel = if (invoice.status == InvoiceStatus.PAID) "PAID" else "UNPAID",
            customerName = invoice.customerName,
            customerPhone = invoice.customerPhone,
            customerEmail = invoice.customerEmail,
            customerAddress = invoice.customerAddress,
            items = data.items.sortedBy { it.sortOrder }.map {
                PrintableItem(it.description, it.quantity, it.unitPrice, it.lineTotal, it.sortOrder)
            },
            vatEnabled = invoice.vatEnabled,
            vatRate = invoice.vatRate,
            discountType = invoice.discountType,
            discountValue = invoice.discountValue,
            subtotal = invoice.subtotal,
            discountAmount = invoice.discountAmount,
            vatAmount = invoice.vatAmount,
            total = invoice.total,
            notes = invoice.notes,
            paymentTerms = invoice.paymentTerms
        )
        return render(profile, document)
    }

    fun generateForSale(profile: BusinessProfile, data: SaleWithItems): File {
        val sale = data.sale
        val document = PrintableDocument(
            kind = DocumentKind.RECEIPT,
            number = sale.saleNumber,
            primaryDate = sale.saleDate,
            secondaryDateLabel = "PAYMENT",
            secondaryDate = sale.saleDate,
            secondaryValueText = sale.paymentMethod.name,
            statusLabel = null,
            customerName = sale.customerName,
            customerPhone = "",
            customerEmail = null,
            customerAddress = null,
            items = data.items.sortedBy { it.sortOrder }.map {
                PrintableItem(it.description, it.quantity, it.unitPrice, it.lineTotal, it.sortOrder)
            },
            vatEnabled = sale.vatEnabled,
            vatRate = sale.vatRate,
            discountType = sale.discountType,
            discountValue = sale.discountValue,
            subtotal = sale.subtotal,
            discountAmount = sale.discountAmount,
            vatAmount = sale.vatAmount,
            total = sale.total,
            notes = sale.notes,
            paymentTerms = null,
            amountTendered = sale.amountTendered,
            changeGiven = sale.changeGiven
        )
        return render(profile, document)
    }

    fun fileFor(kind: DocumentKind, number: String): File = outputFile(kind, number)

    /** A one-page summary report (sales/quotes/invoices totals for a period) meant to be
     * printed at close of day — a simpler layout than the itemised quote/invoice/receipt
     * documents, since there's no line-item table here. */
    fun generateReport(profile: BusinessProfile, data: ReportPdfData): File {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth.toInt(), pageHeight.toInt(), 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas
        var y = margin

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
            profile.address.takeIf { it.isNotBlank() }
        )
        for (line in contactLines) {
            canvas.drawText(line, headerTextX, contactY, smallMuted)
            contactY += 13f
        }
        y = maxOf(contactY, y + 56f) + 10f

        drawDivider(canvas, y)
        y += 24f

        val titleP = textPaint(18f, true, brandColor)
        canvas.drawText("SALES REPORT", margin, y + 4f, titleP)
        canvas.drawTextRightAligned(data.periodLabel.uppercase(), margin + contentWidth, y + 4f, textPaint(11f, true, mutedColor))
        y += 26f
        canvas.drawText("Generated ${CurrencyFormat.formatDate(data.generatedAt)}", margin, y, textPaint(9.5f, false, mutedColor))
        y += 26f

        drawDivider(canvas, y)
        y += 26f

        fun sectionHeader(title: String) {
            canvas.drawText(title.uppercase(), margin, y, textPaint(10f, true, mutedColor))
            y += 22f
        }

        fun statRow(label: String, value: String, valueColor: Int = textColor) {
            canvas.drawText(label, margin, y, textPaint(11.5f, false, textColor))
            canvas.drawTextRightAligned(value, margin + contentWidth, y, textPaint(12.5f, true, valueColor))
            y += 24f
        }

        sectionHeader("Point of sale")
        statRow("Sales made", data.salesCount.toString())
        statRow("Total sales", CurrencyFormat.format(data.salesTotal), brandColor)
        y += 14f

        sectionHeader("Quotations")
        statRow("Quotes created", data.quotesCreated.toString())
        statRow("Quotes accepted", data.quotesAccepted.toString())
        statRow("Total quoted", CurrencyFormat.format(data.quotesTotal), brandColor)
        y += 14f

        sectionHeader("Invoices")
        statRow("Paid (${data.invoicesPaidCount})", CurrencyFormat.format(data.invoicesPaidTotal))
        statRow("Unpaid (${data.invoicesUnpaidCount})", CurrencyFormat.format(data.invoicesUnpaidTotal))
        statRow(
            "Overdue",
            data.invoicesOverdueCount.toString(),
            if (data.invoicesOverdueCount > 0) errorColor else textColor
        )
        y += 30f

        drawDivider(canvas, y)
        y += 24f
        canvas.drawCenteredText("End of report.", pageWidth / 2f, y, textPaint(11f, false, mutedColor))

        pdfDoc.finishPage(page)
        val dir = File(context.cacheDir, "documents").apply { mkdirs() }
        val outFile = File(dir, "Report-${data.generatedAt}.pdf")
        FileOutputStream(outFile).use { pdfDoc.writeTo(it) }
        pdfDoc.close()
        return outFile
    }

    private fun render(profile: BusinessProfile, doc: PrintableDocument): File {
        val pdfDoc = PdfDocument()

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth.toInt(), pageHeight.toInt(), pageNumber).create()
        var page = pdfDoc.startPage(pageInfo)
        var canvas = page.canvas
        var y = margin

        fun startNewPage() {
            pdfDoc.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth.toInt(), pageHeight.toInt(), pageNumber).create()
            page = pdfDoc.startPage(pageInfo)
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
            profile.vatNumber?.takeIf { it.isNotBlank() }?.let { "VAT: $it" },
            profile.registrationNumber?.takeIf { it.isNotBlank() }?.let { "Reg No: $it" }
        )
        for (line in contactLines) {
            canvas.drawText(line, headerTextX, contactY, smallMuted)
            contactY += 13f
        }
        y = maxOf(contactY, y + 56f) + 10f

        drawDivider(canvas, y)
        y += 20f

        // ---- Document meta ----
        val titleP = textPaint(18f, true, brandColor)
        canvas.drawText(doc.kind.label, margin, y + 14f, titleP)
        doc.statusLabel?.let {
            val badgePaint = textPaint(10f, true, mutedColor)
            canvas.drawTextRightAligned(it, margin + contentWidth, y + 12f, badgePaint)
        }
        val metaLabel = textPaint(9.5f, false, mutedColor)
        val metaValue = textPaint(11f, true, textColor)
        val col2 = margin + 220f
        val col3 = margin + 360f
        canvas.drawText("${doc.kind.label} NUMBER", margin, y + 34f, metaLabel)
        canvas.drawText(doc.number, margin, y + 48f, metaValue)
        canvas.drawText("DATE", col2, y + 34f, metaLabel)
        canvas.drawText(CurrencyFormat.formatDate(doc.primaryDate), col2, y + 48f, metaValue)
        canvas.drawText(doc.secondaryDateLabel, col3, y + 34f, metaLabel)
        canvas.drawText(doc.secondaryValueText ?: CurrencyFormat.formatDate(doc.secondaryDate), col3, y + 48f, metaValue)
        y += 62f

        drawDivider(canvas, y)
        y += 18f

        // ---- Customer ----
        val sectionLabel = textPaint(10f, true, mutedColor)
        if (doc.customerName.isNotBlank()) {
            canvas.drawText("CUSTOMER", margin, y, sectionLabel)
            y += 16f
            val custName = textPaint(13f, true, textColor)
            canvas.drawText(doc.customerName, margin, y, custName)
            y += 16f
            val custDetail = textPaint(11f, false, textColor)
            val customerLines = listOfNotNull(
                doc.customerPhone.takeIf { it.isNotBlank() },
                doc.customerEmail?.takeIf { it.isNotBlank() },
                doc.customerAddress?.takeIf { it.isNotBlank() }
            )
            for (line in customerLines) {
                canvas.drawText(line, margin, y, custDetail)
                y += 14f
            }
            y += 14f
        }

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

        for (item in doc.items.sortedBy { it.sortOrder }) {
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
                canvas.drawText("${doc.number} (continued)", margin, y, cont)
                y += 18f
                drawTableHeader()
                ensureSpace(rowHeight)
            }

            canvas.save()
            canvas.translate(colDescX + 8f, y)
            layout.draw(canvas)
            canvas.restore()

            val qtyText = formatNumber(item.quantity)
            canvas.drawText(qtyText, colQtyX, y + 12f, rowValuePaint)
            canvas.drawText(CurrencyFormat.format(item.unitPrice), colPriceX, y + 12f, rowValuePaint)
            canvas.drawText(CurrencyFormat.format(item.lineTotal), colTotalX, y + 12f, rowValuePaint)

            y += rowHeight
            val rowDivider = Paint().apply { color = dividerColor; strokeWidth = 1f }
            canvas.drawLine(margin, y - 6f, margin + contentWidth, y - 6f, rowDivider)
        }

        y += 12f

        // ---- Totals ----
        val totalsHeight = 24f * (2 + (if (doc.discountAmount > 0) 1 else 0) + (if (doc.vatEnabled) 1 else 0)) + 20f
        ensureSpace(totalsHeight)

        val totalsX = margin + contentWidth - 200f
        val totalsValueX = margin + contentWidth
        val totalLabel = textPaint(11f, false, mutedColor)
        val totalValue = textPaint(11f, false, textColor)

        canvas.drawText("Subtotal", totalsX, y, totalLabel)
        canvas.drawTextRightAligned(CurrencyFormat.format(doc.subtotal), totalsValueX, y, totalValue)
        y += 20f

        if (doc.discountAmount > 0) {
            val label = if (doc.discountType == DiscountType.PERCENT) {
                "Discount (${formatNumber(doc.discountValue)}%)"
            } else "Discount"
            canvas.drawText(label, totalsX, y, totalLabel)
            canvas.drawTextRightAligned("-${CurrencyFormat.format(doc.discountAmount)}", totalsValueX, y, totalValue)
            y += 20f
        }

        if (doc.vatEnabled) {
            canvas.drawText("VAT (${formatNumber(doc.vatRate)}%)", totalsX, y, totalLabel)
            canvas.drawTextRightAligned(CurrencyFormat.format(doc.vatAmount), totalsValueX, y, totalValue)
            y += 20f
        }

        val totalBarPaint = Paint().apply { color = brandColor; style = Paint.Style.FILL }
        val barRect = RectF(totalsX - 12f, y - 4f, totalsValueX, y + 24f)
        canvas.drawRoundRect(barRect, 6f, 6f, totalBarPaint)
        val totalLabelWhite = textPaint(12f, true, Color.WHITE)
        val totalValueWhite = textPaint(15f, true, Color.WHITE)
        canvas.drawText("TOTAL", totalsX, y + 15f, totalLabelWhite)
        canvas.drawTextRightAligned(CurrencyFormat.format(doc.total), totalsValueX - 6f, y + 16f, totalValueWhite)
        y += 42f

        if (doc.amountTendered != null) {
            canvas.drawText("Cash tendered", totalsX, y, totalLabel)
            canvas.drawTextRightAligned(CurrencyFormat.format(doc.amountTendered), totalsValueX, y, totalValue)
            y += 20f
        }
        if (doc.changeGiven != null) {
            canvas.drawText("Change", totalsX, y, totalLabel)
            canvas.drawTextRightAligned(CurrencyFormat.format(doc.changeGiven), totalsValueX, y, totalValue)
            y += 20f
        }

        // ---- Notes / payment terms ----
        val notesAndTerms = listOfNotNull(
            doc.paymentTerms?.takeIf { it.isNotBlank() }?.let { "Payment terms" to it },
            doc.notes?.takeIf { it.isNotBlank() }?.let { "Notes" to it }
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

        // ---- Banking details ----
        val bankingLines = listOfNotNull(
            profile.bankName?.takeIf { it.isNotBlank() }?.let { "Bank: $it" },
            profile.accountHolder?.takeIf { it.isNotBlank() }?.let { "Account holder: $it" },
            profile.accountNumber?.takeIf { it.isNotBlank() }?.let { "Account number: $it" },
            profile.branchCode?.takeIf { it.isNotBlank() }?.let { "Branch code: $it" },
            profile.accountType?.takeIf { it.isNotBlank() }?.let { "Account type: $it" }
        )
        if (bankingLines.isNotEmpty()) {
            ensureSpace(30f + bankingLines.size * 14f)
            canvas.drawText("BANKING DETAILS", margin, y, sectionLabel)
            y += 16f
            val bankingPaint = textPaint(10.5f, false, textColor)
            for (line in bankingLines) {
                canvas.drawText(line, margin, y, bankingPaint)
                y += 14f
            }
            y += 12f
        }

        // ---- Footer ----
        ensureSpace(24f)
        val footerPaint = textPaint(11f, false, mutedColor)
        canvas.drawCenteredText("Thank you for your business.", pageWidth / 2f, y + 10f, footerPaint)

        pdfDoc.finishPage(page)

        val outFile = outputFile(doc.kind, doc.number)
        FileOutputStream(outFile).use { pdfDoc.writeTo(it) }
        pdfDoc.close()
        return outFile
    }

    private fun outputFile(kind: DocumentKind, number: String): File {
        val dir = File(context.cacheDir, "documents").apply { mkdirs() }
        val safeName = number.replace(Regex("[^A-Za-z0-9-]"), "_")
        return File(dir, "${kind.fileNamePrefix}-$safeName.pdf")
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

    private fun formatNumber(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
}
