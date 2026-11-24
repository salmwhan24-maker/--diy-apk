package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.local.InvoiceWithItems
import com.example.domain.InvoiceType
import com.example.domain.MoneyMath
import java.io.File
import java.io.FileOutputStream

object InvoiceDocumentRenderer {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    fun renderPdfBytes(context: Context, invoiceWithItems: InvoiceWithItems): ByteArray {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        drawInvoiceOnCanvas(context, page.canvas, invoiceWithItems)
        document.finishPage(page)

        val out = java.io.ByteArrayOutputStream()
        document.writeTo(out)
        document.close()
        return out.toByteArray()
    }

    fun renderPngBytes(context: Context, invoiceWithItems: InvoiceWithItems): ByteArray {
        val scale = 2
        val bitmap = Bitmap.createBitmap(PAGE_WIDTH * scale, PAGE_HEIGHT * scale, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(scale.toFloat(), scale.toFloat())
        drawInvoiceOnCanvas(context, canvas, invoiceWithItems)

        val out = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        bitmap.recycle()
        return out.toByteArray()
    }

    fun createShareablePdfUri(context: Context, invoiceWithItems: InvoiceWithItems): Uri {
        val dir = File(context.cacheDir, "invoices").apply { mkdirs() }
        val file = File(dir, "invoice_${invoiceWithItems.invoice.invoiceNumber}.pdf")
        FileOutputStream(file).use { it.write(renderPdfBytes(context, invoiceWithItems)) }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun createShareablePngUri(context: Context, invoiceWithItems: InvoiceWithItems): Uri {
        val dir = File(context.cacheDir, "invoices").apply { mkdirs() }
        val file = File(dir, "invoice_${invoiceWithItems.invoice.invoiceNumber}.png")
        FileOutputStream(file).use { it.write(renderPngBytes(context, invoiceWithItems)) }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun printInvoice(context: Context, invoiceWithItems: InvoiceWithItems) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "فاتورة_${invoiceWithItems.invoice.invoiceNumber}"
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?,
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder("$jobName.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?,
            ) {
                try {
                    val bytes = renderPdfBytes(context, invoiceWithItems)
                    FileOutputStream(destination?.fileDescriptor).use { it.write(bytes) }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }
        printManager.print(jobName, adapter, PrintAttributes.Builder().build())
    }

    private fun drawInvoiceOnCanvas(
        context: Context,
        canvas: Canvas,
        data: InvoiceWithItems,
    ) {
        val invoice = data.invoice
        val items = data.items
        val type = InvoiceType.fromName(invoice.type)
        val currency = invoice.currencyCode

        val cairoTypeface = try {
            ResourcesCompat.getFont(context, R.font.cairo) ?: Typeface.DEFAULT
        } catch (_: Exception) {
            Typeface.DEFAULT
        }
        val boldTypeface = Typeface.create(cairoTypeface, Typeface.BOLD)

        // Crisp white paper background
        canvas.drawColor(Color.WHITE)

        val goldColor = Color.parseColor("#B88917")
        val darkObsidian = Color.parseColor("#1B1712")
        val lightGoldBg = Color.parseColor("#FAF4E4")
        val borderColor = Color.parseColor("#D9C79E")
        val textDark = Color.parseColor("#1E1A14")
        val textMuted = Color.parseColor("#5C5243")
        val badgeColor = if (type == InvoiceType.SALE) Color.parseColor("#147349") else Color.parseColor("#B55608")

        val margin = 32f
        val contentWidth = PAGE_WIDTH - margin * 2

        // Outer border frame
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = goldColor
        }
        canvas.drawRoundRect(
            RectF(18f, 18f, PAGE_WIDTH - 18f, PAGE_HEIGHT - 18f),
            12f,
            12f,
            framePaint,
        )

        // Header Banner
        val headerRect = RectF(margin, margin, PAGE_WIDTH - margin, margin + 84f)
        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = darkObsidian }
        canvas.drawRoundRect(headerRect, 10f, 10f, headerBgPaint)

        drawRtlText(
            canvas = canvas,
            text = "أبو إسحاق لبيع وشراء الذهب الخام والمجوهرات",
            x = margin + 12f,
            y = margin + 14f,
            width = contentWidth - 24f,
            textSize = 17f,
            color = Color.parseColor("#F2D06B"),
            typeface = boldTypeface,
            alignment = Layout.Alignment.ALIGN_CENTER,
        )

        drawRtlText(
            canvas = canvas,
            text = "هاتف: 779944094   |   ${type.labelAr} رسمية",
            x = margin + 12f,
            y = margin + 46f,
            width = contentWidth - 24f,
            textSize = 12.5f,
            color = Color.WHITE,
            typeface = cairoTypeface,
            alignment = Layout.Alignment.ALIGN_CENTER,
        )

        var currentY = margin + 102f

        // Metadata box
        val metaRect = RectF(margin, currentY, PAGE_WIDTH - margin, currentY + 72f)
        val metaFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = lightGoldBg }
        val metaStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = borderColor
        }
        canvas.drawRoundRect(metaRect, 8f, 8f, metaFillPaint)
        canvas.drawRoundRect(metaRect, 8f, 8f, metaStrokePaint)

