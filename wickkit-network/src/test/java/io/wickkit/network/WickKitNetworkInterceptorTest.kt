@file:Suppress("INACCESSIBLE_TYPE")

package io.wickkit.network

import okhttp3.Authenticator
import okhttp3.Cache
import okhttp3.Call
import okhttp3.CertificatePinner
import okhttp3.Connection
import okhttp3.ConnectionPool
import okhttp3.CookieJar
import okhttp3.Dns
import okhttp3.EventListener
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.BufferedSink
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.Proxy
import java.net.ProxySelector
import java.util.concurrent.TimeUnit
import javax.net.SocketFactory
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

class WickKitNetworkInterceptorTest {

    private val interceptor = WickKitNetworkInterceptor()

    @After
    fun tearDown() {
        MockRuleManager.clear()
    }

    // region success path

    @Test
    fun `returns response from chain when no mock matches`() {
        val request = Request.Builder().url("https://api.example.com/users").build()
        val chain = FakeChain(request) { fakeResponse(it, 200, """{"id":1}""") }

        val result = interceptor.intercept(chain)

        assertEquals(200, result.code)
    }

    @Test
    fun `preserves status code from chain response`() {
        val request = Request.Builder().url("https://api.example.com/users/999").build()
        val chain = FakeChain(request) { fakeResponse(it, 404, """{"error":"not found"}""") }

        val result = interceptor.intercept(chain)

        assertEquals(404, result.code)
    }

    @Test
    fun `passes request to chain unmodified`() {
        val request = Request.Builder()
            .url("https://api.example.com/posts")
            .header("Authorization", "Bearer token")
            .build()
        var interceptedRequest: Request? = null
        val chain = FakeChain(request) { req ->
            interceptedRequest = req
            fakeResponse(req, 200, "[]")
        }

        interceptor.intercept(chain)

        assertEquals("https://api.example.com/posts", interceptedRequest!!.url.toString())
        assertEquals("Bearer token", interceptedRequest.header("Authorization"))
    }

    // endregion

    // region error path

    @Test(expected = IOException::class)
    fun `rethrows IOException from chain`() {
        val request = Request.Builder().url("https://api.example.com/users").build()
        val chain = FakeChain(request) { throw IOException("Connection refused") }

        interceptor.intercept(chain)
    }

    @Test
    fun `IOException message is preserved on rethrow`() {
        val request = Request.Builder().url("https://api.example.com/users").build()
        val chain = FakeChain(request) { throw IOException("timeout after 30s") }

        val thrown = runCatching { interceptor.intercept(chain) }.exceptionOrNull()

        assertEquals("timeout after 30s", thrown!!.message)
    }

    @Test(expected = IOException::class)
    fun `rethrows IOException for POST requests`() {
        val request = Request.Builder()
            .url("https://api.example.com/users")
            .post("{}".toRequestBody("application/json".toMediaType()))
            .build()
        val chain = FakeChain(request) { throw IOException("Network unreachable") }

        interceptor.intercept(chain)
    }

    // endregion

    // region mock path

    @Test
    fun `returns mock response without calling chain when rule matches`() {
        MockRuleManager.add(
            MockRule(
                id = 0,
                urlPattern = "/users",
                method = null,
                statusCode = 200,
                responseBody = """{"mocked":true}""",
            ),
        )
        val request = Request.Builder().url("https://api.example.com/users").build()
        val chain = FakeChain(request) { error("chain.proceed must not be called for mock") }

        val result = interceptor.intercept(chain)

        assertEquals(200, result.code)
        assertEquals("""{"mocked":true}""", result.body.string())
    }

    @Test
    fun `mock response has correct status code`() {
        MockRuleManager.add(
            MockRule(id = 0, urlPattern = "/missing", method = null, statusCode = 404, responseBody = null),
        )
        val request = Request.Builder().url("https://api.example.com/missing").build()
        val chain = FakeChain(request) { error("chain must not be called") }

        val result = interceptor.intercept(chain)

        assertEquals(404, result.code)
    }

