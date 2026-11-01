package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import com.kedevjimmy.pesatrack.data.local.StatementMatchDecisionStore
import com.kedevjimmy.pesatrack.data.local.StatementEvidenceReadModel

class StatementAccountMentionPolicyTest {
    private val row = StatementEvidenceCorrelationPolicy.Observation("row", StatementEvidenceCorrelationPolicy.Source.BANK_STATEMENT,
        "source", "bank", "KES", 1_780_000_000_000L, -500L, "ABC1234567", true, sourceTotalsVerified = true)
    private val accounts = listOf(StatementAccountMentionPolicy.Account("bank", "001234567890"),
        StatementAccountMentionPolicy.Account("wallet", "0712345678"))
    private fun hints(text: String, evidence: StatementEvidenceCorrelationPolicy.Observation = row,
                      owners: List<StatementAccountMentionPolicy.Account> = accounts, excluded: Set<String> = emptySet()) =
        StatementAccountMentionPolicy.find(listOf(StatementAccountMentionPolicy.BankRecord(evidence, text)), owners, excluded)

    @Test fun fullOtherAccountIdentifierCreatesOnlyAMention() {
        val hints = hints("Invented payment 0712345678")
        assertEquals(1, hints.size)
        assertEquals("wallet", hints.single().mentionedAccountId)
        assertEquals("row", hints.single().row.id)
    }

    @Test fun kenyanWalletCountryAndLocalFormatsReferToSameFullNumber() {
        val owners = listOf(accounts.first(), StatementAccountMentionPolicy.Account("wallet", "+254712345678", true))
        assertEquals("wallet", hints("Invented payment 0712345678", owners = owners).single().mentionedAccountId)
        assertEquals("wallet", hints("Invented payment +254712345678", owners = owners).single().mentionedAccountId)
        assertTrue(hints("Invented payment +712345678", owners = owners).isEmpty())
        assertTrue(hints("Invented payment + 712345678", owners = owners).isEmpty())
        assertTrue(hints("Invented payment +44 712345678", owners = owners).isEmpty())
        assertTrue(hints("Invented payment +7 712345678", owners = owners).isEmpty())
        assertTrue(hints("Invented payment +44 0 712345678", owners = owners).isEmpty())
        assertEquals("wallet", hints("Invented payment +254 712345678", owners = owners).single().mentionedAccountId)
        assertEquals("wallet", hints("Invented payment +254-712345678", owners = owners).single().mentionedAccountId)
        assertTrue(hints("Invented payment +254 0 712345678", owners = owners).isEmpty())
        val bankOnly = listOf(accounts.first(), StatementAccountMentionPolicy.Account("other-bank", "254712345678"))
        assertTrue(hints("Invented payment +254712345678", owners = bankOnly).isEmpty())
        assertTrue(hints("Invented payment +254712345678", owners = owners + bankOnly.last()).isEmpty())
        assertTrue(hints("Invented masked number ***45678", owners = owners).isEmpty())
    }

    @Test fun noHintForPartialMaskedNamesOrEmbeddedNumbers() {
        assertTrue(hints("Invented person").isEmpty())
        assertTrue(hints("***45678").isEmpty())
        assertTrue(hints("907123456789").isEmpty())
        assertTrue(hints("refA0712345678B").isEmpty())
        assertTrue(hints("001234567890").isEmpty())
    }

    @Test fun noHintForUnverifiedProtectedOrAlreadyShownRows() {
        assertTrue(hints("0712345678", row.copy(sourceTotalsVerified = false)).isEmpty())
        assertTrue(hints("0712345678", row.copy(roleProtected = true)).isEmpty())
        assertTrue(hints("0712345678", row.copy(ownershipConfirmed = false)).isEmpty())
        assertTrue(hints("0712345678", row.copy(source = StatementEvidenceCorrelationPolicy.Source.ACTIVITY)).isEmpty())
        assertTrue(hints("0712345678", row.copy(sourceId = "")).isEmpty())
        assertTrue(hints("0712345678", owners = accounts.filter { it.id != "bank" }).isEmpty())
        assertTrue(hints("0712345678", excluded = setOf("row")).isEmpty())
    }

    @Test fun duplicateOrSeveralAccountIdentitiesAreNotGuessed() {
        val other = StatementAccountMentionPolicy.Account("other", "0712345678")
        assertTrue(hints("0712345678", owners = accounts + other).isEmpty())
        val second = StatementAccountMentionPolicy.Account("second", "0799999999")
        assertTrue(hints("0712345678 0799999999", owners = accounts + second).isEmpty())
        assertTrue(hints("0712345678", owners = listOf(accounts.first(),
            StatementAccountMentionPolicy.Account("wallet", "ACCT-0712345678"))).isEmpty())
    }

    @Test fun diagnosticStringsDoNotExposeAccountNumbersOrDescriptions() {
        val account = StatementAccountMentionPolicy.Account("wallet", "0712345678")
        val record = StatementAccountMentionPolicy.BankRecord(row, "private 0712345678")
        assertFalse(account.toString().contains("0712345678"))
        assertFalse(record.toString().contains("private"))
        val descriptions = StatementEvidenceReadModel.PrivateBankDescriptions(mapOf("row" to "private 0712345678"))
        assertFalse(descriptions.toString().contains("0712345678"))
        val hint = StatementAccountMentionPolicy.Hint(row.copy(reference = "0712345678", movementMinor = -123456), "wallet")
        val snapshot = StatementMatchDecisionStore.Snapshot(StatementMatchReviewPolicy.Review(emptyList(), emptyList(), 0),
            emptyList(), emptyMap(), false, accountMentions = listOf(hint))
        assertFalse(hint.toString().contains("0712345678"))
        assertFalse(snapshot.toString().contains("0712345678"))
        assertFalse(snapshot.toString().contains("123456"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun duplicateSourceRowsAreRejected() {
        val record = StatementAccountMentionPolicy.BankRecord(row, "0712345678")
        StatementAccountMentionPolicy.find(listOf(record, record), accounts, emptySet())
    }
}
