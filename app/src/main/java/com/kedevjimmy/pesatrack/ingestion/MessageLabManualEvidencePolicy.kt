package com.kedevjimmy.pesatrack.ingestion

data class MessageLabManualEvidenceTarget(
    val shapeId: String,
    val reuseExistingShape: Boolean
)

object MessageLabManualEvidencePolicy {
    const val MANUAL_SOURCE_KIND =
        "manual_message_inspector"

    fun target(
        inspectionMatch:
            MessageLabInspectionMatch,
        manualShapeId: String
    ): MessageLabManualEvidenceTarget {
        val existing =
            inspectionMatch.shape
                ?.takeIf {
                    !inspectionMatch
                        .ambiguousSourceMatch
                }

        return if (existing != null) {
            MessageLabManualEvidenceTarget(
                shapeId =
                    existing.shapeId,
                reuseExistingShape =
                    true
            )
        } else {
            MessageLabManualEvidenceTarget(
                shapeId =
                    manualShapeId,
                reuseExistingShape =
                    false
            )
        }
    }

    fun stableOccurrenceCount(
        existingCount: Int?
    ): Int =
        existingCount
            ?.coerceAtLeast(1)
            ?: 1
}
