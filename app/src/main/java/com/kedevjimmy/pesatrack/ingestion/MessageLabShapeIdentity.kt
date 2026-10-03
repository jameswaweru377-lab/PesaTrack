package com.kedevjimmy.pesatrack.ingestion

import com.kedevjimmy.pesatrack.domain.messagepack.MessageLabRedactor
import java.security.MessageDigest

data class MessageLabShapeIdentity(
    val shapeId: String,
    val familyId: String,
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
        val familyId =
            familyIdForRedacted(
                sender =
                    redacted.sender,
                redactedBody =
                    redacted.body,
                sourcePackage =
                    safePackage,
                sourceKind =
                    safeKind
            )

        return MessageLabShapeIdentity(
            shapeId = shapeId,
            familyId = familyId,
            sender = redacted.sender,
            sourcePackage =
                safePackage,
            sourceKind =
                safeKind,
            redactedBody =
                redacted.body
        )
    }

    fun familyIdForRedacted(
        sender: String,
        redactedBody: String,
        sourcePackage: String?,
        sourceKind: String?
    ): String =
        fingerprint(
            listOf(
                sourceKind
                    ?.trim()
                    ?.lowercase()
                    .orEmpty(),
                sourcePackage
                    ?.trim()
                    ?.lowercase()
                    .orEmpty(),
                canonicalSender(
                    sender
                ),
                canonicalFamilyBody(
                    redactedBody
                )
            ).joinToString("\n")
        )

    fun canonicalFamilyBody(
        redactedBody: String
    ): String =
        redactedBody
            .lowercase()
            .replace(
                Regex(
                    """\\bm[\\s-]*pesa\\b"""
                ),
                "mpesa"
            )
            .replace(
                Regex(
                    """\\bm[\\s-]*shwari\\b"""
                ),
                "mshwari"
            )
            .replace(
                Regex(
                    """\\bpesa[\\s-]*link\\b"""
                ),
                "pesalink"
            )
            .replace(
                Regex(
                    """\\b(?:kshs?|kes|shs?)[.]?\\b"""
                ),
                "<currency>"
            )
            .replace(
                Regex(
                    """[.,:;!?()"\u2019'-]+"""
                ),
                " "
            )
            .replace(
                Regex(
                    """\\s+"""
                ),
                " "
            )
            .trim()

    private fun canonicalSender(
        sender: String
    ): String =
        sender
            .lowercase()
            .replace(
                Regex(
                    """[^a-z0-9]+"""
                ),
                ""
            )
            .ifBlank {
                "<sender>"
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
