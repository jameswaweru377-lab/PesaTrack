package com.kedevjimmy.pesatrack.ui.screens

import android.os.Build
import android.util.Log
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.kedevjimmy.pesatrack.domain.planning.ShoppingNanoDraftPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Gemini Nano is an optional on-device interpreter, never a requirement to scan. */
internal object ShoppingNanoInterpreter {
    private const val TAG = "ShoppingNano"
    private const val MAX_INFERENCE_MS = 20_000L

    data class Draft(val text: String, val source: String)

    suspend fun propose(ocrText: String): Draft {
        val fallback = Draft(ocrText, "Standard offline scanner — please check every item")
        if (ocrText.isBlank() || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return fallback
        return withContext(Dispatchers.Default) {
            val model = try { Generation.getClient() }
            catch (e: Exception) {
                Log.w(TAG, "Gemini Nano client unavailable: ${e.javaClass.simpleName}")
                return@withContext fallback
            }
            try {
                withTimeoutOrNull(MAX_INFERENCE_MS) {
                    when (model.checkStatus()) {
                        FeatureStatus.AVAILABLE -> {
                            val response = model.generateContent(ShoppingNanoDraftPolicy.prompt(ocrText))
                            val generated = response.candidates.firstOrNull()?.text.orEmpty()
                            val accepted = ShoppingNanoDraftPolicy.reviewableDraft(ocrText, generated)
                            if (accepted == null) fallback else Draft(accepted,
                                "Gemini Nano on this phone — AI suggestions require your review")
                        }
                        FeatureStatus.DOWNLOADABLE -> Draft(ocrText,
                            "Standard scanner — Gemini Nano needs its first model download in Android AICore")
                        FeatureStatus.DOWNLOADING -> Draft(ocrText,
                            "Standard scanner — Android is downloading Gemini Nano")
                        else -> fallback
                    }
                } ?: fallback
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                Log.w(TAG, "Gemini Nano scan failed: ${e.javaClass.simpleName}")
                fallback
            } finally {
                model.close()
            }
        }
    }
}
