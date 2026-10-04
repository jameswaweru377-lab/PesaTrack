package com.kedevjimmy.pesatrack.domain.rules

data class PlanningAvailability(
    val availableMinor: Long,
    val shortfallMinor: Long
)

object PlanningAvailabilityPolicy {
    fun from(rawAvailableMinor: Long): PlanningAvailability =
        if (rawAvailableMinor >= 0L) {
            PlanningAvailability(
                availableMinor = rawAvailableMinor,
                shortfallMinor = 0L
            )
        } else {
            PlanningAvailability(
                availableMinor = 0L,
                shortfallMinor =
                    if (rawAvailableMinor == Long.MIN_VALUE) {
                        Long.MAX_VALUE
                    } else {
                        -rawAvailableMinor
                    }
            )
        }
}
