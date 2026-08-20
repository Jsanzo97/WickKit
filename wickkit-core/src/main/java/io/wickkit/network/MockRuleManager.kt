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

    private val idCounter = AtomicLong(0)

    internal val rules: StateFlow<ImmutableList<MockRule>>
        field = MutableStateFlow<PersistentList<MockRule>>(persistentListOf())

    fun findMatch(url: String, method: String): MockRule? = rules.value.firstOrNull { rule ->
        rule.isEnabled &&
            (rule.method == null || rule.method.equals(method, ignoreCase = true)) &&
            url.contains(rule.urlPattern, ignoreCase = true)
    }

    fun add(rule: MockRule) {
        rules.update { it.adding(rule.copy(id = idCounter.getAndIncrement())) }
    }

    fun remove(id: Long) {
        rules.update { current -> current.filter { it.id != id }.toPersistentList() }
    }

    fun clear() {
        rules.value = persistentListOf()
    }

    internal fun toggle(id: Long) {
        rules.update { current ->
            current.map { if (it.id == id) it.copy(isEnabled = !it.isEnabled) else it }
                .toPersistentList()
        }
    }

    internal fun update(rule: MockRule) {
        rules.update { current ->
            current.map { if (it.id == rule.id) rule else it }.toPersistentList()
        }
    }
}
