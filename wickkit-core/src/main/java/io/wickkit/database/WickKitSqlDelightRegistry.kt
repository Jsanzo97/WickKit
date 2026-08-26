package io.wickkit.database

import java.lang.ref.WeakReference

internal object WickKitSqlDelightRegistry {

    private val lock = Any()
    private val drivers = mutableListOf<WeakReference<Any>>()

    @JvmStatic
    fun register(driver: Any) {
        synchronized(lock) {
            drivers.removeAll { it.get() == null }
            if (drivers.none { it.get() === driver }) {
                drivers.add(WeakReference(driver))
            }
        }
    }

    fun notifyTable(tableName: String) {
        val snapshot = synchronized(lock) {
            drivers.removeAll { it.get() == null }
            drivers.mapNotNull { it.get() }
        }
        for (driver in snapshot) {
            runCatching {
                driver.javaClass
                    .getMethod("notifyListeners", Array<String>::class.java)
                    .invoke(driver, arrayOf(tableName) as Any)
            }
        }
    }
}
