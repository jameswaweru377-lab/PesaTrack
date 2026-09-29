package com.kedevjimmy.pesatrack.ingestion

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.kedevjimmy.pesatrack.BuildConfig
import com.kedevjimmy.pesatrack.data.local.EntityEnrichmentCache
import com.kedevjimmy.pesatrack.data.local.TransactionStore
import com.kedevjimmy.pesatrack.domain.intelligence.EntityDescriptorSanitizer
import com.kedevjimmy.pesatrack.domain.intelligence.EntityMatchConfidence
import com.kedevjimmy.pesatrack.domain.intelligence.OnlineEntityProfile
import com.kedevjimmy.pesatrack.domain.model.MpesaTransactionType
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import org.json.JSONObject

object EntityEnrichmentScheduler {
    fun scheduleIfEligible(
        context: Context,
        type: MpesaTransactionType,
        counterparty: String?
    ): Boolean {
        val endpoint =
            BuildConfig
                .ENTITY_ENRICHMENT_BASE_URL
                .trim()
                .trimEnd('/')
        if (endpoint.isBlank()) {
            return false
        }

        val descriptor =
            EntityDescriptorSanitizer.from(
                type = type,
                counterparty = counterparty
            ) ?: return false

        if (
            EntityEnrichmentCache
                .get(context)
                .get(descriptor.value) != null
        ) {
            return false
        }

        val input =
            Data.Builder()
                .putString(
                    EntityEnrichmentWorker
                        .KEY_DESCRIPTOR,
                    descriptor.value
                )
                .build()

        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(
                    NetworkType.CONNECTED
                )
                .build()

        val request =
            OneTimeWorkRequest.Builder(
                EntityEnrichmentWorker::
                    class.java
            )
                .setInputData(input)
                .setConstraints(constraints)
                .build()

        WorkManager.getInstance(
            context.applicationContext
        ).enqueueUniqueWork(
            uniqueWorkName(
                descriptor.value
            ),
            ExistingWorkPolicy.KEEP,
            request
        )

        return true
    }

    private fun uniqueWorkName(
        descriptor: String
    ): String {
        val digest =
            MessageDigest.getInstance("SHA-256")
                .digest(
                    EntityDescriptorSanitizer
                        .key(descriptor)
                        .toByteArray(
                            Charsets.UTF_8
                        )
                )
                .take(8)
                .joinToString("") {
                    "%02x".format(it)
                }
        return "entity-enrichment-$digest"
    }
}

class EntityEnrichmentWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(
    appContext,
    workerParams
) {
    override fun doWork(): Result {
        val descriptor =
            inputData.getString(
                KEY_DESCRIPTOR
            )?.trim()
                ?.takeIf(String::isNotBlank)
                ?: return Result.failure()

        val endpoint =
            BuildConfig
                .ENTITY_ENRICHMENT_BASE_URL
                .trim()
                .trimEnd('/')
        if (endpoint.isBlank()) {
            return Result.success()
        }

        return when (
            val response =
                OnlineEntityEnrichmentClient
                    .resolve(
                        endpoint = endpoint,
                        descriptor = descriptor
                    )
        ) {
            is OnlineEntityLookupResult.Resolved -> {
                EntityEnrichmentCache
                    .get(applicationContext)
                    .put(
                        descriptor =
                            descriptor,
                        profile =
                            response.profile
                    )
                TransactionStore
                    .get(applicationContext)
                    .applyOnlineEntityEnrichment(
                        descriptor =
                            descriptor,
                        profile =
                            response.profile
                    )
                Result.success()
            }

            OnlineEntityLookupResult.NotFound ->
                Result.success()

            is OnlineEntityLookupResult.Retryable ->
                Result.retry()

            is OnlineEntityLookupResult.PermanentFailure ->
                Result.failure()
        }
    }

    companion object {
        const val KEY_DESCRIPTOR =
            "descriptor"
    }
}

