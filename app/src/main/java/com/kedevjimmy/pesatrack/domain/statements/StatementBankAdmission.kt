package com.kedevjimmy.pesatrack.domain.statements

import com.kedevjimmy.pesatrack.domain.model.MoneyAccount

/** Evidence originates from a full-document extractor, never an inferred PDF filename. */
data class StatementDocumentProof(
    val detectedProviderKey: String,
    val detectedAccountIdentifier: String,
    val currency: String,
    val format: String,
    val allSourceRegionsAccountedFor: Boolean,
    val pages: Int,
    val printedPageTotals: Map<Int, StatementPrintedTotal>,
    val printedGrandTotal: StatementPrintedTotal?,
    val openingBalanceMinor: Long?,
    val assurance: StatementDocumentAssurance = StatementDocumentAssurance.FULL,
    /** Independently read from the printed Total footer, never inferred from the final row. */
    val printedClosingBalanceMinor: Long? = null
)

enum class StatementDocumentAssurance {
    FULL, GRAND_TOTAL_AND_CHAIN_ONLY, VERIFIED_DERIVED_OPENING
}

data class StatementPrintedTotal(
    val moneyOutMinor: Long,
    val moneyInMinor: Long
)

sealed class StatementAdmissionResult {
    data class Accepted(
        val accountId: String,
        val accountIdentityHashMaterial: String,
        val profile: StatementSourceProfile,
        val documentFormat: String,
        val openingBalanceMinor: Long?,
        val legs: List<StatementLegEvidence>,
        val sourceRows: Int,
        val moneyInMinor: Long,
        val moneyOutMinor: Long,
        val assurance: StatementDocumentAssurance = StatementDocumentAssurance.FULL
    ) : StatementAdmissionResult()

    /** Only stable error codes are exposed; never render private document cells in errors. */
    enum class Reason {
        UNSUPPORTED_PROVIDER, OWNERSHIP_UNVERIFIED, WRONG_ACCOUNT,
        UNKNOWN_ACCOUNT_IDENTITY, UNKNOWN_CURRENCY, UNVERIFIED_FORMAT,
        INCOMPLETE_DOCUMENT, MISSING_PRINTED_TOTALS, TOTAL_MISMATCH,
        UNVERIFIED_BALANCE
    }
    data class Blocked(val reason: Reason) : StatementAdmissionResult()
}

/** Strict separate boundary between readable banking evidence and saved bank history. */
object StatementBankAdmission {
    const val DERIVED_OPENING_ARCHIVE_FORMAT = "PDF_EQUITY_VERIFIED_DERIVED_OPENING"
    private val identityChars = Regex("[^A-Z0-9]")
    private fun identity(value: String): String = value.uppercase().replace(identityChars, "")

    /** Both ends must prove the same nonprinted opening; then replay the oldest row. */
    private fun verifiedDerivedOpening(
        document: StatementDocumentProof, accepted: StatementTableResult.Accepted,
        pages: List<StatementTablePage>, profile: StatementSourceProfile
    ): Long? {
        val closing = document.printedClosingBalanceMinor ?: return null
        if (closing < 0L || accepted.legs.size < 2 ||
            accepted.legs.zipWithNext().any { (a, b) -> a.occurredAt > b.occurredAt }) return null
        val derived = try {
            val first = accepted.legs.first()
            val last = accepted.legs.last()
            if (closing != last.runningBalanceMinor) return null
            val fromFirst = Math.subtractExact(first.runningBalanceMinor, first.movementMinor)
            val printed = document.printedGrandTotal ?: return null
            val net = Math.subtractExact(printed.moneyInMinor, printed.moneyOutMinor)
            val fromFooter = Math.subtractExact(closing, net)
            if (fromFirst != fromFooter || fromFirst < 0L) return null
            fromFirst
        } catch (_: ArithmeticException) { return null }
        val replay = UniversalStatementTableInterpreter.interpret(
            pages, openingBalanceMinor = derived, profile = profile.table
        ) as? StatementTableResult.Accepted ?: return null
        return derived.takeIf {
            !replay.oldestLegUnanchored && replay.legs == accepted.legs &&
                replay.sourceRows == accepted.sourceRows
        }
    }

