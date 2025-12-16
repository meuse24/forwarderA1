package info.meuse24.smsforwarderneoA1.data.local

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

/**
 * Custom Timber Tree that writes structured logs to JSON Lines file.
 *
 * Features:
 * - JSON Lines format (one JSON object per line)
 * - Structured metadata: component, action, details
 * - Automatic file rotation at configurable size
 * - Thread-safe async writing
 * - Crash-resistant with graceful degradation
 * - Circuit breaker: Auto-disables after 3 consecutive write failures
 *
 * @param context Android context for file storage
 * @param maxFileSizeMB Maximum file size in MB before rotation (default: 5MB)
 */
class FileLoggingTree(
    context: Context,
    maxFileSizeMB: Int = 5
) : Timber.Tree() {

    private val maxFileSize: Long = (maxFileSizeMB * 1024 * 1024).toLong()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val logMutex = Mutex()

    private val baseLogDir: File = context.getExternalFilesDir("logs")
        ?: context.getExternalFilesDir(null)
        ?: File(context.filesDir, "logs")

    private val mainLogFile = File(baseLogDir, "app_log.jsonl")
    private val backupFile = File(baseLogDir, "app_log_backup.jsonl")
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    // Thread-local storage for structured metadata
    private val metadataThreadLocal = ThreadLocal<LogMetadata?>()

    // Flag to track if logging is enabled (graceful degradation if init fails)
    @Volatile
    private var isEnabled = false

    // Circuit breaker: Count consecutive write failures
    private val consecutiveFailures = AtomicInteger(0)

    /**
     * Structured metadata for log entries.
     *
     * @param component Component name (e.g., "SmsForegroundService")
     * @param action Action name (e.g., "FORWARD_SMS")
     * @param details Optional key-value details map
     */
    data class LogMetadata(
        val component: String,
        val action: String,
        val details: Map<String, Any?> = emptyMap()
    )

    init {
        isEnabled = try {
            // Try to create log directory
            if (!baseLogDir.exists() && !baseLogDir.mkdirs()) {
                Log.e("FileLoggingTree", "Failed to create log directory: $baseLogDir")
                false
            } else {
                // Try to create log file
                val fileCreated = if (!mainLogFile.exists()) {
                    try {
                        mainLogFile.createNewFile()
                        true
                    } catch (e: Exception) {
                        Log.e("FileLoggingTree", "Failed to create log file: ${mainLogFile.absolutePath}", e)
                        false
                    }
                } else {
                    true
                }

                if (!fileCreated) {
                    false
                } else if (!mainLogFile.canWrite()) {
                    // Verify file is writable
                    Log.e("FileLoggingTree", "Log file is not writable: ${mainLogFile.absolutePath}")
                    false
                } else {
                    Log.i("FileLoggingTree", "Logging initialized successfully: ${mainLogFile.absolutePath}")
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("FileLoggingTree", "Logging initialization failed - will operate in no-op mode", e)
            false
        }
    }

    /**
     * Set metadata for the next log call.
     * Must be called before Timber.x() to attach metadata.
     *
     * This method is thread-safe using ThreadLocal storage.
     */
    fun setMetadata(component: String, action: String, details: Map<String, Any?> = emptyMap()) {
        metadataThreadLocal.set(LogMetadata(component, action, details))
    }

    /**
     * Timber.Tree override - logs to file with metadata.
     *
     * @param priority Log priority (Log.VERBOSE, Log.DEBUG, Log.INFO, Log.WARN, Log.ERROR)
     * @param tag Timber tag (usually class name)
     * @param message Log message
     * @param t Optional throwable
     */
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (!isEnabled) return // Graceful degradation - no-op if logging disabled

        val metadata = metadataThreadLocal.get()
        metadataThreadLocal.remove() // Clean up after use

        val level = when (priority) {
            Log.VERBOSE -> "VERBOSE"
            Log.DEBUG -> "DEBUG"
            Log.INFO -> "INFO"
            Log.WARN -> "WARNING"
            Log.ERROR -> "ERROR"
            else -> "UNKNOWN"
        }

        scope.launch {
            writeLogToFile(level, tag, message, metadata, t)
        }
    }

    /**
     * Write log entry to file in JSON Lines format.
     *
     * Thread-safe via Mutex lock.
     */
    private suspend fun writeLogToFile(
        level: String,
        tag: String?,
        message: String,
        metadata: LogMetadata?,
        throwable: Throwable?
    ) {
        logMutex.withLock {
            try {
                // Check file size and rotate if needed
                if (mainLogFile.length() > maxFileSize) {
                    rotateLogFiles()
                }

                // Build JSON log entry
                val jsonObject = JSONObject().apply {
                    put("timestamp", getCurrentTimestamp())
                    put("level", level)
                    put("tag", tag ?: "Unknown")
                    put("message", message)

                    // Add metadata if present
                    if (metadata != null) {
                        put("component", metadata.component)
                        put("action", metadata.action)
                        if (metadata.details.isNotEmpty()) {
                            put("details", JSONObject(metadata.details))
                        }
                    }

                    // Add exception info if present
                    if (throwable != null) {
                        put("exception", JSONObject().apply {
                            put("type", throwable.javaClass.simpleName)
                            put("message", throwable.message ?: "")
                            put("stacktrace", throwable.stackTraceToString())
                        })
                    }
                }

                // Append to file (one JSON object per line)
                mainLogFile.appendText(jsonObject.toString() + "\n")

                // Reset failure counter on successful write
                consecutiveFailures.set(0)

            } catch (e: Exception) {
                // Circuit breaker: Count failures and disable if threshold reached
                val failures = consecutiveFailures.incrementAndGet()

                if (failures >= MAX_CONSECUTIVE_FAILURES) {
                    isEnabled = false
                    Log.e(TAG, "Logging disabled after $failures consecutive write failures (last error: ${e.message})", e)
                } else {
                    Log.e(TAG, "Failed to write log to file (failure $failures/$MAX_CONSECUTIVE_FAILURES)", e)
                }
            }
        }
    }

    /**
     * Rotate log files when main file exceeds max size.
     *
     * Old backup is deleted, main becomes backup, new file is created.
     * Uses circuit breaker pattern - disables logging after repeated failures.
     */
    private fun rotateLogFiles() {
        try {
            backupFile.delete()
            mainLogFile.renameTo(backupFile)
            mainLogFile.createNewFile()

            // Reset failure counter on successful rotation
            consecutiveFailures.set(0)

            Log.i(TAG, "Log files rotated (size exceeded ${maxFileSize / (1024 * 1024)}MB)")
        } catch (e: Exception) {
            // Circuit breaker: Count rotation failures and disable if threshold reached
            val failures = consecutiveFailures.incrementAndGet()

            if (failures >= MAX_CONSECUTIVE_FAILURES) {
                isEnabled = false
                Log.e(TAG, "Logging disabled after $failures consecutive rotation failures (last error: ${e.message})", e)
            } else {
                Log.e(TAG, "Failed to rotate log files (failure $failures/$MAX_CONSECUTIVE_FAILURES)", e)
            }
        }
    }

    /**
     * Get current timestamp in configured format.
     */
    private fun getCurrentTimestamp(): String = LocalDateTime.now().format(dateFormat)

    /**
     * Read all log entries from file.
     * Returns list of JSON objects (one per line).
     *
     * Malformed lines are skipped with a warning.
     */
    fun readLogEntries(): List<JSONObject> {
        if (!isEnabled) return emptyList() // Graceful degradation

        return try {
            if (!mainLogFile.exists()) {
                return emptyList()
            }

            mainLogFile.readLines()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    try {
                        JSONObject(line)
                    } catch (e: Exception) {
                        Log.w(TAG, "Skipping malformed log line: ${line.take(100)}...", e)
                        null
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read log entries", e)
            emptyList()
        }
    }

    /**
     * Clear all logs (delete main log file).
     */
    fun clearLog() {
        if (!isEnabled) return // Graceful degradation

        try {
            mainLogFile.delete()
            mainLogFile.createNewFile()
            Log.i(TAG, "Logs cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear log", e)
        }
    }

    companion object {
        private const val TAG = "FileLoggingTree"
        private const val MAX_CONSECUTIVE_FAILURES = 3
    }
}
