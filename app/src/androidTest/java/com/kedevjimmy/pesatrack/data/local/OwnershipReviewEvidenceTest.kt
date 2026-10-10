package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.kedevjimmy.pesatrack.domain.interpretation.*
import com.kedevjimmy.pesatrack.domain.model.AccountType
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class OwnershipReviewEvidenceTest {
    private lateinit var context: Context
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
    }
    @After fun cleanup() {
        PesaTrackDatabase.resetForTests()
        context.deleteDatabase("pesatrack.db")
    }
    @Test fun knownProviderAndWeakBalanceDoNotAskButHighConfidenceBalanceDoes() {
        val store = FinancialUnderstandingStore.get(context)
        val account = FinancialAccountReference("synthetic:bank", "Sample Bank", "SYNTHETIC",
            AccountType.BANK, AccountOwnershipHint.UNKNOWN, FinancialAccountNature.ASSET)
        store.observeEntity(account, 1000)
        assertFalse(store.ownershipReviewEvidence().getValue(account.entityKey).canAsk)
        store.recordBalance(BalanceObservationFact(account, 10000, InterpretationConfidence.LOW), "weak", 2000, null, "TEST")
        assertFalse(store.ownershipReviewEvidence().getValue(account.entityKey).canAsk)
        store.recordBalance(BalanceObservationFact(account, 10000, InterpretationConfidence.HIGH), "strong", 3000, null, "TEST")
        assertTrue(store.ownershipReviewEvidence().getValue(account.entityKey).canAsk)
        assertFalse(account.entityKey in store.confirmedAccountIds())
        assertTrue(store.pendingEntities().any { it.entityKey == account.entityKey })
    }
}
