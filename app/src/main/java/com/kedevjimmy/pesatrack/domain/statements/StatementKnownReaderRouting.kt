package com.kedevjimmy.pesatrack.domain.statements

/** A format match is only a candidate; the user must confirm its issuer. */
object StatementKnownReaderRouting {
    fun uniqueCandidate(matches: List<String>): String? = matches.singleOrNull()
}
