package com.kedevjimmy.pesatrack.domain.interpretation

/** Old execution grammar captured "of SCOM" as the instrument name. */
internal fun legacyOfPrefixInvestmentKey(instrumentKey: String): String? {
    val prefix = "instrument:"
    if (!instrumentKey.startsWith(prefix)) return null
    val suffix = instrumentKey.removePrefix(prefix)
    if (suffix.isBlank() || suffix.startsWith("OF_")) return null
    return "${prefix}OF_$suffix"
}

internal fun canonicalInvestmentLabel(label: String): String =
    label.replace(Regex("(?i)^of\\s+(?=[A-Za-z0-9])"), "").trim()

internal fun canonicalInvestmentKey(key: String): String =
    if (key.startsWith("instrument:OF_") && key.length > "instrument:OF_".length) {
        key.replaceFirst("instrument:OF_", "instrument:")
    } else key
