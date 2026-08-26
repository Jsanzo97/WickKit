package io.wickkit.network

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong

object MockRuleManager {

    internal const val MAX_RULES = 50

    private val idCounter = AtomicLong(0)

    internal val rules: StateFlow<ImmutableList<MockRule>>
        field = MutableStateFlow<PersistentList<MockRule>>(persistentListOf())

    fun findMatch(url: String, method: String): MockRule? = rules.value.firstOrNull { rule ->
        rule.isEnabled &&
            rule.urlPattern.isNotBlank() &&
            (rule.method == null || rule.method.equals(method, ignoreCase = true)) &&
            url.contains(rule.urlPattern, ignoreCase = true)
    }

    fun add(rule: MockRule): Boolean {
        require(rule.urlPattern.isNotBlank()) { "urlPattern must not be blank" }
        var added = false
        rules.update { current ->
            if (current.size >= MAX_RULES) {
                current
            } else {
                added = true
                current.adding(rule.copy(id = idCounter.getAndIncrement()))
            }
        }
        return added
    }

    fun remove(id: Long) {
        rules.update { current -> current.filter { it.id != id }.toPersistentList() }
    }

    fun clear() {
        rules.update { persistentListOf() }
    }

    fun toggle(id: Long) {
        rules.update { current ->
            current.map { if (it.id == id) it.copy(isEnabled = !it.isEnabled) else it }
                .toPersistentList()
        }
    }

    fun update(rule: MockRule) {
        rules.update { current ->
            current.map { if (it.id == rule.id) rule else it }.toPersistentList()
        }
    }
}
