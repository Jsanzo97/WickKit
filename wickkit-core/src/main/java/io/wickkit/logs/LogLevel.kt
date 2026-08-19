package io.wickkit.logs

internal enum class LogLevel(val label: String, val chipLabel: String) {
    VERBOSE("V", "Verbose"),
    DEBUG("D", "Debug"),
    INFO("I", "Info"),
    WARN("W", "Warn"),
    ERROR("E", "Error"),
}
