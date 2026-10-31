package com.kedevjimmy.pesatrack.domain.rules

import com.google.gson.JsonParser
import com.google.gson.Gson
import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
import com.kedevjimmy.pesatrack.domain.model.ReviewState
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import java.security.MessageDigest
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** A fixed invented corpus for default on-device decisions, with no user rules or cached hints. */
class CategorizationExamTest {
    private data class Case(
        val id: String,
        val kind: String,
        val type: MpesaTransactionType,
        val direction: MoneyDirection,
        val category: TransactionCategory,
        val amountMinor: Long,
        val counterparty: String,
        val rawMessage: String,
        val reversalOfCode: String?
    ) {
        fun parsed() = ParsedMpesaTransaction(
            transactionCode = id,
            type = type,
            amountMinor = amountMinor,
            counterparty = counterparty,
            feeMinor = 0L,
            balanceMinor = null,
            occurredAtText = null,
            rawMessage = rawMessage,
            reversalOfCode = reversalOfCode
        )
    }

    private data class Prediction(
        val direction: MoneyDirection,
        val category: TransactionCategory,
        val reviewState: ReviewState
    )

    private enum class Outcome {
        CORRECT, WRONG, NEEDS_REVIEW, UNCLASSIFIED_WITHOUT_REVIEW
    }

    private class Counts {
        var correct = 0
        var wrong = 0
        var needsReview = 0
        var unclassifiedWithoutReview = 0

        fun add(outcome: Outcome) {
            when (outcome) {
                Outcome.CORRECT -> correct++
                Outcome.WRONG -> wrong++
                Outcome.NEEDS_REVIEW -> needsReview++
                Outcome.UNCLASSIFIED_WITHOUT_REVIEW -> unclassifiedWithoutReview++
            }
        }

        fun total() = correct + wrong + needsReview + unclassifiedWithoutReview

        fun asMap() = linkedMapOf(
            "total" to total(),
            "correct" to correct,
            "wrong" to wrong,
            "needs_review" to needsReview,
            "unclassified_without_review" to unclassifiedWithoutReview,
            "correct_percent" to percent(correct),
            "wrong_percent" to percent(wrong),
            "needs_review_percent" to percent(needsReview),
            "unclassified_without_review_percent" to percent(unclassifiedWithoutReview)
        )

        private fun percent(count: Int): String =
            String.format(Locale.ROOT, "%.2f", count * 100.0 / total())
    }

    @Test
    fun inventedCasesAreLabelledAndCoverTenKenyanTransactionTypes() {
        val cases = cases()
        assertEquals(600, cases.size)
        assertEquals(cases.size, cases.map { it.id }.toSet().size)
        assertEquals(
            setOf("paybill", "till", "person_to_person", "fuliza", "fees",
                "reversals", "airtime", "salary", "rent", "school_fees"),
            cases.map { it.kind }.toSet()
        )
        assertTrue(cases.all { it.rawMessage.startsWith("FICTIONAL TEST ONLY - ") })
        assertTrue(cases.all { it.counterparty.startsWith("Exam") })
        assertTrue(cases.all { it.amountMinor > 0L })
        cases.groupingBy { it.kind }.eachCount().values.forEach { assertEquals(60, it) }
    }

