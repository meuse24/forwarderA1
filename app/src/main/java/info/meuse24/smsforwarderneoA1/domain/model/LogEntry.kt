package info.meuse24.smsforwarderneoA1.domain.model

/**
 * Domain model for log entries (Timber-based).
 *
 * Simplified model without Logger.LogLevel dependency.
 */
data class LogEntry(
    val timestamp: String,              // "2025-12-13 14:30:45"
    val level: String,                  // "INFO", "WARNING", "ERROR", "DEBUG", "VERBOSE"
    val tag: String,                    // Timber tag (component name)
    val message: String,                // Human-readable message
    val component: String = "",         // Component name (from metadata)
    val action: String = "",            // Action name (from metadata)
    val details: Map<String, Any?> = emptyMap(),
    val exception: String? = null,      // Exception stacktrace if present
    val shouldHighlight: Boolean = false
) {
    /**
     * Get formatted date for display (date only).
     * Example: "2025-12-13"
     */
    fun getFormattedDate(): String {
        return try {
            timestamp.split(" ").firstOrNull() ?: timestamp
        } catch (e: Exception) {
            timestamp
        }
    }

    /**
     * Get formatted time for display (time only).
     * Example: "14:30:45"
     */
    fun getFormattedTime(): String {
        return try {
            timestamp.split(" ").getOrNull(1) ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Get full message including component, action, and details.
     * Format: "[Component] ACTION | key=value | message"
     */
    fun getFullMessage(): String {
        val parts = mutableListOf<String>()

        // Add level emoji prefix
        val emoji = when (level) {
            "INFO" -> "ℹ️"
            "WARNING" -> "⚠️"
            "ERROR" -> "❌"
            "DEBUG" -> "🔍"
            "VERBOSE" -> "📝"
            else -> ""
        }
        if (emoji.isNotEmpty()) {
            parts.add(emoji)
        }

        if (component.isNotEmpty()) {
            parts.add("[$component]")
        }
        if (action.isNotEmpty()) {
            parts.add(action)
        }
        if (details.isNotEmpty()) {
            val detailsStr = details.entries.joinToString(", ") { "${it.key}=${it.value}" }
            parts.add(detailsStr)
        }
        parts.add(message)

        return parts.joinToString(" | ")
    }

    /**
     * Check if entry is ERROR level (for highlighting).
     */
    fun isError(): Boolean = level == "ERROR"

    /**
     * Check if entry should be highlighted (ERROR or explicit flag).
     */
    fun isHighlighted(): Boolean = shouldHighlight || isError()
}
