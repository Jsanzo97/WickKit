package io.wickkit.network

@Suppress("UnusedParameter", "FunctionOnlyReturningConstant")
object MockRuleManager {
    fun findMatch(url: String, method: String): MockRule? = null
    fun add(rule: MockRule): Boolean = false
    fun remove(id: Long) = Unit
    fun clear() = Unit
    fun toggle(id: Long) = Unit
    fun update(rule: MockRule) = Unit
}
