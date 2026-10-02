package com.kedevjimmy.pesatrack.domain.messagepack

enum class MessageParseRoute {
    TEMPLATE,
    GENERIC_FALLBACK
}

data class MessagePackFieldExtractor(
    val field: String,
    val pattern: String,
    val group: Int = 1,
    val required: Boolean = false
)

data class MessageTemplateDefinition(
    val id: String,
    val revision: Int,
    val country: String,
    val senderIds: List<String>,
    val requiredAnchors: List<String>,
    val anyAnchors: List<String>,
    val eventKind: String,
    val productIdentity: String?,
    val active: Boolean,
    val extractors: List<MessagePackFieldExtractor>,
    val sourcePackages: List<String> = emptyList(),
    val sourceKinds: List<String> = emptyList()
)

data class MessagePackFieldValue(
    val field: String,
    val value: String,
    val startIndex: Int,
    val endIndexExclusive: Int
)

data class MessagePackMatch(
    val templateId: String,
    val revision: Int,
    val eventKind: String,
    val productIdentity: String?,
    val senderMatched: Boolean,
    val matchedAnchors: List<String>,
    val fields: List<MessagePackFieldValue>,
    val sourcePackageMatched: Boolean = false,
    val sourceKindMatched: Boolean = false
) {
    fun fieldValue(name: String): String? =
        fields.firstOrNull {
            it.field.equals(
                name,
                ignoreCase = true
            )
        }?.value
}

data class MessageParseProvenance(
    val route: MessageParseRoute,
    val templateId: String? = null,
    val templateRevision: Int? = null,
    val eventKind: String? = null,
    val productIdentity: String? = null,
    val extractedFields: List<MessagePackFieldValue> =
        emptyList(),
    val reason: String
) {
    companion object {
        fun template(
            match: MessagePackMatch
        ): MessageParseProvenance =
            MessageParseProvenance(
                route = MessageParseRoute.TEMPLATE,
                templateId = match.templateId,
                templateRevision = match.revision,
                eventKind = match.eventKind,
                productIdentity =
                    match.productIdentity,
                extractedFields = match.fields,
                reason =
                    buildString {
                        append(
                            "Matched Message Pack template "
                        )
                        append(
                            match.templateId
                        )
                        append(" revision ")
                        append(
                            match.revision
                        )
                        if (
                            match
                                .sourcePackageMatched
                        ) {
                            append(
                                " using source-package evidence"
                            )
                        }
                        if (
                            match
                                .sourceKindMatched
                        ) {
                            append(
                                " using source-channel evidence"
                            )
                        }
                    }
            )

        fun genericFallback(): MessageParseProvenance =
            MessageParseProvenance(
                route =
                    MessageParseRoute
                        .GENERIC_FALLBACK,
                reason =
                    "No active Message Pack template matched; Financial Intelligence V2 handled the message."
            )
    }
}
