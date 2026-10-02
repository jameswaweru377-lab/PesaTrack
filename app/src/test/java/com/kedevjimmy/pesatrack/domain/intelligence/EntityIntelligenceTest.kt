package com.kedevjimmy.pesatrack.domain.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EntityIntelligenceTest {
    @Test
    fun exactKplcCounterpartyResolvesWithHighConfidence() {
        val result =
            EntityIntelligence.resolve(
                counterparty = "KPLC PREPAID",
                rawMessage = ""
            )

        assertEquals(
            "Kenya Power",
            result?.entity?.canonicalName
        )
        assertEquals(
            EntityMatchConfidence.HIGH,
            result?.confidence
        )
    }

    @Test
    fun unknownPersonalNameDoesNotResolve() {
        assertNull(
            EntityIntelligence.resolve(
                counterparty = "JOHN KAMAU",
                rawMessage = ""
            )
        )
    }

    @Test
    fun saccoCounterpartyResolvesAsFinancialInstitutionWithoutExpenseCategory() {
        val result =
            EntityIntelligence.resolve(
                counterparty =
                    "STIMA SACCO",
                rawMessage = ""
            )

        assertEquals(
            "Stima DT Sacco",
            result?.entity
                ?.canonicalName
        )
        assertEquals(
            EntityKind.SACCO,
            result?.entity?.kind
        )
        assertEquals(
            EntityMatchConfidence.HIGH,
            result?.confidence
        )
        assertNull(
            result?.entity
                ?.defaultCategory
        )
    }

    @Test
    fun longTailBankAliasesResolveOnlyFromCounterpartyEvidence() {
        val dtb =
            EntityIntelligence.resolve(
                counterparty = "DTB",
                rawMessage = ""
            )

        assertEquals(
            "Diamond Trust Bank Kenya",
            dtb?.entity?.canonicalName
        )
        assertEquals(
            EntityKind.BANK,
            dtb?.entity?.kind
        )

        assertNull(
            EntityIntelligence.resolve(
                counterparty = null,
                rawMessage =
                    "NCBA Bank Kenya transaction message"
            )
        )
    }

    @Test
    fun lenderIdentityCanResolveWithoutSupplyingExpenseCategory() {
        val result =
            EntityIntelligence.resolve(
                counterparty = "Branch",
                rawMessage = ""
            )

        assertEquals(
            EntityKind.LENDER,
            result?.entity?.kind
        )
        assertNull(
            result?.entity?.defaultCategory
        )
    }
}
