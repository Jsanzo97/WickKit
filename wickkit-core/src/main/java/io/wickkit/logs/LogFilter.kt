package io.wickkit.logs

internal sealed interface LogFilter {
    data object None : LogFilter
    data class ByText(val query: String) : LogFilter
    data class ByTag(val query: String) : LogFilter
}

internal fun parseLogFilter(query: String): LogFilter {
    if (query.isBlank()) return LogFilter.None
    return if (query.startsWith("tag:", ignoreCase = true)) {
        LogFilter.ByTag(query.drop(4).trim())
    } else {
        LogFilter.ByText(query)
    }
}

internal fun LogEntry.matches(filter: LogFilter): Boolean = when (filter) {
    LogFilter.None -> true

    is LogFilter.ByText ->
        tag.contains(filter.query, ignoreCase = true) ||
            message.contains(filter.query, ignoreCase = true)

    is LogFilter.ByTag ->
        tag.contains(filter.query, ignoreCase = true)
}
