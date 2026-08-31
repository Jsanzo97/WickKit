package io.wickkit.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import java.io.File

internal object DatabaseDiscovery {

    private val REVERSE_DOMAIN_PREFIXES = listOf(
        "com.", "org.", "io.", "net.", "de.", "me.", "co.", "uk.", "fr.", "es.",
    )

    private val EXCLUDED_SIMPLE_PREFIXES = listOf(
        "firebase", // Firebase SDK databases
        "google_app_measurement", // Firebase Analytics
        "gtm_", // Google Tag Manager
    )

    fun findDatabases(context: Context): List<DatabaseEntry> = context.getDatabasePath("_").parentFile
        ?.listFiles()
        ?.filter { it.isFile && !isAuxFile(it.name) && !isExcluded(it.nameWithoutExtension) }
        ?.map { toEntry(it) }
        ?.sortedBy { it.name }
        ?: emptyList()

    private fun isAuxFile(name: String) = name.endsWith("-wal") || name.endsWith("-shm") || name.endsWith("-journal")

    private fun isExcluded(name: String) = REVERSE_DOMAIN_PREFIXES.any { name.startsWith(it) } ||
        EXCLUDED_SIMPLE_PREFIXES.any { name.startsWith(it) }

    private fun toEntry(file: File): DatabaseEntry {
        val status = runCatching {
            SQLiteDatabase.openDatabase(
                file.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY,
            ).close()
            DatabaseStatus.Ok
        }.getOrElse { throwable ->
            when {
                throwable is SQLiteException &&
                    throwable.message?.contains("file is not a database", ignoreCase = true) == true ->
                    DatabaseStatus.Encrypted

                throwable is SQLiteException -> DatabaseStatus.Unsupported

                else -> DatabaseStatus.Unsupported
            }
        }
        return DatabaseEntry(
            name = file.name,
            path = file.absolutePath,
            sizeBytes = file.length(),
            status = status,
        )
    }
}
