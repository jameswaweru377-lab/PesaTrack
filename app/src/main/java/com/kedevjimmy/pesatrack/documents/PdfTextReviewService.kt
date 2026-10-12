package com.kedevjimmy.pesatrack.documents

import android.app.Service
import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.graphics.pdf.LoadParams
import android.os.*
import androidx.annotation.RequiresApi
import com.kedevjimmy.pesatrack.domain.evidence.PdfTextReviewPolicy
import com.kedevjimmy.pesatrack.domain.evidence.PdfReadException
import com.kedevjimmy.pesatrack.domain.evidence.PdfFailure
import com.kedevjimmy.pesatrack.domain.evidence.PdfTextChunkAssembler
import com.kedevjimmy.pesatrack.domain.evidence.PdfTextFragment
import com.kedevjimmy.pesatrack.domain.evidence.PdfTextPositionReflow

/** Private isolated process: receives only a read-only descriptor, never ledger access. */
class PdfTextReviewService : Service() {
    private lateinit var worker: HandlerThread
    private lateinit var messenger: Messenger
    private var started = false

    override fun onCreate() {
        super.onCreate()
        worker = HandlerThread("pdf-text-review").apply { start() }
        messenger = Messenger(Handler(worker.looper) { message ->
            @Suppress("DEPRECATION")
            val descriptor = message.data.getParcelable<ParcelFileDescriptor>("file")
            if (started || descriptor == null) {
                descriptor?.close()
                return@Handler true
            }
            started = true
            val response = Bundle()
            try {
                if (Build.VERSION.SDK_INT >= 35) {
                    val password=message.data.getString("password")
                    message.data.remove("password")
                    val positions=message.data.getBoolean("includePositions",false)
                    val pages=extract(descriptor,password,positions)
                    val recipient=message.replyTo ?: throw PdfReadException(PdfFailure.SERVICE)
                    pages.forEachIndexed { index, page ->
                        val chunks=if(page.text.isEmpty()) listOf("") else page.text.chunked(PdfTextChunkAssembler.CHUNK_CHARACTERS)
                        chunks.forEach { chunk ->
                            recipient.send(Message.obtain().apply {
                                data=Bundle().apply {
                                    putInt("pageIndex",index)
                                    putString("chunk",chunk)
                                }
                            })
                        }
                        if(positions) {
                            recipient.send(Message.obtain().apply {
                                data=Bundle().apply {
                                    putInt("pageIndex",index)
                                    putBoolean("geometryStart",true)
                                }
                            })
                            page.positioned.chunked(
                                com.kedevjimmy.pesatrack.domain.evidence.PdfPositionChunkAssembler.MAX_CHUNK
                            ).forEach { batch ->
                                recipient.send(Message.obtain().apply {
                                    data=Bundle().apply {
                                        putInt("pageIndex",index)
                                        putStringArrayList("geometryTexts",ArrayList(batch.map {it.text}))
                                        putFloatArray("geometryLeft",batch.map {it.left}.toFloatArray())
                                        putFloatArray("geometryTop",batch.map {it.top}.toFloatArray())
                                        putFloatArray("geometryRight",batch.map {it.right}.toFloatArray())
                                        putFloatArray("geometryBottom",batch.map {it.bottom}.toFloatArray())
                                    }
                                })
                            }
                        }
                    }
                    response.putBoolean("done",true)
                    response.putInt("pageCount",pages.size)
                    if(positions) {
                        response.putIntArray("geometryCounts",pages.map {it.positioned.size}.toIntArray())
                        response.putBooleanArray("geometryComplete",pages.map {it.complete}.toBooleanArray())
                    }
                } else throw PdfReadException(PdfFailure.VERSION)
            } catch (e: PdfReadException) {
                response.putString("failure",e.failure.name)
            } catch (_: SecurityException) {
                response.putString("failure",PdfFailure.PASSWORD.name)
            } catch (_: Exception) {
                response.putString("failure",PdfFailure.PARSE.name)
            } finally {
                message.data.remove("password")
                runCatching {descriptor.close()}
            }
            try { message.replyTo?.send(Message.obtain().apply { data = response }) } catch (_: RemoteException) { }
            true
        })
    }

