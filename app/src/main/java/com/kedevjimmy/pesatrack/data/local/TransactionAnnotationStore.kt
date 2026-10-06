package com.kedevjimmy.pesatrack.data.local

import android.content.Context

data class TransactionAnnotation(
    val note: String = "",
    val receiptUri: String? = null
)

class TransactionAnnotationStore private constructor(
    context: Context
) {
    private val prefs =
        context.applicationContext
            .getSharedPreferences(
                "transaction_annotations",
                Context.MODE_PRIVATE
            )

    fun get(transactionId: Long): TransactionAnnotation =
        TransactionAnnotation(
            note =
                prefs.getString(
                    "note:$transactionId",
                    ""
                ).orEmpty(),
            receiptUri =
                prefs.getString(
                    "receipt:$transactionId",
                    null
                )
        )

    fun saveNote(
        transactionId: Long,
        note: String
    ) {
        prefs.edit()
            .putString(
                "note:$transactionId",
                note.trim()
            )
            .apply()
    }

    fun saveReceipt(
        transactionId: Long,
        uri: String?
    ) {
        prefs.edit().apply {
            if (uri.isNullOrBlank()) {
                remove(
                    "receipt:$transactionId"
                )
            } else {
                putString(
                    "receipt:$transactionId",
                    uri
                )
            }
        }.apply()
    }

    companion object {
        @Volatile
        private var instance:
            TransactionAnnotationStore? =
            null

        fun get(
            context: Context
        ): TransactionAnnotationStore =
            instance ?: synchronized(this) {
                instance
                    ?: TransactionAnnotationStore(
                        context
                    ).also {
                        instance = it
                    }
            }
    }
}
