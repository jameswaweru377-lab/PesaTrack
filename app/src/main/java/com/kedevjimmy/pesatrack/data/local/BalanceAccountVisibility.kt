package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import com.kedevjimmy.pesatrack.domain.model.MoneyAccount

/** Current balance presentation only. Account ownership and ledger history remain authoritative. */
internal class BalanceAccountVisibility(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("balance_account_visibility", Context.MODE_PRIVATE)

    fun archivedAccountIds(): Set<String> =
        preferences.getStringSet(ARCHIVED_ACCOUNTS, emptySet()).orEmpty().toSet()

    fun visibleAccounts(accounts: List<MoneyAccount>): List<MoneyAccount> {
        val archived = archivedAccountIds()
        return accounts.filterNot { it.id in archived }
    }

    fun setArchived(accountId: String, archived: Boolean) {
        synchronized(lock) {
            val current = archivedAccountIds()
            val updated = if (archived) current + accountId else current - accountId
            if (updated == current) return
            if (!preferences.edit().putStringSet(ARCHIVED_ACCOUNTS, updated).commit()) {
                preferences.edit().putStringSet(ARCHIVED_ACCOUNTS, current).commit()
                error("Could not save balance account visibility.")
            }
            LedgerUpdateBus.requestPresentationRefresh()
        }
    }

    private companion object {
        const val ARCHIVED_ACCOUNTS = "archived_accounts"
        val lock = Any()
    }
}
