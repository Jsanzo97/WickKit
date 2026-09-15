package io.wickkit.network

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableMap

@Immutable
data class NetworkEntry(
    val id: Long,
    val method: String,
    val url: String,
    val requestHeaders: ImmutableMap<String, String>,
    val requestBody: String?,
    val statusCode: Int?,
    val responseHeaders: ImmutableMap<String, String>,
    val responseBody: String?,
    val durationMs: Long,
    val time: String,
    val error: String?,
    val isMocked: Boolean = false,
)
