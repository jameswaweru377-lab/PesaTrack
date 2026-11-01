package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatementKnownReaderRoutingTest {
    @Test fun oneSuccessfulReaderNeedsUserConfirmation() {
        assertEquals("equity", StatementKnownReaderRouting.uniqueCandidate(listOf("equity")))
    }

    @Test fun unknownOrConflictingMatchesNeverGuess() {
        assertNull(StatementKnownReaderRouting.uniqueCandidate(emptyList()))
        assertNull(StatementKnownReaderRouting.uniqueCandidate(listOf("equity", "other")))
        assertNull(StatementKnownReaderRouting.uniqueCandidate(listOf("equity", "equity")))
    }
}
