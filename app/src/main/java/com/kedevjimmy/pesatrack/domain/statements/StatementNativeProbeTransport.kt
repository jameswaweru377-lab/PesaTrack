package com.kedevjimmy.pesatrack.domain.statements

import com.kedevjimmy.pesatrack.domain.evidence.PdfTextPreview
import com.kedevjimmy.pesatrack.domain.evidence.PdfTextReviewPolicy

enum class NativeProbeBlockReason {
    UNSUPPORTED_DEVICE,
    INCOMPLETE_GEOMETRY,
    EMPTY_OR_SCANNED_DOCUMENT,
    UNRECOGNIZED_LAYOUT,
    UNVERIFIED_MOVEMENTS,
    PDF_READ_FAILURE
}

/** The result of transporting *all* original PDF pages, not a financial admission. */
sealed class NativeProbePreparation {
    data class Ready(val pages: List<StatementProbePage>) : NativeProbePreparation()
    data class Blocked(val reason: NativeProbeBlockReason) : NativeProbePreparation()
}

/** Candidate rows have no verified issuer, account or complete-document evidence. */
sealed class NativeBankProbeResult {
    data class Provisional(val result: StatementProbeResult.Provisional) : NativeBankProbeResult()
    data class Blocked(val reason: NativeProbeBlockReason) : NativeBankProbeResult()
}

/** Fail closed on missing, unreliable or unsupported native PDF geometry. */
object StatementNativeProbeTransport {
    fun prepare(api: Int, preview: PdfTextPreview): NativeProbePreparation {
        fun block(reason: NativeProbeBlockReason) = NativeProbePreparation.Blocked(reason)
        if (!PdfTextReviewPolicy.supported(api))
            return block(NativeProbeBlockReason.UNSUPPORTED_DEVICE)
        val count = preview.pages.size
        if (count !in 1..PdfTextReviewPolicy.MAX_PAGES ||
            preview.positionedPages.size != count || preview.positionedComplete.size != count ||
            preview.positionedComplete.any { !it })
            return block(NativeProbeBlockReason.INCOMPLETE_GEOMETRY)
        if (preview.pages.any { it.isBlank() } || preview.positionedPages.any { it.isEmpty() })
            return block(NativeProbeBlockReason.EMPTY_OR_SCANNED_DOCUMENT)
        return NativeProbePreparation.Ready(preview.positionedPages.mapIndexed { index, words ->
            StatementProbePage(index + 1, words.toList())
        })
    }
}
