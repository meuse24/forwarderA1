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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import info.meuse24.smsforwarderneoA1.R
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
            contentDescription = stringResource(R.string.desc_refresh_logs),
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
            contentDescription = if (showAllLogs) stringResource(R.string.desc_filter_important) else stringResource(R.string.desc_filter_all),
            modifier = Modifier.size(28.dp)
        )
    }
}

/**
 * Button to share log entries as a CSV file.
 *
 * Creates a temporary CSV file and opens the system share sheet.
 *
 * @param context Android context for file operations and intents
 */
@Composable
fun ShareLogIconButton(context: Context) {
    FloatingActionButton(
        onClick = { shareLogsAsCsv(context) },
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Share,
            contentDescription = stringResource(R.string.desc_share_log_entries),
            modifier = Modifier.size(28.dp)
        )
    }
}

/**
 * Creates a temporary CSV file with log entries and opens the system share sheet.
 *
 * Reads JSON logs from FileLoggingTree, converts to CSV, and shares via FileProvider.
 *
 * @param context Android context for file operations and intents
 */
private fun shareLogsAsCsv(context: Context) {
    try {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "sms_forwarder_log_$timeStamp.csv"
        val file = File(context.cacheDir, fileName)

        // Get logs from FileLoggingTree
        val fileTree = LoggingManager.getFileTree()
        val jsonLogs = fileTree.readLogEntries()

        if (jsonLogs.isEmpty()) {
            SnackbarManager.showWarning(
                context.getString(R.string.msg_warning_no_logs_to_share),
                duration = SnackbarManager.Duration.LONG
            )
            return
        }

        // Convert to CSV
        val csvContent = buildString {
            // Header (with German column names)
            appendLine("Zeitstempel;Level;Komponente;Aktion;Nachricht;Details")

            // Rows
            jsonLogs.reversed().forEach { json ->  // Newest first
                val timestamp = json.optString("timestamp", "")
                val level = json.optString("level", "INFO")
                val component = json.optString("component", "Unbekannt")
                val action = json.optString("action", "UNBEKANNT")
                val message = json.optString("message", "")
                    .replace(";", ",")  // Escape semicolons
                    .replace("\n", " ")  // Remove newlines

                // Flatten details
                val details = if (json.has("details")) {
                    val detailsJson = json.getJSONObject("details")
                    detailsJson.keys().asSequence()
                        .joinToString(" ") { key ->
                            "$key=${detailsJson.get(key)}"
                        }
                        .replace(";", ",")
                } else ""

                appendLine("$timestamp;$level;$component;$action;$message;$details")
            }
        }

        file.writeText(csvContent)

        // Share via FileProvider
        val fileUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(
            intent,
            context.getString(R.string.title_share_log_file)
        ))

        LoggingManager.logInfo(
            component = "LogButtons",
            action = "SHARE_LOGS",
            message = "Logs als CSV geteilt",
            details = mapOf(
                "filename" to fileName,
                "size_bytes" to file.length(),
                "entry_count" to jsonLogs.size
            )
        )

    } catch (e: Exception) {
        LoggingManager.logError(
            component = "LogButtons",
            action = "SHARE_LOGS_ERROR",
            message = "Fehler beim Teilen der Logs",
            error = e
        )
        SnackbarManager.showError(context.getString(R.string.error_sharing_logs))
    }
}