    @Test
    fun `mock response with null body returns empty body`() {
        MockRuleManager.add(
            MockRule(id = 0, urlPattern = "/ping", method = null, statusCode = 204, responseBody = null),
        )
        val request = Request.Builder().url("https://api.example.com/ping").build()
        val chain = FakeChain(request) { error("chain must not be called") }

        val result = interceptor.intercept(chain)

        assertEquals(204, result.code)
        assertEquals("", result.body.string())
    }

    @Test
    fun `mock response with custom headers includes them in response`() {
        MockRuleManager.add(
            MockRule(
                id = 0,
                urlPattern = "/data",
                method = null,
                statusCode = 200,
                responseBody = "{}",
                responseHeaders = mapOf("X-Mock" to "true", "Cache-Control" to "no-store"),
            ),
        )
        val request = Request.Builder().url("https://api.example.com/data").build()
        val chain = FakeChain(request) { error("chain must not be called") }

        val result = interceptor.intercept(chain)

        assertEquals("true", result.header("X-Mock"))
        assertEquals("no-store", result.header("Cache-Control"))
    }

    @Test
    fun `mock with unrecognised status code still returns that code`() {
        MockRuleManager.add(
            MockRule(id = 0, urlPattern = "/teapot", method = null, statusCode = 418, responseBody = "I'm a teapot"),
        )
        val request = Request.Builder().url("https://api.example.com/teapot").build()
        val chain = FakeChain(request) { error("chain must not be called") }

        val result = interceptor.intercept(chain)

        assertEquals(418, result.code)
    }

    @Test
    fun `mock with delay does not prevent response`() {
        MockRuleManager.add(
            MockRule(id = 0, urlPattern = "/slow", method = null, statusCode = 200, responseBody = "ok", delayMs = 1),
        )
        val request = Request.Builder().url("https://api.example.com/slow").build()
        val chain = FakeChain(request) { error("chain must not be called") }

        val result = interceptor.intercept(chain)

        assertEquals(200, result.code)
    }

    @Test
    fun `disabled mock rule falls through to chain`() {
        MockRuleManager.add(
            MockRule(
                id = 0,
                urlPattern = "/data",
                method = null,
                statusCode = 200,
                responseBody = "{}",
                isEnabled = false,
            ),
        )
        val request = Request.Builder().url("https://api.example.com/data").build()
        val chain = FakeChain(request) { fakeResponse(it, 503, "real") }

        val result = interceptor.intercept(chain)

        assertEquals(503, result.code)
    }

    @Test
    fun `mock only matches specified HTTP method`() {
        MockRuleManager.add(
            MockRule(id = 0, urlPattern = "/users", method = "POST", statusCode = 201, responseBody = "{}"),
        )
        val request = Request.Builder().url("https://api.example.com/users").build()
        val chain = FakeChain(request) { fakeResponse(it, 200, "real") }

        val result = interceptor.intercept(chain)

        assertEquals(200, result.code)
    }

    // endregion

    // region readRequestBody edge cases

    @Test
    fun `one-shot request body is not consumed and placeholder is stored`() {
        val body = object : RequestBody() {
            override fun contentType() = null
            override fun isOneShot() = true
            override fun writeTo(sink: BufferedSink) = error("must not write one-shot body")
        }
        val request = Request.Builder()
            .url("https://api.example.com/upload")
            .post(body)
            .build()
        val chain = FakeChain(request) { fakeResponse(it, 200, "ok") }

        val result = interceptor.intercept(chain)

        assertEquals(200, result.code)
    }

    @Test
    fun `oversized request body does not crash and chain proceeds`() {
        val bigBody = ByteArray(52_000) { 'x'.code.toByte() }.toRequestBody("application/octet-stream".toMediaType())
        val request = Request.Builder()
            .url("https://api.example.com/upload")
            .post(bigBody)
            .build()
        val chain = FakeChain(request) { fakeResponse(it, 200, "ok") }

        val result = interceptor.intercept(chain)

        assertEquals(200, result.code)
    }

