package com.kedevjimmy.pesatrack.documents

import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kedevjimmy.pesatrack.domain.everyday.PdfStatementReview
import com.kedevjimmy.pesatrack.domain.evidence.PdfReadException
import com.kedevjimmy.pesatrack.domain.evidence.PdfFailure
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import java.io.File
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Completely invented two-page encrypted statement fixture. Instrumentation
 * remains paused; these tests are compiled by CI but not executed by default.
 */
@RunWith(AndroidJUnit4::class)
class PdfBoxStatementExtractorDeviceTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private val secret="fictional-password-for-geometry-fixture"
    private fun generated():File {
        PDFBoxResourceLoader.init(context)
        val path=File.createTempFile("fictional-statement-", ".pdf",context.cacheDir)
        PDDocument().use {doc ->
            val movements=listOf(
                listOf(
                    Triple("QAB1234569", "-20.00", "490.00"),
                    Triple("QAB1234568", "100.00", "510.00")
                ),
                listOf(Triple("QAB1234567","-50.00","410.00"))
            )
            movements.forEachIndexed {pageIndex,receipts ->
                val page=PDPage(PDRectangle.LETTER)
                doc.addPage(page)
                PDPageContentStream(doc,page).use {stream ->
                    fun write(text:String,x:Float,y:Float) {
                        stream.beginText()
                        stream.setFont(PDType1Font.HELVETICA,8f)
                        stream.newLineAtOffset(x,y)
                        stream.showText(text)
                        stream.endText()
                    }
                    write("M-PESA STATEMENT",25f,760f)
                    write("Statement Period: 01 Oct 2026 - 11 Oct 2026",25f,746f)
                    write("DETAILED STATEMENT",25f,731f)
                    write("Receipt No.",25f,704f)
                    write("Completion Time",97f,704f)
                    write("Details",226f,704f)
                    write("Transaction Status",328f,704f)
                    write("Paid In",418f,704f)
                    write("Withdrawn",471f,704f)
                    write("Balance",544f,704f)
                    receipts.forEachIndexed {index,receipt ->
                        val row=pageIndex*2+index
                        val date=when(row) {
                            0->"2026-10-10"
                            1->"2026-10-09"
                            else->"2026-10-08"
                        }
                        val y=678f-index*38f
                        write(receipt.first,25f,y)
                        write(date,97f,y)
                        write("10:30:00",168f,y)
                        write(if(row==1)"Reversal via API" else if(row==0)"Shop" else "Transport",228f,y)
                        write("Completed",339f,y)
                        write(receipt.second,424f,y)
                        write(receipt.third,546f,y)
                        if(row==1)write("from - 071***123456",228f,y-12f)
                    }
                    write("DISCLAIMER",25f,50f)
                }
            }
            doc.protect(StandardProtectionPolicy(
                "invented-owner",secret,AccessPermission()
            ).apply {encryptionKeyLength=128})
            doc.save(path)
        }
        return path
    }
    @Test fun fullyPrivateEncryptedTwoPageStatementReachesFinancialReview() {
        val file=generated()
        try {
            val pages=ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY)
                .use {fd->PdfBoxStatementExtractor.extract(context,fd,secret)}
            assertEquals(2,pages.size)
            val candidates=PdfStatementReview.read(pages,TimeZone.getTimeZone("Africa/Nairobi"))
            assertEquals(candidates.problems.joinToString(),3,candidates.rows.size)
            assertEquals(listOf(-2000L,10000L,-5000L),
                candidates.rows.map {it.signedMinor})
            assertTrue(candidates.rows[1].description.contains("071***123456"))
        } finally {file.delete()}
    }
    @Test fun wrongPasswordCannotReachFinancialReview() {
        val file=generated()
        try {
            try {
                ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY)
                    .use {fd->PdfBoxStatementExtractor.extract(context,fd,"wrong-password")}
                fail("The encrypted document was incorrectly accepted.")
            } catch(error:PdfReadException) {
                assertEquals(PdfFailure.PASSWORD,error.failure)
            }
        } finally {file.delete()}
    }
}
