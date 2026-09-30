package com.kedevjimmy.pesatrack.domain.onboarding

import com.kedevjimmy.pesatrack.domain.intelligence.RemoteEntityEnrichment

object SalaryCounterpartyScreeningPolicy {
    private val financialIntermediarySignals =
        setOf(
            "payment",
            "payments",
            "pay",
            "gateway",
            "wallet",
            "money",
            "fintech",
            "bank",
            "sacco",
            "microfinance",
            "mfb",
            "loan",
            "credit",
            "insurance"
        )

    fun shouldSuppress(
        label: String,
        enrichment:
            RemoteEntityEnrichment? = null
    ): Boolean {
        if (
            enrichment?.categoryHint != null
        ) {
            return true
        }

        val combined =
            listOfNotNull(
                label,
                enrichment?.canonicalName,
                enrichment?.domain
            )
                .joinToString(" ")
                .lowercase()
                .replace(
                    Regex("""[^a-z0-9]+"""),
                    " "
                )
                .replace(
                    Regex("""\s+"""),
                    " "
                )
                .trim()

        if (combined.isBlank()) {
            return false
        }

        val words =
            combined.split(" ")
                .filter(
                    String::isNotBlank
                )

        return words.any {
            it in
                financialIntermediarySignals
        } ||
            enrichment?.domain
                ?.lowercase()
                ?.let { domain ->
                    domain.contains(
                        "pawapay"
                    ) ||
                        domain.contains(
                            "payment"
                        ) ||
                        domain.contains(
                            "gateway"
                        )
                } == true
    }
}
