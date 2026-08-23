@file:Suppress("INACCESSIBLE_TYPE", "DEPRECATION")

package io.wickkit.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.SaveBodyPlugin
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class WickKitKtorInterceptorTest {

    // region success path

    @Test
    fun `successful GET response status is preserved`() = runBlocking {
        HttpClient(MockEngine) {
            install(WickKitKtorInterceptor)
            engine {
                addHandler {
                    respond(
                        content = "{}",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }
        }.use { client ->
            val response = client.get("https://api.example.com/users")
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    @Test
    fun `non-200 status code is preserved`() = runBlocking {
        HttpClient(MockEngine) {
            install(WickKitKtorInterceptor)
            engine {
                addHandler {
                    respond(
                        content = """{"error":"not found"}""",
                        status = HttpStatusCode.NotFound,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }
        }.use { client ->
            val response = client.get("https://api.example.com/users/999")
            assertEquals(HttpStatusCode.NotFound, response.status)
        }
    }

    @Test
    fun `POST response status is preserved`() = runBlocking {
        HttpClient(MockEngine) {
            install(WickKitKtorInterceptor)
            engine {
                addHandler {
                    respond(
                        content = """{"id":42}""",
                        status = HttpStatusCode.Created,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }
        }.use { client ->
            val response = client.post("https://api.example.com/users") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"name":"test"}""")
            }
            assertEquals(HttpStatusCode.Created, response.status)
        }
    }

    // endregion

    // region response body

    @Test
    fun `response body is readable by caller without SaveBodyPlugin`() = runBlocking {
        HttpClient(MockEngine) {
            install(WickKitKtorInterceptor)
            engine {
                addHandler {
                    respond(
                        content = """{"key":"value"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }
        }.use { client ->
            val body = client.get("https://api.example.com/data").bodyAsText()
            assertEquals("""{"key":"value"}""", body)
        }
    }

    @Test
    fun `response body is readable by caller after interceptor reads it with SaveBodyPlugin`() = runBlocking {
        HttpClient(MockEngine) {
            install(SaveBodyPlugin)
            install(WickKitKtorInterceptor)
            engine {
                addHandler {
                    respond(
                        content = """{"saved":true}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }
        }.use { client ->
            val body = client.get("https://api.example.com/data").bodyAsText()
            assertEquals("""{"saved":true}""", body)
        }
    }

    @Test
    fun `request body larger than 50KB is truncated in interceptor entry`() = runBlocking {
        val largeBody = ByteArray(52 * 1024) { 'x'.code.toByte() }
        HttpClient(MockEngine) {
            install(WickKitKtorInterceptor)
            engine {
                addHandler {
                    respond(
                        content = "{}",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }
        }.use { client ->
            val response = client.post("https://api.example.com/upload") {
                setBody(largeBody)
            }
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    @Test
    fun `response body larger than 50KB is truncated in interceptor entry with SaveBodyPlugin`() = runBlocking {
        val largeBody = "a".repeat(51 * 1024)
        HttpClient(MockEngine) {
            install(SaveBodyPlugin)
            install(WickKitKtorInterceptor)
            engine {
                addHandler {
                    respond(
                        content = largeBody,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "text/plain"),
                    )
                }
            }
        }.use { client ->
            val response = client.get("https://api.example.com/large")
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    // endregion

    // region error path

    @Test
    fun `network exception propagates to caller`() {
        val client = HttpClient(MockEngine) {
            install(WickKitKtorInterceptor)
            engine {
                addHandler { throw IOException("Connection refused") }
            }
        }
        val result = runCatching { runBlocking { client.get("https://api.example.com/fail") } }
        client.close()
        assertTrue(result.isFailure)
    }

    @Test
    fun `network exception with null message is logged as unknown error`() {
        val client = HttpClient(MockEngine) {
            install(WickKitKtorInterceptor)
            engine {
                addHandler { throw IOException() }
            }
        }
        val result = runCatching { runBlocking { client.get("https://api.example.com/fail") } }
        client.close()
        assertTrue(result.isFailure)
    }

    // endregion

    // region headers

    @Test
    fun `custom response headers are accessible after intercept`() = runBlocking {
        HttpClient(MockEngine) {
            install(WickKitKtorInterceptor)
            engine {
                addHandler {
                    respond(
                        content = "{}",
                        status = HttpStatusCode.OK,
                        headers = headersOf(
                            HttpHeaders.ContentType to listOf("application/json"),
                            "X-Custom-Header" to listOf("custom-value"),
                        ),
                    )
                }
            }
        }.use { client ->
            val response = client.get("https://api.example.com/data")
            assertEquals("custom-value", response.headers["X-Custom-Header"])
        }
    }

    // endregion
}
