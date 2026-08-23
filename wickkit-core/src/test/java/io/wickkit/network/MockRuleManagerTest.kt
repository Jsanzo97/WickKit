package io.wickkit.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MockRuleManagerTest {

    @Before
    fun setUp() {
        MockRuleManager.rules.value.toList().forEach { MockRuleManager.remove(it.id) }
    }

    // region add / rules

    @Test
    fun `starts empty after clearing all rules`() {
        assertTrue(MockRuleManager.rules.value.isEmpty())
    }

    @Test
    fun `add stores rule with correct fields`() {
        MockRuleManager.add(rule(urlPattern = "/api/users", statusCode = 404))
        val stored = MockRuleManager.rules.value.single()
        assertEquals("/api/users", stored.urlPattern)
        assertEquals(404, stored.statusCode)
    }

    @Test
    fun `add assigns unique ids across calls`() {
        MockRuleManager.add(rule())
        MockRuleManager.add(rule())
        val ids = MockRuleManager.rules.value.map { it.id }
        assertNotEquals(ids[0], ids[1])
    }

    @Test
    fun `add preserves insertion order`() {
        MockRuleManager.add(rule(urlPattern = "/first"))
        MockRuleManager.add(rule(urlPattern = "/second"))
        val rules = MockRuleManager.rules.value
        assertEquals("/first", rules[0].urlPattern)
        assertEquals("/second", rules[1].urlPattern)
    }

    // endregion

    // region remove

    @Test
    fun `remove deletes the rule with the matching id`() {
        MockRuleManager.add(rule(urlPattern = "/to-remove"))
        val id = MockRuleManager.rules.value.single().id
        MockRuleManager.remove(id)
        assertTrue(MockRuleManager.rules.value.isEmpty())
    }

    @Test
    fun `remove does not affect other rules`() {
        MockRuleManager.add(rule(urlPattern = "/keep"))
        MockRuleManager.add(rule(urlPattern = "/remove"))
        val removeId = MockRuleManager.rules.value.first { it.urlPattern == "/remove" }.id
        MockRuleManager.remove(removeId)
        assertEquals(1, MockRuleManager.rules.value.size)
        assertEquals("/keep", MockRuleManager.rules.value.single().urlPattern)
    }

    @Test
    fun `remove with unknown id is a no-op`() {
        MockRuleManager.add(rule())
        MockRuleManager.remove(-1L)
        assertEquals(1, MockRuleManager.rules.value.size)
    }

    // endregion

    // region toggle

    @Test
    fun `toggle disables an enabled rule`() {
        MockRuleManager.add(rule())
        val id = MockRuleManager.rules.value.single().id
        MockRuleManager.toggle(id)
        assertFalse(MockRuleManager.rules.value.single().isEnabled)
    }

    @Test
    fun `toggle re-enables a disabled rule`() {
        MockRuleManager.add(rule(isEnabled = false))
        val id = MockRuleManager.rules.value.single().id
        MockRuleManager.toggle(id)
        assertTrue(MockRuleManager.rules.value.single().isEnabled)
    }

    @Test
    fun `toggle does not affect other rules`() {
        MockRuleManager.add(rule(urlPattern = "/a"))
        MockRuleManager.add(rule(urlPattern = "/b"))
        val idA = MockRuleManager.rules.value.first { it.urlPattern == "/a" }.id
        MockRuleManager.toggle(idA)
        assertTrue(MockRuleManager.rules.value.first { it.urlPattern == "/b" }.isEnabled)
    }

    // endregion

    // region update

    @Test
    fun `update replaces the rule with the matching id`() {
        MockRuleManager.add(rule(urlPattern = "/old", statusCode = 200))
        val id = MockRuleManager.rules.value.single().id
        MockRuleManager.update(rule(urlPattern = "/new", statusCode = 404).copy(id = id))
        val updated = MockRuleManager.rules.value.single()
        assertEquals("/new", updated.urlPattern)
        assertEquals(404, updated.statusCode)
    }

    @Test
    fun `update preserves position and other rules`() {
        MockRuleManager.add(rule(urlPattern = "/first"))
        MockRuleManager.add(rule(urlPattern = "/second"))
        val secondId = MockRuleManager.rules.value.last().id
        MockRuleManager.update(rule(urlPattern = "/updated").copy(id = secondId))
        assertEquals("/first", MockRuleManager.rules.value.first().urlPattern)
        assertEquals("/updated", MockRuleManager.rules.value.last().urlPattern)
    }

    // endregion

    // region findMatch

    @Test
    fun `findMatch returns null when no rules exist`() {
        assertNull(MockRuleManager.findMatch(url = "https://api.example.com/users", method = "GET"))
    }

    @Test
    fun `findMatch returns null when no rule matches the url`() {
        MockRuleManager.add(rule(urlPattern = "/orders"))
        assertNull(MockRuleManager.findMatch(url = "https://api.example.com/users", method = "GET"))
    }

    @Test
    fun `findMatch matches url pattern case-insensitively`() {
        MockRuleManager.add(rule(urlPattern = "/API/Users"))
        assertNotNull(MockRuleManager.findMatch(url = "https://example.com/api/users", method = "GET"))
    }

    @Test
    fun `findMatch matches partial url pattern`() {
        MockRuleManager.add(rule(urlPattern = "/users"))
        assertNotNull(MockRuleManager.findMatch(url = "https://api.example.com/v1/users/42", method = "GET"))
    }

    @Test
    fun `findMatch returns null when method does not match`() {
        MockRuleManager.add(rule(urlPattern = "/users", method = "GET"))
        assertNull(MockRuleManager.findMatch(url = "https://api.example.com/users", method = "POST"))
    }

    @Test
    fun `findMatch matches method case-insensitively`() {
        MockRuleManager.add(rule(urlPattern = "/users", method = "get"))
        assertNotNull(MockRuleManager.findMatch(url = "https://api.example.com/users", method = "GET"))
    }

    @Test
    fun `findMatch with null method matches any http method`() {
        MockRuleManager.add(rule(urlPattern = "/users", method = null))
        assertNotNull(MockRuleManager.findMatch(url = "https://api.example.com/users", method = "DELETE"))
        assertNotNull(MockRuleManager.findMatch(url = "https://api.example.com/users", method = "PATCH"))
    }

    @Test
    fun `findMatch skips disabled rules`() {
        MockRuleManager.add(rule(urlPattern = "/users", isEnabled = false))
        assertNull(MockRuleManager.findMatch(url = "https://api.example.com/users", method = "GET"))
    }

    @Test
    fun `findMatch returns first matching enabled rule`() {
        MockRuleManager.add(rule(urlPattern = "/users", statusCode = 200))
        MockRuleManager.add(rule(urlPattern = "/users", statusCode = 500))
        assertEquals(200, MockRuleManager.findMatch(url = "https://api.example.com/users", method = "GET")!!.statusCode)
    }

    @Test
    fun `findMatch skips disabled rule and returns next matching enabled rule`() {
        MockRuleManager.add(
            rule(
                urlPattern = "/users",
                statusCode = 200,
                isEnabled = false,
            ),
        )
        MockRuleManager.add(rule(urlPattern = "/users", statusCode = 500))
        assertEquals(500, MockRuleManager.findMatch(url = "https://api.example.com/users", method = "GET")!!.statusCode)
    }

    @Test
    fun `findMatch returns the matched rule with its response body`() {
        MockRuleManager.add(rule(urlPattern = "/users", responseBody = """{"id":1}"""))
        val match = MockRuleManager.findMatch(url = "https://api.example.com/users", method = "GET")
        assertEquals("""{"id":1}""", match!!.responseBody)
    }

    // endregion

    private fun rule(
        urlPattern: String = "/api",
        method: String? = null,
        statusCode: Int = 200,
        responseBody: String? = null,
        isEnabled: Boolean = true,
    ) = MockRule(
        id = 0L,
        urlPattern = urlPattern,
        method = method,
        statusCode = statusCode,
        responseBody = responseBody,
        isEnabled = isEnabled,
    )
}
