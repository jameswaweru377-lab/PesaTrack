package com.kedevjimmy.pesatrack.data.remote

import com.kedevjimmy.pesatrack.BuildConfig
import com.kedevjimmy.pesatrack.domain.intelligence.EntityMatchConfidence
import com.kedevjimmy.pesatrack.domain.intelligence.RemoteEntityEnrichment
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class EntityEnrichmentHttpClient(
    private val endpoint:
        String = BuildConfig.ENTITY_ENRICHMENT_URL
) {
    fun isConfigured(): Boolean =
        endpoint.startsWith(
            "https://",
            ignoreCase = true
        )

    fun enrich(
        sanitizedDescriptor: String
    ): RemoteEntityEnrichment? {
        if (!isConfigured()) {
            return null
        }

        val connection =
            (URL(endpoint).openConnection()
                as HttpURLConnection)
                .apply {
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
                }

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

            if (
                connection.responseCode !in
                200..299
            ) {
                return null
            }

            val response =
                connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }

            parseResponse(response)
        } catch (_: Exception) {
            null
        } finally {
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
                        }
            )
        }.getOrNull()
    }
}
