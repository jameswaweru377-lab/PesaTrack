package com.kedevjimmy.pesatrack.documents

import android.content.*
import android.net.Uri
import android.os.*
import androidx.annotation.RequiresApi
import com.kedevjimmy.pesatrack.domain.evidence.PdfTextPreview
import com.kedevjimmy.pesatrack.domain.evidence.PdfTextReviewPolicy
import com.kedevjimmy.pesatrack.domain.evidence.PdfTextChunkAssembler
import com.kedevjimmy.pesatrack.domain.evidence.PdfPositionChunkAssembler
import com.kedevjimmy.pesatrack.domain.evidence.PdfGeometryEvidencePolicy
import com.kedevjimmy.pesatrack.domain.evidence.PdfBoxProbeWire
import com.kedevjimmy.pesatrack.domain.evidence.PdfBoxVerificationReport
import com.kedevjimmy.pesatrack.domain.evidence.DocumentCopySession
import com.kedevjimmy.pesatrack.domain.evidence.PdfReadException
import com.kedevjimmy.pesatrack.domain.evidence.PdfFailure
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
    suspend fun read(
        context: Context, uri: Uri, password: String? = null,
        includePositions: Boolean = false, pdfBoxProbe: Boolean = false,
        pdfBoxStatement: Boolean = false
    ): PdfTextPreview {
        if(Build.VERSION.SDK_INT<35)throw PdfReadException(PdfFailure.VERSION)
        val app = context.applicationContext
        val file = File.createTempFile("pdf-review-", ".pdf", app.cacheDir)
        try {
            return withTimeout(PdfTextReviewPolicy.TIMEOUT_MS) {
                copySource(app, uri, file)
                withContext(Dispatchers.Main) {
                    if (Build.VERSION.SDK_INT >= 35)
                        request(app, file, password, includePositions, pdfBoxProbe, pdfBoxStatement)
                    else throw PdfReadException(PdfFailure.VERSION)
                }
            }
        } catch (_: TimeoutCancellationException) {
            throw PdfReadException(PdfFailure.TIMEOUT)
        } finally { withContext(NonCancellable + Dispatchers.IO) { file.delete() } }
    }

    private suspend fun copySource(app: Context, uri: Uri, file: File) = suspendCancellableCoroutine<Unit> { continuation ->
        val signal = CancellationSignal()
        val session = DocumentCopySession(
            open = {
                val descriptor = try {app.contentResolver.openFileDescriptor(uri, "r", signal)
                    ?: throw PdfReadException(PdfFailure.ACCESS)} catch(_: Exception) {throw PdfReadException(PdfFailure.ACCESS)}
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
            } catch (e: Exception) {
                file.delete()
                if (continuation.isActive) continuation.resumeWithException(e as? PdfReadException ?: PdfReadException(PdfFailure.COPY))
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
    private suspend fun request(
        app: Context, file: File, password: String?,
        includePositions: Boolean, pdfBoxProbe: Boolean, pdfBoxStatement: Boolean
    ): PdfTextPreview {
        require(!(pdfBoxProbe && pdfBoxStatement) &&
            !(includePositions && pdfBoxStatement)) { "Incompatible PDF extraction modes." }
        var bound = false
        lateinit var connection: ServiceConnection
        try {
            return suspendCancellableCoroutine { continuation ->
                fun fail(failure: PdfFailure) {
                    if (continuation.isActive) continuation.resumeWithException(PdfReadException(failure))
                }
                val assembly=PdfTextChunkAssembler()
                val positioned=PdfPositionChunkAssembler()
                val reply = Messenger(Handler(Looper.getMainLooper()) { message ->
                    if (continuation.isActive) {
                        val failure = message.data.getString("failure")
                        if (failure != null) fail(PdfFailure.entries.firstOrNull {it.name==failure} ?: PdfFailure.PARSE)
                        else try {
                            val data=message.data
                            if(data.containsKey("chunk")) {
                                assembly.add(data.getInt("pageIndex",-1),data.getString("chunk") ?: "")
                            } else if(includePositions && data.getBoolean("geometryStart",false)) {
                                positioned.startPage(data.getInt("pageIndex",-1))
                            } else if(includePositions && data.containsKey("geometryTexts")) {
                                positioned.add(
                                    data.getInt("pageIndex",-1),
                                    data.getStringArrayList("geometryTexts")?.toList() ?: error("Missing geometry."),
                                    data.getFloatArray("geometryLeft") ?: error("Missing geometry."),
                                    data.getFloatArray("geometryTop") ?: error("Missing geometry."),
                                    data.getFloatArray("geometryRight") ?: error("Missing geometry."),
                                    data.getFloatArray("geometryBottom") ?: error("Missing geometry.")
                                )
                            } else if(pdfBoxProbe && data.getBoolean("pdfBoxProbeDone",false)) {
                                val report=PdfBoxProbeWire.decode(
                                    data.getInt("pdfBoxPageCount",-1),
                                    data.getIntArray("pdfBoxCountEvidence") ?:
                                        error("Missing PDFBox proof counts.")
                                )
                                // Diagnostic-only. This cannot satisfy the
                                // verified-candidate import pathway.
                                continuation.resume(PdfTextPreview(emptyList(),emptyList(),pdfBoxProbe=report))
                            } else if(data.getBoolean("done",false)) {
                                val n=data.getInt("pageCount",-1)
                                val base=assembly.finish(n)
                                val enriched=if(includePositions) {
                                    val (geometry,complete)=positioned.finish(
                                        n,
                                        data.getIntArray("geometryCounts") ?: error("Missing geometry counts."),
                                        data.getBooleanArray("geometryComplete") ?: error("Missing geometry flags.")
                                    )
                                    val diagnostic=PdfGeometryEvidencePolicy.unpack(
                                        n,
                                        data.getIntArray("geometryReasons") ?: error("Missing geometry reasons.")
                                    )
                                    base.copy(
                                        positionedPages=geometry,
                                        positionedComplete=complete,
                                        geometryDiagnostics=diagnostic
                                    )
                                } else base
                                if(pdfBoxStatement && !data.getBoolean("pdfBoxStatementReady",false))
                                    throw PdfReadException(PdfFailure.PARSE)
                                continuation.resume(
                                    if(pdfBoxStatement)enriched.copy(pdfBoxStatementReady=true,
                                        pdfBoxVerification=PdfBoxVerificationReport.decode(
                                            data.getIntArray("pdfBoxVerification") ?: error("Missing statement proof.")))
                                    else enriched
                                )
                            } else fail(PdfFailure.SERVICE)
                        } catch (e: Exception) {
                            fail((e as? PdfReadException)?.failure ?: PdfFailure.PARSE)
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
                                    data = Bundle().apply {
                                        putParcelable("file", descriptor)
                                        putString("password",password)
                                        putBoolean("includePositions",includePositions)
                                        putBoolean("pdfBoxProbe",pdfBoxProbe)
                                        putBoolean("pdfBoxStatement",pdfBoxStatement)
                                    }
                                })
                                // The isolated process now owns a duplicated descriptor; retain no named document.
                                file.delete()
                            }
                        } catch (_: Exception) { fail(PdfFailure.SERVICE) }
                    }
                    override fun onServiceDisconnected(name: ComponentName) { fail(PdfFailure.SERVICE) }
                    override fun onBindingDied(name: ComponentName) { fail(PdfFailure.SERVICE) }
                    override fun onNullBinding(name: ComponentName) { fail(PdfFailure.SERVICE) }
                }
                bound = app.bindIsolatedService(Intent(app, PdfTextReviewService::class.java), Context.BIND_AUTO_CREATE,
                    UUID.randomUUID().toString().replace("-", ""), app.mainExecutor, connection)
                if (!bound) fail(PdfFailure.SERVICE)
            }
        } finally { if (bound) app.unbindService(connection) }
    }
}
