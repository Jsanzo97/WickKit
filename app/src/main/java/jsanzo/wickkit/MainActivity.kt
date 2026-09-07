package jsanzo.wickkit

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import io.wickkit.WickKit
import io.wickkit.network.WickKitNetworkInterceptor
import jsanzo.wickkit.ui.theme.WickKitTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : ComponentActivity() {

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* granted state is handled by WickKitNotification.canPost() */ }

    private val sampleDb by lazy { SampleDatabase(this) }

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(WickKitNetworkInterceptor())
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.plant(Timber.DebugTree())
        lifecycleScope.launch(Dispatchers.IO) { sampleDb.writableDatabase }
        SampleRemoteConfig.init()
        Timber.tag("Timber").d("Timber debug log")
        Timber.tag("Timber").i("Timber info log")
        Timber.tag("Timber").w("Timber warning log")
        Timber.tag("Timber").e("Timber error log")
        Log.v("AndroidLog", "android.util.Log verbose")
        Log.d("AndroidLog", "android.util.Log debug")
        Log.i("AndroidLog", "android.util.Log info")
        Log.w("AndroidLog", "android.util.Log warning")
        Log.e("AndroidLog", "android.util.Log error")
        println("println goes to logcat as System.out at INFO level")
        enableEdgeToEdge()
        setContent {
            WickKitTheme {
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SampleContent(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    private fun generateSampleRequests() {
        lifecycleScope.launch(Dispatchers.IO) {
            val base = "https://jsonplaceholder.typicode.com"
            runCatching {
                httpClient.newCall(Request.Builder().url("$base/posts/1").build()).execute().close()
            }
            runCatching {
                httpClient.newCall(Request.Builder().url("$base/posts/2").build()).execute().close()
            }
            runCatching {
                val body = """{"title":"WickKit Test","body":"Testing network inspector","userId":1}"""
                    .toRequestBody("application/json".toMediaType())
                httpClient.newCall(
                    Request.Builder().url("$base/posts").post(body).build(),
                ).execute().close()
            }
            runCatching {
                httpClient.newCall(Request.Builder().url("$base/posts/99999").build()).execute().close()
            }
            runCatching {
                httpClient.newCall(Request.Builder().url("$base/users").build()).execute().close()
            }
        }
    }

    @Suppress("TooGenericExceptionThrown")
    @Composable
    private fun SampleContent(modifier: Modifier = Modifier) {
        val context = LocalContext.current
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("WickKit Sample App")
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { WickKit.open(context) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Open Debug Panel")
            }
            SampleButton("Generate Logs") {
                generateSampleLogs()
                Toast.makeText(context, "Sample logs generated", Toast.LENGTH_SHORT).show()
            }
            SampleButton("Make Network Requests") {
                generateSampleRequests()
                Toast.makeText(context, "Network requests sent", Toast.LENGTH_SHORT).show()
            }
            SampleButton("Reseed Sample Database") {
                lifecycleScope.launch(Dispatchers.IO) { sampleDb.reseed() }
                Toast.makeText(context, "Database reseeded", Toast.LENGTH_SHORT).show()
            }
            SampleButton("Seed Sample Preferences") {
                SamplePreferences.seed(context)
                Toast.makeText(context, "Preferences seeded", Toast.LENGTH_SHORT).show()
            }
            SampleButton("Fetch Remote Config") {
                SampleRemoteConfig.fetch()
                Toast.makeText(context, "Fetching Remote Config", Toast.LENGTH_SHORT).show()
            }
            SampleButton("Simulate Memory Leak") {
                context.startActivity(Intent(context, LeakedActivity::class.java))
                Toast.makeText(context, "Leak simulated — check Leaks tab in 5s", Toast.LENGTH_LONG).show()
            }
            SampleButton("Simulate Performance Issues") {
                context.startActivity(Intent(context, JankActivity::class.java))
            }
            SampleButton("Spawn Sample Threads") {
                lifecycleScope.launch(Dispatchers.IO) { spawnSampleThreads() }
                Toast.makeText(context, "Sample threads spawned — check Threads tab", Toast.LENGTH_SHORT).show()
            }
            SampleButton("Simulate Crash") {
                Thread {
                    throw RuntimeException("WickKit sample crash — reopen the app and check the Crashes tab")
                }.apply {
                    name = "wk-sample-crash"
                    start()
                }
            }
        }
    }

    @Composable
    private fun SampleButton(label: String, onClick: () -> Unit) {
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(label)
        }
    }
}

private suspend fun spawnSampleThreads() {
    repeat(3) { index ->
        Thread {
            Thread.sleep(30_000)
        }.apply {
            name = "wk-sample-sleeping-$index"
            isDaemon = true
            start()
        }
    }
    val lock = Any()
    Thread {
        synchronized(lock) { Thread.sleep(30_000) }
    }.apply {
        name = "wk-sample-lock-holder"
        isDaemon = true
        start()
    }
    delay(50.milliseconds)
    repeat(2) { index ->
        Thread {
            synchronized(lock) { Thread.sleep(30_000) }
        }.apply {
            name = "wk-sample-blocked-$index"
            isDaemon = true
            start()
        }
    }
    Thread {
        val end = System.currentTimeMillis() + 30_000
        while (System.currentTimeMillis() < end) {
            Thread.yield()
        }
    }.apply {
        name = "wk-sample-cpu-bound"
        isDaemon = true
        start()
    }
}

private var logCounter = 0

private fun generateSampleLogs() {
    val batchNumber = ++logCounter
    Timber.tag("UserAction").d("Button tapped — batch #$batchNumber")
    Log.i("Network", "GET /api/items?page=$batchNumber → 200 OK (${(50..300).random()}ms)")
    Log.w("Database", "Slow query on items table: ${(200..800).random()}ms")
    Log.e("Auth", "Token refresh failed on attempt $batchNumber")
    println("System.out batch #$batchNumber")
}
