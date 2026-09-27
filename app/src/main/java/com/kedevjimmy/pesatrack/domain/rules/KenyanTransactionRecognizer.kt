package com.kedevjimmy.pesatrack.domain.rules

import com.kedevjimmy.pesatrack.domain.model.MoneyDirection
import com.kedevjimmy.pesatrack.domain.model.ParsedMpesaTransaction
import com.kedevjimmy.pesatrack.domain.model.TransactionCategory

data class BuiltInTransactionClassification(
    val direction: MoneyDirection,
    val category: TransactionCategory
)

object KenyanTransactionRecognizer {
    private val phoneDataTerms = listOf(
        "data bundle",
        "data bundles",
        "sms bundle",
        "sms bundles",
        "airtime",
        "tunukiwa",
        "minutes bundle",
        "voice bundle"
    )

    fun classify(
        parsed: ParsedMpesaTransaction
    ): BuiltInTransactionClassification? {
        val searchable = buildString {
            append(parsed.counterparty.orEmpty())
            append(' ')
            append(parsed.rawMessage)
        }
            .lowercase()
            .replace(Regex("""\s+"""), " ")
            .trim()

        val explicitPhoneData =
            "tunukiwa" in searchable ||
                phoneDataTerms.any(searchable::contains)

        val safaricomPhoneData =
            "safaricom" in searchable &&
                (
                    "bundle" in searchable ||
                        "airtime" in searchable ||
                        "data" in searchable ||
                        "sms" in searchable ||
                        "minutes" in searchable
                    )

        if (explicitPhoneData || safaricomPhoneData) {
            return BuiltInTransactionClassification(
                direction = MoneyDirection.EXPENSE,
                category = TransactionCategory.PHONE_AND_DATA
            )
        }

        return null
    }
}
