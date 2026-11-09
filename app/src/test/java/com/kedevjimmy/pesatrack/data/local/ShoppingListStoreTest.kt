package com.kedevjimmy.pesatrack.data.local

import android.app.Application
import com.kedevjimmy.pesatrack.domain.planning.ShoppingItem
import com.kedevjimmy.pesatrack.domain.planning.ShoppingList
import com.kedevjimmy.pesatrack.security.BackupPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE, sdk = [28])
class ShoppingListStoreTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Before fun clear() {
        context.getSharedPreferences(ShoppingListStore.PREFS_NAME, 0).edit().clear().commit()
    }

    @Test fun listSurvivesStoreRecreationAndEdits() {
        val first = ShoppingList("one", "Monthly shop", "2026-11", 500L)
        ShoppingListStore(context).save(first)
        assertEquals(listOf(first), ShoppingListStore(context).list())

        val changed = first.copy(items = listOf(ShoppingItem("rice", "Rice", 2, 42000L)))
        ShoppingListStore(context).save(changed)
        assertEquals(listOf(changed), ShoppingListStore(context).list())
        ShoppingListStore(context).delete("one")
        assertTrue(ShoppingListStore(context).list().isEmpty())
    }

    @Test fun corruptJsonDoesNotGetOverwrittenBySave() {
        context.getSharedPreferences(ShoppingListStore.PREFS_NAME, 0).edit()
            .putString("lists", "{broken").commit()
        assertThrows(IllegalStateException::class.java) { ShoppingListStore(context).list() }
        assertThrows(IllegalStateException::class.java) {
            ShoppingListStore(context).save(ShoppingList("two", "Groceries", "2026-11", 600L))
        }
        assertEquals("{broken", context.getSharedPreferences(ShoppingListStore.PREFS_NAME, 0)
            .getString("lists", null))
    }

    @Test fun invalidListIsNotSaved() {
        assertThrows(IllegalArgumentException::class.java) {
            ShoppingListStore(context).save(ShoppingList("one", " ", "2026-11", 500L))
        }
        assertTrue(ShoppingListStore(context).list().isEmpty())
    }

    @Test fun privateShoppingPreferencesRemainEligibleForEncryptedBackup() {
        assertTrue(BackupPreferences.allowed(ShoppingListStore.PREFS_NAME))
    }
}
