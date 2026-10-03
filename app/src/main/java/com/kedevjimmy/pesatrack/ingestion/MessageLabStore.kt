package com.kedevjimmy.pesatrack.ingestion

import android.content.Context
import com.kedevjimmy.pesatrack.domain.interpretation.FinancialRelevanceSignal
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
    val occurrenceCount: Int,
    val userDecision:
        MessageLabUserDecision? = null,
    val semanticLabel:
        MessageLabSemanticLabel? = null,
    val relevanceScore: Int? = null,
    val relevanceSignals:
        Set<FinancialRelevanceSignal> =
        emptySet()
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
        sourceKind: String? = null,
        relevanceScore: Int? = null,
        relevanceSignals:
            Set<FinancialRelevanceSignal> =
            emptySet()
    ) {
        val identity =
            MessageLabShapeIdentityFactory
                .create(
                    safeSenderLabel =
                        safeSenderLabel,
                    body = body,
                    sourcePackage =
                        sourcePackage,
                    sourceKind =
                        sourceKind
                )
        val shapeId =
            identity.shapeId

        if (
            userDecision(shapeId) ==
            MessageLabUserDecision
                .NOT_FINANCIAL
        ) {
            return
        }

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
                putRelevanceEvidence(
                    item = item,
                    relevanceScore =
                        relevanceScore,
                    relevanceSignals =
                        relevanceSignals
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
                        identity.sender
                    )
                    if (
                        identity.sourcePackage !=
                        null
                    ) {
                        put(
                            "sourcePackage",
                            identity.sourcePackage
                        )
                    } else {
                        put(
                            "sourcePackage",
                            JSONObject.NULL
                        )
                    }
                    if (
                        identity.sourceKind !=
                        null
                    ) {
                        put(
                            "sourceKind",
                            identity.sourceKind
                        )
                    } else {
                        put(
                            "sourceKind",
                            JSONObject.NULL
                        )
                    }
                    put(
                        "redactedBody",
                        identity.redactedBody
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
                    putRelevanceEvidence(
                        item = this,
                        relevanceScore =
                            relevanceScore,
                        relevanceSignals =
                            relevanceSignals
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
    fun refreshUnsupportedEvidence(
        safeSenderLabel: String?,
        body: String,
        sourcePackage: String? = null,
        sourceKind: String? = null,
        relevanceScore: Int?,
        relevanceSignals:
            Set<FinancialRelevanceSignal>
    ): Boolean {
        val identity =
            MessageLabShapeIdentityFactory
                .create(
                    safeSenderLabel =
                        safeSenderLabel,
                    body = body,
                    sourcePackage =
                        sourcePackage,
                    sourceKind =
                        sourceKind
                )
        val current =
            readArray()
        var changed = false

        for (
            index in 0 until
                current.length()
        ) {
            val item =
                current.getJSONObject(
                    index
                )
            if (
                item.optString(
                    "shapeId"
                ) ==
                identity.shapeId
            ) {
                putRelevanceEvidence(
                    item = item,
                    relevanceScore =
                        relevanceScore,
                    relevanceSignals =
                        relevanceSignals
                )
                changed = true
            }
        }

        if (changed) {
            preferences.edit()
                .putString(
                    KEY_SHAPES,
                    current.toString()
                )
                .apply()
        }

        return changed
    }

    @Synchronized
    fun markUnderstood(
        safeSenderLabel: String?,
        body: String,
        sourcePackage: String? = null,
        sourceKind: String? = null
    ): Boolean {
        val identity =
            MessageLabShapeIdentityFactory
                .create(
                    safeSenderLabel =
                        safeSenderLabel,
                    body = body,
                    sourcePackage =
                        sourcePackage,
                    sourceKind =
                        sourceKind
                )

        val current =
            readArray()
        val next =
            JSONArray()
        var removed = 0

        for (
            index in 0 until
                current.length()
        ) {
            val item =
                current.getJSONObject(
                    index
                )
            if (
                item.optString(
                    "shapeId"
                ) ==
                identity.shapeId
            ) {
                removed +=
                    item.optInt(
                        "occurrenceCount",
                        1
                    ).coerceAtLeast(1)
            } else {
                next.put(item)
            }
        }

        if (removed <= 0) {
            return false
        }

        val retiredCount =
            preferences.getInt(
                KEY_RETIRED_COUNT,
                0
            ) + removed

        val decisions =
            readDecisions()
        decisions.remove(
            identity.shapeId
        )

        preferences.edit()
            .putString(
                KEY_SHAPES,
                next.toString()
            )
            .putString(
                KEY_DECISIONS,
                decisions.toString()
            )
            .putInt(
                KEY_RETIRED_COUNT,
                retiredCount
            )
            .apply()

        return true
    }

    @Synchronized
    fun setUserDecision(
        shapeId: String,
        decision:
            MessageLabUserDecision
    ): Boolean {
        if (
            shapeId.isBlank() ||
            shapeById(shapeId) == null
        ) {
            return false
        }

        val decisions =
            readDecisions()
        val existing =
            decisions.optJSONObject(
                shapeId
            )
        decisions.put(
            shapeId,
            JSONObject().apply {
                put(
                    "decision",
                    decision.name
                )
                put(
                    "decidedAtEpochMs",
                    System.currentTimeMillis()
                )
                existing
                    ?.optString(
                        "semanticLabel"
                    )
                    ?.takeIf(
                        String::isNotBlank
                    )
                    ?.let {
                        put(
                            "semanticLabel",
                            it
                        )
                    }
            }
        )
        preferences.edit()
            .putString(
                KEY_DECISIONS,
                decisions.toString()
            )
            .apply()
        return true
    }

    @Synchronized
    fun clearUserDecision(
        shapeId: String
    ): Boolean {
        val decisions =
            readDecisions()
        if (!decisions.has(shapeId)) {
            return false
        }
        decisions.remove(shapeId)
        preferences.edit()
            .putString(
                KEY_DECISIONS,
                decisions.toString()
            )
            .apply()
        return true
    }

    @Synchronized
    fun setSemanticLabel(
        shapeId: String,
        label:
            MessageLabSemanticLabel
    ): Boolean {
        if (
            userDecision(shapeId) !=
            MessageLabUserDecision
                .CONFIRMED_FINANCIAL
        ) {
            return false
        }

        val decisions =
            readDecisions()
        val existing =
            decisions.optJSONObject(
                shapeId
            ) ?: return false

        existing.put(
            "semanticLabel",
            label.name
        )
        existing.put(
            "semanticLabelAtEpochMs",
            System.currentTimeMillis()
        )

        preferences.edit()
            .putString(
                KEY_DECISIONS,
                decisions.toString()
            )
            .apply()

        return true
    }

    @Synchronized
    fun clearSemanticLabel(
        shapeId: String
    ): Boolean {
        val decisions =
            readDecisions()
        val existing =
            decisions.optJSONObject(
                shapeId
            ) ?: return false

        if (
            !existing.has(
                "semanticLabel"
            )
        ) {
            return false
        }

        existing.remove(
            "semanticLabel"
        )
        existing.remove(
            "semanticLabelAtEpochMs"
        )

        preferences.edit()
            .putString(
                KEY_DECISIONS,
                decisions.toString()
            )
            .apply()

        return true
    }

    fun semanticLabel(
        shapeId: String
    ): MessageLabSemanticLabel? {
        if (shapeId.isBlank()) {
            return null
        }
        val item =
            readDecisions()
                .optJSONObject(
                    shapeId
                )
                ?: return null
        return runCatching {
            MessageLabSemanticLabel
                .valueOf(
                    item.optString(
                        "semanticLabel"
                    )
                )
        }.getOrNull()
    }

    fun userDecision(
        shapeId: String
    ): MessageLabUserDecision? {
        if (shapeId.isBlank()) {
            return null
        }
        val item =
            readDecisions()
                .optJSONObject(
                    shapeId
                )
                ?: return null
        return runCatching {
            MessageLabUserDecision
                .valueOf(
                    item.optString(
                        "decision"
                    )
                )
        }.getOrNull()
    }

    @Synchronized
    fun inspectionMatch(
        safeSenderLabel: String?,
        body: String
    ): MessageLabInspectionMatch {
        if (body.isBlank()) {
            return MessageLabInspectionMatchResolver
                .resolve(
                    emptyList()
                )
        }

        val identity =
            MessageLabShapeIdentityFactory
                .create(
                    safeSenderLabel =
                        safeSenderLabel,
                    body = body,
                    sourcePackage = null,
                    sourceKind = null
                )

        val candidates =
            allStoredShapes()
                .filter {
                    it.sender.equals(
                        identity.sender,
                        ignoreCase = true
                    ) &&
                        it.redactedBody ==
                        identity.redactedBody
                }

        return MessageLabInspectionMatchResolver
            .resolve(candidates)
    }

    fun ignoredShapeCount(): Int =
        allStoredShapes()
            .count {
                it.userDecision ==
                    MessageLabUserDecision
                        .NOT_FINANCIAL
            }

    fun retiredOccurrenceCount(): Int =
        preferences.getInt(
            KEY_RETIRED_COUNT,
            0
        )

    @Synchronized
    fun recentUnsupported(
        limit: Int = MAX_SHAPES
    ): List<UnsupportedMessageShape> =
        allStoredShapes()
            .asReversed()
            .filter {
                it.userDecision !=
                    MessageLabUserDecision
                        .NOT_FINANCIAL
            }
            .take(
                limit.coerceIn(
                    1,
                    MAX_SHAPES
                )
            )

    @Synchronized
    fun prioritizedUnsupported(
        limit: Int = MAX_SHAPES
    ): List<UnsupportedMessageShape> =
        MessageLabEvidenceQueue
            .prioritize(
                allStoredShapes()
                    .filter {
                        it.userDecision !=
                            MessageLabUserDecision
                                .NOT_FINANCIAL
                    }
            )
            .take(
                limit.coerceIn(
                    1,
                    MAX_SHAPES
                )
            )

    @Synchronized
    fun confirmedFinancial(
        limit: Int = MAX_SHAPES
    ): List<UnsupportedMessageShape> =
        MessageLabEvidenceQueue
            .confirmedFinancial(
                allStoredShapes()
            )
            .take(
                limit.coerceIn(
                    1,
                    MAX_SHAPES
                )
            )

    fun confirmedFinancialCount(): Int =
        confirmedFinancial(
            MAX_SHAPES
        ).size

    fun semanticallyLabeledConfirmedCount():
        Int =
        confirmedFinancial(
            MAX_SHAPES
        ).count {
            it.semanticLabel != null
        }

    @Synchronized
    fun parserReadinessSnapshot():
        MessageLabParserReadinessSnapshot =
        MessageLabParserReadiness
            .snapshot(
                allStoredShapes()
            )

    @Synchronized
    fun evidenceReadyParserCandidates(
        limit: Int = MAX_SHAPES
    ): List<UnsupportedMessageShape> =
        MessageLabParserReadiness
            .evidenceReady(
                allStoredShapes()
            )
            .take(
                limit.coerceIn(
                    1,
                    MAX_SHAPES
                )
            )

    fun exportEvidenceReadyParserCandidates(
        limit: Int = 50
    ): String? {
        val shapes =
            evidenceReadyParserCandidates(
                limit
            )
        if (shapes.isEmpty()) {
            return null
        }

        return JSONArray().apply {
            shapes.forEach {
                shape ->
                put(
                    developerFixtureJson(
                        shape
                    )
                )
            }
        }.toString(2)
    }

    fun exportConfirmedFinancialFixtures(
        limit: Int = 50
    ): String? {
        val shapes =
            confirmedFinancial(
                limit
            )
        if (shapes.isEmpty()) {
            return null
        }

        return JSONArray().apply {
            shapes.forEach { shape ->
                put(
                    developerFixtureJson(
                        shape
                    )
                )
            }
        }.toString(2)
    }

    @Synchronized
    fun recentIgnored(
        limit: Int = MAX_SHAPES
    ): List<UnsupportedMessageShape> =
        allStoredShapes()
            .asReversed()
            .filter {
                it.userDecision ==
                    MessageLabUserDecision
                        .NOT_FINANCIAL
            }
            .take(
                limit.coerceIn(
                    1,
                    MAX_SHAPES
                )
            )

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

        return developerFixtureJson(
            shape
        ).toString(2)
    }

    fun recentUnsupportedForSource(
        sourceKey: String,
        limit: Int = MAX_SHAPES
    ): List<UnsupportedMessageShape> =
        recentUnsupported(
            MAX_SHAPES
        )
            .filter {
                FinancialCoverageAnalysis
                    .sourceKey(
                        sourceKind =
                            it.sourceKind,
                        sourcePackage =
                            it.sourcePackage,
                        displayName =
                            it.sender
                    ) ==
                    sourceKey
            }
            .take(
                limit.coerceIn(
                    1,
                    MAX_SHAPES
                )
            )

    fun exportDeveloperFixturesForSource(
        sourceKey: String,
        limit: Int = 25
    ): String? {
        val shapes =
            recentUnsupportedForSource(
                sourceKey = sourceKey,
                limit = limit
            )

        if (shapes.isEmpty()) {
            return null
        }

        return JSONArray().apply {
            shapes.forEach { shape ->
                put(
                    developerFixtureJson(
                        shape
                    )
                )
            }
        }.toString(2)
    }

    private fun developerFixtureJson(
        shape: UnsupportedMessageShape
    ): JSONObject =
        JSONObject().apply {
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
                "parserEvidenceReadiness",
                MessageLabParserReadiness
                    .stateFor(shape)
                    .name
            )
            put(
                "userConfirmedFinancial",
                shape.userDecision ==
                    MessageLabUserDecision
                        .CONFIRMED_FINANCIAL
            )
            shape.semanticLabel
                ?.let {
                    put(
                        "userSemanticLabel",
                        it.name
                    )
                    put(
                        "userSemanticLabelDisplay",
                        it.displayLabel
                    )
                }
            put(
                "occurrenceCount",
                shape.occurrenceCount
            )
            shape.relevanceScore
                ?.let {
                    put(
                        "relevanceScore",
                        it
                    )
                }
            put(
                "relevanceSignals",
                JSONArray().apply {
                    shape.relevanceSignals
                        .map {
                            it.name
                        }
                        .sorted()
                        .forEach {
                            signalName ->
                            put(signalName)
                        }
                }
            )
            put(
                "notes",
                "Redacted locally by PesaTrack Message Lab. Review before contributing."
            )
        }

    private fun shapeById(
        shapeId: String
    ): UnsupportedMessageShape? =
        allStoredShapes()
            .firstOrNull {
                it.shapeId ==
                    shapeId
            }

    private fun allStoredShapes():
        List<UnsupportedMessageShape> {
        val array =
            readArray()
        val decisions =
            readDecisions()

        return buildList {
            for (
                index in 0 until
                    array.length()
            ) {
                val item =
                    array.getJSONObject(
                        index
                    )
                val shapeId =
                    item.optString(
                        "shapeId"
                    )
                add(
                    UnsupportedMessageShape(
                        shapeId =
                            shapeId,
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
                            ),
                        userDecision =
                            decisions
                                .optJSONObject(
                                    shapeId
                                )
                                ?.optString(
                                    "decision"
                                )
                                ?.takeIf(
                                    String::isNotBlank
                                )
                                ?.let {
                                    runCatching {
                                        MessageLabUserDecision
                                            .valueOf(
                                                it
                                            )
                                    }.getOrNull()
                                }
,
                        semanticLabel =
                            decisions
                                .optJSONObject(
                                    shapeId
                                )
                                ?.optString(
                                    "semanticLabel"
                                )
                                ?.takeIf(
                                    String::isNotBlank
                                )
                                ?.let {
                                    runCatching {
                                        MessageLabSemanticLabel
                                            .valueOf(
                                                it
                                            )
                                    }.getOrNull()
                                },
                        relevanceScore =
                            if (
                                item.has(
                                    "relevanceScore"
                                ) &&
                                !item.isNull(
                                    "relevanceScore"
                                )
                            ) {
                                item.optInt(
                                    "relevanceScore"
                                )
                            } else {
                                null
                            },
                        relevanceSignals =
                            item.optJSONArray(
                                "relevanceSignals"
                            )?.let {
                                array ->
                                buildSet {
                                    for (
                                        signalIndex in
                                            0 until
                                                array.length()
                                    ) {
                                        runCatching {
                                            FinancialRelevanceSignal
                                                .valueOf(
                                                    array.optString(
                                                        signalIndex
                                                    )
                                                )
                                        }.getOrNull()
                                            ?.let(::add)
                                    }
                                }
                            }.orEmpty()
                    )
                )
            }
        }
    }

    private fun putRelevanceEvidence(
        item: JSONObject,
        relevanceScore: Int?,
        relevanceSignals:
            Set<FinancialRelevanceSignal>
    ) {
        if (relevanceScore != null) {
            item.put(
                "relevanceScore",
                relevanceScore
            )
        }
        if (relevanceSignals.isNotEmpty()) {
            item.put(
                "relevanceSignals",
                JSONArray().apply {
                    relevanceSignals
                        .map {
                            it.name
                        }
                        .sorted()
                        .forEach {
                            signalName ->
                            put(signalName)
                        }
                }
            )
        }
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

    private fun readDecisions(): JSONObject =
        runCatching {
            JSONObject(
                preferences.getString(
                    KEY_DECISIONS,
                    "{}"
                ) ?: "{}"
            )
        }.getOrElse {
            JSONObject()
        }

    companion object {
        private const val PREFS_NAME =
            "message_lab_v1"
        private const val KEY_SHAPES =
            "unsupported_shapes"
        private const val KEY_RETIRED_COUNT =
            "retired_occurrence_count"
        private const val KEY_DECISIONS =
            "user_shape_decisions"
        private const val MAX_SHAPES = 100

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
