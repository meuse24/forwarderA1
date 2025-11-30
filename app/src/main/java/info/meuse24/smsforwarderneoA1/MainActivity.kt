package info.meuse24.smsforwarderneoA1

// Removed unsafe direct import - use AppContainer.requirePrefsManager() instead
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import info.meuse24.smsforwarderneoA1.AppContainer.prefsManager
import info.meuse24.smsforwarderneoA1.data.local.PermissionHandler
import info.meuse24.smsforwarderneoA1.domain.model.SimInfo
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.CleanupErrorDialog
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.CleanupProgressDialog
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.ExitDialog
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.LoadingScreen
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.SimNumbersDialog
import info.meuse24.smsforwarderneoA1.presentation.ui.components.navigation.BottomNavigationBar
import info.meuse24.smsforwarderneoA1.presentation.ui.components.navigation.CustomTopAppBar
import info.meuse24.smsforwarderneoA1.presentation.ui.screens.help.HelpScreen
import info.meuse24.smsforwarderneoA1.presentation.ui.screens.home.HomeScreen
import info.meuse24.smsforwarderneoA1.presentation.ui.screens.info.InfoScreen
import info.meuse24.smsforwarderneoA1.presentation.ui.screens.logs.LogScreen
import info.meuse24.smsforwarderneoA1.presentation.ui.screens.mail.MailScreen
import info.meuse24.smsforwarderneoA1.presentation.ui.screens.settings.SettingsScreen
import info.meuse24.smsforwarderneoA1.presentation.viewmodel.EmailViewModel
import info.meuse24.smsforwarderneoA1.presentation.viewmodel.LogViewModel
import info.meuse24.smsforwarderneoA1.presentation.viewmodel.NavigationViewModel
import info.meuse24.smsforwarderneoA1.presentation.viewmodel.SimManagementViewModel
import info.meuse24.smsforwarderneoA1.presentation.viewmodel.TestUtilsViewModel
import info.meuse24.smsforwarderneoA1.service.SmsForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val viewModel: ContactsViewModel by viewModels { ContactsViewModel.Factory() }
    private val logViewModel: LogViewModel by viewModels {
        LogViewModel.Factory(AppContainer.requireLogger())
    }
    private val emailViewModel: EmailViewModel by viewModels {
        EmailViewModel.Factory(
            AppContainer.requirePrefsManager(),
            AppContainer.requireLogger()
        )
    }
    private val simManagementViewModel: SimManagementViewModel by viewModels {
        SimManagementViewModel.Factory(AppContainer.requirePrefsManager())
    }
    private val navigationViewModel: NavigationViewModel by viewModels {
        NavigationViewModel.Factory(AppContainer.requirePrefsManager())
    }
    private val testUtilsViewModel: TestUtilsViewModel by viewModels {
        TestUtilsViewModel.Factory(application, AppContainer.requirePrefsManager())
    }
    private val _isLoading = MutableStateFlow(true)
    private val _loadingError = MutableStateFlow<String?>(null)
    private lateinit var permissionHandler: PermissionHandler

    // Contact Picker Launcher
    private val contactPickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        uri?.let {
            lifecycleScope.launch {
                viewModel.handleContactPickerResult(it)
            }
        }
    }

    // Call state management for MMI codes
    private val _callState = MutableStateFlow(TelephonyManager.CALL_STATE_IDLE)
    val callState = _callState
    private var telephonyManager: TelephonyManager? = null
    private var telephonyCallback: TelephonyCallback? = null
    @Suppress("DEPRECATION")
    private var phoneStateListener: PhoneStateListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the MMI code dial callback in ViewModel
        viewModel.onDialMmiCode = { code -> dialCode(code) }

        // Set the contact picker launcher callback
        viewModel.onLaunchContactPicker = { contactPickerLauncher.launch(null) }

        // Set EmailViewModel callback to update service notification when forwarding state changes
        emailViewModel.onForwardingStateChanged = {
            viewModel.updateServiceNotification()
        }

        // Set ContactsViewModel callback to forward errors to NavigationViewModel
        viewModel.onErrorOccurred = { errorState ->
            navigationViewModel.setErrorState(errorState)
        }

        // Normale Statusleiste - kein Edge-to-Edge
        WindowCompat.setDecorFitsSystemWindows(window, true)

        // Status Bar Icons auf dunkel setzen (für besseren Kontrast auf hellem Hintergrund)
        WindowCompat.getInsetsController(window, window.decorView)?.apply {
            isAppearanceLightStatusBars = true
        }

        onBackPressedDispatcher.addCallback(this) {
            // Prüfe ob irgendeine Art der Weiterleitung aktiv ist
            val emailForwardingActive = AppContainer.requirePrefsManager().isForwardSmsToEmail()
            if (viewModel.forwardingActive.value || emailForwardingActive) {
                // Zeige Exit-Dialog mit Optionen zum Deaktivieren/Beibehalten
                navigationViewModel.onShowExitDialog()
            } else {
                // Wenn keine Weiterleitung aktiv ist, beende direkt
                finish()
            }
        }

        // Initialisiere PermissionHandler direkt
        permissionHandler = PermissionHandler(this)

        // Setze UI Content SOFORT mit Loading-State
        setContent {
            MaterialTheme {
                val isLoading by _isLoading.collectAsState()
                val error by _loadingError.collectAsState()
                val isFullyInitialized by AppContainer.isInitialized.collectAsState()

                when {
                    !isFullyInitialized || isLoading -> {
                        LoadingScreen(
                            error = error,
                            onRetry = { retryInitialization() },
                            onExit = { finish() }
                        )
                    }
                    else -> {
                        // Zusätzliche Sicherheitsprüfung vor UI-Erstellung
                        val prefsAvailable = AppContainer.getPrefsManagerSafe() != null
                        val loggerAvailable = AppContainer.getLoggerSafe() != null
                        val permissionAvailable = AppContainer.getPermissionHandlerSafe() != null

                        if (prefsAvailable && loggerAvailable && permissionAvailable) {
                            UI(viewModel, emailViewModel)
                        } else {
                            LoadingScreen(
                                error = "Initialisierung unvollständig - Komponenten nicht verfügbar",
                                onRetry = { retryInitialization() },
                                onExit = { finish() }
                            )
                        }
                    }
                }
            }
        }

        // Führe Initialisierung in separater Coroutine durch
        lifecycleScope.launch {
            try {
                // Warte auf Basis-Initialisierung
                AppContainer.isBasicInitialized.first { it }

                // Führe Activity-Initialisierung durch und prüfe Erfolg
                AppContainer.initializeWithActivity(this@MainActivity)

                // Verifiziere dass Initialisierung erfolgreich war
                if (!AppContainer.isInitialized.value) {
                    throw IllegalStateException("Activity initialization completed but isInitialized is still false")
                }

                // Starte vollständige App-Initialisierung
                initializeApp()

            } catch (e: Exception) {
                _loadingError.value = "Initialisierungsfehler: ${e.message}"
                Log.e("MainActivity", "Error during initialization", e)

                // Ensure loading state shows error
                _isLoading.value = false
            }
        }
    }

    private fun initializeApp() {
        lifecycleScope.launch {
            try {
                _loadingError.value = "Initialisiere App..."

                // Warte auf vollständige AppContainer Initialisierung
                AppContainer.isInitialized.first { it }

                // Prüfe und fordere Berechtigungen an
                permissionHandler.checkPermissions(
                    onGranted = {
                        // Starte Services und weitere Initialisierungen
                        SmsForegroundService.startService(this@MainActivity)

                        // Setup phone state listener for MMI code monitoring
                        setupPhoneStateListener()

                        // Prüfe Battery Optimization Status
                        checkBatteryOptimization()

                        // Füge kleine Verzögerung hinzu um sicherzustellen, dass
                        // Berechtigungen vollständig gewährt wurden
                        lifecycleScope.launch {
                            delay(500) // 500ms Verzögerung

                            // Prüfe und erfasse SIM-Telefonnummern
                            checkAndRequestSimPhoneNumbers()

                            viewModel.initialize() // Dies lädt nun die Kontakte
                        }

                        // Verstecke LoadingScreen
                        _isLoading.value = false
                    },
                    onDenied = {
                        _loadingError.value = "Erforderliche Berechtigungen wurden nicht erteilt"
                        LoggingManager.logWarning(
                            component = "MainActivity",
                            action = "PERMISSIONS_DENIED",
                            message = "Berechtigungen verweigert"
                        )
                    }
                )

            } catch (e: Exception) {
                _loadingError.value = "Fehler bei der Initialisierung: ${e.message}"
                LoggingManager.logError(
                    component = "MainActivity",
                    action = "INIT_ERROR",
                    message = "App-Initialisierung fehlgeschlagen",
                    error = e
                )
            }
        }
    }

    /**
     * Prüft ob Battery Optimization für die App deaktiviert ist
     * und zeigt bei Bedarf einen Dialog an
     */
    private fun checkBatteryOptimization() {
        try {
            val powerManager = getSystemService(POWER_SERVICE) as? PowerManager
            if (powerManager == null) {
                LoggingManager.logWarning(
                    component = "MainActivity",
                    action = "CHECK_BATTERY_OPT",
                    message = "PowerManager nicht verfügbar"
                )
                return
            }

            val isIgnoring = powerManager.isIgnoringBatteryOptimizations(packageName)

            LoggingManager.logInfo(
                component = "MainActivity",
                action = "CHECK_BATTERY_OPT",
                message = "Battery Optimization Status geprüft",
                details = mapOf("isIgnoring" to isIgnoring)
            )

            if (!isIgnoring) {
                showBatteryOptimizationDialog()
            }
        } catch (e: Exception) {
            LoggingManager.logError(
                component = "MainActivity",
                action = "CHECK_BATTERY_OPT",
                message = "Fehler beim Prüfen der Battery Optimization",
                error = e
            )
        }
    }

    /**
     * Zeigt einen Dialog zum Deaktivieren der Battery Optimization
     */
    private fun showBatteryOptimizationDialog() {
        lifecycleScope.launch(Dispatchers.Main) {
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setTitle("Akku-Optimierung deaktivieren")
            builder.setMessage(
                "Für eine zuverlässige SMS-Weiterleitung im Hintergrund sollte die Akku-Optimierung " +
                "für diese App deaktiviert werden.\n\n" +
                "Ohne diese Einstellung kann es vorkommen, dass SMS-Weiterleitungen verzögert werden " +
                "oder nicht funktionieren."
            )
            builder.setPositiveButton("Einstellungen öffnen") { dialog, _ ->
                requestBatteryOptimizationExemption()
                dialog.dismiss()
            }
            builder.setNegativeButton("Später") { dialog, _ ->
                LoggingManager.logInfo(
                    component = "MainActivity",
                    action = "BATTERY_OPT_DIALOG",
                    message = "Nutzer hat Battery Optimization Dialog abgelehnt"
                )
                dialog.dismiss()
            }
            builder.setCancelable(true)
            builder.show()
        }
    }

    /**
     * Öffnet die System-Einstellungen zur Deaktivierung der Battery Optimization
     */
    private fun requestBatteryOptimizationExemption() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = "package:$packageName".toUri()
            }
            startActivity(intent)

            LoggingManager.logInfo(
                component = "MainActivity",
                action = "REQUEST_BATTERY_OPT_EXEMPTION",
                message = "Battery Optimization Einstellungen geöffnet"
            )
        } catch (e: Exception) {
            LoggingManager.logError(
                component = "MainActivity",
                action = "REQUEST_BATTERY_OPT_EXEMPTION",
                message = "Fehler beim Öffnen der Battery Optimization Einstellungen",
                error = e
            )
            SnackbarManager.showError("Einstellungen konnten nicht geöffnet werden")
        }
    }

    /**
     * Prüft SIM-Telefonnummern und fordert fehlende vom User ab
     */
    private suspend fun checkAndRequestSimPhoneNumbers() {
        try {
            val simInfoList = PhoneSmsUtils.getAllSimInfo(this)

            if (simInfoList.isEmpty()) {
                LoggingManager.logWarning(
                    component = "MainActivity",
                    action = "CHECK_SIM_NUMBERS",
                    message = "Keine SIM-Karten gefunden"
                )
                return
            }

            val prefsManager = AppContainer.getPrefsManagerSafe()
            if (prefsManager == null) {
                LoggingManager.logError(
                    component = "MainActivity",
                    action = "CHECK_SIM_NUMBERS",
                    message = "PreferencesManager nicht verfügbar"
                )
                return
            }

            val storedNumbers = prefsManager.getSimPhoneNumbers()
            val missingSims = mutableListOf<SimInfo>()

            // Prüfe jede SIM auf fehlende Telefonnummern
            simInfoList.forEach { simInfo ->
                val stored = storedNumbers[simInfo.subscriptionId]
                if (stored.isNullOrEmpty() && simInfo.phoneNumber.isNullOrEmpty()) {
                    missingSims.add(simInfo)
                } else if (!simInfo.phoneNumber.isNullOrEmpty() && stored != simInfo.phoneNumber) {
                    // Auto-erkannte Nummer in Preferences speichern
                    prefsManager.setSimPhoneNumber(simInfo.subscriptionId, simInfo.phoneNumber)
                    LoggingManager.logInfo(
                        component = "MainActivity",
                        action = "STORE_SIM_NUMBER",
                        message = "Auto-erkannte SIM-Nummer gespeichert",
                        details = mapOf(
                            "subscription_id" to simInfo.subscriptionId,
                            "slot" to simInfo.slotIndex,
                            "carrier" to (simInfo.carrierName ?: "Unknown")
                        )
                    )
                }
            }

            // Falls SIM-Nummern fehlen, zeige Dialog
            if (missingSims.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    simManagementViewModel.requestMissingSimNumbers(missingSims)
                }
            }

            LoggingManager.logInfo(
                component = "MainActivity",
                action = "CHECK_SIM_NUMBERS",
                message = "SIM-Nummern-Prüfung abgeschlossen",
                details = mapOf(
                    "total_sims" to simInfoList.size,
                    "missing_numbers" to missingSims.size,
                    "auto_detected" to simInfoList.count { !it.phoneNumber.isNullOrEmpty() }
                )
            )


        } catch (e: Exception) {
            LoggingManager.logError(
                component = "MainActivity",
                action = "CHECK_SIM_NUMBERS",
                message = "Fehler bei SIM-Nummern-Prüfung",
                error = e
            )
        }
    }

    // LoadingScreen moved to presentation.ui.components.dialogs.LoadingScreen

    private fun retryInitialization() {
        // Reset error state
        _loadingError.value = null
        _isLoading.value = true

        // Restart initialization process
        lifecycleScope.launch {
            try {
                // Reset AppContainer initialization state if needed
                // (AppContainer keeps its initialization state, so we don't reset it)

                // Warte auf Basis-Initialisierung
                AppContainer.isBasicInitialized.first { it }

                // Führe Activity-Initialisierung durch und prüfe Erfolg
                AppContainer.initializeWithActivity(this@MainActivity)

                // Verifiziere dass Initialisierung erfolgreich war
                if (!AppContainer.isInitialized.value) {
                    throw IllegalStateException("Activity initialization completed but isInitialized is still false")
                }

                // Starte vollständige App-Initialisierung
                initializeApp()

            } catch (e: Exception) {
                _loadingError.value = "Initialisierungsfehler: ${e.message}"
                Log.e("MainActivity", "Error during retry initialization", e)

                // Ensure loading state shows error
                _isLoading.value = false
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        try {
            LoggingManager.logInfo(
                component = "MainActivity",
                action = "CONFIG_CHANGED",
                message = "Bildschirmausrichtung wurde geändert",
                details = mapOf(
                    "orientation" to if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) "landscape" else "portrait",
                    "screenWidthDp" to resources.configuration.screenWidthDp,
                    "screenHeightDp" to resources.configuration.screenHeightDp
                )
            )
            viewModel.saveCurrentState()
            //viewModel.loadSavedState()
        } catch (_: UninitializedPropertyAccessException) {
            // ViewModel noch nicht initialisiert - ignorieren
            LoggingManager.logInfo(
                component = "MainActivity",
                action = "CONFIG_CHANGED_SKIP",
                message = "Konfigurationsänderung übersprungen - ViewModel noch nicht initialisiert"
            )
        }
    }

    /**
     * Setup phone state listener to monitor call states for MMI code execution
     */
    private fun setupPhoneStateListener() {
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as? TelephonyManager

        // Prüfe Berechtigung UND TelephonyManager verfügbar
        if (telephonyManager == null) {
            LoggingManager.logWarning(
                component = "MainActivity",
                action = "PHONE_STATE_LISTENER",
                message = "TelephonyManager nicht verfügbar"
            )
            return
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            LoggingManager.logWarning(
                component = "MainActivity",
                action = "PHONE_STATE_LISTENER",
                message = "READ_PHONE_STATE Berechtigung fehlt"
            )
            return
        }

        // Use TelephonyCallback for API 31+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    _callState.value = state
                    LoggingManager.logInfo(
                        component = "MainActivity",
                        action = "CALL_STATE_CHANGED",
                        message = "Telefonstatus geändert",
                        details = mapOf(
                            "state" to when (state) {
                                TelephonyManager.CALL_STATE_IDLE -> "IDLE"
                                TelephonyManager.CALL_STATE_OFFHOOK -> "OFFHOOK"
                                TelephonyManager.CALL_STATE_RINGING -> "RINGING"
                                else -> "UNKNOWN"
                            }
                        )
                    )
                }
            }
            // Safe call ohne force-unwrap
            telephonyCallback?.let { callback ->
                telephonyManager?.registerTelephonyCallback(mainExecutor, callback)
            }
        } else {
            // For older versions use PhoneStateListener
            @Suppress("DEPRECATION")
            phoneStateListener = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    _callState.value = state
                    LoggingManager.logInfo(
                        component = "MainActivity",
                        action = "CALL_STATE_CHANGED",
                        message = "Telefonstatus geändert",
                        details = mapOf(
                            "state" to when (state) {
                                TelephonyManager.CALL_STATE_IDLE -> "IDLE"
                                TelephonyManager.CALL_STATE_OFFHOOK -> "OFFHOOK"
                                TelephonyManager.CALL_STATE_RINGING -> "RINGING"
                                else -> "UNKNOWN"
                            }
                        )
                    )
                }
            }
            // Safe call ohne force-unwrap
            @Suppress("DEPRECATION")
            phoneStateListener?.let { listener ->
                telephonyManager?.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
            }
        }
    }

    /**
     * Dial MMI code with speakerphone enabled and audio focus management.
     * Automatically waits if another call is active.
     */
    fun dialCode(code: String) {
        if (code.isBlank()) {
            SnackbarManager.showWarning("MMI-Code darf nicht leer sein")
            return
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            SnackbarManager.showError("Berechtigung für Telefon-Anrufe nicht erteilt")
            return
        }

        // Internationales Anschaltzeichen "+" durch konfigurierte Anschaltziffernfolge ersetzen
        val dialPrefix = prefsManager.getInternationalDialPrefix()
        val normalizedCode = if (code.contains("+")) {
            code.replace("+", dialPrefix)
        } else {
            code
        }

        // Launch coroutine to wait if call is active
        lifecycleScope.launch {
            val currentCallState = callState.value

            // Wait if call is active (not IDLE)
            if (currentCallState != TelephonyManager.CALL_STATE_IDLE) {
                LoggingManager.logInfo(
                    component = "MainActivity",
                    action = "DIAL_MMI_WAITING",
                    message = "Warte bis aktueller Anruf beendet ist",
                    details = mapOf("code" to normalizedCode, "callState" to currentCallState)
                )
                SnackbarManager.showInfo("Warte bis Anruf beendet ist...")

                // Wait until call is idle
                callState.first { it == TelephonyManager.CALL_STATE_IDLE }

                // Add buffer after call ends
                delay(500)

                LoggingManager.logInfo(
                    component = "MainActivity",
                    action = "DIAL_MMI_READY",
                    message = "Anruf beendet, wähle MMI-Code",
                    details = mapOf("code" to normalizedCode)
                )
            }

            // Zeige Hinweis für 4 Sekunden vor dem Wählen
            SnackbarManager.showInfo(
                message = """
                ⏳ Wählvorgang wird gestartet...

                    ═════════════
                  ⚠️  BITTE WARTEN  ⚠️
                     NICHT BEDIENEN!
                    ═════════════

                ► Den Wählvorgang abwarten
                ► Nichts antippen
                ► App kehrt automatisch zurück
                """.trimIndent(),
                duration = SnackbarManager.Duration.LONG
            )

            LoggingManager.logInfo(
                component = "MainActivity",
                action = "DIAL_MMI_PREPARING",
                message = "Zeige Benutzer-Hinweis vor Wählvorgang",
                details = mapOf("code" to normalizedCode, "delay_ms" to 4000)
            )

            // Warte 4 Sekunden, damit Benutzer die Nachricht lesen kann
            delay(4000)

            // Proceed with dialing
            dialCodeNow(normalizedCode, code)
        }
    }

    /**
     * Internal function to actually dial the MMI code (called after waiting if needed)
     */
    private fun dialCodeNow(normalizedCode: String, originalCode: String) {
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = "tel:${Uri.encode(normalizedCode)}".toUri()
                // Set speakerphone as default for this call
                putExtra("android.telecom.extra.START_CALL_WITH_SPEAKERPHONE", true)
            }

            // Request audio focus for voice feedback
            val audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(audioAttributes)
                    .setOnAudioFocusChangeListener { focusChange ->
                        when (focusChange) {
                            AudioManager.AUDIOFOCUS_GAIN -> {
                                LoggingManager.logInfo(
                                    component = "MainActivity",
                                    action = "AUDIO_FOCUS",
                                    message = "Audio-Fokus erhalten"
                                )
                            }
                            AudioManager.AUDIOFOCUS_LOSS,
                            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                                LoggingManager.logInfo(
                                    component = "MainActivity",
                                    action = "AUDIO_FOCUS",
                                    message = "Audio-Fokus verloren"
                                )
                            }
                        }
                    }
                    .build()

                // Prüfe Audio-Fokus Result
                val focusResult = audioManager.requestAudioFocus(focusRequest)
                when (focusResult) {
                    AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                        LoggingManager.logInfo(
                            component = "MainActivity",
                            action = "AUDIO_FOCUS_GRANTED",
                            message = "Audio-Fokus erfolgreich erhalten"
                        )
                    }
                    AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                        LoggingManager.logWarning(
                            component = "MainActivity",
                            action = "AUDIO_FOCUS_FAILED",
                            message = "Audio-Fokus konnte nicht erhalten werden - kein Audio-Feedback"
                        )
                        // Hinweis: MMI-Code wird trotzdem gewählt, aber ohne Audio-Feedback
                    }
                    AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                        LoggingManager.logInfo(
                            component = "MainActivity",
                            action = "AUDIO_FOCUS_DELAYED",
                            message = "Audio-Fokus verzögert - möglicherweise kein sofortiges Audio-Feedback"
                        )
                    }
                }
            } else {
                LoggingManager.logWarning(
                    component = "MainActivity",
                    action = "AUDIO_MANAGER_NULL",
                    message = "AudioManager nicht verfügbar - kein Audio-Feedback möglich"
                )
            }

            startActivity(intent)

            LoggingManager.logInfo(
                component = "MainActivity",
                action = "DIAL_MMI_CODE",
                message = "MMI-Code gewählt mit Lautsprecher",
                details = mapOf(
                    "original_code" to originalCode,
                    "normalized_code" to normalizedCode,
                    "speakerphone" to true,
                    "plus_replaced" to (originalCode != normalizedCode)
                )
            )

        } catch (e: Exception) {
            LoggingManager.logError(
                component = "MainActivity",
                action = "DIAL_MMI_CODE",
                message = "Fehler beim Wählen des MMI-Codes",
                error = e,
                details = mapOf("code" to originalCode)
            )
            SnackbarManager.showError("Fehler beim Wählen: ${e.message}")
        }
    }

    override fun onDestroy() {
        //viewModel.deactivateForwarding()
        //viewModel.saveCurrentState() // Neue Methode, die wir im ViewModel hinzufügen werden

        // Unregister telephony callbacks
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let {
                telephonyManager?.unregisterTelephonyCallback(it)
            }
        } else {
            // Unregister PhoneStateListener für ältere Versionen
            @Suppress("DEPRECATION")
            phoneStateListener?.let { listener ->
                telephonyManager?.listen(listener, PhoneStateListener.LISTEN_NONE)
            }
        }

        LoggingManager.logInfo(
            component = "MainActivity",
            action = "DESTROY",
            message = "App wird beendet",
            details = mapOf(
                "forwardingActive" to viewModel.forwardingActive.value,
                "timestamp" to System.currentTimeMillis()
            )
        )
        if (!AppContainer.requirePrefsManager().getKeepForwardingOnExit()) {
            SmsForegroundService.stopService(this)
        }

        super.onDestroy()
    }

    @Composable
    fun UI(viewModel: ContactsViewModel, emailViewModel: EmailViewModel) {
        val navController = rememberNavController()
        //val topBarTitle by navigationViewModel.topBarTitle.collectAsState()
        val navigationTarget by navigationViewModel.navigationTarget.collectAsState()
        val showExitDialog by navigationViewModel.showExitDialog.collectAsState()
        val showProgressDialog by viewModel.showProgressDialog.collectAsState()
        val errorState by navigationViewModel.errorState.collectAsState()
        // showOwnNumberMissingDialog StateFlow entfernt
        val showSimNumbersDialog by simManagementViewModel.showSimNumbersDialog.collectAsState()
        val missingSims by simManagementViewModel.missingSims.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()

        // Cleanup Effect
        LaunchedEffect(Unit) {
            viewModel.cleanupCompleted.collect {
                finish()
            }
        }

        // Initialisieren Sie den SnackbarManager mit dem State und Scope
        LaunchedEffect(snackbarHostState, coroutineScope) {
            SnackbarManager.setSnackbarState(snackbarHostState, coroutineScope)
        }

        // Navigation Effect
        LaunchedEffect(navigationTarget) {
            navigationTarget?.let { target ->
                navController.navigate(target) {
                    launchSingleTop = true
                    restoreState = true
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                }
                navigationViewModel.onNavigated()
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {  // Äußere Box für absolutes Positioning
            Scaffold(
                topBar = { CustomTopAppBar(title = "") },
                bottomBar = { BottomNavigationBar(navController, viewModel) }
            ) { innerPadding ->
                NavHost(
                    navController,
                    startDestination = "start",
                    Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                ) {
                    composable("start") {
                        val currentCallState = callState.collectAsState()
                        HomeScreen(
                            viewModel = viewModel,
                            emailViewModel = emailViewModel,
                            testUtilsViewModel = testUtilsViewModel,
                            callState = currentCallState,
                            onNavigateToHelp = { navController.navigate("help") }
                        )
                    }
                    composable("mail") {
                        MailScreen(emailViewModel)
                    }
                    composable("setup") {
                        SettingsScreen(viewModel, emailViewModel, testUtilsViewModel, navigationViewModel)
                    }
                    composable("log") {
                        LogScreen(logViewModel)
                    }
                    composable("info") {
                        InfoScreen()
                    }
                    composable("help") {
                        HelpScreen(
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }

            // Snackbar außerhalb des Scaffolds aber innerhalb der Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)  // Ausrichtung oben
                    .padding(top = 40.dp)  // Abstand zur TopBar
                    .offset(y = 8.dp)  // Feinjustierung
            ) {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            // OwnNumberMissingDialog entfernt - wird über SIM-Verwaltung abgewickelt

            // SIM-Nummern Dialog
            if (showSimNumbersDialog) {
                SimNumbersDialog(
                    missingSims = missingSims,
                    onDismiss = { simManagementViewModel.hideSimNumbersDialog() },
                    onSaveNumber = { subscriptionId, phoneNumber ->
                        simManagementViewModel.saveSimNumber(subscriptionId, phoneNumber)
                    }
                )
            }

            // Exit Dialog
            if (showExitDialog) {
                val selectedContact by viewModel.selectedContact.collectAsState() // Hier collectAsState verwenden
                ExitDialog(
                    contact = selectedContact,
                    initialKeepForwarding = AppContainer.requirePrefsManager().getKeepForwardingOnExit(),
                    onDismiss = { navigationViewModel.hideExitDialog() },
                    onConfirm = { keepForwarding ->
                        navigationViewModel.hideExitDialog()
                        viewModel.startCleanup(keepForwarding)
                    },
                    onSettings = {
                        navigationViewModel.hideExitDialog()
                        navigationViewModel.navigateToSettings()
                    },
                    updateKeepForwardingOnExit = { keepForwarding ->
                        viewModel.updateKeepForwardingOnExit(keepForwarding)
                    }
                )
            }

            // Progress Dialog
            if (showProgressDialog) {
                CleanupProgressDialog()
            }

            // Error Dialog
            errorState?.let { error ->
                CleanupErrorDialog(
                    error = error,
                    onRetry = {
                        navigationViewModel.clearErrorState()
                        viewModel.startCleanup(false)
                    },
                    onIgnore = {
                        navigationViewModel.clearErrorState()
                        finish()
                    },
                    onDismiss = {
                        navigationViewModel.clearErrorState()
                    }
                )
            }
        }
    }

    // ============================================================================
    // Phase 4 Refactoring Complete - All UI Components Extracted
    // ============================================================================
    //
    // Dialogs moved to: presentation.ui.components.dialogs/
    // - LoadingScreen, ExitDialog, CleanupDialogs, SimNumbersDialog, PinDialogs
    //
    // Screens moved to: presentation.ui.screens/
    // - home/ (HomeScreen, FilterAndLogo, ContactList, CallStatusCard, ForwardingStatus, ControlButtons)
    // - mail/ (MailScreen)
    // - settings/ (SettingsScreen + 6 sections)
    // - logs/ (LogScreen, LogTable, LogButtons)
    // - info/ (InfoScreen)
    //
    // Navigation moved to: presentation.ui.components.navigation/
    // - CustomTopAppBar, BottomNavigationBar
    //
    // MainActivity is now focused on:
    // - Activity lifecycle management
    // - Permission handling
    // - Service management
    // - Telephony callbacks
    // - Navigation setup
    //
}

