package com.kedevjimmy.pesatrack.domain.statements

import org.junit.Assert.*
import org.junit.Test

class StatementProvisionalSelectionTest {
    private val status = NativeBankProbeResult.Blocked(NativeProbeBlockReason.UNRECOGNIZED_LAYOUT)
    private val session = StatementProvisionalSelection("invented-bank-id", 7L, status)

    @Test fun wrongAccountHidesPreviousProbe() {
        assertNull(StatementProvisionalSelectionPolicy.visible(session, "another-bank", 7L))
    }

    @Test fun newFileTokenHidesPreviousProbe() {
        assertNull(StatementProvisionalSelectionPolicy.visible(session, "invented-bank-id", 8L))
    }

    @Test fun currentAccountAndFilePreserveStatus() {
        assertEquals(status, StatementProvisionalSelectionPolicy.visible(
            session, "invented-bank-id", 7L))
        assertNull(StatementProvisionalSelectionPolicy.visible(null, "invented-bank-id", 7L))
    }
}
