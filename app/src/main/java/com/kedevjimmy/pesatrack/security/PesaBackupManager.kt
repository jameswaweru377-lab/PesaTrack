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
import javax.crypto.spec.GCMParameterSpec
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

data class BackupPreview(
    val createdAtEpochMs: Long,
    val schemaVersion: Int,
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

        BackupSizePolicy.requireRestorable(encrypted.size.toLong())

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
            readEncryptedBackup(uri)
        return restoreEncryptedBytes(
            encrypted = encrypted,
            passphrase = passphrase
        )
    }

    fun previewEncrypted(
        uri: Uri,
        passphrase: String
    ): BackupPreview {
        requirePassphrase(passphrase)
        return previewEncryptedBytes(
            encrypted =
                readEncryptedBackup(uri),
            passphrase = passphrase
        )
    }

    internal fun exportEncryptedBytes(
        passphrase: String
    ): ByteArray {
        requirePassphrase(passphrase)
        val payload =
            synchronized(
                CanonicalStoreLock.monitor
            ) {
                buildPayload()
            }
        return encrypt(
            plain =
                payload.toString()
                    .toByteArray(
                        Charsets.UTF_8
                    ),
            passphrase =
                passphrase
        ).also { BackupSizePolicy.requireRestorable(it.size.toLong()) }
    }

    internal fun previewEncryptedBytes(
        encrypted: ByteArray,
        passphrase: String
    ): BackupPreview {
        requirePassphrase(passphrase)
        val payload =
            parseEncryptedPayload(
                encrypted = encrypted,
                passphrase = passphrase
            )
        return backupPreview(
            payload
        )
    }

    internal fun restoreEncryptedBytes(
        encrypted: ByteArray,
        passphrase: String
    ): RestoreSummary {
        requirePassphrase(passphrase)
        val payload =
            parseEncryptedPayload(
                encrypted = encrypted,
                passphrase = passphrase
            )
        validateBackupSchema(
            payload
        )

        return synchronized(
            CanonicalStoreLock.monitor
        ) {
            restorePayload(
                payload
            )
        }
    }

    private fun readEncryptedBackup(
        uri: Uri
    ): ByteArray {
        val knownLength =
            runCatching {
                app.contentResolver
                    .openAssetFileDescriptor(
                        uri,
                        "r"
                    )
                    ?.use {
                        it.length
                    }
            }.getOrNull()
                ?: -1L

        require(
            knownLength < 0L ||
                knownLength <=
                MAX_ENCRYPTED_BACKUP_BYTES
        ) {
            "This backup is too large to restore safely."
        }

        val input =
            app.contentResolver
                .openInputStream(uri)
                ?: error(
                    "Could not open the selected backup file."
                )

        return input.use {
            val output =
                ByteArrayOutputStream()
            val buffer =
                ByteArray(8 * 1024)
            var total = 0L
            while (true) {
                val read =
                    it.read(buffer)
                if (read < 0) {
                    break
                }
                total += read
                require(
                    total <=
                        MAX_ENCRYPTED_BACKUP_BYTES
                ) {
                    "This backup is too large to restore safely."
                }
                output.write(
                    buffer,
                    0,
                    read
                )
            }
            output.toByteArray()
        }
    }

    private fun parseEncryptedPayload(
        encrypted: ByteArray,
        passphrase: String
    ): JsonObject {
        BackupSizePolicy.requireRestorable(encrypted.size.toLong())
        val plain =
            decrypt(
                encrypted = encrypted,
                passphrase = passphrase
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
        return payload
    }

    private fun backupPreview(
        payload: JsonObject
    ): BackupPreview {
        synchronized(CanonicalStoreLock.monitor) {
            validatePayload(payload, PesaTrackDatabase.get(app).readableDatabase)
        }
        val tables =
            payload.getAsJsonObject(
                "tables"
            ) ?: JsonObject()
        val preferences =
            payload.getAsJsonObject(
                "preferences"
            ) ?: JsonObject()
        var rows = 0
        tables.entrySet()
            .forEach {
                rows +=
                    runCatching {
                        it.value
                            .asJsonArray
                            .size()
                    }.getOrElse {
                        throw IllegalArgumentException(
                            "This backup contains damaged table data."
                        )
                    }
            }

        return BackupPreview(
            createdAtEpochMs =
                payload.get(
                    "createdAtEpochMs"
                )
                    ?.asLong
                    ?: 0L,
            schemaVersion =
                payload.get(
                    "schemaVersion"
                )
                    ?.asInt
                    ?: 0,
            tableCount =
                tables.size(),
            rowCount = rows,
            preferenceFileCount =
                preferences.size()
        )
    }

    private fun validateBackupSchema(
        payload: JsonObject
    ) {
        val currentSchema =
            PesaTrackDatabase
                .get(app)
                .readableDatabase
                .version
        val backupSchema =
            payload.get(
                "schemaVersion"
            )
                ?.asInt
                ?: 0
        if (currentSchema == 37 && backupSchema in 34..36) {
            val tables = payload.getAsJsonObject("tables")
                ?: throw IllegalArgumentException("This backup is missing its ledger tables.")
            // New tables have no pre-migration records. Never replace existing data.
            val introducedAt35 = listOf(
                "transaction_allocations", "flexible_budget_options", "statement_import_entries"
            )
            val introducedAt36 = listOf(
                "personal_receivable_people", "personal_receivable_events"
            )
            val missing = (if (backupSchema == 34) introducedAt35 else emptyList()) +
                (if (backupSchema < 36) introducedAt36 else emptyList()) +
                listOf("personal_receivable_review_exclusions")
            missing.forEach { name ->
                require(!tables.has(name)) { "Unexpected table in older backup." }
                tables.add(name, com.google.gson.JsonArray())
            }
            payload.addProperty("schemaVersion", currentSchema)
        }
        require(
            payload.get("schemaVersion")?.asInt == currentSchema
        ) {
            "This backup uses a different database version. Restore requires the same version; older backups need a supported migration."
        }
    }

    private fun buildPayload(): JsonObject {
        val database =
            PesaTrackDatabase
                .get(app)
                .readableDatabase
        BackupRestoreJournal(app).recover(database)
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

    private fun validatePayload(root: JsonObject, database: SQLiteDatabase) {
        validateBackupSchema(root)
        val currentTables = userTableNames(database)
        val currentColumns = currentTables.associateWith { tableColumns(database, it) }
        val backupTables = root.getAsJsonObject("tables")
            ?: throw IllegalArgumentException("This backup is missing its ledger tables.")
        require(backupTables.keySet() == currentTables.toSet()) {
            "This backup does not contain the complete current ledger schema."
        }
        backupTables.entrySet().forEach { (table, rows) ->
            require(rows.isJsonArray) { "This backup contains damaged table data." }
            rows.asJsonArray.forEach { row ->
                require(row.isJsonObject && row.asJsonObject.keySet() == currentColumns.getValue(table)) {
                    "This backup contains damaged row data."
                }
                row.asJsonObject.entrySet().forEach { (column, element) ->
                    require(element.isJsonObject)
                    val cell = element.asJsonObject
                    validateCell(cell)
                    putCell(ContentValues(), column, cell)
                }
            }
        }
        val preferences = root.getAsJsonObject("preferences")
            ?: throw IllegalArgumentException("This backup is missing its settings.")
        BackupPreferences.decode(preferences)
    }

    private fun restorePayload(
        root: JsonObject
    ): RestoreSummary {
        val database =
            PesaTrackDatabase
                .get(app)
                .writableDatabase
        validateBackupSchema(
            root
        )

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

        validatePayload(root, database)
        val preferences = root.getAsJsonObject("preferences")
        val journal = BackupRestoreJournal(app)
        val operationId = journal.stage(database, preferences)

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

            journal.markCommitted(database, operationId)
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }

        try {
            journal.recover(database)
        } catch (failure: Exception) {
            BackupRecoveryStatus.failure = failure
            database.close()
            throw IllegalStateException(
                "The ledger was restored, but settings recovery is pending. Free storage and reopen PesaTrack to finish recovery.",
                failure
            )
        }

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

    private fun validateCell(cell: JsonObject) {
        val type = cell.get("type")
        require(type is com.google.gson.JsonPrimitive && type.isString)
        val value = cell.get("value")
        when (type.asString) {
            "null" -> require(value == null || value.isJsonNull)
            "integer" -> {
                require(value is com.google.gson.JsonPrimitive && value.isNumber)
                require(value.asString.toLongOrNull() != null)
            }
            "float" -> {
                require(value is com.google.gson.JsonPrimitive && value.isNumber)
                require(value.asDouble.isFinite())
            }
            "blob", "string" -> require(value is com.google.gson.JsonPrimitive && value.isString)
            else -> throw IllegalArgumentException("Unsupported backup ledger value.")
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
              AND name != '_pesatrack_restore_state'
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

    private fun isBackupPreference(name: String): Boolean = BackupPreferences.allowed(name)

    private fun encrypt(
        plain: ByteArray,
        passphrase: String
    ): ByteArray {
        // AES-GCM has a fixed tag and no padding; include every wire-header length field.
        val encryptedLength = plain.size.toLong() + MAGIC.size + 4L * 4L +
            SALT_BYTES + IV_BYTES + GCM_TAG_BITS / 8
        BackupSizePolicy.requireRestorable(encryptedLength)
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
        return SecretKeySpec(
            PasswordKeyDerivation.derive(passphrase, salt, iterations, KEY_BITS),
            "AES"
        )
    }

    private fun quoteIdentifier(
        value: String
    ): String {
        val quote =
            34.toChar()
                .toString()
        return quote +
            value.replace(
                quote,
                quote + quote
            ) +
            quote
    }

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
        private const val MAX_ENCRYPTED_BACKUP_BYTES =
            BackupSizePolicy.MAX_ENCRYPTED_BYTES
        private val MAGIC =
            "PESABK01"
                .toByteArray(
                    Charsets.US_ASCII
                )
    }
}
