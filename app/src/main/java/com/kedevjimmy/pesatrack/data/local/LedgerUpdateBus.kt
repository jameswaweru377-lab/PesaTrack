package com.kedevjimmy.pesatrack.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class LedgerUpdateState(
    val revision: Long = 0L,
    val processingCount: Int = 0
) {
    val isProcessing: Boolean
        get() = processingCount > 0
}

object LedgerUpdateBus {
    private val mutableState = MutableStateFlow(LedgerUpdateState())
    val state: StateFlow<LedgerUpdateState> = mutableState

    @Synchronized
    fun processingStarted() {
        val current = mutableState.value
        mutableState.value = current.copy(
            processingCount = current.processingCount + 1
        )
    }

    @Synchronized
    fun processingFinished(changed: Boolean) {
        val current = mutableState.value
        mutableState.value = current.copy(
            revision = current.revision + if (changed) 1 else 0,
            processingCount = (current.processingCount - 1).coerceAtLeast(0)
        )
    }

    @Synchronized
    fun notifyChanged() {
        val current = mutableState.value
        mutableState.value = current.copy(revision = current.revision + 1)
    }
}
