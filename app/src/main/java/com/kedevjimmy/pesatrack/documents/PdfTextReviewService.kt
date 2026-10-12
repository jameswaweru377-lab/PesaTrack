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
                    val pages=extract(descriptor,password)
                    val recipient=message.replyTo ?: throw PdfReadException(PdfFailure.SERVICE)
                    pages.forEachIndexed { index, page ->
                        val chunks=if(page.isEmpty()) listOf("") else page.chunked(PdfTextChunkAssembler.CHUNK_CHARACTERS)
                        chunks.forEach { chunk ->
                            recipient.send(Message.obtain().apply {
                                data=Bundle().apply {
                                    putInt("pageIndex",index)
                                    putString("chunk",chunk)
                                }
                            })
                        }
                    }
                    response.putBoolean("done",true)
                    response.putInt("pageCount",pages.size)
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

    @RequiresApi(35)
    private fun extract(descriptor: ParcelFileDescriptor,password: String?): List<String> {
        PdfTextReviewPolicy.requireBytes(descriptor.statSize)
        return PdfRenderer(descriptor,LoadParams.Builder().setPassword(password).build()).use { renderer ->
        if(renderer.pageCount>PdfTextReviewPolicy.MAX_PAGES) throw PdfReadException(PdfFailure.TOO_MANY_PAGES)
        var characters = 0L
        val pages = (0 until renderer.pageCount).map { index ->
            renderer.openPage(index).use { page ->
                val text = StringBuilder()
                val fragments=mutableListOf<PdfTextFragment>()
                for (content in page.textContents) {
                    characters += content.text.length + if (text.isNotEmpty()) 1L else 0L
                    if(characters>PdfTextReviewPolicy.MAX_CHARACTERS) throw PdfReadException(PdfFailure.TOO_MUCH_TEXT)
                    if (text.isNotEmpty()) text.append('\n')
                    text.append(content.text)
                    // Only single-line bounded fragments are safe to reposition.
                    val bounds=content.bounds
                    if(bounds.size==1 && !content.text.contains('\n') && !content.text.contains('\r')) {
                        val box=bounds[0]
                        fragments.add(PdfTextFragment(content.text,box.left,box.top,box.right,box.bottom))
                    }
                }
                val original=text.toString()
                val isStatement=original.contains("M-PESA STATEMENT",true) ||
                    original.contains("RECEIPT NO",true) && original.contains("COMPLETION TIME",true)
                if(isStatement) PdfTextPositionReflow.reflow(fragments,original) else original
            }
        }
        PdfTextReviewPolicy.preview(pages).pages
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
