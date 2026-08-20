package io.wickkit.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.SaveBodyPlugin
import io.ktor.client.plugins.plugin
import io.ktor.client.plugins.pluginOrNull
import io.ktor.client.statement.bodyAsText
import io.ktor.http.content.OutgoingContent
import io.ktor.util.AttributeKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

class WickKitKtorInterceptor private constructor() {

    companion object Plugin : HttpClientPlugin<Unit, WickKitKtorInterceptor> {
        override val key: AttributeKey<WickKitKtorInterceptor> = AttributeKey("WickKit")

        override fun prepare(block: Unit.() -> Unit): WickKitKtorInterceptor = WickKitKtorInterceptor()

        @Suppress("TooGenericExceptionCaught")
        override fun install(plugin: WickKitKtorInterceptor, scope: HttpClient) {
            val idCounter = AtomicLong(0)
            val timeFormat = object : ThreadLocal<SimpleDateFormat>() {
                override fun initialValue() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
            }

            val saveBodyInstalled = scope.pluginOrNull(SaveBodyPlugin) != null
            scope.plugin(HttpSend).intercept { request ->
                val id = idCounter.getAndIncrement()
                val time = timeFormat.get()!!.format(Date())
                val url = request.url.buildString()
                val method = request.method.value
                val requestHeaders = request.headers.build()
                    .entries()
                    .associate { (k, v) -> k to v.joinToString(", ") }
                val requestBody = readRequestBody(request.body)
                val startMs = System.currentTimeMillis()
                val call = try {
                    execute(request)
                } catch (e: Exception) {
                    WickKitNetworkManager.add(
                        NetworkEntry(
                            id = id,
                            method = method,
                            url = url,
                            requestHeaders = requestHeaders,
                            requestBody = requestBody,
                            statusCode = null,
                            responseHeaders = emptyMap(),
                            responseBody = null,
                            durationMs = System.currentTimeMillis() - startMs,
                            time = time,
                            error = e.message ?: "Unknown error",
                        ),
                    )
                    throw e
                }
                val responseBody = if (saveBodyInstalled) {
                    runCatching {
                        val text = call.response.bodyAsText()
                        if (text.length > MAX_BODY_BYTES) {
                            "[body too large: ${text.length} chars]"
                        } else {
                            text
                        }
                    }.getOrNull()
                } else {
                    null
                }
                WickKitNetworkManager.add(
                    NetworkEntry(
                        id = id,
                        method = method,
                        url = url,
                        requestHeaders = requestHeaders,
                        requestBody = requestBody,
                        statusCode = call.response.status.value,
                        responseHeaders = call.response.headers
                            .entries()
                            .associate { (k, v) -> k to v.joinToString(", ") },
                        responseBody = responseBody,
                        durationMs = System.currentTimeMillis() - startMs,
                        time = time,
                        error = null,
                    ),
                )
                call
            }
        }

        private fun readRequestBody(body: Any): String? {
            val content = body as? OutgoingContent ?: return null
            return when (content) {
                is OutgoingContent.ByteArrayContent -> {
                    val bytes = content.bytes()
                    if (bytes.size > MAX_BODY_BYTES) {
                        "[body too large: ${bytes.size} bytes]"
                    } else {
                        bytes.decodeToString()
                    }
                }

                else -> null
            }
        }

        private const val MAX_BODY_BYTES = 50 * 1024
    }
}
