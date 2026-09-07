package io.wickkit.threads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch

class WickKitThreadManagerTest {

    @Before
    fun setUp() {
        WickKitThreadManager.clear()
    }

    // region buildEntries

    @Test
    fun `buildEntries returns empty list for empty map`() {
        val result = WickKitThreadManager.buildEntries(emptyMap())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `buildEntries maps thread name correctly`() {
        val thread = Thread.currentThread()
        val result = WickKitThreadManager.buildEntries(mapOf(thread to emptyArray()))
        assertEquals(thread.name, result.single().name)
    }

    @Test
    fun `buildEntries maps isDaemon as true for daemon thread`() {
        val thread = Thread {}
        thread.isDaemon = true
        val result = WickKitThreadManager.buildEntries(mapOf(thread to emptyArray()))
        assertTrue(result.single().isDaemon)
    }

    @Test
    fun `buildEntries maps isDaemon as false for user thread`() {
        val thread = Thread {}
        thread.isDaemon = false
        val result = WickKitThreadManager.buildEntries(mapOf(thread to emptyArray()))
        assertFalse(result.single().isDaemon)
    }

    @Test
    fun `buildEntries maps priority correctly`() {
        val thread = Thread.currentThread()
        val result = WickKitThreadManager.buildEntries(mapOf(thread to emptyArray()))
        assertEquals(thread.priority, result.single().priority)
    }

    @Test
    fun `buildEntries maps thread group name correctly`() {
        val thread = Thread.currentThread()
        val result = WickKitThreadManager.buildEntries(mapOf(thread to emptyArray()))
        assertEquals(thread.threadGroup?.name.orEmpty(), result.single().threadGroup)
    }

    @Test
    fun `buildEntries converts stack trace elements to strings`() {
        val thread = Thread.currentThread()
        val element = StackTraceElement("com.example.Foo", "bar", "Foo.kt", 42)
        val result = WickKitThreadManager.buildEntries(mapOf(thread to arrayOf(element)))
        assertEquals(element.toString(), result.single().stackTrace.single())
    }

    @Test
    fun `buildEntries preserves stack trace element order`() {
        val thread = Thread.currentThread()
        val elementA = StackTraceElement("com.example.Foo", "alpha", "Foo.kt", 10)
        val elementB = StackTraceElement("com.example.Foo", "beta", "Foo.kt", 20)
        val elementC = StackTraceElement("com.example.Foo", "gamma", "Foo.kt", 30)
        val result = WickKitThreadManager.buildEntries(
            mapOf(thread to arrayOf(elementA, elementB, elementC)),
        )
        val stackTrace = result.single().stackTrace
        assertEquals(elementA.toString(), stackTrace[0])
        assertEquals(elementB.toString(), stackTrace[1])
        assertEquals(elementC.toString(), stackTrace[2])
    }

    @Test
    fun `buildEntries assigns sequential ids starting from zero`() {
        val threadA = Thread {}
        val threadB = Thread {}
        val result = WickKitThreadManager.buildEntries(
            mapOf(threadA to emptyArray(), threadB to emptyArray()),
        )
        val sortedIds = result.map { it.id }.sorted()
        assertEquals(listOf(0L, 1L), sortedIds)
    }

    @Test
    fun `buildEntries excludes TERMINATED threads`() {
        val thread = Thread {}
        thread.start()
        thread.join()
        assertEquals(Thread.State.TERMINATED, thread.state)
        val result = WickKitThreadManager.buildEntries(mapOf(thread to emptyArray()))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `buildEntries maps Thread state correctly`() {
        val thread = Thread.currentThread()
        val result = WickKitThreadManager.buildEntries(mapOf(thread to emptyArray()))
        assertEquals(thread.state, result.single().state)
    }

    @Test
    fun `buildEntries orders RUNNABLE threads before NEW threads`() {
        val runnableThread = Thread.currentThread()
        val newThread = Thread {}
        newThread.name = "zzz-new-thread"
        val result = WickKitThreadManager.buildEntries(
            mapOf(newThread to emptyArray(), runnableThread to emptyArray()),
        )
        val runnableIndex = result.indexOfFirst { it.state == Thread.State.RUNNABLE }
        val newIndex = result.indexOfFirst { it.state == Thread.State.NEW }
        assertTrue(runnableIndex >= 0)
        assertTrue(newIndex >= 0)
        assertTrue(runnableIndex < newIndex)
    }

    @Test
    fun `buildEntries sorts threads with same state alphabetically by name`() {
        val threadA = Thread {}
        val threadB = Thread {}
        val threadC = Thread {}
        threadA.name = "aaa"
        threadB.name = "bbb"
        threadC.name = "ccc"
        val result = WickKitThreadManager.buildEntries(
            mapOf(threadC to emptyArray(), threadA to emptyArray(), threadB to emptyArray()),
        )
        val newThreads = result.filter { it.state == Thread.State.NEW }
        assertEquals(listOf("aaa", "bbb", "ccc"), newThreads.map { it.name })
    }

    @Test
    fun `buildEntries maps TIMED_WAITING state correctly`() {
        val thread = Thread { Thread.sleep(10_000) }
        thread.isDaemon = true
        thread.name = "wk-test-sleeping"
        thread.start()
        awaitState(thread, Thread.State.TIMED_WAITING)
        try {
            val result = WickKitThreadManager.buildEntries(mapOf(thread to emptyArray()))
            assertEquals(Thread.State.TIMED_WAITING, result.single().state)
        } finally {
            thread.interrupt()
            thread.join(500)
        }
    }

    @Test
    fun `buildEntries maps WAITING state correctly`() {
        val latch = CountDownLatch(1)
        val thread = Thread { latch.await() }
        thread.isDaemon = true
        thread.name = "wk-test-waiting"
        thread.start()
        awaitState(thread, Thread.State.WAITING)
        try {
            val result = WickKitThreadManager.buildEntries(mapOf(thread to emptyArray()))
            assertEquals(Thread.State.WAITING, result.single().state)
        } finally {
            latch.countDown()
            thread.join(500)
        }
    }

    @Test
    fun `buildEntries maps BLOCKED state correctly`() {
        val lock = Any()
        val holder = Thread { synchronized(lock) { Thread.sleep(10_000) } }
        holder.isDaemon = true
        holder.name = "wk-test-lock-holder"
        holder.start()
        awaitState(holder, Thread.State.TIMED_WAITING)
        val blocked = Thread { synchronized(lock) { } }
        blocked.isDaemon = true
        blocked.name = "wk-test-blocked"
        blocked.start()
        awaitState(blocked, Thread.State.BLOCKED)
        try {
            val result = WickKitThreadManager.buildEntries(mapOf(blocked to emptyArray()))
            assertEquals(Thread.State.BLOCKED, result.single().state)
        } finally {
            holder.interrupt()
            holder.join(500)
            blocked.join(500)
        }
    }

    @Test
    fun `buildEntries orders BLOCKED threads before RUNNABLE threads`() {
        val lock = Any()
        val holder = Thread { synchronized(lock) { Thread.sleep(10_000) } }
        holder.isDaemon = true
        holder.name = "wk-test-lock-holder"
        holder.start()
        awaitState(holder, Thread.State.TIMED_WAITING)
        val blocked = Thread { synchronized(lock) { } }
        blocked.isDaemon = true
        blocked.name = "zzz-blocked"
        blocked.start()
        awaitState(blocked, Thread.State.BLOCKED)
        try {
            val result = WickKitThreadManager.buildEntries(
                mapOf(blocked to emptyArray(), Thread.currentThread() to emptyArray()),
            )
            val blockedIndex = result.indexOfFirst { it.state == Thread.State.BLOCKED }
            val runnableIndex = result.indexOfFirst { it.state == Thread.State.RUNNABLE }
            assertTrue(blockedIndex >= 0)
            assertTrue(runnableIndex >= 0)
            assertTrue(blockedIndex < runnableIndex)
        } finally {
            holder.interrupt()
            holder.join(500)
            blocked.join(500)
        }
    }

    // endregion

    // region refresh / clear

    @Test
    fun `refresh populates entries with active threads`() {
        WickKitThreadManager.refresh()
        assertTrue(WickKitThreadManager.entries.value.isNotEmpty())
    }

    @Test
    fun `refresh does not include TERMINATED threads`() {
        WickKitThreadManager.refresh()
        val terminated = WickKitThreadManager.entries.value.filter {
            it.state == Thread.State.TERMINATED
        }
        assertTrue(terminated.isEmpty())
    }

    @Test
    fun `clear resets entries to empty list`() {
        WickKitThreadManager.refresh()
        assertTrue(WickKitThreadManager.entries.value.isNotEmpty())
        WickKitThreadManager.clear()
        assertTrue(WickKitThreadManager.entries.value.isEmpty())
    }

    // endregion

    // region helpers

    private fun awaitState(thread: Thread, state: Thread.State) {
        val deadline = System.currentTimeMillis() + 2_000
        while (thread.state != state && System.currentTimeMillis() < deadline) {
            Thread.sleep(10)
        }
    }

    // endregion
}