private sealed interface OnlineEntityLookupResult {
    data class Resolved(
        val profile: OnlineEntityProfile
    ) : OnlineEntityLookupResult

    data object NotFound :
        OnlineEntityLookupResult

    data class Retryable(
        val reason: String
    ) : OnlineEntityLookupResult

    data class PermanentFailure(
        val reason: String
    ) : OnlineEntityLookupResult
}

private object OnlineEntityEnrichmentClient {
    fun resolve(
        endpoint: String,
        descriptor: String
    ): OnlineEntityLookupResult {
        val connection =
            runCatching {
                (
                    URL(
                        endpoint +
                            "/v1/entities/resolve"
                    ).openConnection()
                    as HttpURLConnection
                    )
            }.getOrElse {
                return OnlineEntityLookupResult
                    .Retryable(
                        "connection_setup"
                    )
            }

        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout =
                CONNECT_TIMEOUT_MS
            connection.readTimeout =
                READ_TIMEOUT_MS
            connection.doOutput = true
            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            )
            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            val body =
                JSONObject()
                    .put(
                        "descriptor",
                        descriptor
                    )
                    .toString()

            connection.outputStream.use {
                output ->
                output.write(
                    body.toByteArray(
                        Charsets.UTF_8
                    )
                )
            }

            val code =
                connection.responseCode
            when {
                code in 200..299 ->
                    parseResolved(
                        connection
                            .inputStream
                            .bufferedReader()
                            .use {
                                it.readText()
                            }
                    )

                code == 404 ->
                    OnlineEntityLookupResult
                        .NotFound

                code == 408 ||
                    code == 429 ||
                    code >= 500 ->
                    OnlineEntityLookupResult
                        .Retryable(
                            "http_$code"
                        )

                else ->
                    OnlineEntityLookupResult
                        .PermanentFailure(
                            "http_$code"
                        )
            }
        } catch (
            _: java.io.IOException
        ) {
            OnlineEntityLookupResult
                .Retryable(
                    "io"
                )
        } finally {
            connection.disconnect()
        }
    }

    private fun parseResolved(
        raw: String
    ): OnlineEntityLookupResult {
        val json =
            runCatching {
                JSONObject(raw)
            }.getOrElse {
                return OnlineEntityLookupResult
                    .PermanentFailure(
                        "invalid_json"
                    )
            }

        val status =
            json.optString(
                "status",
                ""
            )
        if (
            status == "not_found" ||
            status == "ambiguous"
        ) {
            return OnlineEntityLookupResult
                .NotFound
        }
        if (status != "resolved") {
            return OnlineEntityLookupResult
                .PermanentFailure(
                    "invalid_status"
                )
        }

        val merchant =
            json.optString(
                "merchant",
                ""
            ).trim()
        if (merchant.isBlank()) {
            return OnlineEntityLookupResult
                .PermanentFailure(
                    "missing_merchant"
                )
        }

        val confidence =
            when (
                json.optString(
                    "confidence",
                    "likely"
                ).lowercase()
            ) {
                "high" ->
                    EntityMatchConfidence.HIGH
                else ->
                    EntityMatchConfidence.LIKELY
            }

        return OnlineEntityLookupResult
            .Resolved(
                OnlineEntityProfile(
                    canonicalName =
                        merchant,
                    domain =
                        json.optString(
                            "domain",
                            ""
                        ).takeIf(
                            String::isNotBlank
                        ),
                    logoUrl =
                        json.optString(
                            "logo",
                            ""
                        ).takeIf(
                            String::isNotBlank
                        ),
                    remoteCategory =
                        json.optString(
                            "category",
                            ""
                        ).takeIf(
                            String::isNotBlank
                        ),
                    confidence =
                        confidence,
                    source =
                        json.optString(
                            "source",
                            "entity_api"
                        )
                )
            )
    }

    private const val CONNECT_TIMEOUT_MS =
        10_000
    private const val READ_TIMEOUT_MS =
        12_000
}
