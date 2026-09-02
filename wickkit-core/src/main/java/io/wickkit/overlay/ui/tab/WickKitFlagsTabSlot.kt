package io.wickkit.overlay.ui.tab

import androidx.compose.runtime.Composable

object WickKitFlagsTabSlot {
    internal var content: (@Composable () -> Unit)? = null
        private set

    fun register(provider: @Composable () -> Unit) {
        content = provider
    }
}
