package io.wickkit.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import java.io.File

object DatabaseDiscovery {

    fun findDatabases(context: Context): List<DatabaseEntry> {
        val dir = context.getDatabasePath("_").parentFile ?: return emptyList()
        return (dir.listFiles() ?: return emptyList())
            .filter { it.isFile && !isAuxFile(it.name) }
            .map { toEntry(it) }
            .sortedBy { it.name }
    }

    private fun isAuxFile(name: String) = name.endsWith("-wal") || name.endsWith("-shm") || name.endsWith("-journal")

    private fun toEntry(file: File): DatabaseEntry {
        val status = runCatching {
            SQLiteDatabase.openDatabase(
                file.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY,
            ).close()
            DatabaseStatus.Ok
        }.getOrElse { e ->
            if (e is SQLiteException) DatabaseStatus.Encrypted else DatabaseStatus.Unsupported
        }
        return DatabaseEntry(
            name = file.name,
            path = file.absolutePath,
            sizeBytes = file.length(),
            status = status,
        )
    }
}
