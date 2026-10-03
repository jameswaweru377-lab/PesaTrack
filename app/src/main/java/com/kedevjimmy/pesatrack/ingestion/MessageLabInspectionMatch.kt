package com.kedevjimmy.pesatrack.ingestion

data class MessageLabInspectionMatch(
    val shape:
        UnsupportedMessageShape?,
    val ambiguousSourceMatch: Boolean,
    val matchedShapeCount: Int
)

object MessageLabInspectionMatchResolver {
    fun resolve(
        candidates:
            List<UnsupportedMessageShape>
    ): MessageLabInspectionMatch =
        when (candidates.size) {
            0 ->
                MessageLabInspectionMatch(
                    shape = null,
                    ambiguousSourceMatch =
                        false,
                    matchedShapeCount = 0
                )

            1 ->
                MessageLabInspectionMatch(
                    shape =
                        candidates.single(),
                    ambiguousSourceMatch =
                        false,
                    matchedShapeCount = 1
                )

            else ->
                MessageLabInspectionMatch(
                    shape = null,
                    ambiguousSourceMatch =
                        true,
                    matchedShapeCount =
                        candidates.size
                )
        }
}
