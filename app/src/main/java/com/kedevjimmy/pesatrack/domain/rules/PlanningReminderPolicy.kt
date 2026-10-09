package com.kedevjimmy.pesatrack.domain.rules
import java.math.BigDecimal
import java.math.RoundingMode
object PlanningReminderPolicy {
    fun budgetLevel(spent: Long, limit: Long): String? {
        if (limit <= 0 || spent < 0) return null
        if (spent >= limit) return "over"
        return if (BigDecimal.valueOf(spent).multiply(BigDecimal.valueOf(100)) >= BigDecimal.valueOf(limit).multiply(BigDecimal.valueOf(95))) "near" else null
    }
    fun goalDue(now: Long, due: Long, saved: Long?, target: Long): Boolean =
        saved != null && target > 0 && saved < target && now >= due
    fun share(amount: Long, total: Long): String = if (total <= 0) "0%" else
        BigDecimal.valueOf(amount.coerceAtLeast(0)).multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(total), 0, RoundingMode.HALF_UP).toPlainString() + "%"
}
