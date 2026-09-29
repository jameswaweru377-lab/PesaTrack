package com.kedevjimmy.pesatrack.domain.intelligence

import com.kedevjimmy.pesatrack.domain.model.TransactionCategory

enum class EntityKind {
    UTILITY,
    TELECOM,
    SUPERMARKET,
    RESTAURANT,
    TRANSPORT,
    ENTERTAINMENT,
    BANK,
    LENDER,
    SAVINGS_PRODUCT,
    MERCHANT
}

enum class EntityMatchConfidence {
    HIGH,
    LIKELY
}

data class CanonicalEntity(
    val id: String,
    val canonicalName: String,
    val kind: EntityKind,
    val aliases: Set<String>,
    val defaultCategory: TransactionCategory? = null,
    val autoReviewEligible: Boolean = false,
    val allowMessageMatch: Boolean = false,
    val domain: String? = null
)

data class EntityLookupCandidate(
    val counterparty: String?,
    val rawMessage: String
)

data class EntityResolution(
    val entity: CanonicalEntity,
    val confidence: EntityMatchConfidence,
    val matchedAlias: String,
    val source: String
)

interface EntityEnrichmentProvider {
    fun resolve(
        candidate: EntityLookupCandidate
    ): EntityResolution?
}

object LocalSeedEntityEnrichmentProvider :
    EntityEnrichmentProvider {

    private val entities = listOf(
        CanonicalEntity(
            id = "ke_kenya_power",
            canonicalName = "Kenya Power",
            kind = EntityKind.UTILITY,
            aliases = setOf(
                "kplc",
                "kplc prepaid",
                "kenya power",
                "kenya power and lighting"
            ),
            defaultCategory =
                TransactionCategory.ELECTRICITY,
            autoReviewEligible = true,
            allowMessageMatch = true,
            domain = "kplc.co.ke"
        ),
        CanonicalEntity(
            id = "ke_safaricom",
            canonicalName = "Safaricom",
            kind = EntityKind.TELECOM,
            aliases = setOf(
                "safaricom",
                "safaricom plc"
            ),
            defaultCategory =
                TransactionCategory.PHONE_AND_DATA,
            autoReviewEligible = false,
            allowMessageMatch = false,
            domain = "safaricom.co.ke"
        ),
        CanonicalEntity(
            id = "ke_naivas",
            canonicalName = "Naivas",
            kind = EntityKind.SUPERMARKET,
            aliases = setOf(
                "naivas",
                "naivas supermarket"
            ),
            defaultCategory =
                TransactionCategory.GROCERIES,
            autoReviewEligible = true,
            allowMessageMatch = true,
            domain = "naivas.online"
        ),
        CanonicalEntity(
            id = "ke_quickmart",
            canonicalName = "Quickmart",
            kind = EntityKind.SUPERMARKET,
            aliases = setOf(
                "quickmart",
                "quick mart"
            ),
            defaultCategory =
                TransactionCategory.GROCERIES,
            autoReviewEligible = true,
            allowMessageMatch = true
        ),
        CanonicalEntity(
            id = "ke_carrefour",
            canonicalName = "Carrefour",
            kind = EntityKind.SUPERMARKET,
            aliases = setOf(
                "carrefour"
            ),
            defaultCategory =
                TransactionCategory.GROCERIES,
            autoReviewEligible = true,
            allowMessageMatch = true
        ),
        CanonicalEntity(
            id = "ke_java_house",
            canonicalName = "Java House",
            kind = EntityKind.RESTAURANT,
            aliases = setOf(
                "java house"
            ),
            defaultCategory =
                TransactionCategory.EATING_OUT,
            autoReviewEligible = true,
            allowMessageMatch = true,
            domain = "javahouseafrica.com"
        ),
        CanonicalEntity(
            id = "ke_uber",
            canonicalName = "Uber",
            kind = EntityKind.TRANSPORT,
            aliases = setOf(
                "uber",
                "uber trip"
            ),
            defaultCategory =
                TransactionCategory.TRANSPORT,
            autoReviewEligible = true,
            allowMessageMatch = true
        ),
        CanonicalEntity(
            id = "ke_bolt",
            canonicalName = "Bolt",
            kind = EntityKind.TRANSPORT,
            aliases = setOf(
                "bolt",
                "bolt ride"
            ),
            defaultCategory =
                TransactionCategory.TRANSPORT,
            autoReviewEligible = true,
            allowMessageMatch = true
        ),
        CanonicalEntity(
            id = "ke_netflix",
            canonicalName = "Netflix",
            kind = EntityKind.ENTERTAINMENT,
            aliases = setOf("netflix"),
            defaultCategory =
                TransactionCategory.ENTERTAINMENT,
            autoReviewEligible = true,
            allowMessageMatch = true
        ),
        CanonicalEntity(
            id = "ke_showmax",
            canonicalName = "Showmax",
            kind = EntityKind.ENTERTAINMENT,
            aliases = setOf("showmax"),
            defaultCategory =
                TransactionCategory.ENTERTAINMENT,
            autoReviewEligible = true,
            allowMessageMatch = true
        ),
        CanonicalEntity(
            id = "ke_mshwari",
            canonicalName = "M-Shwari",
            kind = EntityKind.SAVINGS_PRODUCT,
            aliases = setOf(
                "m shwari",
                "m-shwari",
                "mshwari"
            ),
            autoReviewEligible = false,
            allowMessageMatch = true
        ),
        CanonicalEntity(
            id = "ke_branch",
            canonicalName = "Branch",
            kind = EntityKind.LENDER,
            aliases = setOf(
                "branch",
                "branch international"
            ),
            autoReviewEligible = false,
            allowMessageMatch = false
        ),
        CanonicalEntity(
            id = "ke_tala",
            canonicalName = "Tala",
            kind = EntityKind.LENDER,
            aliases = setOf("tala"),
            autoReviewEligible = false,
            allowMessageMatch = false
        ),
        CanonicalEntity(
            id = "ke_equity",
            canonicalName = "Equity Bank",
            kind = EntityKind.BANK,
            aliases = setOf(
                "equity",
                "equity bank"
            ),
            autoReviewEligible = false,
            allowMessageMatch = false
        ),
        CanonicalEntity(
            id = "ke_kcb",
            canonicalName = "KCB Bank",
            kind = EntityKind.BANK,
            aliases = setOf(
                "kcb",
                "kcb bank"
            ),
            autoReviewEligible = false,
            allowMessageMatch = false
        )
    )

    override fun resolve(
        candidate: EntityLookupCandidate
    ): EntityResolution? {
        val counterparty =
            normalize(candidate.counterparty.orEmpty())

        if (counterparty.isNotBlank()) {
            bestCounterpartyMatch(counterparty)?.let {
                return EntityResolution(
                    entity = it.first,
                    confidence =
                        EntityMatchConfidence.HIGH,
                    matchedAlias = it.second,
                    source = "local_seed"
                )
            }
        }

        val message = normalize(candidate.rawMessage)
        if (message.isBlank()) {
            return null
        }

        val matches =
            entities.asSequence()
                .filter(CanonicalEntity::allowMessageMatch)
                .flatMap { entity ->
                    entity.aliases.asSequence()
                        .map(::normalize)
                        .filter {
                            it.length >= 4 &&
                                containsPhrase(
                                    message,
                                    it
                                )
                        }
                        .map { alias ->
                            entity to alias
                        }
                }
                .sortedByDescending {
                    it.second.length
                }
                .toList()

        if (matches.isEmpty()) {
            return null
        }

        val distinctEntities =
            matches.map { it.first.id }
                .distinct()
        if (distinctEntities.size != 1) {
            return null
        }

        val best = matches.first()
        return EntityResolution(
            entity = best.first,
            confidence =
                EntityMatchConfidence.LIKELY,
            matchedAlias = best.second,
            source = "local_seed"
        )
    }

    private fun bestCounterpartyMatch(
        normalizedCounterparty: String
    ): Pair<CanonicalEntity, String>? =
        entities.asSequence()
            .flatMap { entity ->
                entity.aliases.asSequence()
                    .map(::normalize)
                    .filter { alias ->
                        alias == normalizedCounterparty ||
                            normalizedCounterparty
                                .startsWith("$alias ") ||
                            normalizedCounterparty
                                .endsWith(" $alias")
                    }
                    .map { alias ->
                        entity to alias
                    }
            }
            .maxByOrNull { it.second.length }

    private fun containsPhrase(
        text: String,
        phrase: String
    ): Boolean =
        text == phrase ||
            text.startsWith("$phrase ") ||
            text.endsWith(" $phrase") ||
            " $phrase " in text

    private fun normalize(
        value: String
    ): String =
        value.lowercase()
            .replace(
                Regex("""[^a-z0-9]+"""),
                " "
            )
            .replace(
                Regex("""\s+"""),
                " "
            )
            .trim()
}

object EntityIntelligence {
    private val providers:
        List<EntityEnrichmentProvider> =
        listOf(
            LocalSeedEntityEnrichmentProvider
        )

    fun resolve(
        counterparty: String?,
        rawMessage: String
    ): EntityResolution? {
        val candidate =
            EntityLookupCandidate(
                counterparty = counterparty,
                rawMessage = rawMessage
            )

        return providers.asSequence()
            .mapNotNull {
                it.resolve(candidate)
            }
            .sortedByDescending {
                when (it.confidence) {
                    EntityMatchConfidence.HIGH -> 2
                    EntityMatchConfidence.LIKELY -> 1
                }
            }
            .firstOrNull()
    }
}
