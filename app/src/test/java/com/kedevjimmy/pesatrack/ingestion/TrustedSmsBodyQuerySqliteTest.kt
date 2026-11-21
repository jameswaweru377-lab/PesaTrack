package com.kedevjimmy.pesatrack.ingestion

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Runs the trusted-sender/body selector against real in-memory SQLite. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE, sdk = [28])
class TrustedSmsBodyQuerySqliteTest {
    @Test fun referenceFilterDoesNotEscapeApprovedIdAndSenderGroup() {
        SQLiteDatabase.create(null).use { db ->
            db.execSQL("CREATE TABLE sms (_id INTEGER PRIMARY KEY, address TEXT, body TEXT)")
            listOf(
                Triple(1, "EQUITY", "Ref AB_%\\C"),
                Triple(2, "MPESA", "ref ab_%\\c"),
                Triple(3, "PRIVATE", "Ref AB_%\\C"),
                Triple(4, "EQUITY", "Ref AB_%\\C"),
                Triple(5, "EQUITY", "Ref ABXYC")
            ).forEach { (id, address, body) ->
                db.execSQL("INSERT INTO sms (_id, address, body) VALUES (?, ?, ?)",
                    arrayOf<Any>(id, address, body))
            }

            val query = trustedSmsBodyQuery(
                // The first trusted row has no reference: catches an ungrouped
                // OR expression that applies LIKE only to the last ID.
                listOf(5L to "EQUITY", 1L to "EQUITY", 2L to "MPESA", 3L to "EQUITY"),
                reference = "AB_%\\C"
            )
            val returnedIds = db.rawQuery(
                "SELECT _id FROM sms WHERE ${query.selection} ORDER BY _id",
                query.selectionArgs
            ).use { rows ->
                buildList { while (rows.moveToNext()) add(rows.getLong(0)) }
            }
            assertEquals(listOf(1L, 2L), returnedIds)
        }
    }
}
