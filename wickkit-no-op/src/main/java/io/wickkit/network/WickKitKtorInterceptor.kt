package io.wickkit.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.util.AttributeKey

class WickKitKtorInterceptor private constructor() {
    companion object Plugin : HttpClientPlugin<Unit, WickKitKtorInterceptor> {
        override val key: AttributeKey<WickKitKtorInterceptor> = AttributeKey("WickKit")
        override fun prepare(block: Unit.() -> Unit): WickKitKtorInterceptor = WickKitKtorInterceptor()
        override fun install(plugin: WickKitKtorInterceptor, scope: HttpClient) = Unit
    }
}
