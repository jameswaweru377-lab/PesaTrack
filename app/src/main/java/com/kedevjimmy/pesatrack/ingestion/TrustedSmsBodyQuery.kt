package com.kedevjimmy.pesatrack.ingestion

import android.provider.BaseColumns
import android.provider.Telephony

/** Provider-side restriction; no body is requested without its trusted ID/address pair. */
internal data class TrustedSmsBodyQuery(val selection: String, val selectionArgs: Array<String>)

internal fun trustedSmsBodyQuery(
    idAndSenders: List<Pair<Long, String>>,
    reference: String?
): TrustedSmsBodyQuery {
    require(idAndSenders.isNotEmpty())
    require(reference == null || reference.isNotBlank())

    val pair = "(" + BaseColumns._ID + " = ? AND " + Telephony.Sms.ADDRESS + " = ?)"
    val pairs = idAndSenders.joinToString(" OR ") { pair }
    val args = idAndSenders.flatMap { (id, sender) -> listOf(id.toString(), sender) }
    if (reference == null) return TrustedSmsBodyQuery(pairs, args.toTypedArray())

    // LIKE must escape %, _ and backslash, and the OR group must be parenthesised:
    // otherwise the reference filter would apply only to the final trusted ID.
    val escaped = reference.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
    return TrustedSmsBodyQuery(
        "($pairs) AND ${Telephony.Sms.BODY} LIKE ? ESCAPE '\\'",
        (args + "%$escaped%").toTypedArray()
    )
}
