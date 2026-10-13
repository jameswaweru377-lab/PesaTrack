package com.kedevjimmy.pesatrack.domain.rules

data class SmartImportState(val active: Int = 0, val failed: Boolean = false) {
    val ready: Boolean get() = active == 0 && !failed
    fun begin() = SmartImportState(Math.addExact(active.coerceAtLeast(0),1),if (active > 0) failed else false)
    fun finish(success: Boolean) = SmartImportState((active - 1).coerceAtLeast(0),failed || !success || active <= 0)
    fun recover() = if (active > 0) SmartImportState(0,true) else this
}
