package io.wickkit.network

import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.isSaved
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.statement.bodyAsText
import io.ktor.client.utils.EmptyContent
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headers
import io.ktor.util.AttributeKey
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WickKitKtorInterceptor private constructor() {

    companion object Plugin : HttpClientPlugin<Unit, WickKitKtorInterceptor> {
        override val key: AttributeKey<WickKitKtorInterceptor> = AttributeKey("WickKit")

        override fun prepare(block: Unit.() -> Unit): WickKitKtorInterceptor = WickKitKtorInterceptor()

        @Suppress("TooGenericExceptionCaught")
        override fun install(plugin: WickKitKtorInterceptor, scope: HttpClient) {
            val timeFormat = object : ThreadLocal<SimpleDateFormat>() {
                override fun initialValue() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
            }

            scope.plugin(HttpSend).intercept { request ->
                val id = WickKitNetworkManager.nextId()
                val time = (timeFormat.get() ?: SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())).format(Date())
                val url = request.url.buildString()
                val method = request.method.value
                val requestHeaders = request.headers.build()
                    .entries()
                    .associate { (key, headerValues) -> key to headerValues.joinToString(", ") }
                    .toImmutableMap()
                val requestBody = readRequestBody(request.body)

                val mockRule = MockRuleManager.findMatch(url = url, method = method)
                if (mockRule != null) {
                    val actualDelayMs = mockRule.delayMs.coerceAtMost(MAX_DELAY_MS)
                    if (actualDelayMs > 0) {
                        delay(actualDelayMs)
                    }
                    val call = buildMockCall(scope = scope, request = request, rule = mockRule)
                    WickKitNetworkManager.add(
                        NetworkEntry(
                            id = id,
                            method = method,
                            url = url,
                            requestHeaders = requestHeaders,
                            requestBody = requestBody,
                            statusCode = mockRule.statusCode,
                            responseHeaders = mockRule.responseHeaders,
                            responseBody = mockRule.responseBody,
                            durationMs = actualDelayMs,
                            time = time,
                            error = null,
                            isMocked = true,
                        ),
                    )
                    return@intercept call
                }

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
                            responseHeaders = persistentMapOf(),
                            responseBody = null,
                            durationMs = System.currentTimeMillis() - startMs,
                            time = time,
                            error = e.message ?: "Unknown error",
                        ),
                    )
                    throw e
                }
                val responseBody = readResponseBody(call)
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
                            .associate { (key, headerValues) -> key to headerValues.joinToString(", ") }
                            .toImmutableMap(),
                        responseBody = responseBody,
                        durationMs = System.currentTimeMillis() - startMs,
                        time = time,
                        error = null,
                    ),
                )
                call
            }
        }

        @OptIn(InternalAPI::class)
        private fun buildMockCall(
            scope: HttpClient,
            request: io.ktor.client.request.HttpRequestBuilder,
            rule: MockRule,
        ): HttpClientCall {
            val callContext = scope.coroutineContext + Job(scope.coroutineContext[Job])
            val requestData = HttpRequestData(
                url = request.url.build(),
                method = request.method,
                headers = request.headers.build(),
                body = request.body as? OutgoingContent ?: EmptyContent,
                executionContext = request.executionContext,
                attributes = request.attributes,
            )
            val responseHeaders = headers {
                rule.responseHeaders.forEach { (key, value) -> append(key, value) }
                val hasContentType = rule.responseHeaders.keys.any {
                    it.equals("Content-Type", ignoreCase = true)
                }
                if (!hasContentType) {
                    append("Content-Type", "application/json; charset=utf-8")
                }
            }
            val responseData = HttpResponseData(
                statusCode = HttpStatusCode.fromValue(rule.statusCode),
                requestTime = GMTDate(),
                headers = responseHeaders,
                version = HttpProtocolVersion.HTTP_1_1,
                body = ByteReadChannel((rule.responseBody ?: "").toByteArray(Charsets.UTF_8)),
                callContext = callContext,
            )
            return HttpClientCall(scope, requestData, responseData)
        }

        private suspend fun readResponseBody(call: HttpClientCall): String? {
            if (!call.response.isSaved) return null
            return runCatching {
                val text = call.response.bodyAsText()
                val byteSize = text.toByteArray(Charsets.UTF_8).size
                if (byteSize > MAX_BODY_BYTES) "[body too large: $byteSize bytes]" else text
            }.getOrNull()?.ifEmpty { null }
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
        private const val MAX_DELAY_MS = 30_000L
    }
}
