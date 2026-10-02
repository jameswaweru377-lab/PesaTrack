package com.kedevjimmy.pesatrack.domain.interpretation

import com.kedevjimmy.pesatrack.domain.messagepack.MessagePackFieldValue
import com.kedevjimmy.pesatrack.domain.messagepack.MessagePackMatch
import java.math.RoundingMode

/**
 * Converts only Message Pack investment shapes that contain enough
 * evidence to update the provider-neutral investment event ledger.
 *
 * Amount-only fund top-ups, withdrawals and redemptions deliberately
 * remain provenance-only because they do not establish an executed
 * unit/position change safely.
 */
object MessagePackInvestmentSemanticBridge {
    fun enrich(
        assessment: FinancialSemanticAssessment,
        match: MessagePackMatch?
    ): FinancialSemanticAssessment {
        if (
            match == null ||
            assessment.investment != null
        ) {
            return assessment
        }

        if (
            match.eventKind
                .uppercase() !=
            "INVESTMENT_CREDIT"
        ) {
            return assessment
        }

        val currency =
            match.fieldValue("CURRENCY")
                ?.trim()
                ?.uppercase()
        if (
            currency != null &&
            currency != "KES"
        ) {
            return assessment
        }

        val product =
            match.requiredField(
                "PRODUCT"
            ) ?: return assessment
        val units =
            match.requiredField(
                "UNITS"
            ) ?: return assessment
        val amount =
            match.requiredField(
                "AMOUNT"
            ) ?: return assessment

        val instrumentLabel =
            product.value
                .trim()
                .replace(
                    Regex("""\s+"""),
                    " "
                )
                .takeIf(
                    String::isNotBlank
                ) ?: return assessment
        val quantityMicrounits =
            quantityToMicrounits(
                units.value
            ) ?: return assessment
        if (quantityMicrounits <= 0L) {
            return assessment
        }

        val valueMinor =
            amountToMinor(
                amount.value
            ) ?: return assessment
        if (valueMinor <= 0L) {
            return assessment
        }

        val fact =
            SemanticInvestmentFact(
                action =
                    SemanticInvestmentAction.BUY,
                stage =
                    SemanticInvestmentStage.EXECUTED,
                quantityMicrounits =
                    quantityMicrounits,
                instrumentLabel =
                    instrumentLabel,
                instrumentKey =
                    instrumentKey(
                        instrumentLabel
                    ),
                valueMinor = valueMinor,
                quantitySpan =
                    units.toSpan(),
                instrumentSpan =
                    product.toSpan()
            )

        return assessment.copy(
            primaryIntent =
                FinancialSemanticIntent
                    .INVESTMENT_EXECUTED,
            intents =
                assessment.intents +
                    FinancialSemanticIntent
                        .INVESTMENT_EXECUTED,
            investment = fact,
            confidence =
                InterpretationConfidence.HIGH,
            ruleIds =
                assessment.ruleIds +
                    (
                        "message_pack." +
                            match.templateId +
                            "@" +
                            match.revision +
                            ".investment_credit"
                    )
        )
    }

    private fun MessagePackMatch.requiredField(
        name: String
    ): MessagePackFieldValue? =
        fields.firstOrNull {
            it.field.equals(
                name,
                ignoreCase = true
            )
        }

    private fun MessagePackFieldValue.toSpan():
        MessageSpan =
        MessageSpan(
            startOffset =
                startIndex,
            endOffsetExclusive =
                endIndexExclusive
        )

    private fun quantityToMicrounits(
        raw: String
    ): Long? =
        runCatching {
            raw.replace(",", "")
                .trim()
                .toBigDecimal()
                .movePointRight(6)
                .setScale(
                    0,
                    RoundingMode.HALF_UP
                )
                .longValueExact()
        }.getOrNull()

    private fun amountToMinor(
        raw: String
    ): Long? =
        runCatching {
            raw.replace(",", "")
                .trim()
                .toBigDecimal()
                .movePointRight(2)
                .setScale(
                    0,
                    RoundingMode.HALF_UP
                )
                .longValueExact()
        }.getOrNull()

    private fun instrumentKey(
        label: String
    ): String =
        "instrument:" +
            label.trim()
                .uppercase()
                .replace(
                    Regex("""[^A-Z0-9]+"""),
                    "_"
                )
                .trim('_')
                .take(64)
}
