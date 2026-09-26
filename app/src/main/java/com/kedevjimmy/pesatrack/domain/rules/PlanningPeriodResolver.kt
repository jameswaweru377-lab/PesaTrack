package com.kedevjimmy.pesatrack.domain.rules

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

object PlanningPeriodResolver {
    fun resolve(
        observedAtEpochMs: Long,
        monthOffset: Int,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        val observedMonth = YearMonth.from(
            Instant.ofEpochMilli(observedAtEpochMs).atZone(zoneId)
        )
        return observedMonth.plusMonths(monthOffset.toLong()).toString()
    }

    fun current(
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String = YearMonth.now(zoneId).toString()
}
