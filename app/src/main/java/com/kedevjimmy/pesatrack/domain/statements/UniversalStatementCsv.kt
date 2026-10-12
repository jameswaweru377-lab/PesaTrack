package com.kedevjimmy.pesatrack.domain.statements

import java.util.TimeZone

/**
 * Local-only structured CSV decoder for the V2 read-only interpreter.
 * Reuses the common table model instead of assigning account semantics in CSV.
 * Does not change the existing production StatementCsv import path.
 */
object UniversalStatementCsv {
    private const val MAX_CHARACTERS = 2 * 1024 * 1024
    private const val MAX_CELLS = 40
    private const val MAX_ROWS = 5_001 // header + transaction rows

    /** No partial rows survive an invalid/unclosed quote, oversized input or malformed file. */
    fun decode(text: String): StatementTablePage? {
        if (text.isEmpty() || text.length > MAX_CHARACTERS) return null
        val source = text.removePrefix("\uFEFF")
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var afterQuote = false

        fun endCell(): Boolean {
            currentRow.add(field.toString())
            field.setLength(0)
            afterQuote = false
            return currentRow.size <= MAX_CELLS
        }
        fun endRow(): Boolean {
            if (!endCell()) return false
            // An entirely empty separator row contains no financial information.
            // A partially filled transaction row remains present and must fail
            // downstream if any required cell is absent.
            if (currentRow.any { it.isNotBlank() }) rows.add(currentRow.toList())
            currentRow.clear()
            return rows.size <= MAX_ROWS
        }
        var pos = 0
        while (pos < source.length) {
            val ch = source[pos]
            if (quoted) {
                if (ch == '"') {
                    if (pos + 1 < source.length && source[pos + 1] == '"') {
                        field.append('"')
                        pos++
                    } else {
                        quoted = false
                        afterQuote = true
                    }
                } else {
                    field.append(ch)
                }
            } else {
                when (ch) {
                    '"' -> {
                        if (field.isNotEmpty() || afterQuote) return null
                        quoted = true
                    }
                    ',' -> if (!endCell()) return null
                    '\r', '\n' -> {
                        if (!endRow()) return null
                        if (ch == '\r' && pos + 1 < source.length && source[pos + 1] == '\n') pos++
                    }
                    else -> {
                        if (afterQuote) return null
                        field.append(ch)
                    }
                }
            }
            pos++
        }
        if (quoted) return null
        if (field.isNotEmpty() || currentRow.isNotEmpty() || afterQuote)
            if (!endRow()) return null
        if (rows.size < 2 || rows.first().isEmpty() || rows.first().any { it.isBlank() })
            return null
        return StatementTablePage(1, rows.first(), rows.drop(1))
    }

    fun inspect(
        csvText: String,
        zone: TimeZone = TimeZone.getTimeZone("Africa/Nairobi"),
        openingBalanceMinor: Long? = null,
        profile: StatementFormatProfile = StatementFormatProfile(),
        now: Long = System.currentTimeMillis()
    ): StatementTableResult {
        val table = decode(csvText)
            ?: return StatementTableResult.Blocked(StatementBlockReason.INVALID_STRUCTURE)
        return UniversalStatementTableInterpreter.interpret(
            listOf(table), zone, openingBalanceMinor, profile, now
        )
    }
}
