package jsanzo.wickkit

import android.os.Bundle
import androidx.activity.ComponentActivity

internal class LeakedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LeakSimulator.retained = this
        finish()
    }
}
