package com.kedevjimmy.pesatrack.ingestion

import android.content.Context
import com.kedevjimmy.pesatrack.domain.messagepack.MessageLabRedactor
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject

data class UnsupportedMessageShape(
    val shapeId: String,
    val sender: String,
    val sourcePackage: String?,
    val sourceKind: String?,
    val redactedBody: String,
    val firstSeenAtEpochMs: Long,
    val lastSeenAtEpochMs: Long,
    val occurrenceCount: Int
)

class MessageLabStore private constructor(
    context: Context
) {
    private val preferences =
        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

    @Synchronized
    fun recordUnsupported(
        safeSenderLabel: String?,
        body: String,
        observedAtEpochMs: Long,
        sourcePackage: String? = null,
        sourceKind: String? = null
    ) {
        val redacted =
            MessageLabRedactor.redact(
                safeSenderLabel =
                    safeSenderLabel,
                body = body
            )

        val safeSourcePackage =
            sourcePackage
                ?.trim()
                ?.takeIf(
                    String::isNotBlank
                )
                ?.take(
                    MAX_PACKAGE_LENGTH
                )
        val safeSourceKind =
            sourceKind
                ?.trim()
                ?.takeIf(
                    String::isNotBlank
                )
                ?.take(
                    MAX_SOURCE_KIND_LENGTH
                )

        val shapeId =
            fingerprint(
                listOf(
                    safeSourceKind.orEmpty(),
                    safeSourcePackage.orEmpty(),
                    redacted.sender,
                    redacted.body
                ).joinToString("\n")
            )

        val current =
            readArray()
        val next =
            JSONArray()
        var matched = false

        val start =
            maxOf(
                0,
                current.length() -
                    (MAX_SHAPES - 1)
            )

        for (
            index in start until
                current.length()
        ) {
            val item =
                current.getJSONObject(
                    index
                )
            if (
                item.optString(
                    "shapeId"
                ) == shapeId
            ) {
                matched = true
                item.put(
                    "lastSeenAtEpochMs",
                    observedAtEpochMs
                )
                item.put(
                    "occurrenceCount",
                    item.optInt(
                        "occurrenceCount",
                        1
                    ) + 1
                )
            }
            next.put(item)
        }

        if (!matched) {
            next.put(
                JSONObject().apply {
                    put(
                        "shapeId",
                        shapeId
                    )
                    put(
                        "sender",
                        redacted.sender
                    )
                    if (
                        safeSourcePackage !=
                        null
                    ) {
                        put(
                            "sourcePackage",
                            safeSourcePackage
                        )
                    } else {
                        put(
                            "sourcePackage",
                            JSONObject.NULL
                        )
                    }
                    if (
                        safeSourceKind !=
                        null
                    ) {
                        put(
                            "sourceKind",
                            safeSourceKind
                        )
                    } else {
                        put(
                            "sourceKind",
                            JSONObject.NULL
                        )
                    }
                    put(
                        "redactedBody",
                        redacted.body
                    )
                    put(
                        "firstSeenAtEpochMs",
                        observedAtEpochMs
                    )
                    put(
                        "lastSeenAtEpochMs",
                        observedAtEpochMs
                    )
                    put(
                        "occurrenceCount",
                        1
                    )
                }
            )
        }

        preferences.edit()
            .putString(
                KEY_SHAPES,
                next.toString()
            )
            .apply()
    }

    @Synchronized
    fun recentUnsupported(
        limit: Int = MAX_SHAPES
    ): List<UnsupportedMessageShape> {
        val array =
            readArray()
        val bounded =
            limit.coerceIn(
                1,
                MAX_SHAPES
            )
        val start =
            maxOf(
                0,
                array.length() -
                    bounded
            )

        return buildList {
            for (
                index in
                    array.length() - 1
                        downTo start
            ) {
                val item =
                    array.getJSONObject(
                        index
                    )
                add(
                    UnsupportedMessageShape(
                        shapeId =
                            item.optString(
                                "shapeId"
                            ),
                        sender =
                            item.optString(
                                "sender",
                                "<SENDER>"
                            ),
                        sourcePackage =
                            if (
                                item.isNull(
                                    "sourcePackage"
                                )
                            ) {
                                null
                            } else {
                                item.optString(
                                    "sourcePackage"
                                )
                                    .trim()
                                    .takeIf(
                                        String::isNotBlank
                                    )
                            },
                        sourceKind =
                            if (
                                item.isNull(
                                    "sourceKind"
                                )
                            ) {
                                null
                            } else {
                                item.optString(
                                    "sourceKind"
                                )
                                    .trim()
                                    .takeIf(
                                        String::isNotBlank
                                    )
                            },
                        redactedBody =
                            item.optString(
                                "redactedBody"
                            ),
                        firstSeenAtEpochMs =
                            item.optLong(
                                "firstSeenAtEpochMs"
                            ),
                        lastSeenAtEpochMs =
                            item.optLong(
                                "lastSeenAtEpochMs"
                            ),
                        occurrenceCount =
                            item.optInt(
                                "occurrenceCount",
                                1
                            )
                    )
                )
            }
        }
    }

    fun exportDeveloperFixture(
        shapeId: String
    ): String? {
        val shape =
            recentUnsupported(
                MAX_SHAPES
            ).firstOrNull {
                it.shapeId ==
                    shapeId
            } ?: return null

        return JSONObject().apply {
            put(
                "id",
                "unsupported_" +
                    shape.shapeId
                        .take(12)
            )
            put(
                "title",
                shape.sender
            )
            put(
                "body",
                shape.redactedBody
            )
            shape.sourcePackage
                ?.let {
                    put(
                        "sourcePackage",
                        it
                    )
                }
            shape.sourceKind
                ?.let {
                    put(
                        "sourceKind",
                        it
                    )
                }
            put(
                "expectedRoute",
                "UNSUPPORTED"
            )
            put(
                "notes",
                "Redacted locally by PesaTrack Message Lab. Review before contributing."
            )
        }.toString(2)
    }

    private fun readArray(): JSONArray =
        runCatching {
            JSONArray(
                preferences.getString(
                    KEY_SHAPES,
                    "[]"
                ) ?: "[]"
            )
        }.getOrElse {
            JSONArray()
        }

    private fun fingerprint(
        value: String
    ): String {
        val digest =
            MessageDigest
                .getInstance("SHA-256")
                .digest(
                    value.toByteArray(
                        Charsets.UTF_8
                    )
                )
        return digest.joinToString("") {
            byte ->
            "%02x".format(byte)
        }
    }

    companion object {
        private const val PREFS_NAME =
            "message_lab_v1"
        private const val KEY_SHAPES =
            "unsupported_shapes"
        private const val MAX_SHAPES = 100
        private const val MAX_PACKAGE_LENGTH = 120
        private const val MAX_SOURCE_KIND_LENGTH = 80

        @Volatile
        private var instance:
            MessageLabStore? = null

        fun get(
            context: Context
        ): MessageLabStore =
            instance ?: synchronized(this) {
                instance
                    ?: MessageLabStore(
                        context.applicationContext
                    ).also {
                        instance = it
                    }
            }
    }
}
