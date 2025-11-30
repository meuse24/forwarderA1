package info.meuse24.smsforwarderneoA1.presentation.ui.screens.logs

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import info.meuse24.smsforwarderneoA1.AppContainer
import info.meuse24.smsforwarderneoA1.LoggingManager
import info.meuse24.smsforwarderneoA1.SnackbarManager
import info.meuse24.smsforwarderneoA1.presentation.viewmodel.LogViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Button to refresh/reload log entries from storage.
 *
 * @param logViewModel The LogViewModel instance for log operations
 */
@Composable
fun RefreshLogButton(logViewModel: LogViewModel) {
    FloatingActionButton(
        onClick = { logViewModel.reloadLogs() },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Logs aktualisieren",
            modifier = Modifier.size(28.dp)
        )
    }
}

/**
 * Button to toggle log filtering between all logs and important logs only.
 *
 * @param logViewModel The LogViewModel instance for log filtering
 * @param showAllLogs Current filter state (true = showing all, false = showing important only)
 */
@Composable
fun FilterLogButton(logViewModel: LogViewModel, showAllLogs: Boolean) {
    FloatingActionButton(
        onClick = { logViewModel.toggleLogFilter() },
        containerColor = if (showAllLogs)
            MaterialTheme.colorScheme.secondaryContainer
        else
            MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = if (showAllLogs)
            MaterialTheme.colorScheme.onSecondaryContainer
        else
            MaterialTheme.colorScheme.onTertiaryContainer,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
    ) {
        Icon(
            imageVector = if (showAllLogs) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            contentDescription = if (showAllLogs) "Nur wichtige Logs" else "Alle Logs anzeigen",
            modifier = Modifier.size(28.dp)
        )
    }
}

/**
 * Button to share log entries as a CSV file.
 *
 * Creates a temporary CSV file and opens the system share sheet.
 * Shows warning if no logs are available.
 *
 * @param context Android context for file operations and intents
 * @param logEntries HTML string of log entries (for empty check)
 */
@Composable
fun ShareLogIconButton(context: Context, logEntries: String) {
    FloatingActionButton(
        onClick = {
            if (logEntries.isNotEmpty()) {
                shareLogsAsFile(context)
            } else {
                SnackbarManager.showWarning(
                    "Keine Log-Einträge zum Teilen vorhanden",
                    duration = SnackbarManager.Duration.LONG
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Share,
            contentDescription = "Log-Einträge teilen",
            modifier = Modifier.size(28.dp)
        )
    }
}

/**
 * Creates a temporary CSV file with log entries and opens the system share sheet.
 *
 * File is created in the app's cache directory with a timestamped filename.
 * Uses FileProvider for secure file sharing.
 *
 * @param context Android context for file operations and intents
 */
private fun shareLogsAsFile(context: Context) {
    try {
        // Erstelle temporäre Datei
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "sms_forwarder_log_$timeStamp.csv"
        val file = File(context.cacheDir, fileName)

        // Hole CSV-Daten vom Logger und schreibe sie in die Datei
        val csvContent = AppContainer.requireLogger().getLogEntriesAsCsv()
        file.writeText(csvContent)

        // Erstelle FileProvider URI
        val fileUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        // Erstelle und starte Share Intent
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Log-Datei teilen"))

        LoggingManager.logInfo(
            component = "MainActivity",
            action = "SHARE_LOGS",
            message = "CSV-Log-Datei wurde zum Teilen erstellt",
            details = mapOf(
                "filename" to fileName,
                "size" to file.length()
            )
        )

    } catch (e: Exception) {
        LoggingManager.logError(
            component = "MainActivity",
            action = "SHARE_LOGS_ERROR",
            message = "Fehler beim Erstellen der CSV-Datei",
            error = e
        )
        SnackbarManager.showError("Fehler beim Teilen der Logs")
    }
}
