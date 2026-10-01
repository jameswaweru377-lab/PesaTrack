package com.kedevjimmy.pesatrack.domain.messagepack

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.InputStreamReader

data class MessagePackLoadResult(
    val templates: List<MessageTemplateDefinition>,
    val errors: List<String>
)

object BundledKenyaMessagePackLoader {
    private const val INDEX_RESOURCE =
        "message-packs/ke/index.json"
    private const val SUPPORTED_SCHEMA_VERSION = 1

    fun load(
        classLoader: ClassLoader =
            BundledKenyaMessagePackLoader::class.java
                .classLoader
                ?: ClassLoader
                    .getSystemClassLoader()
    ): MessagePackLoadResult {
        val errors = mutableListOf<String>()
        val index =
            readObject(
                classLoader = classLoader,
                path = INDEX_RESOURCE
            ) ?: return MessagePackLoadResult(
                templates = emptyList(),
                errors =
                    listOf(
                        "Missing $INDEX_RESOURCE"
                    )
            )

        val schemaVersion =
            index.get("schemaVersion")
                ?.asInt
        if (
            schemaVersion !=
            SUPPORTED_SCHEMA_VERSION
        ) {
            return MessagePackLoadResult(
                templates = emptyList(),
                errors =
                    listOf(
                        "Unsupported message-pack schema " +
                            "$schemaVersion"
                    )
            )
        }

        val templatePaths =
            index.getAsJsonArray("templates")
                ?.mapNotNull { item ->
                    runCatching {
                        item.asString
                            .trim()
                            .takeIf(
                                String::isNotBlank
                            )
                    }.getOrNull()
                }
                .orEmpty()

        val loaded =
            templatePaths.mapNotNull { path ->
                val fullPath =
                    if (
                        path.startsWith(
                            "message-packs/"
                        )
                    ) {
                        path
                    } else {
                        "message-packs/ke/$path"
                    }

                runCatching {
                    parseTemplate(
                        readObject(
                            classLoader,
                            fullPath
                        ) ?: error(
                            "Missing $fullPath"
                        )
                    )
                }.onFailure {
                    errors +=
                        "$fullPath: " +
                            (
                                it.message
                                    ?: "invalid template"
                            )
                }.getOrNull()
            }

        val duplicateRevisionKeys =
            loaded.groupBy {
                it.id to it.revision
            }.filterValues {
                it.size > 1
            }.keys

        duplicateRevisionKeys.forEach {
            errors +=
                "Duplicate template revision: " +
                    it.first +
                    "@" +
                    it.second
        }

        val active =
            loaded.groupBy(
                MessageTemplateDefinition::id
            ).mapNotNull { (_, revisions) ->
                revisions.maxByOrNull {
                    it.revision
                }?.takeIf {
                    it.active
                }
            }.sortedBy {
                it.id
            }

        return MessagePackLoadResult(
            templates = active,
            errors = errors
        )
    }

    private fun parseTemplate(
        root: JsonObject
    ): MessageTemplateDefinition {
        val id =
            root.requiredString("id")
        val revision =
            root.get("revision")
                ?.asInt
                ?: error(
                    "$id is missing revision"
                )
        require(revision > 0) {
            "$id revision must be positive"
        }

        val extractors =
            root.getAsJsonArray("extractors")
                ?.map { item ->
                    val value =
                        item.asJsonObject
                    val pattern =
                        value.requiredString(
                            "pattern"
                        )

                    // Fail at load time rather than at message
                    // ingestion time if a data-pack regex is
                    // malformed.
                    Regex(
                        pattern,
                        setOf(
                            RegexOption.IGNORE_CASE,
                            RegexOption.MULTILINE
                        )
                    )

                    MessagePackFieldExtractor(
                        field =
                            value.requiredString(
                                "field"
                            ),
                        pattern = pattern,
                        group =
                            value.get("group")
                                ?.asInt
                                ?: 1,
                        required =
                            value.get("required")
                                ?.asBoolean
                                ?: false
                    )
                }
                .orEmpty()

        return MessageTemplateDefinition(
            id = id,
            revision = revision,
            country =
                root.get("country")
                    ?.asString
                    ?.trim()
                    ?.ifBlank { "KE" }
                    ?: "KE",
            senderIds =
                root.stringArray(
                    "senderIds"
                ),
            requiredAnchors =
                root.stringArray(
                    "requiredAnchors"
                ),
            anyAnchors =
                root.stringArray(
                    "anyAnchors"
                ),
            eventKind =
                root.requiredString(
                    "eventKind"
                ),
            productIdentity =
                root.get("productIdentity")
                    ?.takeUnless {
                        it.isJsonNull
                    }
                    ?.asString
                    ?.trim()
                    ?.takeIf(
                        String::isNotBlank
                    ),
            active =
                root.get("active")
                    ?.asBoolean
                    ?: true,
            extractors = extractors
        )
    }

    private fun readObject(
        classLoader: ClassLoader,
        path: String
    ): JsonObject? =
        classLoader.getResourceAsStream(path)
            ?.use { stream ->
                InputStreamReader(
                    stream,
                    Charsets.UTF_8
                ).use { reader ->
                    JsonParser
                        .parseReader(reader)
                        .asJsonObject
                }
            }

    private fun JsonObject.requiredString(
        key: String
    ): String =
        get(key)
            ?.takeUnless {
                it.isJsonNull
            }
            ?.asString
            ?.trim()
            ?.takeIf(
                String::isNotBlank
            )
            ?: error(
                "Missing required field '$key'"
            )

    private fun JsonObject.stringArray(
        key: String
    ): List<String> =
        getAsJsonArray(key)
            ?.mapNotNull { item ->
                runCatching {
                    item.asString
                        .trim()
                        .takeIf(
                            String::isNotBlank
                        )
                }.getOrNull()
            }
            .orEmpty()
}
