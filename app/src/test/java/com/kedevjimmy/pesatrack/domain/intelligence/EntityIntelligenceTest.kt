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
