package io.wickkit.database

import org.junit.Assert.assertEquals
import org.junit.Test

class WickKitSqlDelightRegistryTest {

    class FakeDriver {
        val notifications = mutableListOf<String>()

        @Suppress("UNUSED")
        fun notifyListeners(tables: Array<String>) {
            notifications.addAll(tables)
        }
    }

    @Test
    fun `register and notifyTable invokes notifyListeners on driver`() {
        val driver = FakeDriver()
        WickKitSqlDelightRegistry.register(driver)

        WickKitSqlDelightRegistry.notifyTable("orders")

        assertEquals(listOf("orders"), driver.notifications)
    }

    @Test
    fun `register deduplicates same driver instance`() {
        val driver = FakeDriver()
        WickKitSqlDelightRegistry.register(driver)
        WickKitSqlDelightRegistry.register(driver)

        WickKitSqlDelightRegistry.notifyTable("products")

        assertEquals(listOf("products"), driver.notifications)
    }

    @Test
    fun `notifyTable is resilient to drivers without notifyListeners`() {
        val driverWithoutMethod = object {}
        WickKitSqlDelightRegistry.register(driverWithoutMethod)

        WickKitSqlDelightRegistry.notifyTable("users")
    }
}
