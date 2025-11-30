package info.meuse24.smsforwarderneoA1.domain.model

/**
 * Domain model representing a log entry.
 *
 * Contains timestamp, message text, phone number, and highlight flag.
 */
data class LogEntry(
    val timestamp: String,
    val text: String,
    val number: String,
    val shouldHighlight: Boolean = false
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
