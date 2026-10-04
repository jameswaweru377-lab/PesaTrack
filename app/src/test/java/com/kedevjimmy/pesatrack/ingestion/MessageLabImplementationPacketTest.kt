package com.kedevjimmy.pesatrack.ingestion

import com.kedevjimmy.pesatrack.domain.interpretation.FinancialRelevanceSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageLabImplementationPacketTest {
    @Test
    fun unapprovedEvidenceCannotProduceImplementationPacket() {
        val representative =
            shape(
                id = "candidate",
                review = null
            )

        assertNull(
            MessageLabImplementationPacketGenerator
                .generate(
                    representative =
                        representative,
                    allShapes =
                        listOf(
                            representative
                        )
                )
        )
    }

    @Test
    fun approvedFamilyPacketIncludesAllActiveVariantsAndDraft() {
        val representative =
            shape(
                id = "variant-a",
                body =
                    "<TX_CODE> Confirmed. Loan of Ksh <AMOUNT> repaid. Loan balance is Ksh <AMOUNT>.",
                semanticLabel =
                    MessageLabSemanticLabel
                        .LOAN_REPAYMENT,
                review =
                    MessageLabImplementationReviewState
                        .READY_FOR_PARSER_WORK,
                occurrences = 2,
                familyOccurrences = 5,
                familyVariants = 3
            )
        val sibling =
            shape(
                id = "variant-b",
                body =
                    "<TX_CODE> confirmed Loan of KES <AMOUNT> repaid. Loan balance is KES <AMOUNT>!",
                semanticLabel =
                    MessageLabSemanticLabel
                        .LOAN_REPAYMENT,
                review = null,
                occurrences = 3,
                familyOccurrences = 5,
                familyVariants = 3
            )
        val ignored =
            shape(
                id = "ignored",
                body =
                    "Promotional version <AMOUNT>",
                semanticLabel = null,
                review = null,
                occurrences = 1,
                familyOccurrences = 5,
                familyVariants = 3,
                decision =
                    MessageLabUserDecision
                        .NOT_FINANCIAL
            )

        val packet =
            MessageLabImplementationPacketGenerator
                .generate(
                    representative =
                        representative,
                    allShapes =
                        listOf(
                            ignored,
                            sibling,
                            representative
                        )
                )!!

        assertEquals(
            representative.familyId,
            packet.familyId
        )
        assertEquals(
            "DEBT_REPAYMENT",
            packet.parserDraft
                .suggestedEventKind
        )
        assertEquals(
            5,
            packet.familyOccurrenceCount
        )
        assertEquals(
            3,
            packet.familyVariantCount
        )
        assertEquals(
            listOf(
                "variant-a",
                "variant-b"
            ),
            packet.variants.map {
                it.shapeId
            }
        )
        assertTrue(
            packet.validationChecklist
                .any {
                    it.contains(
                        "zero observed outstanding"
                    )
                }
        )
    }

    @Test
    fun packetJsonIsExplicitlyManualAndContainsSafetyChecklist() {
        val representative =
            shape(
                review =
                    MessageLabImplementationReviewState
                        .READY_FOR_PARSER_WORK
            )

        val json =
            MessageLabImplementationPacketGenerator
                .generate(
                    representative =
                        representative,
                    allShapes =
                        listOf(
                            representative
                        )
                )!!
                .toJson()

        assertTrue(
            json.contains(
                "\"status\": \"READY_FOR_PARSER_WORK\""
            )
        )
        assertTrue(
            json.contains(
                "\"requiresManualImplementation\": true"
            )
        )
        assertTrue(
            json.contains(
                "\"representativeParserDraft\""
            )
        )
        assertTrue(
            json.contains(
                "\"implementationChecklist\""
            )
        )
        assertTrue(
            json.contains(
                "\"validationChecklist\""
            )
        )
        assertTrue(
            json.contains(
                "must never be loaded as an active parser"
            )
        )
        assertFalse(
            json.contains(
                "\"active\": true"
            )
        )
    }

    @Test
    fun bundleKeepsOneApprovedRepresentativePerFamilyMeaning() {
        val first =
            shape(
                id = "first",
                review =
                    MessageLabImplementationReviewState
                        .READY_FOR_PARSER_WORK,
                occurrences = 4
            )
        val second =
            shape(
                id = "second",
                review =
                    MessageLabImplementationReviewState
                        .READY_FOR_PARSER_WORK,
                occurrences = 2
            )

        val bundle =
            MessageLabImplementationPacketGenerator
                .bundleJson(
                    allShapes =
                        listOf(
                            second,
                            first
                        )
                )!!

        assertTrue(
            bundle.contains(
                "\"representativeShapeId\": \"first\""
            )
        )
        assertFalse(
            bundle.contains(
                "\"representativeShapeId\": \"second\""
            )
        )
        assertTrue(
            bundle.contains(
                "\"shapeId\": \"second\""
            )
        )
    }

    @Test
    fun transferPacketCarriesOwnershipAndMirrorValidation() {
        val representative =
            shape(
                semanticLabel =
                    MessageLabSemanticLabel
                        .TRANSFER,
                review =
                    MessageLabImplementationReviewState
                        .READY_FOR_PARSER_WORK
            )

        val packet =
            MessageLabImplementationPacketGenerator
                .generate(
                    representative =
                        representative,
                    allShapes =
                        listOf(
                            representative
                        )
                )!!

        assertTrue(
            packet.implementationChecklist
                .any {
                    it.contains(
                        "ownership/self-transfer"
                    )
                }
        )
        assertTrue(
            packet.validationChecklist
                .any {
                    it.contains(
                        "mirrored channel copies"
                    )
                }
        )
    }

    private fun shape(
        id: String = "shape-a",
        familyId: String = "family-a",
        body: String =
            "KES <AMOUNT> paid to <ENTITY>.",
        semanticLabel:
            MessageLabSemanticLabel? =
            MessageLabSemanticLabel
                .PAYMENT,
        review:
            MessageLabImplementationReviewState?,
        occurrences: Int = 2,
        familyOccurrences: Int = 2,
        familyVariants: Int = 1,
        decision:
            MessageLabUserDecision =
            MessageLabUserDecision
                .CONFIRMED_FINANCIAL
    ) =
        UnsupportedMessageShape(
            shapeId = id,
            familyId = familyId,
            sender = "Source",
            sourcePackage = null,
            sourceKind =
                "historical_financial_message",
            redactedBody = body,
            firstSeenAtEpochMs = 1L,
            lastSeenAtEpochMs = 2L,
            occurrenceCount =
                occurrences,
            userDecision =
                decision,
            semanticLabel =
                semanticLabel,
            relevanceScore = 8,
            relevanceSignals =
                setOf(
                    FinancialRelevanceSignal
                        .CURRENCY_AMOUNT,
                    FinancialRelevanceSignal
                        .FINANCIAL_ACTION
                ),
            familyOccurrenceCount =
                familyOccurrences,
            familyVariantCount =
                familyVariants,
            implementationReview =
                review
        )
}
