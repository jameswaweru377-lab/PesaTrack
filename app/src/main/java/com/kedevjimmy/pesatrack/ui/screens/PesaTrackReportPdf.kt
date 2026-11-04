package com.kedevjimmy.pesatrack.ui.screens

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.ContextCompat
import com.kedevjimmy.pesatrack.R
import java.io.OutputStream
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

/** Offline, user-directed PDF creation. No raw SMS, uploads, or new storage permission. */
internal fun writePesaTrackReportPdf(
    context: Context,
    content: ReportPdfContent,
    stream: OutputStream,
    brandColor: Int
) {
    val document = PdfDocument()
    try {
        val page = ReportPdfPageWriter(context, document, content, brandColor)
        page.open()
        page.summary()
        page.trend()
        page.categories()
        page.largestExpenses()
        page.coverage()
        page.finish()
        document.writeTo(stream)
    } finally {
        document.close()
    }
}

private class ReportPdfPageWriter(
    private val context: Context,
    private val document: PdfDocument,
    private val content: ReportPdfContent,
    private val primary: Int
) {
    private val width = 595f
    private val height = 842f
    private val margin = 42f
    private val ink = Color.rgb(21, 20, 52)
    private val muted = Color.rgb(93, 94, 118)
    private val pale = Color.rgb(247, 245, 255)
    private val rule = Color.rgb(229, 226, 240)
    private var index = 0
    private lateinit var page: PdfDocument.Page
    private lateinit var canvas: Canvas
    private var y = 0f

    private fun paint(color: Int, size: Float = 11f, bold: Boolean = false) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

    fun open() {
        index++
        page = document.startPage(PdfDocument.PageInfo.Builder(width.toInt(), height.toInt(), index).create())
        canvas = page.canvas
        canvas.drawColor(Color.WHITE)
        canvas.drawRect(0f, 0f, width, 99f, paint(primary))
        ContextCompat.getDrawable(context, R.drawable.ic_pesatrack_logo)?.let { logo ->
            logo.setBounds(42, 18, 68, 44)
            logo.draw(canvas)
        }
        canvas.drawText("PesaTrack", 76f, 37f, paint(Color.WHITE, 15f, bold = true))
        canvas.drawText("YOUR MONEY REPORT", margin, 66f, paint(Color.WHITE, 22f, bold = true))
        canvas.drawText(content.periodLabel, margin, 86f, paint(Color.WHITE, 10f))
        val footer = "Personal summary · Not a bank statement   |   Page $index"
        canvas.drawLine(margin, 802f, width - margin, 802f, paint(rule))
        canvas.drawText(footer, margin, 822f, paint(muted, 9f))
        y = 129f
    }

    private fun nextPage(required: Float) {
        if (y + required <= 782f) return
        document.finishPage(page)
        open()
    }

    private fun heading(text: String, room: Float = 34f) {
        nextPage(room)
        canvas.drawText(text, margin, y + 17f, paint(ink, 15f, bold = true))
        y += 30f
    }

    fun summary() {
        heading("Overview")
        val blocks = listOf(
            "Money in" to content.incomeMinor,
            "Money spent" to content.spentMinor,
            "Fees (separate)" to content.feesMinor,
            "Own-account transfers" to content.transferMinor
        )
        blocks.forEachIndexed { i, (name, amount) ->
            val x = margin + (i % 2) * 261f
            val top = y + (i / 2) * 80f
            canvas.drawRoundRect(x, top, x + 250f, top + 70f, 12f, 12f, paint(pale))
            canvas.drawText(name, x + 13f, top + 25f, paint(muted, 10f))
            fitText(money(amount), x + 13f, top + 51f, 226f, paint(ink, 17f, bold = true))
        }
        y += 173f
        if (content.needsReviewCount > 0) {
            paragraph("${content.needsReviewCount} Activity items still need review; this report does not turn uncertain evidence into confirmed spending.")
        }
    }

    fun trend() {
        heading("Spending over time", 180f)
        if (content.trend.isEmpty()) {
            paragraph("No trend observations for the selected period.")
            return
        }
        val values = content.trend.map { it.spentMinor.coerceAtLeast(0L) }
        val top = y + 8f
        val left = margin + 43f
        val right = width - margin - 13f
        val bottom = top + 115f
        val maxAmount = max(1L, values.maxOrNull() ?: 1L)
        val maxLabel = shortMoney(maxAmount)
        canvas.drawText(maxLabel, margin, top + 9f, paint(muted, 9f))
        canvas.drawText("0", margin + 19f, bottom, paint(muted, 9f))
        canvas.drawLine(left, top, left, bottom, paint(rule))
        canvas.drawLine(left, bottom, right, bottom, paint(rule))
        val step = if (values.size > 1) (right - left) / (values.size - 1) else 0f
        var priorX = 0f
        var priorY = 0f
        values.forEachIndexed { i, amount ->
            val x = left + i * step
            val pointY = bottom - (amount.toDouble() / maxAmount * 104.0).toFloat()
            if (i > 0) {
                canvas.drawLine(priorX, priorY, x, pointY, paint(primary).apply { strokeWidth = 2.8f })
            }
            canvas.drawCircle(x, pointY, 3.4f, paint(primary))
            if (content.trend.size <= 7 || i % 2 == 0 || i == values.lastIndex) {
                canvas.drawText(content.trend[i].label, x - 11f, bottom + 18f, paint(muted, 9f))
            }
            priorX = x
            priorY = pointY
        }
        y = bottom + 36f
    }

    fun categories() {
        heading("Where your money went")
        if (content.categories.isEmpty()) {
            paragraph("No categorized spending is available for this period.")
            return
        }
        val total = content.categories.sumOf { it.amountMinor.coerceAtLeast(0L) }.coerceAtLeast(1L)
        content.categories.forEach { category ->
            nextPage(46f)
            fitText(category.label, margin, y + 12f, 245f, paint(ink, 11f, bold = true))
            val ratio = (category.amountMinor.toDouble() / total).coerceIn(0.0, 1.0).toFloat()
            val money = money(category.amountMinor)
            fitRight(money, width - margin, y + 12f, 160f, paint(ink, 10f))
            canvas.drawRoundRect(margin, y + 24f, width - margin, y + 31f, 3f, 3f, paint(rule))
            canvas.drawRoundRect(margin, y + 24f, margin + (width - 2 * margin) * ratio, y + 31f, 3f, 3f, paint(primary))
            y += 42f
        }
        y += 6f
    }

    fun largestExpenses() {
        heading("Largest recorded expenses")
        paragraph("Original expense entries before refunds and fees; split-allocation rows are excluded.")
        if (content.largestExpenses.isEmpty()) {
            paragraph("No eligible expense entries in this period.")
            return
        }
        content.largestExpenses.forEachIndexed { i, entry ->
            nextPage(44f)
            val label = "${i + 1}. ${entry.label}"
            fitText(label, margin, y + 14f, 335f, paint(ink, 11f, bold = true))
            fitRight(money(entry.amountMinor), width - margin, y + 14f, 145f, paint(ink, 10f))
            fitText(entry.category.replace('_', ' '), margin + 14f, y + 29f, 300f, paint(muted, 9f))
            canvas.drawLine(margin, y + 37f, width - margin, y + 37f, paint(rule))
            y += 42f
        }
    }

    fun coverage() {
        heading("How to read this report", 80f)
        paragraph(content.coverageNote)
        paragraph("Generated locally on ${java.text.SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date())}. The exported PDF is not encrypted. Store or share it carefully.")
    }

    private fun paragraph(text: String) {
        val words = text.split(' ')
        val p = paint(muted, 10f)
        var line = ""
        words.forEach { word ->
            val proposed = if (line.isEmpty()) word else "$line $word"
            if (p.measureText(proposed) > width - margin * 2 && line.isNotEmpty()) {
                nextPage(20f)
                canvas.drawText(line, margin, y + 10f, p)
                y += 17f
                line = word
            } else line = proposed
        }
        if (line.isNotEmpty()) {
            nextPage(20f)
            canvas.drawText(line, margin, y + 10f, p)
            y += 21f
        }
    }

    private fun fitText(value: String, x: Float, baseline: Float, available: Float, p: Paint) {
        var display = value
        while (p.measureText(display) > available && display.length > 3) {
            display = display.dropLast(2).trimEnd() + "…"
        }
        canvas.drawText(display, x, baseline, p)
    }

    private fun fitRight(value: String, right: Float, baseline: Float, available: Float, p: Paint) {
        fitText(value, right - minOf(available, p.measureText(value)), baseline, available, p)
    }

    private fun shortMoney(minor: Long): String = "KES " +
        NumberFormat.getNumberInstance(Locale.US).format(BigDecimal.valueOf(minor, 2))

    private fun money(minor: Long): String = "KES " +
        NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }.format(BigDecimal.valueOf(minor, 2))

    fun finish() { document.finishPage(page) }
}