        // Type Badge
        val badgeRect = RectF(margin + 12f, currentY + 12f, margin + 122f, currentY + 38f)
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = badgeColor }
        canvas.drawRoundRect(badgeRect, 6f, 6f, badgePaint)
        drawRtlText(
            canvas = canvas,
            text = type.labelAr,
            x = margin + 14f,
            y = currentY + 16f,
            width = 106f,
            textSize = 11.5f,
            color = Color.WHITE,
            typeface = boldTypeface,
            alignment = Layout.Alignment.ALIGN_CENTER,
        )

        val colRightX = margin + 132f
        val colRightW = contentWidth - 144f
        drawRtlText(
            canvas = canvas,
            text = "رقم الفاتورة: ${invoice.invoiceNumber}     |     التاريخ: ${MoneyMath.formatDateTime(invoice.createdAtMillis)}",
            x = colRightX,
            y = currentY + 13f,
            width = colRightW,
            textSize = 11.5f,
            color = textDark,
            typeface = boldTypeface,
            alignment = Layout.Alignment.ALIGN_NORMAL,
        )

        val partyLabel = if (type == InvoiceType.SALE) "اسم العميل" else "اسم البائع"
        val partyText = invoice.partyName.ifBlank { "عميل نقدي" }
        val phonePart = if (invoice.partyPhone.isNotBlank()) "  (${invoice.partyPhone})" else ""
        drawRtlText(
            canvas = canvas,
            text = "$partyLabel: $partyText$phonePart",
            x = margin + 12f,
            y = currentY + 44f,
            width = contentWidth - 24f,
            textSize = 11.5f,
            color = textDark,
            typeface = cairoTypeface,
            alignment = Layout.Alignment.ALIGN_NORMAL,
        )

        currentY += 88f

        // Table Header
        val tableHeaderRect = RectF(margin, currentY, PAGE_WIDTH - margin, currentY + 28f)
        val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2A2218") }
        canvas.drawRoundRect(tableHeaderRect, 6f, 6f, tableHeaderPaint)

        // Columns RTL (right-to-left positions):
        // الصنف/الوصف | العيار | الوزن | سعر الجرام | المصنعية | الإجمالي
        val colWidths = floatArrayOf(155f, 76f, 70f, 78f, 70f, contentWidth - (155f + 76f + 70f + 78f + 70f))
        val colRightEdges = FloatArray(6)
        var edge = PAGE_WIDTH - margin
        for (i in colWidths.indices) {
            colRightEdges[i] = edge
            edge -= colWidths[i]
        }

        val headers = listOf("الصنف والوصف", "العيار", "الوزن (جم)", "سعر الجرام", "المصنعية", "الإجمالي")
        headers.forEachIndexed { idx, title ->
            val left = colRightEdges[idx] - colWidths[idx] + 4f
            drawRtlText(
                canvas = canvas,
                text = title,
                x = left,
                y = currentY + 6f,
                width = colWidths[idx] - 8f,
                textSize = 10f,
                color = Color.parseColor("#F2D06B"),
                typeface = boldTypeface,
                alignment = if (idx == 0) Layout.Alignment.ALIGN_NORMAL else Layout.Alignment.ALIGN_CENTER,
            )
        }

        currentY += 32f

        // Table Rows
        val rowAltPaint = Paint().apply { color = Color.parseColor("#FBF8F1") }
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#E5DAC2")
            strokeWidth = 0.8f
        }

        val maxRows = items.take(10)
        maxRows.forEachIndexed { index, item ->
            val rowHeight = 30f
            if (index % 2 == 1) {
                canvas.drawRect(margin, currentY, PAGE_WIDTH - margin, currentY + rowHeight, rowAltPaint)
            }
            val desc = if (item.description.isNotBlank()) "${item.categoryType} - ${item.description}" else item.categoryType
            val rowCells = listOf(
                desc,
                item.karatLabel,
                MoneyMath.formatPlainDecimal(MoneyMath.parseDecimal(item.weightGrams), 3),
                MoneyMath.formatAmount(MoneyMath.parseDecimal(item.pricePerGram)),
                MoneyMath.formatAmount(MoneyMath.parseDecimal(item.workmanshipAmount)),
                MoneyMath.formatAmount(MoneyMath.parseDecimal(item.lineTotalAmount)),
            )

            rowCells.forEachIndexed { idx, cellText ->
                val left = colRightEdges[idx] - colWidths[idx] + 4f
                drawRtlText(
                    canvas = canvas,
                    text = cellText,
                    x = left,
                    y = currentY + 7f,
                    width = colWidths[idx] - 8f,
                    textSize = 10f,
                    color = textDark,
                    typeface = if (idx == 5) boldTypeface else cairoTypeface,
                    alignment = if (idx == 0) Layout.Alignment.ALIGN_NORMAL else Layout.Alignment.ALIGN_CENTER,
                )
            }

            currentY += rowHeight
            canvas.drawLine(margin, currentY, PAGE_WIDTH - margin, currentY, dividerPaint)
        }

        currentY += 16f

        // Notes if any
        if (invoice.notes.isNotBlank()) {
            drawRtlText(
                canvas = canvas,
                text = "ملاحظات: ${invoice.notes}",
                x = margin,
                y = currentY,
                width = contentWidth,
                textSize = 11f,
                color = textMuted,
                typeface = cairoTypeface,
                alignment = Layout.Alignment.ALIGN_NORMAL,
            )
            currentY += 28f
        }

        // Totals Box (Left/End side or full clean summary card)
        val totalsRect = RectF(margin, currentY, PAGE_WIDTH - margin, currentY + 116f)
        canvas.drawRoundRect(totalsRect, 8f, 8f, metaFillPaint)
        canvas.drawRoundRect(totalsRect, 8f, 8f, metaStrokePaint)

        val totalWeight = MoneyMath.formatGrams(MoneyMath.parseDecimal(invoice.totalWeightGrams))
        val subtotalStr = MoneyMath.formatMoney(MoneyMath.parseDecimal(invoice.subtotalAmount), currency)
        val discountStr = MoneyMath.formatMoney(MoneyMath.parseDecimal(invoice.discountAmount), currency)
        val netTotalStr = MoneyMath.formatMoney(MoneyMath.parseDecimal(invoice.netTotalAmount), currency)

        drawRtlText(
            canvas = canvas,
            text = "إجمالي الوزن:  $totalWeight",
            x = margin + 16f,
            y = currentY + 12f,
            width = contentWidth - 32f,
            textSize = 11.5f,
            color = textDark,
            typeface = cairoTypeface,
            alignment = Layout.Alignment.ALIGN_NORMAL,
        )
        drawRtlText(
            canvas = canvas,
            text = "المجموع الفرعي:  $subtotalStr",
            x = margin + 16f,
            y = currentY + 34f,
            width = contentWidth - 32f,
            textSize = 11.5f,
            color = textDark,
            typeface = cairoTypeface,
            alignment = Layout.Alignment.ALIGN_NORMAL,
        )
        drawRtlText(
            canvas = canvas,
            text = "الخصم:  $discountStr",
            x = margin + 16f,
            y = currentY + 56f,
            width = contentWidth - 32f,
            textSize = 11.5f,
            color = textDark,
            typeface = cairoTypeface,
            alignment = Layout.Alignment.ALIGN_NORMAL,
        )
        canvas.drawLine(margin + 16f, currentY + 78f, PAGE_WIDTH - margin - 16f, currentY + 78f, framePaint)
        drawRtlText(
            canvas = canvas,
            text = "الصافي النهائي:  $netTotalStr",
            x = margin + 16f,
            y = currentY + 86f,
            width = contentWidth - 32f,
            textSize = 14.5f,
            color = goldColor,
            typeface = boldTypeface,
            alignment = Layout.Alignment.ALIGN_NORMAL,
        )

        // Signatures near bottom
        val sigY = PAGE_HEIGHT - 120f
        drawRtlText(
            canvas = canvas,
            text = "توقيع المحل (أبو إسحاق): __________________          توقيع العميل: __________________",
            x = margin,
            y = sigY,
            width = contentWidth,
            textSize = 11f,
            color = textDark,
            typeface = cairoTypeface,
            alignment = Layout.Alignment.ALIGN_CENTER,
        )

        // Footer
        drawRtlText(
            canvas = canvas,
            text = "أبو إسحاق لبيع وشراء الذهب الخام والمجوهرات   •   للتواصل: 779944094",
            x = margin,
            y = PAGE_HEIGHT - 56f,
            width = contentWidth,
            textSize = 10.5f,
            color = textMuted,
            typeface = boldTypeface,
            alignment = Layout.Alignment.ALIGN_CENTER,
        )
    }

    private fun drawRtlText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        width: Float,
        textSize: Float,
        color: Int,
        typeface: Typeface,
        alignment: Layout.Alignment,
    ) {
        val safeWidth = width.toInt().coerceAtLeast(1)
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSize
            this.color = color
            this.typeface = typeface
        }
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, safeWidth)
            .setAlignment(alignment)
            .setTextDirection(TextDirectionHeuristics.RTL)
            .setIncludePad(false)
            .build()
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }
}
