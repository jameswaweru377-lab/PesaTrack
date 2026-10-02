package com.kedevjimmy.pesatrack.ingestion

import com.kedevjimmy.pesatrack.domain.messagepack.MessageLabRedactor
import java.security.MessageDigest

data class MessageLabShapeIdentity(
    val shapeId: String,
    val sender: String,
    val sourcePackage: String?,
    val sourceKind: String?,
    val redactedBody: String
)

object MessageLabShapeIdentityFactory {
    fun create(
        safeSenderLabel: String?,
        body: String,
        sourcePackage: String?,
        sourceKind: String?
    ): MessageLabShapeIdentity {
        val redacted =
            MessageLabRedactor.redact(
                safeSenderLabel =
                    safeSenderLabel,
                body = body
            )

        val safePackage =
            sourcePackage
                ?.trim()
                ?.lowercase()
                ?.takeIf(
                    String::isNotBlank
                )
                ?.take(
                    MAX_PACKAGE_LENGTH
                )
        val safeKind =
            sourceKind
                ?.trim()
                ?.lowercase()
                ?.takeIf(
                    String::isNotBlank
                )
                ?.take(
                    MAX_SOURCE_KIND_LENGTH
                )

        val shapeId =
            fingerprint(
                listOf(
                    safeKind.orEmpty(),
                    safePackage.orEmpty(),
                    redacted.sender,
                    redacted.body
                ).joinToString("\n")
            )

        return MessageLabShapeIdentity(
            shapeId = shapeId,
            sender = redacted.sender,
            sourcePackage =
                safePackage,
            sourceKind =
                safeKind,
            redactedBody =
                redacted.body
        )
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

    private const val
        MAX_PACKAGE_LENGTH = 120
    private const val
        MAX_SOURCE_KIND_LENGTH = 80
}
