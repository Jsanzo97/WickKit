package io.wickkit.network

import androidx.compose.runtime.Stable

@Stable
data class MockRule(
    val id: Long,
    val urlPattern: String,
    val method: String?,
    val statusCode: Int,
    val responseBody: String?,
    val responseHeaders: Map<String, String> = emptyMap(),
    val delayMs: Long = 0L,
    val isEnabled: Boolean = true,
)
