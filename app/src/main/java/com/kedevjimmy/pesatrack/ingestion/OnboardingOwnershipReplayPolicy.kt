package com.kedevjimmy.pesatrack.ingestion

/** The initial scan cannot attach unowned bank credits or record deferred debits. */
internal fun shouldReplaySmsAfterOwnership(
    initialImport: HistoricalImportResult?,
    selectedAccountCount: Int,
    smsAllowed: Boolean,
    startEpochMs: Long?
): Boolean = selectedAccountCount > 0 && smsAllowed && startEpochMs != null &&
    // A recreated, failed or cancelled first scan did not prove there are no
    // unowned records. Replay then, as well as when explicit pending work exists.
    (initialImport == null || initialImport.cancelled || initialImport.pendingReview > 0)
