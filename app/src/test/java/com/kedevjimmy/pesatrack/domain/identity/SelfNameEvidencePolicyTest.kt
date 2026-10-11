package com.kedevjimmy.pesatrack.domain.identity

import org.junit.Assert.assertEquals
import org.junit.Test

class SelfNameEvidencePolicyTest {
    @Test fun exactThreeDistinctNamesAreStrongIdentityEvidence() {
        assertEquals(SelfNameEvidenceLevel.EXACT_THREE_NAMES,
            SelfNameEvidencePolicy.compare("James Mwangi Waweru", "JAMES MWANGI WAWERU"))
        assertEquals(SelfNameEvidenceLevel.EXACT_THREE_NAMES,
            SelfNameEvidencePolicy.compare("  James   Mwangi Waweru ", "James Mwangi Waweru"))
    }

    @Test fun reorderedThreeNamesAreNotStrongEvidence() {
        assertEquals(SelfNameEvidenceLevel.NO_MATCH,
            SelfNameEvidencePolicy.compare("James Mwangi Waweru", "Waweru James Mwangi"))
    }
    @Test fun unsupportedExtraTokensCannotBeDiscardedIntoAnExactMatch() {
        for (candidate in listOf("James Mwangi Waweru 123", "James X Mwangi Waweru", "James Mwangi Waweru!")) {
            assertEquals(SelfNameEvidenceLevel.NO_MATCH,
                SelfNameEvidencePolicy.compare("James Mwangi Waweru", candidate))
        }
    }

    @Test fun twoOfThreeRequiresExplicitConfirmation() {
        assertEquals(SelfNameEvidenceLevel.TWO_OF_THREE_REVIEW,
            SelfNameEvidencePolicy.compare("James Mwangi Waweru", "James Waweru"))
        assertEquals(SelfNameEvidenceLevel.TWO_OF_THREE_REVIEW,
            SelfNameEvidencePolicy.compare("James Mwangi Waweru", "James Otieno Waweru"))
    }

    @Test fun oneNameAndSubstringLookalikesNeverMatch() {
        for (candidate in listOf("James", "Jameson Otieno", "James Otieno",
            "Mr James", "JAMES & SONS LTD", "Waweru")) {
            assertEquals(candidate, SelfNameEvidenceLevel.NO_MATCH,
                SelfNameEvidencePolicy.compare("James Mwangi Waweru", candidate))
        }
    }

    @Test fun invalidProfileCannotDriveIdentity() {
        for (profile in listOf(null, "", "James Waweru", "John John Doe", "James 123 Waweru")) {
            assertEquals(SelfNameEvidenceLevel.NO_MATCH,
                SelfNameEvidencePolicy.compare(profile, "James Mwangi Waweru"))
        }
    }

    @Test fun unexpectedExtraWordsAreNotDiscarded() {
        assertEquals(SelfNameEvidenceLevel.NO_MATCH,
            SelfNameEvidencePolicy.compare("James Mwangi Waweru", "Mr James Mwangi Waweru"))
        assertEquals(SelfNameEvidenceLevel.NO_MATCH,
            SelfNameEvidencePolicy.compare("James Mwangi Waweru", "James Otieno Kamau"))
    }
}