    @Test
    fun `request body write failure is handled and chain still proceeds`() {
        val failingBody = object : RequestBody() {
            override fun contentType() = null
            override fun writeTo(sink: BufferedSink) = throw IOException("write failed")
        }
        val request = Request.Builder()
            .url("https://api.example.com/data")
            .post(failingBody)
            .build()
        val chain = FakeChain(request) { fakeResponse(it, 200, "ok") }

        val result = interceptor.intercept(chain)

        assertEquals(200, result.code)
    }

    // endregion

    // region IOException null message

    @Test(expected = IOException::class)
    fun `IOException with null message is rethrown`() {
        val request = Request.Builder().url("https://api.example.com/users").build()
        val chain = FakeChain(request) { throw IOException() }

        interceptor.intercept(chain)
    }

    // endregion

    private fun fakeResponse(request: Request, code: Int, body: String): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message("")
        .body(body.toResponseBody("application/json; charset=utf-8".toMediaType()))
        .build()

    private class FakeChain(
        private val request: Request,
        private val onProceed: (Request) -> Response,
    ) : Interceptor.Chain {
        override fun request(): Request = request
        override fun proceed(request: Request): Response = onProceed(request)
        override fun connection(): Connection? = null
        override fun call(): Call = throw UnsupportedOperationException()
        override fun connectTimeoutMillis(): Int = 0
        override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun readTimeoutMillis(): Int = 0
        override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun writeTimeoutMillis(): Int = 0
        override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this

        // New in OkHttp 4.12 — not exercised by the interceptor under test
        override val authenticator: Authenticator get() = error("not used")
        override val cache: Cache? get() = null
        override val certificatePinner: CertificatePinner get() = error("not used")
        override val connectionPool: ConnectionPool get() = error("not used")
        override val cookieJar: CookieJar get() = error("not used")
        override val dns: Dns get() = error("not used")
        override val eventListener: EventListener get() = error("not used")
        override val followRedirects: Boolean get() = false
        override val followSslRedirects: Boolean get() = false
        override val hostnameVerifier: HostnameVerifier get() = error("not used")
        override val proxy: Proxy? get() = null
        override val proxyAuthenticator: Authenticator get() = error("not used")
        override val proxySelector: ProxySelector get() = error("not used")
        override val retryOnConnectionFailure: Boolean get() = false
        override val socketFactory: SocketFactory get() = error("not used")
        override val sslSocketFactoryOrNull: SSLSocketFactory? get() = null
        override val x509TrustManagerOrNull: X509TrustManager? get() = null

        override fun withAuthenticator(authenticator: Authenticator): Interceptor.Chain = this
        override fun withCache(cache: Cache?): Interceptor.Chain = this
        override fun withCertificatePinner(certificatePinner: CertificatePinner): Interceptor.Chain = this
        override fun withConnectionPool(connectionPool: ConnectionPool): Interceptor.Chain = this
        override fun withCookieJar(cookieJar: CookieJar): Interceptor.Chain = this
        override fun withDns(dns: Dns): Interceptor.Chain = this
        override fun withHostnameVerifier(hostnameVerifier: HostnameVerifier): Interceptor.Chain = this
        override fun withProxy(proxy: Proxy?): Interceptor.Chain = this
        override fun withProxyAuthenticator(proxyAuthenticator: Authenticator): Interceptor.Chain = this
        override fun withProxySelector(proxySelector: ProxySelector): Interceptor.Chain = this
        override fun withRetryOnConnectionFailure(retryOnConnectionFailure: Boolean): Interceptor.Chain = this
        override fun withSocketFactory(socketFactory: SocketFactory): Interceptor.Chain = this
        override fun withSslSocketFactory(
            sslSocketFactory: SSLSocketFactory?,
            x509TrustManager: X509TrustManager?,
        ): Interceptor.Chain = this
    }
}
