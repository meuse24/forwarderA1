package info.meuse24.smsforwarderneoA1.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import info.meuse24.smsforwarderneoA1.data.local.Logger
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

    // Relevant Actions: Only these log entries are shown when filtering is active
    // Filter zeigt nur: Aktivierung, Deaktivierung, Statusabfrage, Reset, Weiterleitungen
    // Note: All ERROR level logs are also shown (automatic error inclusion via LogLevel)
    companion object {
        private val RELEVANT_ACTIONS = setOf(
            // Rufumleitung aktivieren/deaktivieren
            "ACTIVATE_FORWARDING",
            "DEACTIVATE_FORWARDING",

            // Statusabfrage
            "QUERY_FORWARDING_STATUS",

            // Reset
            "RESET_ALL_FORWARDING",

            // SMS-Weiterleitung (nur die tatsächliche Weiterleitung)
            "FORWARD_SMS",
            "SEND_SMS",

            // Email-Weiterleitung (nur die tatsächliche Weiterleitung)
            "EMAIL_FORWARD"
            // Note: ERROR level logs are automatically included via LogLevel check
        )
    }

    // StateFlows
    private val _logEntriesHtml = MutableStateFlow("")
    val logEntriesHtml: StateFlow<String> = _logEntriesHtml

    private val _logEntries = MutableStateFlow<List<LogEntry>>(emptyList())
    val logEntries: StateFlow<List<LogEntry>> = _logEntries

    private val _showAllLogs = MutableStateFlow(true)  // Default: show all logs
    val showAllLogs: StateFlow<Boolean> = _showAllLogs.asStateFlow()

    /**
     * Reload log entries from storage.
     *
     * Applies current filter settings (_showAllLogs) to determine which entries to display.
     * - If showAllLogs = true: displays all log entries
     * - If showAllLogs = false: shows only:
     *   - Aktivierung/Deaktivierung der Rufumleitung
     *   - Statusabfragen
     *   - Reset-Vorgänge
     *   - Durchführung der Weiterleitungen (Call, SMS, Email)
     *   - Alle ERROR-Level Logs
     *
     * Updates both HTML and list representations of logs.
     */
    fun reloadLogs() {
        viewModelScope.launch {
            try {
                val showAll = _showAllLogs.value

                // Load all entries first
                val allEntries = logger.getLogEntriesAsList()

                // Apply filter if needed
                _logEntries.value = if (showAll) {
                    // Show all entries
                    allEntries
                } else {
                    // Show ONLY relevant actions + ERROR level logs
                    allEntries.filter { entry ->
                        val actionMatch = Regex("""\]\s+(\w+)(\s+\||$)""").find(entry.text)
                        val action = actionMatch?.groupValues?.get(1)

                        // Bedingung 1: ACTION in RELEVANT_ACTIONS
                        // Bedingung 2: LogLevel == ERROR (nur schwere Fehler)
                        action != null && (
                            action in RELEVANT_ACTIONS ||
                            entry.logLevel == Logger.LogLevel.ERROR
                        )
                    }
                }

                // Update HTML representation
                _logEntriesHtml.value = logger.getLogEntriesHtml(
                    filterNoise = !showAll,
                    noiseActions = if (!showAll) {
                        // Invert: Remove everything NOT in RELEVANT_ACTIONS and not ERROR level
                        allEntries
                            .filter { entry ->
                                val actionMatch = Regex("""\]\s+(\w+)(\s+\||$)""").find(entry.text)
                                val action = actionMatch?.groupValues?.get(1)
                                // Invertiert: Alles was NICHT angezeigt werden soll
                                !(action != null && (
                                    action in RELEVANT_ACTIONS ||
                                    entry.logLevel == Logger.LogLevel.ERROR
                                ))
                            }
                            .mapNotNull { entry ->
                                val actionMatch = Regex("""\]\s+(\w+)(\s+\||$)""").find(entry.text)
                                actionMatch?.groupValues?.get(1)
                            }
                            .toSet()
                    } else {
                        emptySet()
                    }
                )
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
