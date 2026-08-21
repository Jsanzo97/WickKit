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
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber

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
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    generateSampleLogs()
                    Toast.makeText(context, "Sample logs generated", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Generate Logs")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    generateSampleRequests()
                    Toast.makeText(context, "Network requests sent", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Make Network Requests")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    lifecycleScope.launch(Dispatchers.IO) { sampleDb.reseed() }
                    Toast.makeText(context, "Database reseeded", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Reseed Sample Database")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    SamplePreferences.seed(context)
                    Toast.makeText(context, "Preferences seeded", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Seed Sample Preferences")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    SampleRemoteConfig.fetch()
                    Toast.makeText(context, "Fetching Remote Config", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Fetch Remote Config")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    context.startActivity(Intent(context, LeakedActivity::class.java))
                    Toast.makeText(context, "Leak simulated — check Leaks tab in 5s", Toast.LENGTH_LONG).show()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Simulate Memory Leak")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    context.startActivity(Intent(context, JankActivity::class.java))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Simulate Performance Issues")
            }
        }
    }
}

private var logCounter = 0

private fun generateSampleLogs() {
    val n = ++logCounter
    Timber.tag("UserAction").d("Button tapped — batch #$n")
    Log.i("Network", "GET /api/items?page=$n → 200 OK (${(50..300).random()}ms)")
    Log.w("Database", "Slow query on items table: ${(200..800).random()}ms")
    Log.e("Auth", "Token refresh failed on attempt $n")
    println("System.out batch #$n")
}