    private data class ExtractedPage(
        val text: String,
        val positioned: List<PdfTextFragment>,
        val complete: Boolean
    )
    @RequiresApi(35)
    private fun extract(descriptor: ParcelFileDescriptor,password: String?,withPositions:Boolean): List<ExtractedPage> {
        PdfTextReviewPolicy.requireBytes(descriptor.statSize)
        return PdfRenderer(descriptor,LoadParams.Builder().setPassword(password).build()).use { renderer ->
        if(renderer.pageCount>PdfTextReviewPolicy.MAX_PAGES) throw PdfReadException(PdfFailure.TOO_MANY_PAGES)
        var characters = 0L
        var positionedTotal = 0
        val pages = (0 until renderer.pageCount).map { index ->
            renderer.openPage(index).use { page ->
                val text = StringBuilder()
                val fragments=mutableListOf<PdfTextFragment>()
                val positioned=mutableListOf<PdfTextFragment>()
                var complete = true
                for (content in page.textContents) {
                    characters += content.text.length + if (text.isNotEmpty()) 1L else 0L
                    if(characters>PdfTextReviewPolicy.MAX_CHARACTERS) throw PdfReadException(PdfFailure.TOO_MUCH_TEXT)
                    if (text.isNotEmpty()) text.append('\n')
                    text.append(content.text)
                    val bounds=content.bounds
                    // Preserve the old reading-order reflow only for the
                    // originally supported single-line native fragments.
                    if(bounds.size==1 && !content.text.contains('\n') && !content.text.contains('\r')) {
                        val box=bounds[0]
                        fragments.add(PdfTextFragment(content.text,box.left,box.top,box.right,box.bottom))
                    }
                    if(withPositions) {
                        // Wrapped Details may be one native text item containing
                        // several visual lines. Retain its complete bounded box
                        // for explicit column/receipt reconstruction instead of
                        // dropping it and making the amount parser guess.
                        val limit=com.kedevjimmy.pesatrack.domain.evidence.PdfPositionChunkAssembler.MAX_FRAGMENT_TEXT
                        val countLimit=com.kedevjimmy.pesatrack.domain.evidence.PdfPositionChunkAssembler.MAX_TOTAL_FRAGMENTS
                        val valid=bounds.isNotEmpty() && bounds.size<=24 &&
                            content.text.isNotBlank() && content.text.length<=limit &&
                            content.text.none {it.isISOControl() && it !in setOf('\n','\r','\t')} &&
                            bounds.all {box ->
                                box.left.isFinite() && box.right.isFinite() &&
                                box.top.isFinite() && box.bottom.isFinite() &&
                                box.left < box.right && box.top < box.bottom
                            } && positionedTotal<countLimit
                        if(valid) {
                            val fragment=PdfTextFragment(
                                content.text,
                                bounds.minOf {it.left},bounds.minOf {it.top},
                                bounds.maxOf {it.right},bounds.maxOf {it.bottom}
                            )
                            positioned.add(fragment)
                            positionedTotal++
                        } else complete=false
                    }
                }
                val original=text.toString()
                val isStatement=original.contains("M-PESA STATEMENT",true) ||
                    original.contains("RECEIPT NO",true) && original.contains("COMPLETION TIME",true)
                val normalized=if(isStatement) PdfTextPositionReflow.reflow(fragments,original) else original
                ExtractedPage(normalized,if(withPositions)positioned else emptyList(),withPositions && complete)
            }
        }
        PdfTextReviewPolicy.preview(pages.map {it.text})
        pages
        }
    }

    override fun onBind(intent: Intent): IBinder = messenger.binder
    override fun onDestroy() {
        worker.quitSafely()
        super.onDestroy()
        // Unbinding (including timeout/cancellation) must terminate native parsing too.
        // This service is the sole component in the dedicated isolated process.
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}
