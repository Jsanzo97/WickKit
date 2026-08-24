package io.wickkit.logs

import android.os.Process
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal object WickKitLogcat {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // threadtime format: MM-DD HH:MM:SS.mmm  PID  TID LEVEL TAG: message
    private val lineRegex = Regex(
        """^\d{2}-\d{2} (\d{2}:\d{2}:\d{2}\.\d{3})\s+\d+\s+\d+\s+([VDIWEF])\s+(.*?):\s*(.*)$""",
    )

    private val levelMap = mapOf(
        'V' to LogLevel.VERBOSE,
        'D' to LogLevel.DEBUG,
        'I' to LogLevel.INFO,
        'W' to LogLevel.WARN,
        'E' to LogLevel.ERROR,
        'F' to LogLevel.ERROR,
    )

    fun start() {
        val pid = Process.myPid()
        scope.launch {
            var backoffMs = 1_000L
            while (true) {
                runCatching { readProcess(pid) }
                delay(backoffMs.milliseconds)
                backoffMs = (backoffMs * 2).coerceAtMost(30_000L)
            }
        }
    }

    private fun readProcess(pid: Int) {
        val process = Runtime.getRuntime().exec(
            arrayOf("logcat", "--pid=$pid", "-v", "threadtime"),
        )
        process.inputStream.bufferedReader().lineSequence().forEach { line ->
            parseLine(line)?.let { (level, tag, message, time) ->
                WickKitLogManager.add(
                    level = level,
                    tag = tag,
                    message = message,
                    time = time,
                )
            }
        }
    }

    private fun parseLine(line: String): ParsedLine? {
        val match = lineRegex.find(line) ?: return null
        val time = match.groupValues[1]
        val level = levelMap[match.groupValues[2].firstOrNull()] ?: return null
        val tag = match.groupValues[3].trim()
        val message = match.groupValues[4]
        return ParsedLine(
            level = level,
            tag = tag,
            message = message,
            time = time,
        )
    }

    private data class ParsedLine(
        val level: LogLevel,
        val tag: String,
        val message: String,
        val time: String,
    )
}
