package com.kedevjimmy.pesatrack.domain.identity

import com.kedevjimmy.pesatrack.domain.intelligence.EntityKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentityGraphPolicyTest {
    @Test
    fun normalizesKenyanPhoneFormsToOneAlias() {
        assertEquals(
            "0712345678",
            IdentityGraphPolicy
                .normalizeKenyanPhone(
                    "+254712345678"
                )
        )
        assertEquals(
            "0712345678",
            IdentityGraphPolicy
                .normalizeKenyanPhone(
                    "712345678"
                )
        )
        assertEquals(
            "0112345678",
            IdentityGraphPolicy
                .normalizeKenyanPhone(
                    "254112345678"
                )
        )
    }

    @Test
    fun rejectsIncompleteOrNonKenyanPhoneEvidence() {
        assertNull(
            IdentityGraphPolicy
                .normalizeKenyanPhone(
                    "2547****321"
                )
        )
        assertNull(
            IdentityGraphPolicy
                .normalizeKenyanPhone(
                    "12345"
                )
        )
    }

    @Test
    fun phoneEvidenceSeparatesSameNamedCounterparties() {
        val first =
            IdentityGraphPolicy
                .counterpartyIdentityId(
                    name = "Alex Kimani",
                    phone = "0711111111"
                )
        val second =
            IdentityGraphPolicy
                .counterpartyIdentityId(
                    name = "Alex Kimani",
                    phone = "0722222222"
                )

        assertNotEquals(
            first,
            second
        )
    }

    @Test
    fun sameUnknownNameCreatesOnlyAProvisionalStableCluster() {
        val first =
            IdentityGraphPolicy
                .counterpartyIdentityId(
                    name = "Alex Kimani",
                    phone = null
                )
        val second =
            IdentityGraphPolicy
                .counterpartyIdentityId(
                    name = "ALEX   KIMANI",
                    phone = null
                )

        assertEquals(
            first,
            second
        )
        assertTrue(
            first.startsWith(
                "counterparty:name:"
            )
        )

        // A stable name cluster is useful for asking
        // one question later. It is not evidence that
        // every person with this name is the same human.
        assertEquals(
            IdentityNodeKind.UNKNOWN,
            IdentityGraphPolicy
                .nodeKindFor(null)
        )
    }

    @Test
    fun repetitionNeverTurnsUnknownEntityIntoBusiness() {
        assertEquals(
            IdentityNodeKind.UNKNOWN,
            IdentityGraphPolicy
                .nodeKindFor(null)
        )
        assertEquals(
            IdentityNodeKind.BUSINESS,
            IdentityGraphPolicy
                .nodeKindFor(
                    EntityKind.SUPERMARKET
                )
        )
        assertEquals(
            IdentityNodeKind.BILLER,
            IdentityGraphPolicy
                .nodeKindFor(
                    EntityKind.UTILITY
                )
        )
    }

    @Test
    fun plausiblePersonNameIsOnlyPromptEvidence() {
        assertTrue(
            IdentityGraphPolicy
                .isPlausiblePersonName(
                    "Alex Kimani"
                )
        )
        assertFalse(
            IdentityGraphPolicy
                .isPlausiblePersonName(
                    "KCB"
                )
        )
        assertFalse(
            IdentityGraphPolicy
                .isPlausiblePersonName(
                    "M-PESA"
                )
        )
        assertFalse(
            IdentityGraphPolicy
                .isPlausiblePersonName(
                    "SHOP 123"
                )
        )
    }

    @Test
    fun knownCatalogIdentityWinsOverNameHash() {
        val id =
            IdentityGraphPolicy
                .counterpartyIdentityId(
                    name = "Naivas Foodmarket",
                    phone = null,
                    knownEntityId =
                        "naivas"
                )

        assertEquals(
            "catalog:naivas",
            id
        )
    }
}
