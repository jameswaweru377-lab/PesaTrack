package com.kedevjimmy.pesatrack.domain.statements

import com.kedevjimmy.pesatrack.domain.model.MoneyAccount

/** The provisional recognizer cannot authorize saving without independent source proof. */
object StatementFallbackAdmissionPolicy {
    fun verify(
        probe: StatementProbeResult,
        verifiedProof: StatementDocumentProof?,
        profile: StatementSourceProfile?,
        account: MoneyAccount,
        confirmedIds: Set<String>,
        providerKey: String?
    ): StatementAdmissionResult {
        fun blocked(reason: StatementAdmissionResult.Reason) = StatementAdmissionResult.Blocked(reason)
        if (probe !is StatementProbeResult.Provisional || verifiedProof == null)
            return blocked(StatementAdmissionResult.Reason.INCOMPLETE_DOCUMENT)
        if (verifiedProof.assurance != StatementDocumentAssurance.FULL)
            return blocked(StatementAdmissionResult.Reason.INCOMPLETE_DOCUMENT)
        if (profile == null)
            return blocked(StatementAdmissionResult.Reason.UNSUPPORTED_PROVIDER)
        val admitted = StatementBankAdmission.verify(
            profile, account, confirmedIds, providerKey, verifiedProof, probe.tables
        )
        if (admitted is StatementAdmissionResult.Accepted &&
            (admitted.legs != probe.legs || admitted.sourceRows != probe.sourceRows ||
                probe.oldestLegUnanchored != (admitted.assurance ==
                    StatementDocumentAssurance.GRAND_TOTAL_AND_CHAIN_ONLY)))
            return blocked(StatementAdmissionResult.Reason.UNVERIFIED_BALANCE)
        return admitted
    }
}
