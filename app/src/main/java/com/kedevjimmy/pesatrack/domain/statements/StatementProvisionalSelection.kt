package com.kedevjimmy.pesatrack.domain.statements

/** In-memory preview is bound to one account and one explicitly chosen file. */
data class StatementProvisionalSelection(
    val accountId: String,
    val documentToken: Long,
    val result: NativeBankProbeResult
)

object StatementProvisionalSelectionPolicy {
    fun visible(
        selection: StatementProvisionalSelection?,
        accountId: String,
        documentToken: Long
    ): NativeBankProbeResult? = selection?.takeIf {
        it.accountId == accountId && it.documentToken == documentToken
    }?.result
}
