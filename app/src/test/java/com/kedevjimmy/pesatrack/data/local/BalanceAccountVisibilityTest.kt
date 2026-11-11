package com.kedevjimmy.pesatrack.data.local

import android.app.Application
import com.kedevjimmy.pesatrack.domain.model.AccountType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE, sdk = [28])
class BalanceAccountVisibilityTest {
    @Test fun archiveAndRestoreOnlyChangePresentation() {
        val context = RuntimeEnvironment.getApplication()
        val consent = AccountOwnershipConsentStore(context)
        val first = consent.createOwnedAccount("Sample Bank", AccountType.BANK)
        val sibling = consent.createOwnedAccount("Sample Bank", AccountType.BANK)
        val initialConsent = consent.confirmedRevisions()
        val before = LedgerUpdateBus.state.value

        BalanceAccountVisibility(context).setArchived(first.id, true)

        val reopened = BalanceAccountVisibility(context)
        assertEquals(setOf(first.id), reopened.archivedAccountIds())
        assertEquals(listOf(sibling), reopened.visibleAccounts(listOf(first, sibling)))
        assertEquals(initialConsent, consent.confirmedRevisions())
        assertTrue(AccountStore.get(context).listAccounts().any { it.id == first.id && it.enabled })
        assertEquals(before.dataRevision, LedgerUpdateBus.state.value.dataRevision)
        assertEquals(before.revision + 1L, LedgerUpdateBus.state.value.revision)

        reopened.setArchived(first.id, true)
        assertEquals(before.revision + 1L, LedgerUpdateBus.state.value.revision)
        reopened.setArchived(first.id, false)
        assertEquals(listOf(first, sibling), reopened.visibleAccounts(listOf(first, sibling)))
        assertEquals(initialConsent, consent.confirmedRevisions())
        assertEquals(before.revision + 2L, LedgerUpdateBus.state.value.revision)
    }
}
