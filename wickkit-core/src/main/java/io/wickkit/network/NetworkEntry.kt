package io.wickkit.network

import androidx.compose.runtime.Stable

@Stable
data class NetworkEntry(
    val id: Long,
    val method: String,
    val url: String,
    val requestHeaders: Map<String, String>,
    val requestBody: String?,
    val statusCode: Int?,
    val responseHeaders: Map<String, String>,
    val responseBody: String?,
    val durationMs: Long,
    val time: String,
    val error: String?,
    val isMocked: Boolean = false,
)
