package io.wickkit.network

import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

class WickKitNetworkInterceptor : Interceptor {

    private val timeFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        val method = request.method
        val id = idCounter.getAndIncrement()
        val time = timeFormat.get()!!.format(Date())
        val requestHeaders = request.headers.toFlatMap()
        val requestBody = readRequestBody(request)

        val mockRule = MockRuleManager.findMatch(url = url, method = method)
        if (mockRule != null) {
            return serveMock(
                request = request,
                rule = mockRule,
                id = id,
                time = time,
                requestBody = requestBody,
            )
        }

        val startMs = System.currentTimeMillis()
        return try {
            val response = chain.proceed(request)
            val durationMs = System.currentTimeMillis() - startMs
            WickKitNetworkManager.add(
                NetworkEntry(
                    id = id,
                    method = method,
                    url = url,
                    requestHeaders = requestHeaders,
                    requestBody = requestBody,
                    statusCode = response.code,
                    responseHeaders = response.headers.toFlatMap(),
                    responseBody = runCatching { response.peekBody(MAX_BODY_BYTES).string() }.getOrNull(),
                    durationMs = durationMs,
                    time = time,
                    error = null,
                ),
            )
            response
        } catch (e: java.io.IOException) {
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
    }

    private fun serveMock(
        request: okhttp3.Request,
        rule: MockRule,
        id: Long,
        time: String,
        requestBody: String?,
    ): Response {
        val requestHeaders = request.headers.toFlatMap()
        if (rule.delayMs > 0) {
            try {
                Thread.sleep(rule.delayMs.coerceAtMost(MAX_DELAY_MS))
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        val contentType = (rule.responseHeaders["Content-Type"] ?: "application/json; charset=utf-8").toMediaType()
        val mockBody = (rule.responseBody ?: "").toResponseBody(contentType)
        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(rule.statusCode)
            .message(statusMessage(rule.statusCode))
            .body(mockBody)
            .apply { rule.responseHeaders.forEach { (key, value) -> header(key, value) } }
            .build()
        WickKitNetworkManager.add(
            NetworkEntry(
                id = id,
                method = request.method,
                url = request.url.toString(),
                requestHeaders = requestHeaders,
                requestBody = requestBody,
                statusCode = rule.statusCode,
                responseHeaders = rule.responseHeaders,
                responseBody = rule.responseBody,
                durationMs = rule.delayMs,
                time = time,
                error = null,
                isMocked = true,
            ),
        )
        return response
    }

    private fun readRequestBody(request: okhttp3.Request): String? = request.body?.let { body ->
        when {
            body.isOneShot() -> "[one-shot body]"

            else -> runCatching {
                val buffer = Buffer()
                body.writeTo(buffer)
                if (buffer.size > MAX_BODY_BYTES) "[body too large: ${buffer.size} bytes]" else buffer.readUtf8()
            }.getOrNull()
        }
    }

    private fun Headers.toFlatMap(): Map<String, String> = names().associateWith { name ->
        values(name).joinToString(", ")
    }

    private fun statusMessage(code: Int): String = STATUS_MESSAGES[code] ?: ""

    private companion object {
        private val idCounter = AtomicLong(0)
        private const val MAX_BODY_BYTES = 50 * 1024L
        private const val MAX_DELAY_MS = 30_000L
        private val STATUS_MESSAGES = mapOf(
            200 to "OK", 201 to "Created", 204 to "No Content",
            301 to "Moved Permanently", 302 to "Found", 304 to "Not Modified",
            400 to "Bad Request", 401 to "Unauthorized", 403 to "Forbidden",
            404 to "Not Found", 405 to "Method Not Allowed", 409 to "Conflict",
            422 to "Unprocessable Entity", 429 to "Too Many Requests",
            500 to "Internal Server Error", 502 to "Bad Gateway",
            503 to "Service Unavailable", 504 to "Gateway Timeout",
        )
    }
}
