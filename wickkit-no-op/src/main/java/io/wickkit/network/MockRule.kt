package io.wickkit.network

import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf

data class MockRule(
    val id: Long,
    val urlPattern: String,
    val method: String?,
    val statusCode: Int,
    val responseBody: String?,
    val responseHeaders: ImmutableMap<String, String> = persistentMapOf(),
    val delayMs: Long = 0L,
    val isEnabled: Boolean = true,
)
