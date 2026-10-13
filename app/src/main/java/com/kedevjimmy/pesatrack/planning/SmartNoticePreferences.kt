package com.kedevjimmy.pesatrack.planning

import android.content.Context
import com.kedevjimmy.pesatrack.domain.rules.*
import com.kedevjimmy.pesatrack.data.local.CanonicalStoreLock
import java.security.MessageDigest

/** Notification metadata only; never an accounting decision or a backup of financial data. */
class SmartNoticePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("review_attention_reminders", Context.MODE_PRIVATE)
    fun enabled(kind: SmartNoticeKind) = prefs.getBoolean("${kind.name.lowercase()}_enabled", false)
    fun setEnabled(kind: SmartNoticeKind, value: Boolean) {
        val edit = prefs.edit().putBoolean("${kind.name.lowercase()}_enabled", value)
        if (kind == SmartNoticeKind.PAYMENT && value && !enabled(kind))
            edit.putLong("payment_enabled_at", System.currentTimeMillis())
        edit.apply()
    }
    fun anyEnabled() = SmartNoticeKind.entries.any(::enabled)
    fun mode() = prefs.getString("smart_mode", "banner") ?: "banner"
    fun setMode(mode: String) = synchronized(CanonicalStoreLock.monitor) {
        require(mode in setOf("banner","quiet","off"))
        prefs.edit().putString("smart_mode",mode).apply()
    }
    fun snoozeUntil() = prefs.getLong("smart_snooze",0)
    fun snooze(until: Long) = synchronized(CanonicalStoreLock.monitor) { prefs.edit().putLong("smart_snooze",until).apply() }
    fun paymentEnabledAt() = prefs.getLong("payment_enabled_at",0)
    fun lastBanner() = maxOf(prefs.getLong("smart_last_banner",0),prefs.getLong("last_shown",0))
    fun settledAt(): Long {
        if (!prefs.contains("smart_settled_at")) changed()
        return prefs.getLong("smart_settled_at",0)
    }
    fun changed() { prefs.edit().putLong("smart_settled_at",System.currentTimeMillis()).apply() }
    private fun importState(): SmartImportState {
        val state = prefs.getString("smart_import_state","success")
        return SmartImportState(maxOf(if (state == "running") 1 else 0,prefs.getInt("smart_active_imports",0)),
            prefs.getBoolean("smart_import_batch_failed",state == "failed"))
    }
    private fun writeImport(state: SmartImportState) {
        prefs.edit().putInt("smart_active_imports",state.active).putBoolean("smart_import_batch_failed",state.failed)
            .putString("smart_import_state",if (state.active > 0) "running" else if (state.failed) "failed" else "success")
            .putLong("smart_settled_at",System.currentTimeMillis()).apply()
    }
    fun importReady() = importState().ready
    fun importStarted() = synchronized(CanonicalStoreLock.monitor) { writeImport(importState().begin()) }
    fun importFinished(success: Boolean) = synchronized(CanonicalStoreLock.monitor) { writeImport(importState().finish(success)) }
    fun recoverInterruptedImport() = synchronized(CanonicalStoreLock.monitor) {
        val state = importState()
        if (state.active > 0) writeImport(state.recover())
    }
    fun state(key: String): SmartNoticeState {
        // Preserve #360's seven-day same-set pause when upgrading an existing installation.
        val legacy = key == "activity" && !prefs.contains("shown:$key")
        return SmartNoticeState(if (legacy) hash(prefs.getString("last_fingerprint","") ?: "")
            else prefs.getString("rev:$key","") ?: "",
            prefs.getLong(if (legacy) "last_shown" else "shown:$key",0),prefs.getBoolean("dismissed:$key",false))
    }
    fun shown(candidate: SmartNoticeCandidate, now: Long) {
        prefs.edit().putString("rev:${candidate.key}",candidate.revision)
            .putLong("shown:${candidate.key}",now).putBoolean("dismissed:${candidate.key}",false)
            .putLong("smart_last_banner",now).putString("smart_posted_key",candidate.key)
            .putString("smart_posted_revision",candidate.revision).apply()
        if (candidate.kind == SmartNoticeKind.DISCOVERY) completeDiscovery()
    }
    fun dismissed(key: String, revision: String) {
        prefs.edit().putString("rev:$key",revision).putBoolean("dismissed:$key",true).apply()
    }
    fun undoDismissal(key: String) { prefs.edit().putBoolean("dismissed:$key",false).apply() }
    fun postedKey() = prefs.getString("smart_posted_key",null)
    fun postedRevision() = prefs.getString("smart_posted_revision",null)
    fun clearPosted() { prefs.edit().remove("smart_posted_key").remove("smart_posted_revision").apply() }
    fun discoveryPending() = !prefs.getBoolean("receivables_discovered",false)
    fun discoveryRequestedAgain() = prefs.getBoolean("discovery_requested_again",false)
    fun completeDiscovery() = synchronized(CanonicalStoreLock.monitor) { prefs.edit().putBoolean("receivables_discovered",true)
        .putBoolean("discovery_requested_again",false).apply() }
    fun resetDiscovery() = synchronized(CanonicalStoreLock.monitor) { prefs.edit().putBoolean("receivables_discovered",false)
        .putBoolean("discovery_requested_again",true)
        .remove("shown:discovery").remove("dismissed:discovery").apply() }
    fun personLastShown(person: String) = prefs.getLong("person:${hash(person)}",0)
    fun personShown(person: String, now: Long) { prefs.edit().putLong("person:${hash(person)}",now).apply() }
    companion object {
        fun hash(value: String): String = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}
