package info.meuse24.smsforwarderneoA1

import android.annotation.SuppressLint
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import info.meuse24.smsforwarderneoA1.R
import info.meuse24.smsforwarderneoA1.data.local.FileLoggingTree
import info.meuse24.smsforwarderneoA1.data.local.PermissionHandler
import info.meuse24.smsforwarderneoA1.data.local.SharedPreferencesManager
import timber.log.Timber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.io.File

object AppContainer {
    private lateinit var application: SmsForwarderApplication
    private var activity: MainActivity? = null // Optional statt lateinit

    private val _isBasicInitialized = MutableStateFlow(false)
    val isBasicInitialized = _isBasicInitialized.asStateFlow() // Public StateFlow

    private val _isFullyInitialized = MutableStateFlow(false)
    val isInitialized = _isFullyInitialized.asStateFlow()

    lateinit var prefsManager: SharedPreferencesManager
        private set

    lateinit var permissionHandler: PermissionHandler
        private set

    private lateinit var phoneUtils: PhoneSmsUtils.Companion

    fun initializeCritical(app: SmsForwarderApplication) {
        Log.d("AppContainer", "Starting critical initialization")
        application = app
        prefsManager = SharedPreferencesManager(app)
        PhoneSmsUtils.initialize()
        phoneUtils = PhoneSmsUtils
        _isBasicInitialized.value = true
        Log.d("AppContainer", "Critical initialization complete")
    }

    fun initializeWithActivity(mainActivity: MainActivity) {
        Log.d("AppContainer", "Starting activity-dependent initialization")
        try {
            activity = mainActivity
            permissionHandler = PermissionHandler(mainActivity)
            _isFullyInitialized.value = true
            Log.d("AppContainer", "Full initialization complete")
        } catch (e: Exception) {
            Log.e("AppContainer", "Error in activity initialization", e)
            throw e
        }
    }

    // Hilfsmethode zum Prüfen des Initialisierungsstatus
    fun isBasicInitialized() = _isBasicInitialized.value

    // Sichere Zugriffsmethoden mit Initialisierungsprüfung
    fun getPrefsManagerSafe(): SharedPreferencesManager? {
        return if (::prefsManager.isInitialized) prefsManager else null
    }

    fun getPermissionHandlerSafe(): PermissionHandler? {
        return if (::permissionHandler.isInitialized) permissionHandler else null
    }

    // Blocking-Zugriff für kritische Operationen (wirft Exception wenn nicht initialisiert)
    fun requirePrefsManager(): SharedPreferencesManager {
        if (!::prefsManager.isInitialized) {
            throw IllegalStateException("PrefsManager not initialized. Call initializeCritical() first.")
        }
        return prefsManager
    }

    fun requirePermissionHandler(): PermissionHandler {
        if (!::permissionHandler.isInitialized) {
            throw IllegalStateException("PermissionHandler not initialized. Call initializeWithActivity() first.")
        }
        return permissionHandler
    }

    fun getApplication(): SmsForwarderApplication = application
}

class SmsForwarderApplication : Application() {
    private val applicationScope = CoroutineScope(Job() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        // Initialisiere LoggingManager ZUERST, bevor andere Komponenten loggen wollen
        initializeBaseComponents()
        // Dann kritische Komponenten (die nun loggen können)
        initializeCriticalComponents()
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Existenzprüfung vor try-catch Block
            val existingChannel = notificationManager?.getNotificationChannel("MY_CHANNEL_ID")
            if (existingChannel != null) {
                return
            }

            try {
                val channel = NotificationChannel(
                    "MY_CHANNEL_ID",
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = getString(R.string.notification_channel_description)
                    setShowBadge(true)
                    enableLights(false)
                    enableVibration(false)
                    vibrationPattern = null
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }

                notificationManager?.createNotificationChannel(channel)

            } catch (e: Exception) {
                // Use Log.e directly since LoggingManager is not yet initialized
                Log.e("SmsForwarderApplication", "Failed to create notification channel", e)
            }
        }
    }

    private fun initializeCriticalComponents() {
        try {
            AppContainer.initializeCritical(this)
        } catch (e: Exception) {
            Log.e("SmsForwarderApplication", "Critical initialization failed", e)
            throw e
        }
    }

    private fun initializeBaseComponents() {
        try {
            // Initialize LoggingManager with Application context
            LoggingManager.initialize(this)  // 'this' ist der Application context

            // Add initial log entry after proper initialization
            LoggingManager.logInfo(
                component = "Application",
                action = "INIT",
                message = "Base components initialized successfully"
            )
        } catch (e: Exception) {
            Log.e("SmsForwarderApplication", "Base initialization failed", e)
            throw e
        }
    }


    override fun onTerminate() {
        super.onTerminate()
        applicationScope.cancel()
        LoggingManager.logInfo(
            component = "Application",
            action = "TERMINATE",
            message = "Application is terminating"
        )
    }

    override fun onLowMemory() {
        super.onLowMemory()
        LoggingManager.logWarning(
            component = "Application",
            action = "LOW_MEMORY",
            message = "System reports low memory",
            details = mapOf(
                "free_memory" to Runtime.getRuntime().freeMemory(),
                "total_memory" to Runtime.getRuntime().totalMemory()
            )
        )
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Moderne Alternative zu TRIM_MEMORY_MODERATE
        @Suppress("DEPRECATION")
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE) {
            LoggingManager.logWarning(
                component = "Application",
                action = "TRIM_MEMORY",
                message = "System requests memory trim",
                details = mapOf("level" to level)
            )
        }
    }

    companion object {
        @Volatile
        private var instance: SmsForwarderApplication? = null

    }
}

