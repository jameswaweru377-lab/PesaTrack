package com.kedevjimmy.pesatrack.data.local

import android.database.sqlite.SQLiteDatabase

/** Builds the actual v33 debt table without newer SQLite DROP COLUMN syntax. */
internal fun removeV34DebtProvenanceColumn(database: SQLiteDatabase) {
    val createSql = database.rawQuery(
        "SELECT sql FROM sqlite_master WHERE type='table' AND name='debt_events'", null
    ).use { cursor -> check(cursor.moveToFirst()); cursor.getString(0) }
    val legacySql = createSql.replace("amount_is_net_proceeds INTEGER,", "")
    check(legacySql != createSql)
    val indexes = database.rawQuery(
        "SELECT sql FROM sqlite_master WHERE type='index' AND tbl_name='debt_events' AND sql IS NOT NULL", null
    ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }
    val columns = database.rawQuery("PRAGMA table_info(debt_events)", null).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                if (name != "amount_is_net_proceeds") add(name)
            }
        }
    }.joinToString(",") { "\"$it\"" }
    database.beginTransaction()
    try {
        database.execSQL("ALTER TABLE debt_events RENAME TO debt_events_v34_fixture")
        database.execSQL(legacySql)
        database.execSQL("INSERT INTO debt_events($columns) SELECT $columns FROM debt_events_v34_fixture")
        database.execSQL("DROP TABLE debt_events_v34_fixture")
        indexes.forEach { database.execSQL(it) }
        database.setTransactionSuccessful()
    } finally {
        database.endTransaction()
    }
}
