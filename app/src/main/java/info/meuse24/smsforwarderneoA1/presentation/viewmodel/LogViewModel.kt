package info.meuse24.smsforwarderneoA1.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import info.meuse24.smsforwarderneoA1.Logger
import info.meuse24.smsforwarderneoA1.LoggingManager
import info.meuse24.smsforwarderneoA1.domain.model.LogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing log entries and log filtering.
 *
 * Handles:
 * - Loading log entries in HTML and list format
 * - Filtering logs (show all vs. important only)
 * - Reloading logs from storage
 *
 * Extracted from ContactsViewModel as part of Phase 5 refactoring.
 *
 * @param logger Logger instance for accessing log entries
 */
class LogViewModel(
    private val logger: Logger
) : ViewModel() {

    // Noise Actions: Log entries that are hidden by default when filtering
    companion object {
        private val NOISE_ACTIONS = setOf(
            "HEARTBEAT",
            "UPDATE_NOTIFICATION",
            "LOAD_CONTACTS",
            "LOAD_CONTACTS_START",
            "CONTACTS_RELOAD",
            "FILTER_CONTACTS",
            "FILTER_APPLIED",
            "GET_PREFERENCE",
            "SET_PREFERENCE",
            "SAVE_STATE",
            "VALIDATE_STATE",
            "REGISTER_OBSERVER",
            "UNREGISTER_OBSERVER",
            "WAKE_LOCK_ACQUIRED",
            "WAKE_LOCK_RELEASED",
            "LOW_MEMORY",
            "TRIM_MEMORY",
            "CONFIG_CHANGED",
            "CONTACTS_CHANGED_SKIPPED",
            "APPLY_FILTER"
        )
    }

    // StateFlows
    private val _logEntriesHtml = MutableStateFlow("")
    val logEntriesHtml: StateFlow<String> = _logEntriesHtml

    private val _logEntries = MutableStateFlow<List<LogEntry>>(emptyList())
    val logEntries: StateFlow<List<LogEntry>> = _logEntries

    private val _showAllLogs = MutableStateFlow(false)  // Default: only important logs
    val showAllLogs: StateFlow<Boolean> = _showAllLogs.asStateFlow()

    /**
     * Reload log entries from storage.
     *
     * Applies current filter settings (_showAllLogs) to determine which entries to display.
     * - If showAllLogs = true: displays all log entries
     * - If showAllLogs = false: filters out NOISE_ACTIONS for cleaner view
     *
     * Updates both HTML and list representations of logs.
     */
    fun reloadLogs() {
        viewModelScope.launch {
            try {
                val showAll = _showAllLogs.value
                _logEntriesHtml.value = logger.getLogEntriesHtml(
                    filterNoise = !showAll,
                    noiseActions = NOISE_ACTIONS
                )

                // Load all entries and filter client-side if necessary
                val allEntries = logger.getLogEntriesAsList()
                _logEntries.value = if (!showAll) {
                    // Filter out noise actions
                    allEntries.filter { entry ->
                        val actionMatch = Regex("""\]\s+(\w+)(\s+\||$)""").find(entry.text)
                        val action = actionMatch?.groupValues?.get(1)
                        action == null || action !in NOISE_ACTIONS
                    }
                } else {
                    allEntries
                }
            } catch (e: Exception) {
                LoggingManager.logError(
                    component = "LogViewModel",
                    action = "RELOAD_LOGS_ERROR",
                    message = "Fehler beim Neuladen der Log-Einträge",
                    error = e
                )
            }
        }
    }

    /**
     * Toggle log filter between showing all logs and important logs only.
     *
     * Automatically reloads logs with the new filter setting.
     */
    fun toggleLogFilter() {
        _showAllLogs.value = !_showAllLogs.value
        reloadLogs()  // Reload logs with new filter
    }

    /**
     * Factory for creating LogViewModel instances.
     *
     * Provides logger dependency from AppContainer.
     */
    class Factory(
        private val logger: Logger
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LogViewModel::class.java)) {
                return LogViewModel(logger) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
