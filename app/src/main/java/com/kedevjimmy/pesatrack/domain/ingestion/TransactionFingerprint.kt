package com.kedevjimmy.pesatrack.domain.ingestion

import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
import java.security.MessageDigest

object TransactionFingerprint {
    fun of(parsed: ParsedMpesaTransaction): String {
        parsed.transactionCode?.trim()?.uppercase()?.takeIf { it.isNotBlank() }?.let {
            return "code:$it"
        }

        val canonical = buildString {
            append(parsed.type.name)
            append('|')
            append(parsed.amountMinor)
            append('|')
            append(parsed.feeMinor)
            append('|')
            append(parsed.balanceMinor ?: "")
            append('|')
            append(parsed.counterparty.orEmpty().trim().uppercase())
            append('|')
            append(parsed.occurredAtText.orEmpty().trim().uppercase())
            append('|')
            append(parsed.rawMessage.replace(Regex("""\s+"""), " ").trim().uppercase())
        }

        val digest = MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }
}
