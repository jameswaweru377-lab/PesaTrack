package com.kedevjimmy.pesatrack.ingestion

/** The initial scan cannot attach unowned bank credits or record deferred debits. */
internal fun shouldReplaySmsAfterOwnership(
    initialImport: HistoricalImportResult?,
    selectedAccountCount: Int,
    smsAllowed: Boolean,
    startEpochMs: Long?
): Boolean = selectedAccountCount > 0 && (
    // Once an import has reported deferred bank movements or cancellation, a
    // later permission revocation must not silently discard that knowledge.
    (initialImport != null && (initialImport.cancelled || initialImport.pendingReview > 0)) ||
        // A recreated scan has an unknown outcome; retry only when SMS access
        // is available. An always-denied/manual-only setup can still proceed.
        (initialImport == null && smsAllowed && startEpochMs != null)
    )

/** Do not present an interrupted provider read as a completed bank history. */
internal fun ownershipReplayCompleted(result: HistoricalImportResult?): Boolean =
    result != null && !result.cancelled

/** An interrupted ownership replay remains outstanding even if SMS access changes. */
internal fun ownershipReplayRequired(
    previouslyIncomplete: Boolean,
    initialImport: HistoricalImportResult?,
    selectedAccountCount: Int,
    smsAllowed: Boolean,
    startEpochMs: Long?,
    priorScanUnverified: Boolean = false
): Boolean = previouslyIncomplete || (priorScanUnverified && selectedAccountCount > 0) || shouldReplaySmsAfterOwnership(
    initialImport, selectedAccountCount, smsAllowed, startEpochMs
)
