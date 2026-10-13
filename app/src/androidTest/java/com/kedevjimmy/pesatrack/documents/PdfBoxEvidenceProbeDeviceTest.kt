package com.kedevjimmy.pesatrack.documents

import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.domain.evidence.PdfFailure
import com.kedevjimmy.pesatrack.domain.evidence.PdfReadException
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Synthetic-only device acceptance; intentionally NOT executed while
 * PesaTrack instrumentation is paused. Must compile in Android CI.
 */
@RunWith(AndroidJUnit4::class)
class PdfBoxEvidenceProbeDeviceTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val password = "fictional-test-password"

    private fun encryptedTwoPagePdf(): File {
        PDFBoxResourceLoader.init(context)
        val file=File.createTempFile("synthetic-pdfbox-", ".pdf",context.cacheDir)
        PDDocument().use {document->
            listOf(
                "QAB1234569 2026-10-10 Synthetic purchase Completed -20.00 490.00",
                "QAB1234568 2026-10-09 Synthetic refund Completed 100.00 510.00"
            ).forEach {line ->
                val page=PDPage(PDRectangle.LETTER)
                document.addPage(page)
                PDPageContentStream(document,page).use {canvas ->
                    canvas.beginText()
                    canvas.setFont(PDType1Font.HELVETICA,10f)
                    canvas.newLineAtOffset(24f,700f)
                    canvas.showText(line)
                    canvas.endText()
                }
            }
            document.protect(
                StandardProtectionPolicy("fictional-owner",password,AccessPermission()).apply {
                    encryptionKeyLength=128
                }
            )
            document.save(file)
        }
        return file
    }

    @Test fun offlinePasswordPdfCanYieldIndependentPositionCountsWithoutLedgerPosting() {
        val file=encryptedTwoPagePdf()
        try {
            val report=ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY).use {fd->
                PdfBoxEvidenceProbe.inspect(context,fd,password)
            }
            assertEquals(2,report.pages.size)
            assertTrue(report.pages.all {it.textCharacters>0 && it.positionedWords>0})
            assertEquals(2,report.totalCompleted)
            assertTrue(report.safeSummary().contains("read-only diagnostic"))
        } finally {file.delete()}
    }

    @Test fun wrongPasswordIsRejectedBeforeAnyEvidenceLeavesTheIsolatedWorker() {
        val file=encryptedTwoPagePdf()
        try {
            try {
                ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY).use {fd->
                    PdfBoxEvidenceProbe.inspect(context,fd,"incorrect-test-only")
                }
                fail("Wrong password unexpectedly opened synthetic PDF")
            } catch(e:PdfReadException) {
                assertEquals(PdfFailure.PASSWORD,e.failure)
            }
        } finally {file.delete()}
    }
}