object LoggingManager {
    private lateinit var fileTree: FileLoggingTree
    private val initialized = AtomicBoolean(false)

    /**
     * Initialize Timber logging with FileLoggingTree.
     * Called once in SmsForwarderApplication.onCreate().
     */
    fun initialize(context: Context) {
        val prefs = try {
            SharedPreferencesManager(context)
        } catch (e: Exception) {
            Log.w("LoggingManager", "Could not read preferences, using default log size", e)
            null
        }
        val maxLogSizeMB = prefs?.getMaxLogSizeMB() ?: 5

        // Initialize FileLoggingTree
        fileTree = FileLoggingTree(context, maxLogSizeMB)

        if (BuildConfig.DEBUG) {
            // Debug: Logcat + File
            Timber.plant(Timber.DebugTree())
            Timber.plant(fileTree)
        } else {
            // Release: File only (logcat stripped by ProGuard)
            Timber.plant(fileTree)
        }

        initialized.set(true)

        // Delete old XML logs from previous system
        deleteOldXmlLogs(context)

        // Log initialization
        fileTree.setMetadata(
            component = "LoggingManager",
            action = "INIT",
            details = mapOf(
                "max_log_size_mb" to maxLogSizeMB,
                "debug_mode" to BuildConfig.DEBUG
            )
        )
        Timber.tag("LoggingManager").i("Logging system initialized with Timber")
    }

    /**
     * Get FileLoggingTree instance for reading logs.
     */
    fun getFileTree(): FileLoggingTree {
        require(initialized.get()) { "LoggingManager not initialized" }
        return fileTree
    }

    /**
     * Delete old XML logs from previous logging system.
     * Called once during migration.
     */
    private fun deleteOldXmlLogs(context: Context) {
        try {
            val baseLogDir = context.getExternalFilesDir("logs")
                ?: context.getExternalFilesDir(null)
                ?: File(context.filesDir, "logs")

            val oldFiles = listOf("app_log.xml", "app_log_backup.xml")
            var deletedCount = 0

            oldFiles.forEach { filename ->
                val file = File(baseLogDir, filename)
                if (file.exists()) {
                    file.delete()
                    deletedCount++
                    Log.i("LoggingManager", "Deleted old XML log: $filename")
                }
            }

            if (deletedCount > 0) {
                Timber.tag("LoggingManager").i("Migration: Deleted $deletedCount old XML log file(s)")
            }
        } catch (e: Exception) {
            Log.e("LoggingManager", "Failed to delete old XML logs", e)
        }
    }

    // ========== BACKWARD-COMPATIBLE API ==========
    // All 253 existing log calls continue to work unchanged!

    fun logInfo(
        component: String,
        action: String,
        message: String,
        details: Map<String, Any?> = emptyMap()
    ) {
        if (!initialized.get()) {
            Log.e("LoggingManager", "Logging before initialization: $message")
            return
        }

        fileTree.setMetadata(component, action, details)
        Timber.tag(component).i(message)
    }

