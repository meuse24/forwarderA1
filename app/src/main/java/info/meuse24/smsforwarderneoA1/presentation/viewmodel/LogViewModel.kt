package info.meuse24.smsforwarderneoA1.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import info.meuse24.smsforwarderneoA1.LoggingManager
import info.meuse24.smsforwarderneoA1.domain.model.LogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

/**
 * ViewModel for managing log entries and log filtering.
 *
 * Reads logs from FileLoggingTree (JSON Lines format) and provides filtering.
 */
class LogViewModel : ViewModel() {

    companion object {
        // Actions to show when filtering is enabled (important logs only)
        private val RELEVANT_ACTIONS = setOf(
            "ACTIVATE_FORWARDING",
            "DEACTIVATE_FORWARDING",
            "QUERY_FORWARDING_STATUS",
            "RESET_ALL_FORWARDING",
            "FORWARD_SMS",
            "SEND_SMS",
            "EMAIL_FORWARD"
        )

        // Highlight patterns for important events
        private val HIGHLIGHT_PATTERNS = listOf(
            "CRITICAL",
            "FAILURE",
            "FAILED",
            "EXCEPTION",
            "PERMISSION_DENIED",
            "WAKE_LOCK_ERROR",
            "CONNECTION_FAILED",
            "INVALID_NUMBER",
            "SECURITY_ERROR",
            "AUTHENTICATION_FAILED",
            "LOOP_PROTECTION"
        )
    }

    // StateFlows
    private val _logEntries = MutableStateFlow<List<LogEntry>>(emptyList())
    val logEntries: StateFlow<List<LogEntry>> = _logEntries

    private val _showAllLogs = MutableStateFlow(true)
    val showAllLogs: StateFlow<Boolean> = _showAllLogs.asStateFlow()

    /**
     * Reload log entries from FileLoggingTree.
     *
     * Applies current filter settings (_showAllLogs) to determine which entries to display.
     * Performs I/O operations on Dispatchers.IO to prevent UI blocking.
     */
    fun reloadLogs() {
        viewModelScope.launch {
            try {
                // Perform file reading and parsing on IO dispatcher
                val filteredEntries = withContext(Dispatchers.IO) {
                    val fileTree = LoggingManager.getFileTree()
                    val jsonLogs = fileTree.readLogEntries()

                    // Convert JSON to LogEntry domain models
                    val allEntries = jsonLogs.mapNotNull { json ->
                        parseJsonToLogEntry(json)
                    }.reversed() // Show newest first

                    // Apply filter on IO thread as well
                    if (_showAllLogs.value) {
                        allEntries
                    } else {
                        allEntries.filter { entry ->
                            entry.action in RELEVANT_ACTIONS || entry.level == "ERROR"
                        }
                    }
                }

                // Update StateFlow on Main thread (automatic via viewModelScope)
                _logEntries.value = filteredEntries

            } catch (e: Exception) {
                Timber.tag("LogViewModel").e(e, "Failed to reload logs")
                _logEntries.value = emptyList()
            }
        }
    }

    /**
     * Parse JSON object to LogEntry.
     */
    private fun parseJsonToLogEntry(json: JSONObject): LogEntry? {
        return try {
            val timestamp = json.optString("timestamp", "")
            val level = json.optString("level", "INFO")
            val tag = json.optString("tag", "")
            val message = json.optString("message", "")
            val component = json.optString("component", tag)
            val action = json.optString("action", "")

            // Parse details if present
            val details = mutableMapOf<String, Any?>()
            if (json.has("details")) {
                val detailsJson = json.getJSONObject("details")
                detailsJson.keys().forEach { key ->
                    details[key] = detailsJson.get(key)
                }
            }

            // Parse exception if present
            val exception = if (json.has("exception")) {
                val exJson = json.getJSONObject("exception")
                exJson.optString("stacktrace", null)
            } else null

            // Determine if should highlight
            val shouldHighlight = level == "ERROR" ||
                    HIGHLIGHT_PATTERNS.any { pattern ->
                        message.contains(pattern, ignoreCase = true) ||
                        action.contains(pattern, ignoreCase = true)
                    }

            LogEntry(
                timestamp = timestamp,
                level = level,
                tag = tag,
                message = message,
                component = component,
                action = action,
                details = details,
                exception = exception,
                shouldHighlight = shouldHighlight
            )
        } catch (e: Exception) {
            Timber.tag("LogViewModel").w(e, "Failed to parse log entry")
            null
        }
    }

    /**
     * Toggle log filter between showing all logs and important logs only.
     *
     * Automatically reloads logs with the new filter setting.
     */
    fun toggleLogFilter() {
        _showAllLogs.value = !_showAllLogs.value
        reloadLogs()
    }

    /**
     * Factory for creating LogViewModel instances.
     * No longer requires Logger dependency.
     */
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LogViewModel::class.java)) {
                return LogViewModel() as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
