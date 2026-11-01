package com.kedevjimmy.pesatrack.documents

import android.content.Context
import android.net.Uri
import android.os.Build
import com.kedevjimmy.pesatrack.domain.evidence.PdfFailure
import com.kedevjimmy.pesatrack.domain.evidence.PdfReadException
import com.kedevjimmy.pesatrack.domain.statements.NativeBankProbeResult
import com.kedevjimmy.pesatrack.domain.statements.NativeProbeBlockReason
import com.kedevjimmy.pesatrack.domain.statements.NativeProbePreparation
import com.kedevjimmy.pesatrack.domain.statements.StatementNativeProbeTransport
import com.kedevjimmy.pesatrack.domain.statements.StatementProbeReason
import com.kedevjimmy.pesatrack.domain.statements.StatementProbeResult
import com.kedevjimmy.pesatrack.domain.statements.UniversalStatementReadOnlyProbe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Selectable-text PDF fallback: no issuer claim, database call or Activity posting. */
object OfflineNativeBankProbeReader {
    suspend fun inspect(context: Context, uri: Uri, password: String): NativeBankProbeResult {
        if (password.length > 256)
            return NativeBankProbeResult.Blocked(NativeProbeBlockReason.PDF_READ_FAILURE)
        return try {
            val preview = PdfTextReviewReader.read(context, uri, password, includePositions = true)
            withContext(Dispatchers.Default) {
                when (val transport = StatementNativeProbeTransport.prepare(Build.VERSION.SDK_INT, preview)) {
                    is NativeProbePreparation.Blocked -> NativeBankProbeResult.Blocked(
                        transport.reason, transport.pageNumber, transport.safeDetail
                    )
                    is NativeProbePreparation.Ready -> when (val probe =
                        UniversalStatementReadOnlyProbe.inspect(transport.pages)) {
                        is StatementProbeResult.Provisional -> NativeBankProbeResult.Provisional(probe)
                        is StatementProbeResult.Blocked -> NativeBankProbeResult.Blocked(
                            reason = when (probe.reason) {
                                StatementProbeReason.INVALID_PAGE_SEQUENCE -> NativeProbeBlockReason.INCOMPLETE_GEOMETRY
                                StatementProbeReason.UNRECOGNIZED_TABLE -> NativeProbeBlockReason.UNRECOGNIZED_LAYOUT
                                StatementProbeReason.UNVERIFIED_MOVEMENTS -> NativeProbeBlockReason.UNVERIFIED_MOVEMENTS
                            },
                            pageNumber = probe.pageNumber
                        )
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: PdfReadException) {
            when (e.failure) {
                PdfFailure.PASSWORD -> throw StatementPdfOpeningPasswordRequiredException()
                PdfFailure.VERSION -> NativeBankProbeResult.Blocked(NativeProbeBlockReason.UNSUPPORTED_DEVICE)
                PdfFailure.NO_TEXT -> NativeBankProbeResult.Blocked(NativeProbeBlockReason.EMPTY_OR_SCANNED_DOCUMENT)
                else -> NativeBankProbeResult.Blocked(NativeProbeBlockReason.PDF_READ_FAILURE)
            }
        } catch (_: Exception) {
            // A source-provider/native failure is never permission for partial recognition.
            NativeBankProbeResult.Blocked(NativeProbeBlockReason.PDF_READ_FAILURE)
        }
    }
}