    fun logWarning(
        component: String,
        action: String,
        message: String,
        details: Map<String, Any?> = emptyMap()
    ) {
        if (!initialized.get()) return

        fileTree.setMetadata(component, action, details)
        Timber.tag(component).w(message)
    }

    fun logError(
        component: String,
        action: String,
        message: String,
        error: Throwable? = null,
        details: Map<String, Any?> = emptyMap()
    ) {
        if (!initialized.get()) return

        fileTree.setMetadata(component, action, details)
        if (error != null) {
            Timber.tag(component).e(error, message)
        } else {
            Timber.tag(component).e(message)
        }
    }

    fun logDebug(
        component: String,
        action: String,
        message: String,
        details: Map<String, Any?> = emptyMap()
    ) {
        if (!initialized.get()) return

        fileTree.setMetadata(component, action, details)
        Timber.tag(component).d(message)
    }
}

object SnackbarManager {

        private var snackbarHostState: SnackbarHostState? = null
        private var coroutineScope: CoroutineScope? = null
        private val pendingMessages = mutableListOf<SnackbarConfig>()
        private val lock = Any()

        enum class SnackbarType {
            INFO,
            SUCCESS,
            WARNING,
            ERROR
        }

        enum class Duration(val composeDuration: SnackbarDuration) {
            SHORT(SnackbarDuration.Short),
            LONG(SnackbarDuration.Long),
        }

        data class SnackbarConfig(
            val message: String,
            val type: SnackbarType = SnackbarType.INFO,
            val duration: Duration = Duration.SHORT,
            val actionText: String? = null,
            val action: (() -> Unit)? = null
        )

        fun setSnackbarState(hostState: SnackbarHostState, scope: CoroutineScope) {
            synchronized(lock) {
                snackbarHostState = hostState
                coroutineScope = scope

                // Zeige ausstehende Nachrichten
                if (pendingMessages.isNotEmpty()) {
                    scope.launch {
                        pendingMessages.toList().forEach { config ->
                            showMessage(config)
                            pendingMessages.remove(config)
                        }
                    }
                }
            }
        }

        private fun showMessage(config: SnackbarConfig) {
            synchronized(lock) {
                val hostState = snackbarHostState
                val scope = coroutineScope

                if (hostState == null || scope == null) {

                    pendingMessages.add(config)
                    return
                }

                scope.launch {
                    try {
                        val result = hostState.showSnackbar(
                            message = config.message,
                            actionLabel = config.actionText,
                            duration = config.duration.composeDuration,
                            withDismissAction = true
                        )

                        when (result) {
                            SnackbarResult.ActionPerformed -> {
                                config.action?.invoke()
                            }
                            SnackbarResult.Dismissed -> {
                                // Snackbar wurde geschlossen
                            }
                        }
                    } catch (e: Exception) {
                        LoggingManager.logError(
                            component = "SnackbarManager",
                            action = "SHOW_MESSAGE_ERROR",
                            message = "Fehler beim Anzeigen der Snackbar",
                            error = e
                        )
                    }
                }
            }
        }

    // Helper methods
    fun showInfo(
        message: String,
        duration: Duration = Duration.SHORT,
        actionText: String? = null,
        action: (() -> Unit)? = null
    ) {
        showMessage(
            SnackbarConfig(
                message = message,
                type = SnackbarType.INFO,
                duration = duration,
                actionText = actionText,
                action = action
            )
        )
    }

    fun showSuccess(
        message: String,
        duration: Duration = Duration.SHORT,
        actionText: String? = null,
        action: (() -> Unit)? = null
    ) {
        showMessage(
            SnackbarConfig(
                message = message,
                type = SnackbarType.SUCCESS,
                duration = duration,
                actionText = actionText,
                action = action
            )
        )
    }

    fun showWarning(
        message: String,
        duration: Duration = Duration.LONG,
        actionText: String? = null,
        action: (() -> Unit)? = null
    ) {
        showMessage(
            SnackbarConfig(
                message = message,
                type = SnackbarType.WARNING,
                duration = duration,
                actionText = actionText,
                action = action
            )
        )
    }

    fun showError(
        message: String,
        duration: Duration = Duration.LONG,
        actionText: String? = null,
        action: (() -> Unit)? = null
    ) {
        showMessage(
            SnackbarConfig(
                message = message,
                type = SnackbarType.ERROR,
                duration = duration,
                actionText = actionText,
                action = action
            )
        )
    }
}

