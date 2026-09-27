package com.kedevjimmy.pesatrack.domain.parser

import com.kedevjimmy.pesatrack.domain.interpretation.DebtCompletion
import com.kedevjimmy.pesatrack.domain.interpretation.DebtDrawdownEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.DebtRepaymentEventFact
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialMessageInterpreter

enum class FulizaRepaymentCompletion {
    PARTIAL,
    FULL,
    UNKNOWN
}

data class ParsedFulizaRepayment(
    val transactionCode: String?,
    val repaidAmountMinor: Long,
    val completion: FulizaRepaymentCompletion,
    val outstandingAfterMinor: Long?,
    val availableLimitMinor: Long?,
    val mpesaBalanceMinor: Long?,
    val rawMessage: String
)

data class ParsedFulizaEvent(
    val transactionCode: String?,
    val financedAmountMinor: Long,
    val accessFeeMinor: Long,
    val outstandingAmountMinor: Long?,
    val availableLimitMinor: Long?,
    val rawMessage: String
)

/**
 * Compatibility adapter for the legacy Fuliza persistence path.
 *
 * Message meaning is no longer parsed here. The provider-independent
 * financial interpreter produces generic debt events; this adapter only
 * maps known Fuliza events into the older DTOs until the remaining
 * persistence/pairing code is migrated.
 */
object FulizaParser {
    private const val FULIZA_PROVIDER_KEY =
        "SAFARICOM_FULIZA"

    fun parse(
        message: String
    ): ParsedFulizaEvent? {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "MPESA",
                body = message
            )

        val event = interpretation.events
            .filterIsInstance<DebtDrawdownEventFact>()
            .firstOrNull {
                it.liability.providerKey ==
                    FULIZA_PROVIDER_KEY
            } ?: return null

        return ParsedFulizaEvent(
            transactionCode =
                interpretation.transactionCode,
            financedAmountMinor =
                event.amountMinor,
            accessFeeMinor =
                event.feeMinor,
            outstandingAmountMinor =
                event.outstandingAfterMinor,
            availableLimitMinor =
                event.availableCreditAfterMinor,
            rawMessage = message
        )
    }

    fun parseRepayment(
        message: String
    ): ParsedFulizaRepayment? {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "MPESA",
                body = message
            )

        val event = interpretation.events
            .filterIsInstance<DebtRepaymentEventFact>()
            .firstOrNull {
                it.liability.providerKey ==
                    FULIZA_PROVIDER_KEY
            } ?: return null

        return ParsedFulizaRepayment(
            transactionCode =
                interpretation.transactionCode,
            repaidAmountMinor =
                event.amountMinor,
            completion = when (event.completion) {
                DebtCompletion.PARTIAL ->
                    FulizaRepaymentCompletion.PARTIAL
                DebtCompletion.FULL ->
                    FulizaRepaymentCompletion.FULL
                DebtCompletion.UNKNOWN ->
                    FulizaRepaymentCompletion.UNKNOWN
            },
            outstandingAfterMinor =
                event.outstandingAfterMinor,
            availableLimitMinor =
                event.availableCreditAfterMinor,
            mpesaBalanceMinor =
                event.walletBalanceAfterMinor,
            rawMessage = message
        )
    }

    fun isFulizaDrawdown(
        message: String
    ): Boolean =
        parse(message) != null

    fun isFulizaRepayment(
        message: String
    ): Boolean =
        parseRepayment(message) != null

    fun isFulizaDebtEvent(
        message: String
    ): Boolean =
        isFulizaDrawdown(message) ||
            isFulizaRepayment(message)
}
