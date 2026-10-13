package com.kedevjimmy.pesatrack.documents

import android.content.Context
import android.os.ParcelFileDescriptor
import com.kedevjimmy.pesatrack.domain.evidence.PdfBoxPageProbe
import com.kedevjimmy.pesatrack.domain.evidence.PdfBoxProbeReport
import com.kedevjimmy.pesatrack.domain.evidence.PdfBoxWordBounds
import com.kedevjimmy.pesatrack.domain.evidence.PdfFailure
import com.kedevjimmy.pesatrack.domain.evidence.PdfReadException
import com.kedevjimmy.pesatrack.domain.evidence.PdfTextReviewPolicy
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.IOException

/**
 * Experimental independent PDFBox-Android extractor.
 *
 * INVOCATION IS DIAGNOSTIC-ONLY. Nothing returned by this class can create a
 * StatementRow, transaction or balance update. Neither source strings nor
 * glyph locations are persisted, logged or sent across the process boundary.
 *
 * The caller runs this in PesaTrack's already isolated :pdf_review service,
 * never the Compose/UI process. Original PDF bytes remain on the device.
 */
internal object PdfBoxEvidenceProbe {
    private const val MAX_POSITIONED_WORDS = 100_000
    private val receiptLine = Regex(
        "(?m)^[ \\t]*[A-Z0-9]{10}[ \\t]+(?:[0-9]{4}-[0-9]{2}-[0-9]{2}|[0-9]{2}/[0-9]{2}/[0-9]{4})",
        RegexOption.IGNORE_CASE
    )
    private val completed = Regex("\\bCOMPLETED\\b", RegexOption.IGNORE_CASE)

    /** The original file descriptor is consumed and closed by this method. */
    fun inspect(
        context: Context,
        descriptor: ParcelFileDescriptor,
        password: String?
    ): PdfBoxProbeReport {
        PdfTextReviewPolicy.requireBytes(descriptor.statSize)
        PDFBoxResourceLoader.init(context.applicationContext)
        try {
            ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { input ->
                PDDocument.load(input, password.orEmpty()).use { document ->
                    val count = document.numberOfPages
                    if (count !in 1..PdfTextReviewPolicy.MAX_PAGES)
                        throw PdfReadException(PdfFailure.TOO_MANY_PAGES)
                    var characters = 0L
                    var positioned = 0
                    val pages = (0 until count).map { pageIndex ->
                        val page = document.getPage(pageIndex)
                        val crop = page.cropBox
                        // TextPosition.getXDirAdj/getYDirAdj are top-left,
                        // direction-adjusted coordinates; swap dimensions
                        // for 90/270-degree page rotation before bounding.
                        val rotated = (page.rotation % 180 + 180) % 180 == 90
                        val pageWidth = if (rotated) crop.height else crop.width
                        val pageHeight = if (rotated) crop.width else crop.height
                        val stripper = CountingStripper(pageWidth, pageHeight) {
                            positioned++
                            if (positioned > MAX_POSITIONED_WORDS)
                                throw PdfReadException(PdfFailure.TOO_MUCH_TEXT)
                        }
                        stripper.setStartPage(pageIndex + 1)
                        stripper.setEndPage(pageIndex + 1)
                        stripper.setSortByPosition(true)
                        val pageText = stripper.getText(document)
                        characters += pageText.length
                        if (characters > PdfTextReviewPolicy.MAX_CHARACTERS)
                            throw PdfReadException(PdfFailure.TOO_MUCH_TEXT)
                        PdfBoxPageProbe(
                            number = pageIndex + 1,
                            textCharacters = pageText.length,
                            positionedWords = stripper.positionedWords,
                            unpositionedWords = stripper.unpositionedWords,
                            invalidRectangles = stripper.invalidRectangles,
                            possibleReceiptRows = receiptLine.findAll(pageText).count(),
                            completedMarkers = completed.findAll(pageText).count()
                        )
                    }
                    if (pages.all { it.textCharacters == 0 })
                        throw PdfReadException(PdfFailure.NO_TEXT)
                    return PdfBoxProbeReport(pages)
                }
            }
        } catch (_: InvalidPasswordException) {
            throw PdfReadException(PdfFailure.PASSWORD)
        } catch (error: PdfReadException) {
            throw error
        } catch (_: IOException) {
            throw PdfReadException(PdfFailure.PARSE)
        }
    }

    /**
     * PDFBox supplies word glyphs via writeString after sorting and spacing.
     * Bounded rectangle math is shared with synthetic JVM tests.
     */
    private class CountingStripper(
        private val pageWidth: Float,
        private val pageHeight: Float,
        private val onWord: () -> Unit
    ) : PDFTextStripper() {
        var positionedWords = 0
            private set
        var unpositionedWords = 0
            private set
        var invalidRectangles = 0
            private set

        override fun writeString(text: String, textPositions: MutableList<TextPosition>) {
            if (text.isNotBlank()) {
                onWord()
                if (textPositions.isEmpty()) {
                    unpositionedWords++
                } else {
                    val glyphBounds = textPositions.map { glyph ->
                        val left = glyph.xDirAdj
                        val top = glyph.yDirAdj
                        PdfBoxWordBounds.Rect(
                            left, top, left + glyph.widthDirAdj, top + glyph.heightDir
                        )
                    }
                    if (PdfBoxWordBounds.fromCharacters(
                            glyphBounds, pageHeight, pageWidth
                        ) == null) invalidRectangles++
                    else positionedWords++
                }
            }
            super.writeString(text, textPositions)
        }
    }
}
