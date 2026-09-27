package com.kedevjimmy.pesatrack.domain.parser

import com.kedevjimmy.pesatrack.domain.interpretation.AvailableCreditObservationFact
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
 * Message structure is no longer parsed here. The provider-independent
 * FinancialMessageInterpreter extracts and resolves the debt semantics,
 * then this adapter maps those generic facts into the existing storage
 * contract while that storage path is being migrated.
 */
object FulizaParser {
    private const val FULIZA_PROVIDER_KEY =
        "SAFARICOM_FULIZA"
    private const val MPESA_PROVIDER_KEY =
        "SAFARICOM_MPESA"

    fun parse(
        message: String
    ): ParsedFulizaEvent? {
        val interpretation =
            FinancialMessageInterpreter.interpret(
                title = "MPESA",
                body = message
            )

        val drawdown = interpretation.events
            .filterIsInstance<DebtDrawdownEventFact>()
            .firstOrNull {
                it.liability.providerKey ==
                    FULIZA_PROVIDER_KEY
            } ?: return null

        return ParsedFulizaEvent(
            transactionCode =
                interpretation.transactionCode,
            financedAmountMinor =
                drawdown.amountMinor,
            accessFeeMinor =
                drawdown.feeMinor,
            outstandingAmountMinor =
                drawdown.outstandingAfterMinor,
            availableLimitMinor =
                availableCredit(
                    interpretation.events
                        .filterIsInstance<
                            AvailableCreditObservationFact
                        >()
                ),
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

        val repayment = interpretation.events
            .filterIsInstance<DebtRepaymentEventFact>()
            .firstOrNull {
                it.liability.providerKey ==
                    FULIZA_PROVIDER_KEY
            } ?: return null

        return ParsedFulizaRepayment(
            transactionCode =
                interpretation.transactionCode,
            repaidAmountMinor =
                repayment.amountMinor,
            completion =
                repayment.completion
                    .toLegacyCompletion(),
            outstandingAfterMinor =
                repayment.outstandingAfterMinor,
            availableLimitMinor =
                availableCredit(
                    interpretation.events
                        .filterIsInstance<
                            AvailableCreditObservationFact
                        >()
                ),
            mpesaBalanceMinor =
                interpretation.balances
                    .firstOrNull {
                        it.account.providerKey ==
                            MPESA_PROVIDER_KEY
                    }
                    ?.balanceMinor,
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

    private fun availableCredit(
        observations:
            List<AvailableCreditObservationFact>
    ): Long? =
        observations.firstOrNull {
            it.liability.providerKey ==
                FULIZA_PROVIDER_KEY
        }?.availableCreditMinor

    private fun DebtCompletion
        .toLegacyCompletion():
        FulizaRepaymentCompletion =
        when (this) {
            DebtCompletion.PARTIAL ->
                FulizaRepaymentCompletion.PARTIAL
            DebtCompletion.FULL ->
                FulizaRepaymentCompletion.FULL
            DebtCompletion.UNKNOWN ->
                FulizaRepaymentCompletion.UNKNOWN
        }
}