    fun verify(
        profile: StatementSourceProfile?,
        selectedAccount: MoneyAccount,
        confirmedOwnedAccountIds: Set<String>,
        confirmedProviderKey: String?,
        document: StatementDocumentProof,
        pages: List<StatementTablePage>
    ): StatementAdmissionResult {
        fun block(reason: StatementAdmissionResult.Reason) = StatementAdmissionResult.Blocked(reason)
        if (profile == null) return block(StatementAdmissionResult.Reason.UNSUPPORTED_PROVIDER)
        if (!selectedAccount.enabled || selectedAccount.id !in confirmedOwnedAccountIds ||
            selectedAccount.type != profile.accountType)
            return block(StatementAdmissionResult.Reason.OWNERSHIP_UNVERIFIED)
        if (confirmedProviderKey != profile.providerKey || document.detectedProviderKey != profile.providerKey)
            return block(StatementAdmissionResult.Reason.WRONG_ACCOUNT)
        val ownedIdentity = identity(selectedAccount.identifier.orEmpty())
        val printedIdentity = identity(document.detectedAccountIdentifier)
        // Never match an account by a short suffix, bank title or masked digits.
        if (ownedIdentity.length < 8 || printedIdentity.length < 8 ||
            ownedIdentity.any { !it.isDigit() } || printedIdentity.any { !it.isDigit() })
            return block(StatementAdmissionResult.Reason.UNKNOWN_ACCOUNT_IDENTITY)
        if (ownedIdentity != printedIdentity) return block(StatementAdmissionResult.Reason.WRONG_ACCOUNT)
        if (document.currency != profile.currency) return block(StatementAdmissionResult.Reason.UNKNOWN_CURRENCY)
        if (document.format !in profile.sampleValidatedFormats)
            return block(StatementAdmissionResult.Reason.UNVERIFIED_FORMAT)
        val derivedOpening = document.assurance == StatementDocumentAssurance.VERIFIED_DERIVED_OPENING
        val unanchored = derivedOpening ||
            document.assurance == StatementDocumentAssurance.GRAND_TOTAL_AND_CHAIN_ONLY
        if (unanchored && profile.providerKey != "KENYAN_BANK_EQUITY")
            return block(StatementAdmissionResult.Reason.UNVERIFIED_FORMAT)
        if (!document.allSourceRegionsAccountedFor || document.pages !in 1..160)
            return block(StatementAdmissionResult.Reason.INCOMPLETE_DOCUMENT)
        val financialPages = pages.map { it.pageNumber }.toSet()
        if (financialPages.isEmpty() || financialPages.any { it !in 1..document.pages })
            return block(StatementAdmissionResult.Reason.INCOMPLETE_DOCUMENT)
        if (unanchored && (financialPages != (1..document.pages).toSet() ||
                document.openingBalanceMinor != null || document.printedPageTotals.isNotEmpty()))
            return block(StatementAdmissionResult.Reason.INCOMPLETE_DOCUMENT)
        val accepted = UniversalStatementTableInterpreter.interpret(
            pages, openingBalanceMinor = document.openingBalanceMinor, profile = profile.table
        ) as? StatementTableResult.Accepted
            ?: return block(StatementAdmissionResult.Reason.UNVERIFIED_BALANCE)
        if (accepted.oldestLegUnanchored != unanchored || accepted.sourceRows != accepted.legs.size ||
            accepted.legs.isEmpty() || accepted.legs.any { it.pageNumber !in 1..document.pages })
            return block(StatementAdmissionResult.Reason.UNVERIFIED_BALANCE)
        if (!financialPages.containsAll(document.printedPageTotals.keys))
            return block(StatementAdmissionResult.Reason.INCOMPLETE_DOCUMENT)
        if ((document.printedGrandTotal == null &&
                (unanchored || document.printedPageTotals.keys != financialPages)) ||
            (derivedOpening && document.printedClosingBalanceMinor == null))
            return block(StatementAdmissionResult.Reason.MISSING_PRINTED_TOTALS)
        val actualIn = runCatching { accepted.legs.filter { it.movementMinor > 0 }
            .fold(0L) { total, leg -> Math.addExact(total, leg.movementMinor) } }.getOrNull()
            ?: return block(StatementAdmissionResult.Reason.TOTAL_MISMATCH)
        val actualOut = runCatching { accepted.legs.filter { it.movementMinor < 0 }
            .fold(0L) { total, leg -> Math.addExact(total, Math.negateExact(leg.movementMinor)) } }.getOrNull()
            ?: return block(StatementAdmissionResult.Reason.TOTAL_MISMATCH)
        val printed = document.printedGrandTotal
        if (printed != null && (actualIn != printed.moneyInMinor || actualOut != printed.moneyOutMinor))
            return block(StatementAdmissionResult.Reason.TOTAL_MISMATCH)
        for (page in document.printedPageTotals.keys) {
            val pageLegs = accepted.legs.filter { it.pageNumber == page }
            val pageIn = runCatching { pageLegs.filter { it.movementMinor > 0 }
                .fold(0L) { total, leg -> Math.addExact(total, leg.movementMinor) } }.getOrNull()
                ?: return block(StatementAdmissionResult.Reason.TOTAL_MISMATCH)
            val pageOut = runCatching { pageLegs.filter { it.movementMinor < 0 }
                .fold(0L) { total, leg -> Math.addExact(total, Math.negateExact(leg.movementMinor)) } }.getOrNull()
                ?: return block(StatementAdmissionResult.Reason.TOTAL_MISMATCH)
            val pageTotal = document.printedPageTotals[page]!!
            if (pageIn != pageTotal.moneyInMinor || pageOut != pageTotal.moneyOutMinor)
                return block(StatementAdmissionResult.Reason.TOTAL_MISMATCH)
        }
        val opening = if (derivedOpening) {
            verifiedDerivedOpening(document, accepted, pages, profile)
                ?: return block(StatementAdmissionResult.Reason.UNVERIFIED_BALANCE)
        } else document.openingBalanceMinor
        return StatementAdmissionResult.Accepted(
            selectedAccount.id, ownedIdentity, profile,
            if (derivedOpening) DERIVED_OPENING_ARCHIVE_FORMAT else document.format,
            opening, accepted.legs, accepted.sourceRows,
            actualIn, actualOut, document.assurance
        )
    }
}
