package com.kedevjimmy.pesatrack.planning

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Bounded private metadata, not a copy of raw SMS, bank receipts or notification payloads. */
object LocalNotificationInboxStore {
    private const val PREFS = "pesatrack_notification_inbox"
    private const val ITEMS = "items"
    private val lock = Any()
    private val changes = MutableStateFlow(0L)
    val revision: StateFlow<Long> = changes

    fun list(context: Context): List<LocalInboxNotice> = synchronized(lock) {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(ITEMS, "[]") ?: "[]"
        runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                LocalInboxNotice(
                    key = item.getString("key"), category = item.getString("category"),
                    title = item.getString("title"), summary = item.getString("summary"),
                    route = item.getString("route"), createdAt = item.getLong("at"),
                    read = item.getBoolean("read")
                )
            }
        }.onFailure { Log.w("PesaTrackInbox", "Invalid local notification history format") }
            .getOrDefault(emptyList())
    }

    fun record(context: Context, key: String, category: String, title: String,
               summary: String, route: String, now: Long = System.currentTimeMillis()) {
        if (!LocalNotificationInboxPolicy.supportsRoute(route)) {
            Log.w("PesaTrackInbox", "Unsupported notification destination")
            return
        }
        runCatching {
            synchronized(lock) {
                write(context, LocalNotificationInboxPolicy.record(list(context),
                    LocalInboxNotice(key, category, title, summary, route, now, read = false)))
            }
        }.onFailure { Log.w("PesaTrackInbox", "Could not save local notification history") }
    }

    fun markRead(context: Context, key: String) = synchronized(lock) {
        write(context, LocalNotificationInboxPolicy.markRead(list(context), key))
    }

    fun markAllRead(context: Context) = synchronized(lock) {
        write(context, LocalNotificationInboxPolicy.markAllRead(list(context)))
    }

    private fun write(context: Context, notices: List<LocalInboxNotice>) {
        val json = JSONArray()
        notices.forEach { item ->
            json.put(JSONObject().put("key", item.key).put("category", item.category)
                .put("title", item.title).put("summary", item.summary)
                .put("route", item.route).put("at", item.createdAt)
                .put("read", item.read))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(ITEMS, json.toString()).apply()
        changes.value += 1L
    }
}
