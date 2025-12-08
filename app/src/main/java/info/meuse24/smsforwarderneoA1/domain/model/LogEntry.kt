package info.meuse24.smsforwarderneoA1.domain.model

import info.meuse24.smsforwarderneoA1.data.local.Logger

/**
 * Domain model representing a log entry.
 *
 * Contains timestamp, message text, phone number, highlight flag, and log level.
 */
data class LogEntry(
    val timestamp: String,
    val text: String,
    val number: String,
    val shouldHighlight: Boolean = false,
    val logLevel: Logger.LogLevel = Logger.LogLevel.INFO
) {
    fun getFormattedDate(): String {
        return try {
            val lines = timestamp.split("\n")
            if (lines.size >= 2) "#${lines[0].removePrefix("#")}\n${lines[1]}"
            else timestamp
        } catch (e: Exception) {
            timestamp
        }
    }

    fun getFormattedTime(): String {
        return try {
            val lines = timestamp.split("\n")
            if (lines.size >= 3) lines[2]
            else ""
        } catch (e: Exception) {
            ""
        }
    }

    fun getMessage(): String = text

    fun isHighlighted(): Boolean = shouldHighlight
}
