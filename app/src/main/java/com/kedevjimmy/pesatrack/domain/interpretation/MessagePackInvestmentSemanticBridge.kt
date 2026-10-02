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
        if (match == null) {
            return assessment
        }

        val eventKind =
            match.eventKind.uppercase()
        if (
            !eventKind.startsWith(
                "INVESTMENT_"
            )
        ) {
            return assessment
        }

        if (assessment.investment != null) {
            return assessment.copy(
                ruleIds =
                    assessment.ruleIds +
                        packRuleId(
                            match,
                            "investment"
                        )
            )
        }

        if (
            eventKind !=
            "INVESTMENT_CREDIT"
        ) {
            return provenanceOnly(
                assessment = assessment,
                match = match
            )
        }

        val currency =
            match.fieldValue("CURRENCY")
                ?.trim()
                ?.uppercase()
        if (
            currency != null &&
            currency != "KES"
        ) {
            return provenanceOnly(
                assessment = assessment,
                match = match
            )
        }

        val product =
            match.requiredField(
                "PRODUCT"
            ) ?: return provenanceOnly(
                assessment = assessment,
                match = match
            )
        val units =
            match.requiredField(
                "UNITS"
            ) ?: return provenanceOnly(
                assessment = assessment,
                match = match
            )
        val amount =
            match.requiredField(
                "AMOUNT"
            ) ?: return provenanceOnly(
                assessment = assessment,
                match = match
            )

        val instrumentLabel =
            product.value
                .trim()
                .replace(
                    Regex("""\s+"""),
                    " "
                )
                .takeIf(
                    String::isNotBlank
                ) ?: return provenanceOnly(
                    assessment = assessment,
                    match = match
                )
        val quantityMicrounits =
            quantityToMicrounits(
                units.value
            ) ?: return provenanceOnly(
                assessment = assessment,
                match = match
            )
        if (quantityMicrounits <= 0L) {
            return provenanceOnly(
                assessment = assessment,
                match = match
            )
        }

        val valueMinor =
            amountToMinor(
                amount.value
            ) ?: return provenanceOnly(
                assessment = assessment,
                match = match
            )
        if (valueMinor <= 0L) {
            return provenanceOnly(
                assessment = assessment,
                match = match
            )
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
                    packRuleId(
                        match,
                        "investment_credit"
                    )
        )
    }

    private fun provenanceOnly(
        assessment:
            FinancialSemanticAssessment,
        match: MessagePackMatch
    ): FinancialSemanticAssessment {
        val ordinaryMovementIntents =
            setOf(
                FinancialSemanticIntent.PAYMENT,
                FinancialSemanticIntent.RECEIPT,
                FinancialSemanticIntent.TRANSFER
            )

        return assessment.copy(
            primaryIntent =
                FinancialSemanticIntent
                    .CONFIRMATION_ONLY,
            intents =
                (
                    assessment.intents -
                        ordinaryMovementIntents
                ) +
                    FinancialSemanticIntent
                        .CONFIRMATION_ONLY,
            role =
                FinancialSemanticRole
                    .INFORMATION,
            investment = null,
            confidence =
                InterpretationConfidence.HIGH,
            ruleIds =
                assessment.ruleIds +
                    packRuleId(
                        match,
                        "investment_provenance"
                    )
        )
    }

    private fun packRuleId(
        match: MessagePackMatch,
        suffix: String
    ): String =
        "message_pack." +
            match.templateId +
            "@" +
            match.revision +
            "." +
            suffix

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
