package com.kedevjimmy.pesatrack.security

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.AtomicFile
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.util.UUID

/** Intent precedes SQLite commit; its marker commits in the same transaction as the ledger. */
internal class BackupRestoreJournal(private val context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "backup-restore-journal.json"))

    fun stage(database: SQLiteDatabase, preferences: JsonObject): String {
        recover(database)
        // An operational table, excluded from financial exports and replacement.
        database.execSQL("CREATE TABLE IF NOT EXISTS $MARKER_TABLE (operation_id TEXT NOT NULL)")
        val id = UUID.randomUUID().toString()
        val intent = JsonObject().apply {
            addProperty("operationId", id)
            add("preferences", JsonObject().apply {
                preferences.entrySet().filter { BackupPreferences.allowed(it.key) }
                    .forEach { (name, values) -> add(name, values) }
            })
        }
        val serialized = intent.toString().toByteArray(Charsets.UTF_8)
        val output = file.startWrite()
        try {
            output.write(serialized)
            output.fd.sync()
            file.finishWrite(output)
            // AtomicFile logs some rename failures instead of throwing; never commit without intent.
            check(file.baseFile.isFile && file.baseFile.length() == serialized.size.toLong()) {
                "Restore could not stage durable settings. The current ledger was kept."
            }
        } catch (failure: Throwable) {
            file.failWrite(output)
            throw failure
        }
        return id
    }

    fun markCommitted(database: SQLiteDatabase, operationId: String) {
        check(database.inTransaction())
        database.delete(MARKER_TABLE, null, null)
        database.execSQL("INSERT INTO $MARKER_TABLE(operation_id) VALUES (?)", arrayOf(operationId))
    }

    fun recover(database: SQLiteDatabase) {
        val exists = database.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(MARKER_TABLE)
        ).use { it.moveToFirst() }
        val committed = if (exists) database.rawQuery(
            "SELECT operation_id FROM $MARKER_TABLE", null
        ).use { if (it.moveToFirst()) it.getString(0) else null } else null
        val intent = try {
            file.openRead().use { JsonParser.parseString(it.readBytes().toString(Charsets.UTF_8)).asJsonObject }
        } catch (missing: java.io.FileNotFoundException) {
            check(committed == null) { "Restore recovery settings are missing. Ledger access is paused." }
            return
        }
        val id = intent.get("operationId").asString
        if (committed != null) {
            check(committed == id) { "Restore recovery does not match the saved ledger." }
            val decoded = BackupPreferences.decode(intent.getAsJsonObject("preferences"))
            BackupPreferences.write(context, decoded)
            database.delete(MARKER_TABLE, null, null)
        }
        // No marker means SQLite rolled back and no preference writes have started.
        file.delete()
    }

    companion object {
        const val MARKER_TABLE = "_pesatrack_restore_state"

        fun isPending(context: Context): Boolean {
            val base = File(context.noBackupFilesDir, "backup-restore-journal.json")
            return base.exists() || File(base.path + ".bak").exists()
        }
    }
}
