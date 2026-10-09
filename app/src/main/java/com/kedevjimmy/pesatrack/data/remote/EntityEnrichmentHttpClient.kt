package com.kedevjimmy.pesatrack.data.remote

import com.kedevjimmy.pesatrack.BuildConfig
import com.kedevjimmy.pesatrack.domain.intelligence.EntityMatchConfidence
import com.kedevjimmy.pesatrack.domain.intelligence.RemoteEntityEnrichment
import com.kedevjimmy.pesatrack.domain.intelligence.RemoteOrganizationRole
import com.kedevjimmy.pesatrack.domain.intelligence.SalaryCandidateSuitability
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.json.JSONObject

sealed interface EntityEnrichmentFetchResult {
    data class Resolved(
        val value: RemoteEntityEnrichment
    ) : EntityEnrichmentFetchResult

    data object NotFound :
        EntityEnrichmentFetchResult

    data class Retryable(
        val reason: String
    ) : EntityEnrichmentFetchResult

    data class PermanentFailure(
        val reason: String
    ) : EntityEnrichmentFetchResult
}

class EntityEnrichmentHttpClient(
    private val endpoint:
        String = BuildConfig.ENTITY_ENRICHMENT_URL,
    private val clientId: String? = null,
    private val openConnection: (URL) -> HttpURLConnection = {
        it.openConnection() as HttpURLConnection
    }
) {
    fun isConfigured(): Boolean =
        endpoint.startsWith(
            "https://",
            ignoreCase = true
        )

    fun enrich(
        sanitizedDescriptor: String
    ): RemoteEntityEnrichment? =
        when (
            val result =
                fetch(
                    sanitizedDescriptor
                )
        ) {
            is EntityEnrichmentFetchResult
                .Resolved ->
                result.value
            else -> null
        }

    fun fetch(
        sanitizedDescriptor: String
    ): EntityEnrichmentFetchResult {
        if (!isConfigured()) {
            return EntityEnrichmentFetchResult
                .PermanentFailure(
                    "endpoint_not_configured"
                )
        }

        val connection =
            runCatching {
                (
                    openConnection(URL(endpoint))
                    ).apply {
                        requestMethod = "POST"
                        connectTimeout = 5_000
                        readTimeout = 7_000
                        doOutput = true
                        setRequestProperty(
                            "Content-Type",
                            "application/json"
                        )
                        setRequestProperty(
                            "Accept",
                            "application/json"
                        )
                        clientId
                            ?.trim()
                            ?.takeIf {
                                CLIENT_ID_REGEX
                                    .matches(it)
                            }
                            ?.let {
                                setRequestProperty(
                                    "X-PesaTrack-Client-Id",
                                    it
                                )
                            }
                    }
            }.getOrElse {
                return EntityEnrichmentFetchResult
                    .Retryable(
                        "connection_setup"
                    )
            }

        val deadlineNanos = System.nanoTime() +
            TimeUnit.MILLISECONDS.toNanos(REQUEST_DEADLINE_MS)
        val deadlineTask = deadlineScheduler.schedule(
            { connection.disconnect() },
            REQUEST_DEADLINE_MS,
            TimeUnit.MILLISECONDS
        )
        return try {
            val request =
                JSONObject()
                    .put(
                        "descriptor",
                        sanitizedDescriptor
                    )
                    .toString()

            connection.outputStream.use {
                output ->
                output.write(
                    request.toByteArray(
                        Charsets.UTF_8
                    )
                )
            }

            val responseCode =
                connection.responseCode
            BoundedEnrichmentResponse.requireBeforeDeadline(deadlineNanos)

            when {
                responseCode in 200..299 -> {
                    if (connection.contentLengthLong > MAX_RESPONSE_BYTES) {
                        return EntityEnrichmentFetchResult.PermanentFailure("response_too_large")
                    }
                    val response = connection.inputStream.use {
                        BoundedEnrichmentResponse.read(it, MAX_RESPONSE_BYTES, deadlineNanos)
                    }
                    val parsed =
                        parseResponse(response)
                    BoundedEnrichmentResponse.requireBeforeDeadline(deadlineNanos)
                    if (parsed == null) {
                        EntityEnrichmentFetchResult
                            .PermanentFailure(
                                "invalid_response"
                            )
                    } else {
                        EntityEnrichmentFetchResult
                            .Resolved(parsed)
                    }
                }

                responseCode == 404 ->
                    EntityEnrichmentFetchResult
                        .NotFound

                responseCode == 408 ||
                    responseCode == 429 ||
                    responseCode >= 500 ->
                    EntityEnrichmentFetchResult
                        .Retryable(
                            "http_" +
                                responseCode
                        )

                else ->
                    EntityEnrichmentFetchResult
                        .PermanentFailure(
                            "http_" +
                                responseCode
                        )
            }
        } catch (
            _: java.io.IOException
        ) {
            EntityEnrichmentFetchResult
                .Retryable("io")
        } finally {
            deadlineTask.cancel(false)
            connection.disconnect()
        }
    }

    internal fun parseResponse(
        raw: String
    ): RemoteEntityEnrichment? {
        return runCatching {
            val json = JSONObject(raw)
            val canonicalName =
                json.optString(
                    "canonicalName"
                ).trim()
            if (
                canonicalName.length !in
                2..80
            ) {
                return null
            }

            val confidence =
                runCatching {
                    EntityMatchConfidence
                        .valueOf(
                            json.optString(
                                "confidence"
                            )
                        )
                }.getOrDefault(
                    EntityMatchConfidence
                        .LIKELY
                )

            val category =
                json.optString(
                    "categoryHint"
                )
                    .takeIf(
                        String::isNotBlank
                    )
                    ?.let {
                        runCatching {
                            TransactionCategory
                                .valueOf(it)
                        }.getOrNull()
                    }

            RemoteEntityEnrichment(
                canonicalName =
                    canonicalName,
                domain =
                    json.optString(
                        "domain"
                    )
                        .trim()
                        .takeIf(
                            String::isNotBlank
                        ),
                logoUrl =
                    json.optString(
                        "logoUrl"
                    )
                        .trim()
                        .takeIf(
                            String::isNotBlank
                        ),
                categoryHint = category,
                confidence = confidence,
                source =
                    json.optString(
                        "source",
                        "remote"
                    )
                        .trim()
                        .ifBlank {
                            "remote"
                        },
                organizationRole =
                    runCatching {
                        RemoteOrganizationRole
                            .valueOf(
                                json.optString(
                                    "organizationRole",
                                    "UNKNOWN"
                                )
                            )
                    }.getOrDefault(
                        RemoteOrganizationRole
                            .UNKNOWN
                    ),
                salaryCandidateSuitability =
                    runCatching {
                        SalaryCandidateSuitability
                            .valueOf(
                                json.optString(
                                    "salaryCandidateSuitability",
                                    "REVIEW"
                                )
                            )
                    }.getOrDefault(
                        SalaryCandidateSuitability
                            .REVIEW
                    )
            )
        }.getOrNull()
    }

    private companion object {
        const val MAX_RESPONSE_BYTES = 65_536
        const val REQUEST_DEADLINE_MS = 12_000L
        val deadlineScheduler = Executors.newScheduledThreadPool(2) { task ->
            Thread(task, "entity-enrichment-deadline").apply { isDaemon = true }
        }
        val CLIENT_ID_REGEX =
            Regex(
                """^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-4[a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$"""
            )
    }
}
