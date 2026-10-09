package com.kedevjimmy.pesatrack.ingestion

import android.content.Context

data class ObservedNotificationSource(val packageName: String, val label: String)

/** No notification title/body, sender, transaction or account data is stored here. */
class NotificationSourceTrustStore(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isApproved(packageName: String): Boolean = synchronized(monitor) {
        NotificationSourceTrustPolicy.isApproved(packageName, app.packageName, approved())
    }

    fun setApproved(packageName: String, enabled: Boolean): Boolean = synchronized(monitor) {
        if (!NotificationSourceTrustPolicy.isEligible(packageName, app.packageName)) return@synchronized false
        val updated = approved().toMutableSet()
        if (enabled) updated.add(packageName) else updated.remove(packageName)
        prefs.edit().putStringSet("approved", updated).commit()
    }

    /** Skipping message access revokes existing sources, including queued work. */
    fun revokeAll(): Boolean = synchronized(monitor) {
        val saved = prefs.edit().putStringSet("approved", emptySet()).commit()
        if (!saved) prefs.edit().remove("approved").apply()
        saved
    }

    // Queued work rechecks approval at admission. Never hold a preferences lock
    // during database work: Settings and Android notification callbacks must stay responsive.
    fun withApprovedSource(packageName: String, action: () -> Unit): Boolean {
        if (!isApproved(packageName)) return false
        action()
        return true
    }

    fun observe(packageName: String, appLabel: String?) = synchronized(monitor) {
        if (!NotificationSourceTrustPolicy.isEligible(packageName, app.packageName)) return@synchronized
        val packages = prefs.getStringSet("observed", emptySet()).orEmpty().toMutableSet()
        val editor = prefs.edit()
        if (!packages.contains(packageName) && packages.size >= MAX_OBSERVED) {
            val oldestUnapproved = packages.firstOrNull { it !in approved() }
            if (oldestUnapproved == null) return@synchronized
            packages.remove(oldestUnapproved)
            editor.remove("label:$oldestUnapproved")
        }
        packages.add(packageName)
        editor.putStringSet("observed", packages)
            .putString("label:$packageName", appLabel?.trim()?.take(80)?.takeIf(String::isNotBlank) ?: packageName)
            .apply()
    }

    fun observedSources(): List<ObservedNotificationSource> = synchronized(monitor) {
        (prefs.getStringSet("observed", emptySet()).orEmpty() + approved())
            .filter { NotificationSourceTrustPolicy.isEligible(it, app.packageName) }
            .map { ObservedNotificationSource(it, prefs.getString("label:$it", it) ?: it) }
            .sortedBy { it.label.lowercase() }
    }

    private fun approved(): Set<String> = prefs.getStringSet("approved", emptySet()).orEmpty()

    companion object {
        const val PREFS_NAME = "notification_source_trust"
        private const val MAX_OBSERVED = 100
        private val monitor = Any()
    }
}