    @Test
    fun defaultOfflineExamWrongRateNeverExceedsFrozenBaseline() {
        val originalBytes = resource("/categorization-exam-v1.tsv")
        val digest = MessageDigest.getInstance("SHA-256").digest(originalBytes)
            .joinToString("") { "%02x".format(it) }
        val baseline = JsonParser.parseString(
            resource("/categorization-exam-baseline-v1.json").toString(Charsets.UTF_8)
        ).asJsonObject
        val cases = cases()
        assertEquals(baseline.get("corpus_sha256").asString, digest)
        assertEquals(baseline.get("total").asInt, cases.size)

        val all = Counts()
        val perKind = sortedMapOf<String, Counts>()
        cases.forEach { sample ->
            val result = grade(sample, offlineDefaultDecision(sample.parsed()))
            all.add(result)
            perKind.getOrPut(sample.kind) { Counts() }.add(result)
        }
        assertEquals(cases.size, all.total())
        assertEquals(cases.size, perKind.values.sumOf { it.total() })
        val report = linkedMapOf<String, Any>(
            "scope" to "default offline classification; no personal rules or cached remote hints",
            "corpus_sha256" to digest,
            "overall" to all.asMap(),
            "by_transaction_kind" to perKind.mapValues { it.value.asMap() }
        )
        println("CATEGORIZATION_EXAM_RESULT=${Gson().toJson(report)}")
        assertTrue(
            "Wrong-answer rate increased: ${all.wrong}/${all.total()} versus " +
                "${baseline.get("wrong_ceiling").asInt}/${baseline.get("total").asInt}",
            all.wrong * baseline.get("total").asInt <=
                baseline.get("wrong_ceiling").asInt * all.total()
        )
        // This snapshot is deliberately reviewed when the categorizer changes.
        // A better result may update the snapshot, but cannot raise the wrong ceiling.
        assertEquals(baseline.get("correct").asInt, all.correct)
        assertEquals(baseline.get("wrong").asInt, all.wrong)
        assertEquals(baseline.get("needs_review").asInt, all.needsReview)
        assertEquals(
            baseline.get("unclassified_without_review").asInt,
            all.unclassifiedWithoutReview
        )
        val byKind = baseline.getAsJsonObject("by_transaction_kind")
        perKind.forEach { (kind, count) ->
            val recorded = byKind.getAsJsonObject(kind)
            assertEquals(kind, recorded.get("total").asInt, count.total())
            assertEquals(kind, recorded.get("correct").asInt, count.correct)
            assertEquals(kind, recorded.get("wrong").asInt, count.wrong)
            assertEquals(kind, recorded.get("needs_review").asInt, count.needsReview)
            assertEquals(kind, recorded.get("unclassified_without_review").asInt,
                count.unclassifiedWithoutReview)
        }
    }

    private fun grade(sample: Case, prediction: Prediction): Outcome = when {
        prediction.reviewState == ReviewState.NEEDS_REVIEW -> Outcome.NEEDS_REVIEW
        prediction.category == TransactionCategory.UNCATEGORIZED ->
            Outcome.UNCLASSIFIED_WITHOUT_REVIEW
        prediction.direction == sample.direction && prediction.category == sample.category ->
            Outcome.CORRECT
        else -> Outcome.WRONG
    }

    private fun offlineDefaultDecision(parsed: ParsedMpesaTransaction): Prediction {
        KenyanTransactionRecognizer.classify(parsed)?.let {
            return Prediction(it.direction, it.category, ReviewState.AUTO_CLASSIFIED)
        }
        val fallback = DefaultCategoryFallbackPolicy.classify(parsed)
        return Prediction(fallback.direction, fallback.category, fallback.reviewState)
    }

    private fun cases(): List<Case> =
        resource("/categorization-exam-v1.tsv").toString(Charsets.UTF_8)
            .lineSequence().filter { it.isNotBlank() }.drop(1).map { line ->
                val fields = line.split('\t')
                require(fields.size == 9) { "Invalid invented case field count" }
                Case(
                    id = fields[0], kind = fields[1],
                    type = MpesaTransactionType.valueOf(fields[2]),
                    direction = MoneyDirection.valueOf(fields[3]),
                    category = TransactionCategory.valueOf(fields[4]),
                    amountMinor = fields[5].toLong(), counterparty = fields[6],
                    rawMessage = fields[7], reversalOfCode = fields[8].takeUnless { it == "-" }
                )
            }.toList()

    private fun resource(path: String): ByteArray =
        requireNotNull(javaClass.getResourceAsStream(path)) { "Missing exam resource: $path" }
            .use { it.readBytes() }
}
