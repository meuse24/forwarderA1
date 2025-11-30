package info.meuse24.smsforwarderneoA1.presentation.ui.screens.logs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import info.meuse24.smsforwarderneoA1.domain.model.LogEntry
import info.meuse24.smsforwarderneoA1.presentation.viewmodel.LogViewModel

/**
 * CSS styles for HTML log table display.
 *
 * Provides responsive table styling with:
 * - Sticky header
 * - Dark mode support
 * - Mobile-optimized font sizes
 * - Highlight styling for important entries
 */
private fun getLogTableCSS(): String = """
    <style>
        html, body {
            margin: 0;
            padding: 0;
            width: 100%;
            height: 100%;
            overflow-x: hidden;
            font-family: -apple-system, BlinkMacSystemFont, sans-serif;
            font-size: 11px;
            line-height: 1.2;
        }
        .table-container {
            position: relative;
            height: 100%;
            overflow-y: auto;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            table-layout: fixed;
            position: relative;
        }
        thead {
            position: sticky;
            top: 0;
            z-index: 10;
            background-color: #f0f0f0;
        }
        th {
            background-color: #f0f0f0;
            position: sticky;
            top: 0;
            z-index: 10;
            padding: 8px 4px;
            text-align: left;
            border: none;
            border-bottom: 2px solid #ddd;
            font-size: 12px;
            font-weight: bold;
            overflow: hidden;
            text-overflow: ellipsis;
        }
        tbody {
            position: relative;
        }
        td {
            padding: 4px 4px;
            border: none;
            border-bottom: 1px solid #eee;
            vertical-align: top;
            font-size: 10px;
            max-width: 0;
            overflow: hidden;
            text-overflow: ellipsis;
            word-wrap: break-word;
        }
        .time-column {
            width: 90px;
            min-width: 90px;
            max-width: 90px;
        }
        .time-cell {
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }
        .date {
            font-weight: normal;
            font-size: 9px;
        }
        .time {
            color: #666;
            font-size: 9px;
        }
        .entry-column {
            word-break: break-all;
            white-space: normal;
            width: auto;
            overflow-wrap: break-word;
        }
        .sms-forward {
            color: #d32f2f;
            font-weight: 500;
        }
        tr:hover {
            background: #f8f8f8;
        }
        @media (prefers-color-scheme: dark) {
            body {
                background-color: #121212;
                color: #ffffff;
            }
            thead, th {
                background-color: #1e1e1e;
                color: #ffffff;
                border-bottom-color: #444;
            }
            td {
                border-bottom-color: #333;
            }
            tr:hover {
                background: #1a1a1a;
            }
            .time {
                color: #999;
            }
        }
    </style>
"""

/**
 * Modifies HTML log output with CSS styling and table wrapper.
 *
 * @param logEntriesHtml Raw HTML log entries
 * @return Styled HTML with CSS and container divs
 */
private fun modifyLogHtml(logEntriesHtml: String): String {
    return logEntriesHtml
        .replace("<html>", "<html><head>${getLogTableCSS()}</head>")
        .replace("<table", "<div class=\"table-container\"><table class=\"log-table\"")
        .replace("</table>", "</table></div>")
        .replace("<td>", "<td class=\"entry-col\">")
}

/**
 * Log table component displaying log entries in a scrollable list.
 *
 * Features:
 * - Fixed header row
 * - Lazy-loaded log entries for performance
 * - Empty state message
 *
 * @param logViewModel The LogViewModel instance containing log entries
 */
@Composable
fun LogTable(logViewModel: LogViewModel) {
    val logEntries by logViewModel.logEntries.collectAsState()

    if (logEntries.isEmpty()) {
        Text("Keine Log-Einträge vorhanden oder Fehler beim Laden der Logs.")
    } else {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Fixed Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Zeit",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(0.3f)
                    )
                    Text(
                        text = "Eintrag",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(0.7f)
                    )
                }
            }

            // Scrollable Content
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(logEntries) { logEntry ->
                    LogEntryRow(logEntry)
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                }
            }
        }
    }
}

/**
 * Individual log entry row component.
 *
 * Displays:
 * - Formatted date and time
 * - Log message with highlighting for important entries
 *
 * @param logEntry The log entry to display
 */
@Composable
fun LogEntryRow(logEntry: LogEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Zeit-Spalte
        Column(
            modifier = Modifier.weight(0.3f)
        ) {
            Text(
                text = logEntry.getFormattedDate(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = logEntry.getFormattedTime(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Eintrags-Spalte
        Text(
            text = logEntry.getMessage(),
            style = MaterialTheme.typography.bodySmall,
            color = if (logEntry.isHighlighted())
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.7f)
        )
    }
}
