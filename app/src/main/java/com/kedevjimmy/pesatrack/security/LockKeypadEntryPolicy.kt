package com.kedevjimmy.pesatrack.security

/** Restrict the visual keypad to the existing local PIN rules. */
object LockKeypadEntryPolicy {
    fun indicatorCount(storedLength: Int?): Int = storedLength?.takeIf { it in 4..8 } ?: 8

    fun appendDigit(current: String, digit: Char, storedLength: Int?): String =
        if (digit !in '0'..'9' || current.length >= indicatorCount(storedLength)) current
        else current + digit

    fun removeLast(current: String): String = current.dropLast(1)
}
