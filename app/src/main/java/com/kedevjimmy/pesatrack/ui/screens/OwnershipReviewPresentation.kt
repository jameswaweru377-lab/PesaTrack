package com.kedevjimmy.pesatrack.ui.screens

import com.kedevjimmy.pesatrack.domain.identity.OwnershipReviewEvidence

internal fun ownershipEvidenceSummary(evidence: OwnershipReviewEvidence): String =
    buildList {
        if (evidence.hasBalance) add("A recorded account balance")
        if (evidence.transferCount > 0) add(
            "${evidence.transferCount} recorded transfer" + if (evidence.transferCount == 1) "" else "s"
        )
    }.joinToString(" • ").ifEmpty { "Evidence is no longer available. Check again later." }
