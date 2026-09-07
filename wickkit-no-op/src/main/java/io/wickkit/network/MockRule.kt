package io.wickkit.network

data class MockRule(
    val id: Long = 0L,
    val urlPattern: String = "",
    val method: String? = null,
    val statusCode: Int = 200,
    val responseBody: String? = null,
    val responseHeaders: Map<String, String> = emptyMap(),
    val delayMs: Long = 0L,
    val isEnabled: Boolean = true,
)
