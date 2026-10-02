package com.kedevjimmy.pesatrack.domain.messagepack

class MessagePackRouter(
    templates: List<MessageTemplateDefinition>
) {
    private val templates =
        templates.filter {
            it.active
        }

    fun route(
        title: String?,
        body: String,
        sourcePackage: String? = null,
        sourceKind: String? = null
    ): MessagePackMatch? {
        val normalizedSender =
            normalizeSender(title)
        val normalizedSourcePackage =
            normalizePackage(
                sourcePackage
            )
        val normalizedSourceKind =
            normalizeSourceKind(
                sourceKind
            )

        return templates
            .asSequence()
            .mapNotNull { template ->
                evaluate(
                    template = template,
                    normalizedSender =
                        normalizedSender,
                    normalizedSourcePackage =
                        normalizedSourcePackage,
                    normalizedSourceKind =
                        normalizedSourceKind,
                    body = body
                )
            }
            .sortedWith(
                compareByDescending<ScoredMatch> {
                    it.score
                }.thenByDescending {
                    it.match.revision
                }.thenBy {
                    it.match.templateId
                }
            )
            .firstOrNull()
            ?.match
    }

    private fun evaluate(
        template: MessageTemplateDefinition,
        normalizedSender: String,
        normalizedSourcePackage: String,
        normalizedSourceKind: String,
        body: String
    ): ScoredMatch? {
        val normalizedAllowedSenders =
            template.senderIds
                .map(::normalizeSender)
                .filter(
                    String::isNotBlank
                )
                .toSet()

        val senderMatched =
            normalizedAllowedSenders.isEmpty() ||
                normalizedSender in
                normalizedAllowedSenders

        if (!senderMatched) {
            return null
        }

        val normalizedAllowedPackages =
            template.sourcePackages
                .map(::normalizePackage)
                .filter(
                    String::isNotBlank
                )
                .toSet()
        val sourcePackageMatched =
            normalizedAllowedPackages
                .isEmpty() ||
                normalizedSourcePackage in
                normalizedAllowedPackages

        if (!sourcePackageMatched) {
            return null
        }

        val normalizedAllowedKinds =
            template.sourceKinds
                .map(::normalizeSourceKind)
                .filter(
                    String::isNotBlank
                )
                .toSet()
        val sourceKindMatched =
            normalizedAllowedKinds
                .isEmpty() ||
                normalizedSourceKind in
                normalizedAllowedKinds

        if (!sourceKindMatched) {
            return null
        }

        val requiredMatched =
            template.requiredAnchors
                .all {
                    body.contains(
                        it,
                        ignoreCase = true
                    )
                }
        if (!requiredMatched) {
            return null
        }

        val matchedAnyAnchors =
            template.anyAnchors
                .filter {
                    body.contains(
                        it,
                        ignoreCase = true
                    )
                }

        if (
            template.anyAnchors.isNotEmpty() &&
            matchedAnyAnchors.isEmpty()
        ) {
            return null
        }

        val fields =
            mutableListOf<
                MessagePackFieldValue
            >()

        template.extractors.forEach {
            extractor ->
            val regex =
                Regex(
                    extractor.pattern,
                    setOf(
                        RegexOption.IGNORE_CASE,
                        RegexOption.MULTILINE
                    )
                )
            val match =
                regex.find(body)
            val group =
                match?.groups
                    ?.let { groups ->
                        if (
                            extractor.group >= 0 &&
                            extractor.group <
                                groups.size
                        ) {
                            groups[
                                extractor.group
                            ]
                        } else {
                            null
                        }
                    }

            if (
                extractor.required &&
                (
                    match == null ||
                        group == null
                    )
            ) {
                return null
            }

            if (group != null) {
                fields +=
                    MessagePackFieldValue(
                        field =
                            extractor.field,
                        value =
                            group.value.trim(),
                        startIndex =
                            group.range.first,
                        endIndexExclusive =
                            group.range.last + 1
                    )
            }
        }

        val matchedAnchors =
            template.requiredAnchors +
                matchedAnyAnchors

        // Prefer the most specific data shape:
        // explicit sender + longer/more anchors +
        // successful field extraction.
        val score =
            (
                if (
                    normalizedAllowedPackages
                        .isNotEmpty()
                ) {
                    20_000
                } else {
                    0
                }
            ) +
                (
                    if (
                        normalizedAllowedKinds
                            .isNotEmpty()
                    ) {
                        15_000
                    } else {
                        0
                    }
                ) +
                (
                    if (
                        normalizedAllowedSenders
                            .isNotEmpty()
                    ) {
                        10_000
                    } else {
                        0
                    }
                ) +
                matchedAnchors.sumOf {
                    it.length
                } * 10 +
                fields.size

        return ScoredMatch(
            match =
                MessagePackMatch(
                    templateId =
                        template.id,
                    revision =
                        template.revision,
                    eventKind =
                        template.eventKind,
                    productIdentity =
                        template.productIdentity,
                    senderMatched =
                        senderMatched,
                    sourcePackageMatched =
                        normalizedAllowedPackages
                            .isNotEmpty(),
                    sourceKindMatched =
                        normalizedAllowedKinds
                            .isNotEmpty(),
                    matchedAnchors =
                        matchedAnchors,
                    fields = fields
                ),
            score = score
        )
    }

    private fun normalizeSender(
        raw: String?
    ): String =
        raw.orEmpty()
            .uppercase()
            .filter(
                Char::isLetterOrDigit
            )

    private fun normalizePackage(
        raw: String?
    ): String =
        raw.orEmpty()
            .trim()
            .lowercase()

    private fun normalizeSourceKind(
        raw: String?
    ): String =
        raw.orEmpty()
            .trim()
            .lowercase()

    private data class ScoredMatch(
        val match: MessagePackMatch,
        val score: Int
    )
}

object KenyaMessagePack {
    private val loadResult by lazy {
        BundledKenyaMessagePackLoader.load()
    }

    private val router by lazy {
        MessagePackRouter(
            loadResult.templates
        )
    }

    val templateCount: Int
        get() =
            loadResult.templates.size

    val loadErrors: List<String>
        get() =
            loadResult.errors

    fun route(
        title: String?,
        body: String,
        sourcePackage: String? = null,
        sourceKind: String? = null
    ): MessagePackMatch? =
        router.route(
            title = title,
            body = body,
            sourcePackage =
                sourcePackage,
            sourceKind =
                sourceKind
        )
}
