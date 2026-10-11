package com.kedevjimmy.pesatrack.documents

import android.app.Service
import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.os.*
import androidx.annotation.RequiresApi
import com.kedevjimmy.pesatrack.domain.evidence.PdfTextReviewPolicy

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
                    response.putStringArrayList("pages", ArrayList(extract(descriptor)))
                } else throw IllegalArgumentException("Unsupported Android version")
            } catch (_: SecurityException) {
                response.putString("error", "Protected PDFs cannot be read. Choose an unprotected copy or use manual entry.")
            } catch (e: IllegalArgumentException) {
                // Only our fixed policy messages may be shown; native errors can contain private content.
                response.putString("error", "This PDF cannot be previewed. Use an unprotected document up to 2 MB, 40 pages and 100,000 text characters. Scanned PDFs need OCR, which is not available.")
            } catch (_: Exception) {
                response.putString("error", "Could not read this PDF. It may be damaged or unsupported. Nothing was imported.")
            } finally {
                descriptor.close()
            }
            try { message.replyTo?.send(Message.obtain().apply { data = response }) } catch (_: RemoteException) { }
            true
        })
    }

    @RequiresApi(35)
    private fun extract(descriptor: ParcelFileDescriptor): List<String> = PdfRenderer(descriptor).use { renderer ->
        PdfTextReviewPolicy.requireBytes(descriptor.statSize)
        require(renderer.pageCount in 1..PdfTextReviewPolicy.MAX_PAGES)
        var characters = 0L
        val pages = (0 until renderer.pageCount).map { index ->
            renderer.openPage(index).use { page ->
                val text = StringBuilder()
                for (content in page.textContents) {
                    characters += content.text.length + if (text.isNotEmpty()) 1L else 0L
                    require(characters <= PdfTextReviewPolicy.MAX_CHARACTERS)
                    if (text.isNotEmpty()) text.append('\n')
                    text.append(content.text)
                }
                text.toString()
            }
        }
        PdfTextReviewPolicy.preview(pages).pages
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
