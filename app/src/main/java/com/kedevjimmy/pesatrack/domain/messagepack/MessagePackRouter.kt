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
        body: String
    ): MessagePackMatch? {
        val normalizedSender =
            normalizeSender(title)

        return templates
            .asSequence()
            .mapNotNull { template ->
                evaluate(
                    template = template,
                    normalizedSender =
                        normalizedSender,
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
                    ?.getOrNull(
                        extractor.group
                    )

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
        body: String
    ): MessagePackMatch? =
        router.route(
            title = title,
            body = body
        )
}
