package com.kedevjimmy.pesatrack.security

import android.content.Context
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

/** Parses all values before any preference editor or ledger transaction is opened. */
internal object BackupPreferences {
    fun allowed(name: String): Boolean = name != AppLockManager.PREFS_NAME &&
        name != com.kedevjimmy.pesatrack.ingestion.NotificationSourceTrustStore.PREFS_NAME &&
        !name.startsWith("androidx.") && !name.startsWith("com.google.") &&
        !name.contains("workmanager", ignoreCase = true) &&
        name.isNotBlank() && name.none { it == '/' || it == '\\' || it == '\u0000' }

    fun decode(preferences: JsonObject): Map<String, Map<String, Any>> =
        preferences.entrySet().filter { allowed(it.key) }.associate { (name, values) ->
            require(values.isJsonObject) { "This backup contains damaged settings." }
            name to values.asJsonObject.entrySet().associate { (key, element) ->
                require(element.isJsonObject) { "This backup contains damaged settings." }
                val cell = element.asJsonObject
                val type = cell.get("type")
                require(type is JsonPrimitive && type.isString)
                val value = cell.get("value") ?: error("Missing backup setting value.")
                val decoded: Any = when (type.asString) {
                    "boolean" -> {
                        require(value is JsonPrimitive && value.isBoolean)
                        value.asBoolean
                    }
                    "int" -> strictInteger(value).also {
                        require(it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong())
                    }.toInt()
                    "long" -> strictInteger(value)
                    "float" -> {
                        require(value is JsonPrimitive && value.isNumber)
                        value.asFloat.also { require(it.isFinite()) }
                    }
                    "string" -> {
                        require(value is JsonPrimitive && value.isString)
                        value.asString
                    }
                    "set" -> {
                        require(value.isJsonArray)
                        value.asJsonArray.map {
                            require(it is JsonPrimitive && it.isString)
                            it.asString
                        }.toSet()
                    }
                    else -> throw IllegalArgumentException("Unsupported backup setting type.")
                }
                key to decoded
            }
        }

    private fun strictInteger(value: com.google.gson.JsonElement): Long {
        require(value is JsonPrimitive && value.isNumber)
        return value.asString.toLongOrNull()
            ?: throw IllegalArgumentException("Invalid integer backup setting.")
    }

    fun write(context: Context, values: Map<String, Map<String, Any>>) {
        values.forEach { (name, entries) ->
            check(allowed(name))
            val editor = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear()
            entries.forEach { (key, value) ->
                when (value) {
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is String -> editor.putString(key, value)
                    is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
                    else -> error("Unsupported staged setting.")
                }
            }
            check(editor.commit()) { "Restore settings could not be saved. Recovery will retry on restart." }
        }
    }
}
