package io.wickkit.network

import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.SaveBodyPlugin
import io.ktor.client.plugins.plugin
import io.ktor.client.plugins.pluginOrNull
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
                    .associate { (key, headerValues) -> key to headerValues.joinToString(", ") }
                val requestBody = readRequestBody(request.body)

                val mockRule = MockRuleManager.findMatch(url = url, method = method)
                if (mockRule != null) {
                    if (mockRule.delayMs > 0) {
                        delay(mockRule.delayMs.coerceAtMost(MAX_DELAY_MS))
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
                            durationMs = mockRule.delayMs,
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
                            responseHeaders = emptyMap(),
                            responseBody = null,
                            durationMs = System.currentTimeMillis() - startMs,
                            time = time,
                            error = e.message ?: "Unknown error",
                        ),
                    )
                    throw e
                }
                val responseBody = readResponseBody(call, saveBodyInstalled)
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
                            .associate { (key, headerValues) -> key to headerValues.joinToString(", ") },
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
                if (!rule.responseHeaders.containsKey("Content-Type")) {
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

        private suspend fun readResponseBody(
            call: io.ktor.client.call.HttpClientCall,
            saveBodyInstalled: Boolean,
        ): String? {
            if (!saveBodyInstalled) return null
            return runCatching {
                val text = call.response.bodyAsText()
                val byteSize = text.toByteArray(Charsets.UTF_8).size
                if (byteSize > MAX_BODY_BYTES) "[body too large: $byteSize bytes]" else text
            }.getOrNull()
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
