package com.kedevjimmy.pesatrack.documents

import android.content.Context
import android.graphics.Path
import android.graphics.PointF
import android.os.ParcelFileDescriptor
import com.kedevjimmy.pesatrack.domain.evidence.*
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.contentstream.PDFGraphicsStreamEngine
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImage
import java.io.IOException

/**
 * Read-only, isolated-service feasibility probe. It counts STROKED orthogonal
 * PDF vector line intersections, NOT table rows. It never exposes lines, PDF
 * contents, identifiers, money, graphics or passwords outside the worker.
 */
internal object PdfBoxGridProbe {
    fun inspect(context: Context, descriptor: ParcelFileDescriptor, password: String?): PdfGridProbeReport {
        PdfTextReviewPolicy.requireBytes(descriptor.statSize)
        PDFBoxResourceLoader.init(context.applicationContext)
        try {
            ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { input ->
                PDDocument.load(input,password.orEmpty()).use { doc ->
                    if (doc.numberOfPages !in 1..PdfTextReviewPolicy.MAX_PAGES)
                        throw PdfReadException(PdfFailure.TOO_MANY_PAGES)
                    return PdfGridProbeReport((0 until doc.numberOfPages).map { index ->
                        val page = doc.getPage(index)
                        VectorScanner(page).scan(index + 1)
                    })
                }
            }
        } catch (_: InvalidPasswordException) {
            throw PdfReadException(PdfFailure.PASSWORD)
        } catch (e: PdfReadException) {
            throw e
        } catch (_: IOException) {
            throw PdfReadException(PdfFailure.PARSE)
        }
    }

    private class VectorScanner(private val target: PDPage) : PDFGraphicsStreamEngine(target) {
        private val segments = mutableListOf<PdfGridSegment>()
        private val pending = mutableListOf<PdfGridSegment>()
        private var current: PointF? = null
        private var initial: PointF? = null
        private var operations = 0
        private fun guard() {
            operations++
            if (operations > 60000 || pending.size > 10000 || segments.size > 10000)
                throw PdfReadException(PdfFailure.TOO_MUCH_TEXT)
        }
        fun scan(number: Int): PdfGridPageEvidence {
            processPage(target)
            val crop = target.cropBox
            return PdfGridVectorEvidence.analyze(number,crop.width,crop.height,segments)
        }
        override fun moveTo(x: Float,y: Float) {
            guard()
            current=PointF(x,y)
            initial=PointF(x,y)
        }
        override fun lineTo(x: Float,y: Float) {
            guard()
            val from=current ?: throw PdfReadException(PdfFailure.PARSE)
            pending.add(PdfGridSegment(from.x,from.y,x,y))
            current=PointF(x,y)
        }
        override fun appendRectangle(p0:PointF,p1:PointF,p2:PointF,p3:PointF) {
            guard()
            val p=listOf(p0,p1,p2,p3,p0)
            for(i in 0..3) pending.add(PdfGridSegment(p[i].x,p[i].y,p[i+1].x,p[i+1].y))
            current=p0
            initial=p0
        }
        override fun getCurrentPoint():PointF = current ?: PointF(0f,0f)
        override fun closePath() {
            guard()
            val last=current
            val first=initial
            if(last!=null && first!=null)
                pending.add(PdfGridSegment(last.x,last.y,first.x,first.y))
            current=first
        }
        override fun curveTo(x1:Float,y1:Float,x2:Float,y2:Float,x3:Float,y3:Float) {
            guard()
            // Curves are deliberately not interpreted as table boundaries.
            current=PointF(x3,y3)
        }
        private fun reset() {
            pending.clear()
            current=null
            initial=null
        }
        private fun commit() {
            guard()
            if(segments.size + pending.size > 10000) throw PdfReadException(PdfFailure.TOO_MUCH_TEXT)
            segments.addAll(pending)
            reset()
        }
        override fun strokePath()=commit()
        override fun fillAndStrokePath(windingRule:Path.FillType)=commit()
        override fun fillPath(windingRule:Path.FillType)=reset()
        override fun endPath()=reset()
        override fun clip(windingRule:Path.FillType) { /* no drawing: preserve current path until endPath */ }
        override fun drawImage(pdImage:PDImage) { /* deliberately do not decode images/OCR */ }
        override fun shadingFill(shadingName:COSName) { /* not evidence of table lines */ }
    }
}
