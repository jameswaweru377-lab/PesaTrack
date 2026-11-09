package com.kedevjimmy.pesatrack.data.local

import android.content.Context
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.kedevjimmy.pesatrack.domain.planning.ShoppingItem
import com.kedevjimmy.pesatrack.domain.planning.ShoppingList
import com.kedevjimmy.pesatrack.domain.planning.ShoppingListPolicy

/** Private, backup-eligible planning metadata; never part of the money ledger. */
class ShoppingListStore(context: Context) {
    companion object {
        const val PREFS_NAME = "shopping_lists_v1"
        private const val KEY = "lists"
        private val lock = Any()
    }

    private val preferences = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun list(): List<ShoppingList> = synchronized(lock) { read() }

    fun save(value: ShoppingList) = synchronized(lock) {
        val checked = ShoppingListPolicy.validate(value)
        val current = read()
        val updated = (current.filterNot { it.id == checked.id } + checked)
            .sortedWith(compareByDescending<ShoppingList> { it.createdAtEpochMs }.thenBy { it.id })
        require(updated.size <= ShoppingListPolicy.MAX_LISTS) {
            "You can save up to ${ShoppingListPolicy.MAX_LISTS} shopping lists."
        }
        persist(updated)
    }

    fun delete(id: String) = synchronized(lock) {
        persist(read().filterNot { it.id == id })
    }

    private fun read(): List<ShoppingList> {
        val raw = preferences.getString(KEY, null) ?: return emptyList()
        try {
            val root = JsonParser.parseString(raw).asJsonObject
            require(root.get("version").asInt == 1)
            val array = root.getAsJsonArray("lists")
            require(array.size() <= ShoppingListPolicy.MAX_LISTS)
            val values = array.map { element ->
                val entry = element.asJsonObject
                ShoppingListPolicy.validate(ShoppingList(
                    id = entry.get("id").asString,
                    title = entry.get("title").asString,
                    month = entry.get("month").asString,
                    createdAtEpochMs = entry.get("created").asLong,
                    items = entry.getAsJsonArray("items").map { row ->
                        val item = row.asJsonObject
                        ShoppingItem(
                            id = item.get("id").asString,
                            name = item.get("name").asString,
                            quantity = item.get("quantity").asInt,
                            unitPriceMinor = item.get("priceMinor")
                                .takeUnless { it.isJsonNull }?.asLong,
                            bought = item.get("bought").asBoolean
                        )
                    }
                ))
            }
            require(values.distinctBy { it.id }.size == values.size)
            return values.sortedWith(compareByDescending<ShoppingList> { it.createdAtEpochMs }
                .thenBy { it.id })
        } catch (failure: Exception) {
            throw IllegalStateException(
                "Could not read shopping lists. Your saved data has not been changed.", failure
            )
        }
    }

    private fun persist(values: List<ShoppingList>) {
        val root = JsonObject().apply {
            addProperty("version", 1)
            add("lists", JsonArray().apply {
                values.forEach { list ->
                    add(JsonObject().apply {
                        addProperty("id", list.id)
                        addProperty("title", list.title)
                        addProperty("month", list.month)
                        addProperty("created", list.createdAtEpochMs)
                        add("items", JsonArray().apply {
                            list.items.forEach { item ->
                                add(JsonObject().apply {
                                    addProperty("id", item.id)
                                    addProperty("name", item.name)
                                    addProperty("quantity", item.quantity)
                                    if (item.unitPriceMinor == null) add("priceMinor", null)
                                    else addProperty("priceMinor", item.unitPriceMinor)
                                    addProperty("bought", item.bought)
                                })
                            }
                        })
                    })
                }
            })
        }
        check(preferences.edit().putString(KEY, root.toString()).commit()) {
            "Shopping list could not be saved. Please try again."
        }
    }
}
