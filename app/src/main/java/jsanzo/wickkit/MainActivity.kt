package jsanzo.wickkit

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.wickkit.WickKit
import jsanzo.wickkit.ui.theme.WickKitTheme
import timber.log.Timber

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.plant(Timber.DebugTree())

        // All of these are captured by WickKit via logcat — no special integration needed
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
            val context = LocalContext.current
            WickKitTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("WickKit Sample App")
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { WickKit.open(context) }) {
                            Text("Open Debug Panel")
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = { generateSampleLogs() }) {
                            Text("Generate Logs")
                        }
                    }
                }
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
