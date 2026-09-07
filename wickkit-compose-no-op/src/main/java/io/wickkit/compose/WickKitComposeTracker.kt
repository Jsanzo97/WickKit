package io.wickkit.compose

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

object WickKitComposeTracker {

    @Suppress("UnusedParameter")
    fun onRecompose(name: String) = Unit
    fun reset() = Unit

    @Suppress("FunctionOnlyReturningConstant")
    fun isPluginActive(): Boolean = false
    fun computeEntries(): ImmutableList<ComposableEntry> = persistentListOf()
}
