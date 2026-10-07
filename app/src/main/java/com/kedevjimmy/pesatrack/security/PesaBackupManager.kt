package com.kedevjimmy.pesatrack.security

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.Base64
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.kedevjimmy.pesatrack.data.local.CanonicalStoreLock
import com.kedevjimmy.pesatrack.data.local.LedgerUpdateBus
import com.kedevjimmy.pesatrack.data.local.PesaTrackDatabase
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class BackupSummary(
    val tableCount: Int,
    val rowCount: Int,
    val preferenceFileCount: Int
)

data class RestoreSummary(
    val tableCount: Int,
    val rowCount: Int,
    val preferenceFileCount: Int
)

class PesaBackupManager(
    context: Context
) {
    private val app =
        context.applicationContext

    fun exportEncrypted(
        uri: Uri,
        passphrase: String
    ): BackupSummary {
        requirePassphrase(passphrase)

        val payload =
            synchronized(
                CanonicalStoreLock.monitor
            ) {
                buildPayload()
            }
        val plain =
            payload.toString()
                .toByteArray(
                    Charsets.UTF_8
                )
        val encrypted =
            encrypt(
                plain = plain,
                passphrase =
                    passphrase
            )

        app.contentResolver
            .openOutputStream(
                uri,
                "wt"
            )
            ?.use {
                it.write(encrypted)
                it.flush()
            }
            ?: error(
                "Could not open the selected backup file."
            )

        val tables =
            payload
                .getAsJsonObject(
                    "tables"
                )
        var rows = 0
        tables.entrySet()
            .forEach {
                rows +=
                    it.value
                        .asJsonArray
                        .size()
            }

        return BackupSummary(
            tableCount =
                tables.size(),
            rowCount = rows,
            preferenceFileCount =
                payload
                    .getAsJsonObject(
                        "preferences"
                    )
                    .size()
        )
    }

    fun restoreEncrypted(
        uri: Uri,
        passphrase: String
    ): RestoreSummary {
        requirePassphrase(passphrase)

        val encrypted =
            app.contentResolver
                .openInputStream(uri)
                ?.use {
                    it.readBytes()
                }
                ?: error(
                    "Could not open the selected backup file."
                )
        val plain =
            decrypt(
                encrypted = encrypted,
                passphrase =
                    passphrase
            )
        val payload =
            runCatching {
                JsonParser
                    .parseString(
                        String(
                            plain,
                            Charsets.UTF_8
                        )
                    )
                    .asJsonObject
            }.getOrElse {
                throw IllegalArgumentException(
                    "This backup is damaged or the passphrase is incorrect."
                )
            }

        val formatVersion =
            payload
                .get("formatVersion")
                ?.asInt
                ?: 0
        require(
            formatVersion ==
                FORMAT_VERSION
        ) {
            "This backup format is not supported."
        }

        return synchronized(
            CanonicalStoreLock.monitor
        ) {
            restorePayload(
                payload
            )
        }
    }

    private fun buildPayload(): JsonObject {
        val database =
            PesaTrackDatabase
                .get(app)
                .readableDatabase
        val root =
            JsonObject().apply {
                addProperty(
                    "formatVersion",
                    FORMAT_VERSION
                )
                addProperty(
                    "schemaVersion",
                    database.version
                )
                addProperty(
                    "createdAtEpochMs",
                    System
                        .currentTimeMillis()
                )
            }

        val tables =
            JsonObject()
        userTableNames(
            database
        ).forEach { table ->
            tables.add(
                table,
                dumpTable(
                    database,
                    table
                )
            )
        }
        root.add(
            "tables",
            tables
        )
        root.add(
            "preferences",
            dumpPreferences()
        )
        return root
    }

    private fun restorePayload(
        root: JsonObject
    ): RestoreSummary {
        val database =
            PesaTrackDatabase
                .get(app)
                .writableDatabase
        val backupSchema =
            root.get("schemaVersion")
                ?.asInt
                ?: 0
        require(
            backupSchema > 0 &&
                backupSchema <=
                database.version
        ) {
            "This backup was created by a newer PesaTrack version."
        }

        val currentTables =
            userTableNames(
                database
            )
        val currentColumns =
            currentTables
                .associateWith {
                    tableColumns(
                        database,
                        it
                    )
                }
        val backupTables =
            root.getAsJsonObject(
                "tables"
            ) ?: JsonObject()

        var restoredRows = 0
        var restoredTables = 0

        database.beginTransaction()
        try {
            currentTables
                .forEach { table ->
                    database.delete(
                        table,
                        null,
                        null
                    )
                }

            backupTables
                .entrySet()
                .forEach {
                        (table, value) ->
                    val validColumns =
                        currentColumns[
                            table
                        ]
                            ?: return@forEach
                    val rows =
                        value.asJsonArray

                    rows.forEach {
                            rowElement ->
                        val values =
                            ContentValues()
                        val row =
                            rowElement
                                .asJsonObject
                        row.entrySet()
                            .forEach {
                                    (
                                        column,
                                        cellElement
                                    ) ->
                                if (
                                    column in
                                    validColumns
                                ) {
                                    putCell(
                                        values =
                                            values,
                                        column =
                                            column,
                                        cell =
                                            cellElement
                                                .asJsonObject
                                    )
                                }
                            }

                        database.insertOrThrow(
                            table,
                            null,
                            values
                        )
                        restoredRows++
                    }
                    restoredTables++
                }

            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }

        val preferences =
            root.getAsJsonObject(
                "preferences"
            ) ?: JsonObject()
        restorePreferences(
            preferences
        )

        LedgerUpdateBus
            .notifyChanged()

        return RestoreSummary(
            tableCount =
                restoredTables,
            rowCount =
                restoredRows,
            preferenceFileCount =
                preferences.size()
        )
    }

    private fun dumpTable(
        database: SQLiteDatabase,
        table: String
    ): JsonArray {
        val rows =
            JsonArray()
        database.rawQuery(
            "SELECT * FROM " +
                quoteIdentifier(
                    table
                ),
            null
        ).use { cursor ->
            while (
                cursor.moveToNext()
            ) {
                val row =
                    JsonObject()
                cursor.columnNames
                    .forEachIndexed {
                            index,
                            column ->
                        row.add(
                            column,
                            cursorCell(
                                cursor,
                                index
                            )
                        )
                    }
                rows.add(row)
            }
        }
        return rows
    }

    private fun cursorCell(
        cursor: Cursor,
        index: Int
    ): JsonObject =
        JsonObject().apply {
            when (
                cursor.getType(index)
            ) {
                Cursor.FIELD_TYPE_NULL -> {
                    addProperty(
                        "type",
                        "null"
                    )
                }

                Cursor.FIELD_TYPE_INTEGER -> {
                    addProperty(
                        "type",
                        "integer"
                    )
                    addProperty(
                        "value",
                        cursor.getLong(
                            index
                        )
                    )
                }

                Cursor.FIELD_TYPE_FLOAT -> {
                    addProperty(
                        "type",
                        "float"
                    )
                    addProperty(
                        "value",
                        cursor.getDouble(
                            index
                        )
                    )
                }

                Cursor.FIELD_TYPE_BLOB -> {
                    addProperty(
                        "type",
                        "blob"
                    )
                    addProperty(
                        "value",
                        Base64
                            .encodeToString(
                                cursor.getBlob(
                                    index
                                ),
                                Base64.NO_WRAP
                            )
                    )
                }

                else -> {
                    addProperty(
                        "type",
                        "string"
                    )
                    addProperty(
                        "value",
                        cursor.getString(
                            index
                        )
                    )
                }
            }
        }

    private fun putCell(
        values: ContentValues,
        column: String,
        cell: JsonObject
    ) {
        when (
            cell.get("type")
                ?.asString
        ) {
            "null" ->
                values.putNull(
                    column
                )
            "integer" ->
                values.put(
                    column,
                    cell.get("value")
                        .asLong
                )
            "float" ->
                values.put(
                    column,
                    cell.get("value")
                        .asDouble
                )
            "blob" ->
                values.put(
                    column,
                    Base64.decode(
                        cell.get("value")
                            .asString,
                        Base64.NO_WRAP
                    )
                )
            else ->
                values.put(
                    column,
                    cell.get("value")
                        ?.asString
                )
        }
    }

    private fun userTableNames(
        database: SQLiteDatabase
    ): List<String> =
        database.rawQuery(
            """
            SELECT name
            FROM sqlite_master
            WHERE type = 'table'
              AND name NOT LIKE 'sqlite_%'
              AND name != 'android_metadata'
            ORDER BY name
            """.trimIndent(),
            null
        ).use { cursor ->
            buildList {
                while (
                    cursor.moveToNext()
                ) {
                    add(
                        cursor.getString(0)
                    )
                }
            }
        }

    private fun tableColumns(
        database: SQLiteDatabase,
        table: String
    ): Set<String> =
        database.rawQuery(
            "PRAGMA table_info(" +
                quoteIdentifier(
                    table
                ) +
                ")",
            null
        ).use { cursor ->
            buildSet {
                val nameIndex =
                    cursor
                        .getColumnIndexOrThrow(
                            "name"
                        )
                while (
                    cursor.moveToNext()
                ) {
                    add(
                        cursor.getString(
                            nameIndex
                        )
                    )
                }
            }
        }

    private fun dumpPreferences(): JsonObject {
        val result =
            JsonObject()
        preferenceNames()
            .forEach { name ->
                val values =
                    app.getSharedPreferences(
                        name,
                        Context.MODE_PRIVATE
                    ).all
                val json =
                    JsonObject()
                values.forEach {
                        (key, value) ->
                    preferenceCell(
                        value
                    )
                        ?.let {
                            json.add(
                                key,
                                it
                            )
                        }
                }
                result.add(
                    name,
                    json
                )
            }
        return result
    }

    private fun restorePreferences(
        preferences: JsonObject
    ) {
        preferences.entrySet()
            .forEach {
                    (
                        name,
                        valuesElement
                    ) ->
                if (
                    !isBackupPreference(
                        name
                    )
                ) {
                    return@forEach
                }
                val editor =
                    app.getSharedPreferences(
                        name,
                        Context.MODE_PRIVATE
                    )
                        .edit()
                        .clear()
                valuesElement
                    .asJsonObject
                    .entrySet()
                    .forEach {
                            (
                                key,
                                cellElement
                            ) ->
                        val cell =
                            cellElement
                                .asJsonObject
                        when (
                            cell
                                .get("type")
                                ?.asString
                        ) {
                            "boolean" ->
                                editor.putBoolean(
                                    key,
                                    cell.get("value")
                                        .asBoolean
                                )
                            "int" ->
                                editor.putInt(
                                    key,
                                    cell.get("value")
                                        .asInt
                                )
                            "long" ->
                                editor.putLong(
                                    key,
                                    cell.get("value")
                                        .asLong
                                )
                            "float" ->
                                editor.putFloat(
                                    key,
                                    cell.get("value")
                                        .asFloat
                                )
                            "set" ->
                                editor
                                    .putStringSet(
                                        key,
                                        cell
                                            .getAsJsonArray(
                                                "value"
                                            )
                                            .map {
                                                it.asString
                                            }
                                            .toSet()
                                    )
                            else ->
                                editor.putString(
                                    key,
                                    cell.get("value")
                                        ?.asString
                                )
                        }
                    }
                editor.apply()
            }
    }

    private fun preferenceCell(
        value: Any?
    ): JsonObject? =
        when (value) {
            is Boolean ->
                typedPreference(
                    "boolean",
                    value
                )
            is Int ->
                typedPreference(
                    "int",
                    value
                )
            is Long ->
                typedPreference(
                    "long",
                    value
                )
            is Float ->
                typedPreference(
                    "float",
                    value
                )
            is String ->
                typedPreference(
                    "string",
                    value
                )
            is Set<*> ->
                JsonObject().apply {
                    addProperty(
                        "type",
                        "set"
                    )
                    add(
                        "value",
                        JsonArray().apply {
                            value
                                .filterIsInstance<
                                    String
                                    >()
                                .forEach {
                                    add(it)
                                }
                        }
                    )
                }
            else -> null
        }

    private fun typedPreference(
        type: String,
        value: Any
    ): JsonObject =
        JsonObject().apply {
            addProperty(
                "type",
                type
            )
            when (value) {
                is Boolean ->
                    addProperty(
                        "value",
                        value
                    )
                is Number ->
                    addProperty(
                        "value",
                        value
                    )
                else ->
                    addProperty(
                        "value",
                        value.toString()
                    )
            }
        }

    private fun preferenceNames(): List<String> {
        val directory =
            File(
                app.applicationInfo
                    .dataDir,
                "shared_prefs"
            )
        return directory
            .listFiles()
            .orEmpty()
            .mapNotNull {
                it.name
                    .takeIf {
                            fileName ->
                        fileName.endsWith(
                            ".xml"
                        )
                    }
                    ?.removeSuffix(
                        ".xml"
                    )
            }
            .filter(
                ::isBackupPreference
            )
            .sorted()
    }

    private fun isBackupPreference(
        name: String
    ): Boolean =
        name !=
            AppLockManager.PREFS_NAME &&
            !name.startsWith(
                "androidx."
            ) &&
            !name.startsWith(
                "com.google."
            ) &&
            !name.contains(
                "workmanager",
                ignoreCase = true
            )

    private fun encrypt(
        plain: ByteArray,
        passphrase: String
    ): ByteArray {
        val salt =
            ByteArray(SALT_BYTES)
                .also {
                    SecureRandom()
                        .nextBytes(it)
                }
        val iv =
            ByteArray(IV_BYTES)
                .also {
                    SecureRandom()
                        .nextBytes(it)
                }
        val key =
            deriveKey(
                passphrase,
                salt
            )
        val cipher =
            Cipher.getInstance(
                "AES/GCM/NoPadding"
            )
        cipher.init(
            Cipher.ENCRYPT_MODE,
            key,
            GCMParameterSpec(
                GCM_TAG_BITS,
                iv
            )
        )
        val encrypted =
            cipher.doFinal(plain)

        return ByteArrayOutputStream()
            .also {
                    buffer ->
                DataOutputStream(
                    buffer
                ).use { output ->
                    output.write(
                        MAGIC
                    )
                    output.writeInt(
                        PBKDF_ITERATIONS
                    )
                    output.writeInt(
                        salt.size
                    )
                    output.write(salt)
                    output.writeInt(
                        iv.size
                    )
                    output.write(iv)
                    output.writeInt(
                        encrypted.size
                    )
                    output.write(
                        encrypted
                    )
                }
            }
            .toByteArray()
    }

    private fun decrypt(
        encrypted: ByteArray,
        passphrase: String
    ): ByteArray {
        val input =
            DataInputStream(
                ByteArrayInputStream(
                    encrypted
                )
            )
        val magic =
            ByteArray(MAGIC.size)
        input.readFully(magic)
        require(
            magic.contentEquals(
                MAGIC
            )
        ) {
            "This is not a PesaTrack backup."
        }

        val iterations =
            input.readInt()
        require(
            iterations in
                50_000..500_000
        ) {
            "Invalid backup encryption parameters."
        }

        val saltSize =
            input.readInt()
        require(
            saltSize in 8..64
        ) {
            "Invalid backup salt."
        }
        val salt =
            ByteArray(saltSize)
        input.readFully(salt)

        val ivSize =
            input.readInt()
        require(
            ivSize in 12..32
        ) {
            "Invalid backup IV."
        }
        val iv =
            ByteArray(ivSize)
        input.readFully(iv)

        val cipherSize =
            input.readInt()
        require(
            cipherSize > 0 &&
                cipherSize <=
                encrypted.size
        ) {
            "Invalid backup payload."
        }
        val cipherBytes =
            ByteArray(cipherSize)
        input.readFully(
            cipherBytes
        )

        val key =
            deriveKey(
                passphrase,
                salt,
                iterations
            )
        return try {
            Cipher.getInstance(
                "AES/GCM/NoPadding"
            ).apply {
                init(
                    Cipher.DECRYPT_MODE,
                    key,
                    GCMParameterSpec(
                        GCM_TAG_BITS,
                        iv
                    )
                )
            }.doFinal(
                cipherBytes
            )
        } catch (
            failure: Exception
        ) {
            throw IllegalArgumentException(
                "Could not decrypt this backup. Check the passphrase and file.",
                failure
            )
        }
    }

    private fun deriveKey(
        passphrase: String,
        salt: ByteArray,
        iterations: Int =
            PBKDF_ITERATIONS
    ): SecretKeySpec {
        val spec =
            PBEKeySpec(
                passphrase
                    .toCharArray(),
                salt,
                iterations,
                KEY_BITS
            )
        return try {
            SecretKeySpec(
                SecretKeyFactory
                    .getInstance(
                        "PBKDF2WithHmacSHA256"
                    )
                    .generateSecret(
                        spec
                    )
                    .encoded,
                "AES"
            )
        } finally {
            spec.clearPassword()
        }
    }

    private fun quoteIdentifier(
        value: String
    ): String =
        """ +
            value.replace(
                """,
                """"
            ) +
            """

    private fun requirePassphrase(
        passphrase: String
    ) {
        require(
            passphrase.length >= 8
        ) {
            "Use a backup passphrase with at least 8 characters."
        }
    }

    companion object {
        private const val FORMAT_VERSION =
            1
        private const val PBKDF_ITERATIONS =
            180_000
        private const val KEY_BITS =
            256
        private const val SALT_BYTES =
            16
        private const val IV_BYTES =
            12
        private const val GCM_TAG_BITS =
            128
        private val MAGIC =
            "PESABK01"
                .toByteArray(
                    Charsets.US_ASCII
                )
    }
}
