package com.kedevjimmy.pesatrack.domain.evidence

/** Count-only production proof summary. No amounts, names, references or geometry. */
data class PdfBoxVerificationReport(
    val receiptRows: Int,
    val cashRows: Int,
    val verifiedTransitions: Int,
    val totalsVerified: Boolean,
    val openingVerified: Boolean,
    val closingVerified: Boolean
) {
    init {
        require(receiptRows in 1..5_000 && cashRows in 0..receiptRows &&
            verifiedTransitions in 0 until receiptRows) { "Invalid statement proof counts." }
    }
    fun safeSummary(): String =
        "$receiptRows receipt rows reconstructed · $cashRows cash movements · " +
            "${receiptRows-cashRows} informational rows.\n" +
            "$verifiedTransitions receipt-group balance transitions verified.\n" +
            "Statement totals: ${if(totalsVerified) "verified" else "not independently verified"}. " +
            "Opening balance: ${if(openingVerified) "verified" else "not independently verified"}. " +
            "Closing balance: ${if(closingVerified) "verified" else "not independently verified"}.\n" +
            "Confirm any unverified statement totals and boundary balances against the original PDF. " +
            "Cash direction does not establish income, spending, transfers or loan meaning. " +
            "Nothing has been imported."

    fun encode(): IntArray = intArrayOf(receiptRows,cashRows,verifiedTransitions,
        if(totalsVerified)1 else 0,if(openingVerified)1 else 0,if(closingVerified)1 else 0)

    companion object {
        fun decode(values: IntArray): PdfBoxVerificationReport {
            require(values.size==6 && values.drop(3).all {it in 0..1}) { "Invalid statement proof transfer." }
            return PdfBoxVerificationReport(values[0],values[1],values[2],values[3]==1,values[4]==1,values[5]==1)
        }
    }
}
