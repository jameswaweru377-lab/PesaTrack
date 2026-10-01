package com.kedevjimmy.pesatrack.domain.interpretation

import com.kedevjimmy.pesatrack.domain.messagepack.MessagePackMatch
import java.math.BigDecimal
import java.math.RoundingMode

object MessagePackCandidateBridge {
    fun enrich(
        candidates: FinancialMessageCandidates,
        match: MessagePackMatch?,
        message: NormalizedFinancialMessage
    ): FinancialMessageCandidates {
        if (
            match == null ||
            candidates.actions.isNotEmpty()
        ) {
            return candidates
        }

        val amountField =
            match.fields.firstOrNull {
                it.field.equals(
                    "AMOUNT",
                    ignoreCase = true
                )
            } ?: return candidates

        val amountMinor =
            amountToMinor(
                amountField.value
            ) ?: return candidates

        val amountSpan =
            spanForValue(
                body = message.body,
                value = amountField.value
            ) ?: return candidates

        val action =
            when (
                match.eventKind
                    .uppercase()
            ) {
                "PAYMENT" ->
                    actionForParty(
                        kind =
                            FinancialActionCandidateKind
                                .PAYMENT_PAID_TO,
                        partyField =
                            "COUNTERPARTY",
                        amountMinor =
                            amountMinor,
                        amountSpan =
                            amountSpan,
                        match = match,
                        message = message
                    )

                "RECEIPT" ->
                    actionForParty(
                        kind =
                            FinancialActionCandidateKind
                                .RECEIPT_FROM,
                        partyField =
                            "COUNTERPARTY",
                        amountMinor =
                            amountMinor,
                        amountSpan =
                            amountSpan,
                        match = match,
                        message = message
                    )

                "TRANSFER" ->
                    transferAction(
                        amountMinor =
                            amountMinor,
                        amountSpan =
                            amountSpan,
                        match = match,
                        message = message
                    )

                else -> null
            } ?: return candidates

        return candidates.copy(
            actions =
                candidates.actions +
                    action
        )
    }

    private fun transferAction(
        amountMinor: Long,
        amountSpan: MessageSpan,
        match: MessagePackMatch,
        message: NormalizedFinancialMessage
    ): FinancialActionCandidate? {
        val destination =
            match.fields.firstOrNull {
                it.field.equals(
                    "DESTINATION",
                    ignoreCase = true
                )
            }
        if (destination != null) {
            return action(
                kind =
                    FinancialActionCandidateKind
                        .TRANSFER_TO_ACCOUNT,
                partyValue =
                    destination.value,
                amountMinor =
                    amountMinor,
                amountSpan =
                    amountSpan,
                match = match,
                message = message
            )
        }

        val source =
            match.fields.firstOrNull {
                it.field.equals(
                    "SOURCE",
                    ignoreCase = true
                )
            }
        if (source != null) {
            return action(
                kind =
                    FinancialActionCandidateKind
                        .TRANSFER_FROM_ACCOUNT,
                partyValue =
                    source.value,
                amountMinor =
                    amountMinor,
                amountSpan =
                    amountSpan,
                match = match,
                message = message
            )
        }

        // A transfer-network notification such as
        // PesaLink can contain a counterparty but no
        // authoritative source/destination account.
        // Do not invent an account relationship here.
        return null
    }

    private fun actionForParty(
        kind: FinancialActionCandidateKind,
        partyField: String,
        amountMinor: Long,
        amountSpan: MessageSpan,
        match: MessagePackMatch,
        message: NormalizedFinancialMessage
    ): FinancialActionCandidate? {
        val party =
            match.fields.firstOrNull {
                it.field.equals(
                    partyField,
                    ignoreCase = true
                )
            } ?: return null

        return action(
            kind = kind,
            partyValue =
                party.value,
            amountMinor =
                amountMinor,
            amountSpan =
                amountSpan,
            match = match,
            message = message
        )
    }

    private fun action(
        kind: FinancialActionCandidateKind,
        partyValue: String,
        amountMinor: Long,
        amountSpan: MessageSpan,
        match: MessagePackMatch,
        message: NormalizedFinancialMessage
    ): FinancialActionCandidate? {
        val partySpan =
            spanForValue(
                body = message.body,
                value = partyValue
            ) ?: return null

        val accountReference =
            match.fieldValue(
                "ACCOUNT_REFERENCE"
            )

        return FinancialActionCandidate(
            kind = kind,
            amountMinor = amountMinor,
            amountSpan = amountSpan,
            partyLabel =
                partyValue.trim(),
            partySpan = partySpan,
            partyPhone = null,
            accountReference =
                accountReference,
            accountReferenceSpan =
                accountReference?.let {
                    spanForValue(
                        body =
                            message.body,
                        value = it
                    )
                },
            ruleId =
                "message_pack." +
                    match.templateId +
                    "@" +
                    match.revision
        )
    }

    private fun spanForValue(
        body: String,
        value: String
    ): MessageSpan? {
        val trimmed =
            value.trim()
        if (trimmed.isBlank()) {
            return null
        }

        val start =
            body.indexOf(
                string = trimmed,
                ignoreCase = true
            )
        if (start < 0) {
            return null
        }

        return MessageSpan(
            startOffset = start,
            endOffsetExclusive =
                start + trimmed.length
        )
    }

    private fun amountToMinor(
        raw: String
    ): Long? =
        runCatching {
            BigDecimal(
                raw.replace(
                    ",",
                    ""
                ).trim()
            ).setScale(
                2,
                RoundingMode.HALF_UP
            ).movePointRight(2)
                .longValueExact()
        }.getOrNull()
}
