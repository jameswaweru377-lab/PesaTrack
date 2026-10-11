package com.kedevjimmy.pesatrack.domain.evidence

import java.io.File
import java.io.InputStream
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** One cancellable source copy. Closing the source must release a stalled read. */
class DocumentCopySession(
    private val open: () -> InputStream,
    private val cancelOpen: () -> Unit = {}
) {
    private val cancelled = AtomicBoolean(false)
    private val closeRequested = AtomicBoolean(false)
    private val source = AtomicReference<InputStream?>()

    fun requestCancellation() { cancelled.set(true) }
    fun cancel() {
        requestCancellation()
        if (closeRequested.compareAndSet(false, true)) {
            runCatching { source.get()?.close() }
            runCatching(cancelOpen)
        }
    }
    private fun checkActive() {
        if (cancelled.get() || Thread.currentThread().isInterrupted) throw CancellationException()
    }
    fun copyTo(file: File) {
        var successful = false
        try {
            checkActive()
            open().use { input ->
                source.set(input)
                checkActive()
                file.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var count = 0L
                    while (true) {
                        checkActive()
                        val read = input.read(buffer)
                        checkActive()
                        if (read < 0) break
                        if(read==0)continue
                        count += read
                        PdfTextReviewPolicy.requireBytes(count)
                        output.write(buffer, 0, read)
                    }
                    PdfTextReviewPolicy.requireBytes(count)
                }
            }
            val signature = ByteArray(5)
            val size = file.inputStream().use { it.read(signature) }
            if(size!=5 || !signature.contentEquals("%PDF-".toByteArray(Charsets.US_ASCII))) throw PdfReadException(PdfFailure.NOT_PDF)
            checkActive()
            successful = true
        } finally {
            source.set(null)
            if (!successful || cancelled.get()) file.delete()
        }
    }
}
