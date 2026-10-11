package com.kedevjimmy.pesatrack.documents

import android.content.*
import android.net.Uri
import android.os.*
import androidx.annotation.RequiresApi
import com.kedevjimmy.pesatrack.domain.evidence.PdfTextPreview
import com.kedevjimmy.pesatrack.domain.evidence.PdfTextReviewPolicy
import com.kedevjimmy.pesatrack.domain.evidence.DocumentCopySession
import kotlinx.coroutines.*
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object PdfTextReviewReader {
    private val copyExecutor = Executors.newSingleThreadExecutor { Thread(it, "pdf-source-copy").apply { isDaemon = true } }
    private val cancelExecutor = Executors.newSingleThreadExecutor { Thread(it, "pdf-source-cancel").apply { isDaemon = true } }
    /** Startup cleanup only, before any preview can be opened in this process. */
    fun clearAbandonedFiles(context: Context) {
        context.cacheDir.listFiles()?.filter { it.name.startsWith("pdf-review-") && it.name.endsWith(".pdf") }?.forEach { it.delete() }
    }
    suspend fun read(context: Context, uri: Uri): PdfTextPreview {
        require(Build.VERSION.SDK_INT >= 35) { "PDF text preview needs Android 15 or newer. CSV and manual entry remain available." }
        val app = context.applicationContext
        val file = File.createTempFile("pdf-review-", ".pdf", app.cacheDir)
        try {
            return withTimeout(PdfTextReviewPolicy.TIMEOUT_MS) {
                copySource(app, uri, file)
                withContext(Dispatchers.Main) {
                    if (Build.VERSION.SDK_INT >= 35) request(app, file)
                    else throw IllegalArgumentException("PDF text preview needs Android 15 or newer.")
                }
            }
        } catch (_: TimeoutCancellationException) {
            throw IllegalArgumentException("PDF review took too long. Try a smaller document. Nothing was imported.")
        } finally { withContext(NonCancellable + Dispatchers.IO) { file.delete() } }
    }

    private suspend fun copySource(app: Context, uri: Uri, file: File) = suspendCancellableCoroutine<Unit> { continuation ->
        val signal = CancellationSignal()
        val session = DocumentCopySession(
            open = {
                val descriptor = app.contentResolver.openFileDescriptor(uri, "r", signal)
                    ?: throw IllegalArgumentException("Could not open the PDF.")
                ParcelFileDescriptor.AutoCloseInputStream(descriptor)
            },
            cancelOpen = { signal.cancel() }
        )
        val future = AtomicReference<Future<*>?>()
        continuation.invokeOnCancellation {
            session.requestCancellation()
            future.get()?.cancel(true)
            // Never wait on a provider's cancellation IPC from the UI thread.
            cancelExecutor.execute { session.cancel() }
        }
        val task = copyExecutor.submit {
            try {
                session.copyTo(file)
                if (continuation.isActive) continuation.resume(Unit) else file.delete()
            } catch (_: Exception) {
                file.delete()
                if (continuation.isActive) continuation.resumeWithException(IllegalArgumentException(
                    "Could not read this PDF. Choose a readable PDF up to 2 MB. Nothing was imported."
                ))
            }
        }
        future.set(task)
        if (!continuation.isActive) {
            session.requestCancellation()
            task.cancel(true)
            cancelExecutor.execute { session.cancel() }
        }
    }

    @RequiresApi(35)
    private suspend fun request(app: Context, file: File): PdfTextPreview {
        var bound = false
        lateinit var connection: ServiceConnection
        try {
            return suspendCancellableCoroutine { continuation ->
                fun fail(message: String) {
                    if (continuation.isActive) continuation.resumeWithException(IllegalArgumentException(message))
                }
                val reply = Messenger(Handler(Looper.getMainLooper()) { message ->
                    if (continuation.isActive) {
                        val error = message.data.getString("error")
                        if (error != null) fail(error) else {
                            try {
                                val pages = message.data.getStringArrayList("pages") ?: emptyList()
                                continuation.resume(PdfTextReviewPolicy.preview(pages))
                            } catch (_: Exception) { fail("No readable PDF text was returned. Nothing was imported.") }
                        }
                    }
                    true
                })
                connection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                        if (!continuation.isActive) return
                        try {
                            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                                Messenger(binder).send(Message.obtain().apply {
                                    replyTo = reply
                                    data = Bundle().apply { putParcelable("file", descriptor) }
                                })
                                // The isolated process now owns a duplicated descriptor; retain no named document.
                                file.delete()
                            }
                        } catch (_: Exception) { fail("Could not start PDF text review. Nothing was imported.") }
                    }
                    override fun onServiceDisconnected(name: ComponentName) { fail("PDF review stopped. Try a smaller document. Nothing was imported.") }
                    override fun onBindingDied(name: ComponentName) { fail("PDF review stopped. Nothing was imported.") }
                    override fun onNullBinding(name: ComponentName) { fail("PDF review is unavailable. Nothing was imported.") }
                }
                bound = app.bindIsolatedService(Intent(app, PdfTextReviewService::class.java), Context.BIND_AUTO_CREATE,
                    UUID.randomUUID().toString(), app.mainExecutor, connection)
                if (!bound) fail("PDF text review is unavailable. Nothing was imported.")
            }
        } finally { if (bound) app.unbindService(connection) }
    }
}
