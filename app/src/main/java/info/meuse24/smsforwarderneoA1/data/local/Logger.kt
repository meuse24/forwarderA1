package info.meuse24.smsforwarderneoA1.data.local

import android.content.Context
import android.os.Build
import android.util.Log
import info.meuse24.smsforwarderneoA1.domain.model.LogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.w3c.dom.Element
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Logging system with structured metadata and XML file persistence.
 *
 * Features:
 * - Structured logging with component, action, and details
 * - XML file storage with rotation
 * - HTML/CSV export
 * - Highlight patterns for important events
 * - Coroutine-based async writing
 */
class Logger(
    context: Context,
    private val maxFileSize: Long = 5 * 1024 * 1024 // 5MB
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val logMutex = Mutex()
    private val baseLogDir: File = context.getExternalFilesDir("logs")
        ?: context.getExternalFilesDir(null)
        ?: File(context.filesDir, "logs")
    private val mainLogFile = File(baseLogDir, "app_log.xml")
    private val backupFile = File(baseLogDir, "app_log_backup.xml")
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private var logEntryCounter = 0

    init {
        require(baseLogDir.exists() || baseLogDir.mkdirs()) { "Log directory could not be created: $baseLogDir" }
        if (!mainLogFile.exists()) {
            createEmptyLogFile()
        }
        addInitialLogEntry()
    }

    enum class LogLevel {
        INFO, WARNING, ERROR, DEBUG
    }

    data class LogMetadata(
        val component: String,
        val action: String,
        val details: Map<String, Any?> = emptyMap()
    )

    fun log(
        level: LogLevel,
        metadata: LogMetadata,
        message: String,
        exception: Throwable? = null
    ) {
        val logMessage = buildLogMessage(metadata, message)
        when (level) {
            LogLevel.INFO -> Log.i(TAG, logMessage)
            LogLevel.WARNING -> Log.w(TAG, logMessage)
            LogLevel.ERROR -> Log.e(TAG, logMessage, exception)
            LogLevel.DEBUG -> Log.d(TAG, logMessage)
        }

        val prefix = when (level) {
            LogLevel.INFO -> "ℹ️"
            LogLevel.WARNING -> "⚠️"
            LogLevel.ERROR -> "❌"
            LogLevel.DEBUG -> "🔍"
        }

        val logEntry = buildString {
            append(prefix)
            append(" ")
            append(logMessage)
            if (exception != null) {
                append(" | Exception: ${exception.message}")
            }
        }

        scope.launch {
            writeLogToFile(getCurrentTimestamp(), logEntry)
        }
    }

    private fun buildLogMessage(metadata: LogMetadata, message: String): String {
        return buildString {
            append("[${metadata.component}]")
            append(" ${metadata.action}")
            if (metadata.details.isNotEmpty()) {
                append(" | ")
                append(metadata.details.entries.joinToString(", ") { "${it.key}=${it.value}" })
            }
            append(" | ")
            append(message)
        }
    }

    private suspend fun writeLogToFile(timestamp: String, entry: String) {
        logMutex.withLock {
            try {
                if (mainLogFile.length() > maxFileSize) {
                    rotateLogFiles()
                }
                logEntryCounter++
                appendToLogFile(timestamp, entry, logEntryCounter)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write log", e)
            }
        }
    }

    private fun appendToLogFile(timestamp: String, entry: String, entryNumber: Int) {
        try {
            val document = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(mainLogFile)

            val root = document.documentElement
            val sanitizedEntry = sanitizeXmlText(entry)
            val newEntry = document.createElement("logEntry").apply {
                appendChild(document.createElement("number").apply { textContent = entryNumber.toString() })
                appendChild(document.createElement("time").apply { textContent = timestamp })
                appendChild(document.createElement("text").apply { textContent = sanitizedEntry })
            }
            root.appendChild(newEntry)

            TransformerFactory.newInstance().newTransformer().transform(
                DOMSource(document),
                StreamResult(mainLogFile)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to append log entry", e)
        }
    }

    /**
     * Sanitizes text for XML by removing invalid XML characters.
     * Valid XML chars: #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
     */
    private fun sanitizeXmlText(text: String): String {
        return text.filter { char ->
            val code = char.code
            code == 0x9 || code == 0xA || code == 0xD ||
            (code in 0x20..0xD7FF) ||
            (code in 0xE000..0xFFFD)
        }
    }

    private fun rotateLogFiles() {
        backupFile.delete()
        mainLogFile.renameTo(backupFile)
        createEmptyLogFile()
    }

    private fun createEmptyLogFile() {
        try {
            mainLogFile.writeText(
                """<?xml version="1.0" encoding="UTF-8"?>
                   <logEntries>
                   </logEntries>
                """.trimIndent()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create empty log file", e)
        }
    }

    private fun addInitialLogEntry() {
        val initMessage = buildString {
            append("Logger initialized. ")
            append("Device: ${Build.MANUFACTURER} ${Build.MODEL}, ")
            append("Android: ${Build.VERSION.RELEASE} ")
            append("(API ${Build.VERSION.SDK_INT})")
        }
        log(LogLevel.INFO, LogMetadata("Logger", "INIT"), initMessage)
    }

    fun getLogEntries(): String = readLogEntries { entry ->
        val time = entry.getElementsByTagName("time").item(0).textContent
        val text = entry.getElementsByTagName("text").item(0).textContent

        // Prüfe ob einer der Patterns im Text vorkommt
        val shouldHighlight = highlightPatterns.any { pattern ->
            text.contains(pattern, ignoreCase = true)
        }

        // Füge einen Farbcode für roten Text hinzu wenn nötig
        val textWithColor = if (shouldHighlight) {
            "\u001B[31m$text\u001B[0m"  // Rot für hervorgehobenen Text
        } else {
            text
        }

        "$time - $textWithColor\n"
    }

    private fun readLogEntries(process: (Element) -> String): String {
        return try {
            val document = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(mainLogFile)
            val entries = document.getElementsByTagName("logEntry")
            buildString {
                for (i in entries.length - 1 downTo 0) {
                    val entry = entries.item(i) as? Element ?: continue
                    append(process(entry))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read log entries - XML corrupted, recreating log file", e)
            // Backup corrupted file and create new one
            try {
                val corruptedBackup = File(baseLogDir, "app_log_corrupted_${System.currentTimeMillis()}.xml")
                mainLogFile.renameTo(corruptedBackup)
                createEmptyLogFile()
                addInitialLogEntry()
                log(LogLevel.WARNING, LogMetadata("Logger", "RECOVER_CORRUPTED_LOG"),
                    "Previous log file was corrupted and backed up to: ${corruptedBackup.name}")
            } catch (recoveryError: Exception) {
                Log.e(TAG, "Failed to recover from corrupted log", recoveryError)
            }
            ""
        }
    }

    private val highlightPatterns = listOf(
        "ACTIVATE_FORWARDING",
        "EXCEPTION",
        "FAILURE",
        "CRITICAL",
        "SMS_FORWARD_FAILED",
        "EMAIL_FORWARD_ERROR",
        "PERMISSION_DENIED",
        "WAKE_LOCK_ERROR",
        "CONNECTION_FAILED",
        "INVALID_NUMBER",
        "SECURITY_ERROR",
        "AUTHENTICATION_FAILED"
    )

    private fun readLogEntries(
        filterNoise: Boolean = false,
        noiseActions: Set<String> = emptySet(),
        process: (Element, Boolean) -> String
    ): String {
        return try {
            val document = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(mainLogFile)
            val entries = document.getElementsByTagName("logEntry")
            buildString {
                for (i in entries.length - 1 downTo 0) {
                    val entry = entries.item(i) as? Element ?: continue
                    val entryText = entry.getElementsByTagName("text").item(0).textContent
                    entry.getElementsByTagName("number").item(0)?.textContent?.toIntOrNull()

                    // Extrahiere ACTION aus dem Log-Text
                    // Format: [Component] ACTION | details | message
                    val actionMatch = Regex("""\]\s+(\w+)(\s+\||$)""").find(entryText)
                    val action = actionMatch?.groupValues?.get(1)

                    // Debug: Log the filtering
                    if (filterNoise) {
                        Log.d(TAG, "Filtering: action='$action', in noiseActions=${action in noiseActions}, text='${entryText.take(100)}'")
                    }

                    // Filtere Noise-Actions wenn aktiviert
                    if (filterNoise && action != null && action in noiseActions) {
                        Log.d(TAG, "  -> FILTERED OUT: $action")
                        continue  // Überspringe diesen Eintrag
                    }

                    // Prüfe, ob einer der Patterns im Text vorkommt
                    val shouldHighlight = highlightPatterns.any { pattern ->
                        entryText.contains(pattern, ignoreCase = true)
                    }

                    // Füge die Nummer des Log-Eintrags hinzu, falls vorhanden
                    val processedEntry = process(entry, shouldHighlight)
                    append(processedEntry)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read log entries (filtered) - XML corrupted, recreating log file", e)
            // Backup corrupted file and create new one
            try {
                val corruptedBackup = File(baseLogDir, "app_log_corrupted_${System.currentTimeMillis()}.xml")
                mainLogFile.renameTo(corruptedBackup)
                createEmptyLogFile()
                addInitialLogEntry()
                log(LogLevel.WARNING, LogMetadata("Logger", "RECOVER_CORRUPTED_LOG"),
                    "Previous log file was corrupted and backed up to: ${corruptedBackup.name}")
            } catch (recoveryError: Exception) {
                Log.e(TAG, "Failed to recover from corrupted log", recoveryError)
            }
            ""
        }
    }

    fun getLogEntriesHtml(
        filterNoise: Boolean = false,
        noiseActions: Set<String> = emptySet()
    ): String = buildString {
        append(HTML_HEADER)
        append(readLogEntries(filterNoise, noiseActions) { entry, shouldHighlight ->
            val timestamp = entry.getElementsByTagName("time").item(0).textContent
            val text = entry.getElementsByTagName("text").item(0).textContent
            val number = entry.getElementsByTagName("number").item(0)?.textContent ?: "N/A"

            // Timestamp umformatieren
            val formattedTimestamp = LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            val date = formattedTimestamp.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            val time = formattedTimestamp.format(DateTimeFormatter.ofPattern("HHmmss"))
            val textClass = if (shouldHighlight) "text-red-600" else ""

            """
        <tr>
            <td class="time-column">
                <div class="time-cell">
                <span class="date">#$number</span>
                    <span class="date">$date</span>
                    <span class="time">$time</span>
                </div>
            </td>
            <td class="entry-column $textClass">$text</td>
        </tr>
        """.trimIndent()
        })
        append(HTML_FOOTER)
    }

    fun getLogEntriesAsList(): List<LogEntry> {
        return try {
            val document = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(mainLogFile)
            val entries = document.getElementsByTagName("logEntry")
            val logEntries = mutableListOf<LogEntry>()

            for (i in entries.length - 1 downTo 0) {
                val entry = entries.item(i) as? Element ?: continue
                val timestamp = entry.getElementsByTagName("time").item(0).textContent
                val text = entry.getElementsByTagName("text").item(0).textContent
                val number = entry.getElementsByTagName("number").item(0)?.textContent ?: "N/A"

                // Check if this entry should be highlighted
                val shouldHighlight = highlightPatterns.any { pattern ->
                    text.contains(pattern, ignoreCase = true)
                }

                // Format timestamp for display
                val formattedTimestamp = try {
                    val dateTime = LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    val date = dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yy"))
                    val time = dateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                    "#$number\n$date\n$time"
                } catch (e: Exception) {
                    "#$number\n$timestamp"
                }

                logEntries.add(LogEntry(
                    timestamp = formattedTimestamp,
                    text = text,
                    number = number,
                    shouldHighlight = shouldHighlight
                ))
            }

            logEntries
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read log entries as list - XML corrupted, recreating log file", e)
            // Backup corrupted file and create new one
            try {
                val corruptedBackup = File(baseLogDir, "app_log_corrupted_${System.currentTimeMillis()}.xml")
                mainLogFile.renameTo(corruptedBackup)
                createEmptyLogFile()
                addInitialLogEntry()
                log(LogLevel.WARNING, LogMetadata("Logger", "RECOVER_CORRUPTED_LOG"),
                    "Previous log file was corrupted and backed up to: ${corruptedBackup.name}")
            } catch (recoveryError: Exception) {
                Log.e(TAG, "Failed to recover from corrupted log", recoveryError)
            }
            emptyList()
        }
    }

    private fun filterNonAscii(text: String): String {
        val germanReplacements = mapOf(
            'ä' to "ae",
            'ö' to "oe",
            'ü' to "ue",
            'Ä' to "Ae",
            'Ö' to "Oe",
            'Ü' to "Ue",
            'ß' to "ss"
        )

        return text.map { char ->
            when {
                germanReplacements.containsKey(char) -> germanReplacements[char]
                char.code in 32..126 || char == '\n' || char == '\r' || char == '\t' -> char.toString()
                else -> ""
            }
        }.joinToString("")
    }

    fun getLogEntriesAsCsv(): String = buildString {
        append("Nr;Datum;Zeit;Eintrag\n")
        append(readLogEntries { entry ->
            val timestamp = entry.getElementsByTagName("time").item(0).textContent
            val text = filterNonAscii(entry.getElementsByTagName("text").item(0).textContent)
            val number = entry.getElementsByTagName("number").item(0)?.textContent ?: "N/A"

            val (date, time) = timestamp.split(" ", limit = 2)
            val replacedText = replaceSpecialCharacters(text)

            "$number;$date;$time;$replacedText\n"
        })
    }

    // Hilfsfunktion zum Ersetzen von Umlauten und ß
    private fun replaceSpecialCharacters(text: String): String {
        return text
            .replace("ä", "ae")
            .replace("ö", "oe")
            .replace("ü", "ue")
            .replace("Ä", "Ae")
            .replace("Ö", "Oe")
            .replace("Ü", "Ue")
            .replace("ß", "ss")
            .replace(";", " ")  // Semikolon durch Leerzeichen ersetzen
            .replace("\n", " ")  // Zeilenumbruch durch Leerzeichen ersetzen
            .replace("\r", " ")  // Wagenrücklauf durch Leerzeichen ersetzen
            .replaceFirst("]", ";")
    }

    fun clearLog() {
        createEmptyLogFile()
        addInitialLogEntry()
    }

    private fun getCurrentTimestamp(): String = LocalDateTime.now().format(dateFormat)

    companion object {
        private const val TAG = "Logger"
        private val HTML_HEADER = """
    <!DOCTYPE html>
    <html lang="de">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
            body {
                font-family: 'Courier New', Courier, monospace; /* Monospaced font */
                font-size: 12px; /* Kleinere Schriftgröße */
                margin: 0;
                padding: 16px;
                background: #f5f5f5;
            }
            table {
                width: 100%;
                border-collapse: collapse;
                background: white;
                box-shadow: 0 1px 3px rgba(0,0,0,0.1);
            }
            th {
                background: #4CAF50;
                color: white;
                padding: 8px;
                text-align: left;
                font-size: 14px;
            }
            td {
                padding: 8px;
                border-bottom: 1px solid #eee;
            }
            .number-column {
                width: 60px;
                text-align: right;
                font-weight: bold;
            }

            .time-column {
                width: 140px;
                white-space: nowrap;
            }
            .time-cell {
                display: flex;
                flex-direction: column;
                font-family: 'Courier New', Courier, monospace; /* Monospaced font for timestamp */
            }
            .date {
                color: #666;
                font-size: 0.9em;
            }
            .time {
                color: #666;
                font-size: 0.9em;
            }
            .entry-column {
                font-family: 'Courier New', Courier, monospace; /* Monospaced font for entries */
                font-size: 12px; /* Same smaller font size */
            }
            tr:hover {
                background: #f8f8f8;
            }
            .text-red-600 {
                color: #DC2626 !important;
            }
        </style>
    </head>
    <body>
        <table>
            <thead>
                <tr>
                    <th class="time-column">Zeit</th>
                    <th class="entry-column">Eintrag</th>
                </tr>
            </thead>
            <tbody>
""".trimIndent()
        private const val HTML_FOOTER = """
            </tbody>
        </table>
    </body>
    </html>
"""
    }
}
