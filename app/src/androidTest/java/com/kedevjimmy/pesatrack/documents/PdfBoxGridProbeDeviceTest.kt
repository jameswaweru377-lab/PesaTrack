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
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Compile-only while instrumentation is paused; synthetic document, no financial data. */
@RunWith(AndroidJUnit4::class)
class PdfBoxGridProbeDeviceTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun source(ruled:Boolean,filled:Boolean=false):File {
        PDFBoxResourceLoader.init(context)
        val f=File.createTempFile("invented-grid-", ".pdf", context.cacheDir)
        PDDocument().use { doc ->
            val p=PDPage(PDRectangle.LETTER)
            doc.addPage(p)
            PDPageContentStream(doc,p).use {draw ->
                if(ruled) {
                    for(y in listOf(100f,200f,300f)) {
                        draw.moveTo(30f,y);draw.lineTo(550f,y);draw.stroke()
                    }
                    for(x in listOf(35f,250f,540f)) {
                        draw.moveTo(x,90f);draw.lineTo(x,450f);draw.stroke()
                    }
                }
                if(filled) {
                    // Some PDF exporters draw grid rules as narrow filled
                    // rectangles instead of stroking a line.
                    for(y in listOf(100f,200f,300f)) {
                        draw.addRect(30f,y,520f,1f);draw.fill()
                    }
                    for(x in listOf(35f,250f,540f)) {
                        draw.addRect(x,90f,1f,360f);draw.fill()
                    }
                }
            }
            doc.protect(StandardProtectionPolicy("invented-owner","invented-password",AccessPermission()).apply{
                encryptionKeyLength=128
            })
            doc.save(f)
        }
        return f
    }
    private fun probe(f:File,password:String)=ParcelFileDescriptor.open(
        f,ParcelFileDescriptor.MODE_READ_ONLY
    ).use {fd->PdfBoxGridProbe.inspect(context,fd,password)}

    @Test fun syntheticRuledEncryptedPdfReportsPossibleGrid() {
        val f=source(true)
        try {
            val result=probe(f,"invented-password")
            assertEquals(1,result.pages.size)
            assertTrue(result.pages.first().possibleVectorGrid)
            assertTrue(result.safeSummary().contains("no transactions were imported"))
        } finally {f.delete()}
    }
    @Test fun syntheticFilledRuleDocumentReportsAThinFillGrid() {
        val f=source(ruled=false,filled=true)
        try {
            val evidence=probe(f,"invented-password").pages.single()
            assertTrue(evidence.possibleVectorGrid)
            assertTrue(evidence.thinFilledHorizontalRules >= 3)
            assertTrue(evidence.thinFilledVerticalRules >= 3)
        } finally {f.delete()}
    }
    @Test fun blankBorderlessDocumentNeverClaimsVectorGrid() {
        val f=source(false)
        try {assertFalse(probe(f,"invented-password").pages.first().possibleVectorGrid)}
        finally {f.delete()}
    }
    @Test fun incorrectPasswordRefusesInspection() {
        val f=source(true)
        try {
            try {probe(f,"wrong-password");fail("Expected password failure")}
            catch(e:PdfReadException){assertEquals(PdfFailure.PASSWORD,e.failure)}
        } finally {f.delete()}
    }
}
