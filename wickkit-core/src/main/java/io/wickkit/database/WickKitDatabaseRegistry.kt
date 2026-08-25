package io.wickkit.database

import android.database.sqlite.SQLiteDatabase
import java.lang.ref.WeakReference

internal object WickKitDatabaseRegistry {

    private val lock = Any()
    private val connections = mutableListOf<WeakReference<SQLiteDatabase>>()

    @JvmStatic
    fun register(db: SQLiteDatabase) {
        synchronized(lock) {
            connections.removeAll { it.get() == null }
            if (connections.none { it.get() === db }) {
                connections.add(WeakReference(db))
            }
        }
    }

    fun find(path: String): SQLiteDatabase? = synchronized(lock) {
        connections.removeAll { it.get() == null }
        connections.mapNotNull { it.get() }.firstOrNull { db ->
            db.isOpen && !db.isReadOnly && db.path == path
        }
    }
}
