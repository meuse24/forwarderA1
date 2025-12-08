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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import info.meuse24.smsforwarderneoA1.AppContainer.prefsManager
import info.meuse24.smsforwarderneoA1.data.local.PermissionHandler
import info.meuse24.smsforwarderneoA1.domain.model.SimInfo
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.CleanupErrorDialog
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.CleanupProgressDialog
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.CriticalPermissionsDialog
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.ExitDialog
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.LoadingScreen
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.MmiWarningDialog
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.SimNumbersDialog
import info.meuse24.smsforwarderneoA1.presentation.ui.components.navigation.BottomNavigationBar
import info.meuse24.smsforwarderneoA1.presentation.ui.components.navigation.CustomTopAppBar
import info.meuse24.smsforwarderneoA1.presentation.ui.screens.help.HelpScreen
import info.meuse24.smsforwarderneoA1.presentation.ui.screens.home.HomeScreen
import info.meuse24.smsforwarderneoA1.presentation.ui.screens.info.InfoScreen
import info.meuse24.smsforwarderneoA1.presentation.ui.screens.logs.LogScreen
import info.meuse24.smsforwarderneoA1.presentation.ui.screens.mail.MailScreen
import info.meuse24.smsforwarderneoA1.presentation.ui.screens.privacy.PrivacyPolicyScreen
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

    // State für kritischen Berechtigungs-Dialog
    private val _showCriticalPermissionsDialog = MutableStateFlow(false)
    private val _missingPermissions = MutableStateFlow<List<String>>(emptyList())

    // State für MMI Warning Dialog
    private val _showMmiWarningDialog = MutableStateFlow(false)

    // State für Privacy Policy
    private val _showPrivacyPolicy = MutableStateFlow(false)

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

    override fun attachBaseContext(newBase: android.content.Context) {
        // Read language from plain SharedPreferences (stored separately for early access)
        val prefs = newBase.getSharedPreferences("app_language_prefs", android.content.Context.MODE_PRIVATE)
        val languageCode = prefs.getString("app_language", null)
        val context = info.meuse24.smsforwarderneoA1.util.LocaleHelper.wrapContext(newBase, languageCode)
        super.attachBaseContext(context)
    }

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
                val showCriticalPermissionsDialog by _showCriticalPermissionsDialog.collectAsState()
                val showPrivacyPolicy by _showPrivacyPolicy.collectAsState()

                when {
                    // Zeige Privacy Policy Screen als allererstes (wenn noch nicht akzeptiert)
                    showPrivacyPolicy -> {
                        PrivacyPolicyScreen(
                            onAccept = {
                                AppContainer.requirePrefsManager().setPrivacyPolicyAccepted(true)
                                _showPrivacyPolicy.value = false

                                LoggingManager.logInfo(
                                    component = "MainActivity",
                                    action = "PRIVACY_POLICY_ACCEPTED",
                                    message = "Datenschutzerklärung akzeptiert"
                                )

                                // Zeige LoadingScreen und starte normale Initialisierung
                                _isLoading.value = true
                                _loadingError.value = getString(R.string.msg_requesting_permissions)

                                lifecycleScope.launch {
                                    // Kleine Verzögerung damit LoadingScreen sichtbar wird
                                    delay(300)
                                    initializeApp()
                                }
                            },
                            onDecline = {
                                LoggingManager.logWarning(
                                    component = "MainActivity",
                                    action = "PRIVACY_POLICY_DECLINED",
                                    message = "Datenschutzerklärung abgelehnt - App wird beendet"
                                )

                                // Zeige AlertDialog mit Begründung
                                AlertDialog.Builder(this@MainActivity)
                                    .setTitle(getString(R.string.dialog_title_privacy_required))
                                    .setMessage(getString(R.string.dialog_msg_privacy_required))
                                    .setPositiveButton(getString(R.string.btn_privacy_ok)) { _, _ ->
                                        finish()
                                    }
                                    .setCancelable(false)
                                    .show()
                            }
                        )
                    }
                    // Zeige LoadingScreen nur wenn nicht der CriticalPermissionsDialog aktiv ist
                    (!isFullyInitialized || isLoading) && !showCriticalPermissionsDialog -> {
                        LoadingScreen(
                            error = error,
                            onRetry = if (error != null) { { retryInitialization() } } else null,
                            onExit = if (error != null) { { finish() } } else null
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
                            // Zeige Fehler-LoadingScreen nur wenn nicht CriticalPermissionsDialog aktiv
                            if (!showCriticalPermissionsDialog) {
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

                // Prüfe ob Privacy Policy bereits akzeptiert wurde
                val privacyAccepted = AppContainer.requirePrefsManager().isPrivacyPolicyAccepted()

                if (!privacyAccepted) {
                    // Zeige Privacy Policy Screen
                    _isLoading.value = false
                    _showPrivacyPolicy.value = true

                    LoggingManager.logInfo(
                        component = "MainActivity",
                        action = "SHOW_PRIVACY_POLICY",
                        message = "Datenschutzerklärung wird angezeigt (erster App-Start)"
                    )
                } else {
                    // Privacy Policy bereits akzeptiert - normale Initialisierung
                    initializeApp()
                }

            } catch (e: Exception) {
                // Special handling for encryption initialization failure
                _loadingError.value = when (e) {
                    is info.meuse24.smsforwarderneoA1.data.local.PreferencesInitializationException -> {
                        getString(R.string.error_security_storage, e.message ?: "")
                    }
                    else -> getString(R.string.error_initialization_generic, e.message ?: "")
                }
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
            SnackbarManager.showWarning(getString(R.string.snackbar_mmi_code_empty))
            return
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            SnackbarManager.showError(getString(R.string.snackbar_call_permission_missing))
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
                SnackbarManager.showInfo(getString(R.string.snackbar_wait_for_call_end))

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

            // Show warning dialog BEFORE dialing (if enabled)
            if (prefsManager.isMmiWarningEnabled()) {
                LoggingManager.logInfo(
                    component = "MainActivity",
                    action = "DIAL_MMI_PREPARING",
                    message = "Zeige Benutzer-Warnung vor Wählvorgang",
                    details = mapOf("code" to normalizedCode, "delay_ms" to 4000)
                )

                // Show in-app dialog for 4 seconds
                _showMmiWarningDialog.value = true
                delay(4000)
                _showMmiWarningDialog.value = false
            } else {
                LoggingManager.logInfo(
                    component = "MainActivity",
                    action = "DIAL_MMI_NO_WARNING",
                    message = "MMI-Warnung deaktiviert in Einstellungen",
                    details = mapOf("code" to normalizedCode)
                )
            }

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
            SnackbarManager.showError(getString(R.string.snackbar_dial_error, e.message ?: ""))
        }
    }

    /**
     * Prüft Berechtigungen bei jedem Resume der Activity.
     * Wichtig: Erkennt wenn User Berechtigungen während der Laufzeit widerruft.
     */
    override fun onResume() {
        super.onResume()

        // Nur prüfen wenn App vollständig initialisiert ist UND nicht mehr im Loading-State
        // UND Privacy Policy nicht angezeigt wird
        if (!AppContainer.isInitialized.value ||
            !::permissionHandler.isInitialized ||
            _isLoading.value ||
            _showPrivacyPolicy.value) {
            return
        }

        // Prüfe ob alle Berechtigungen noch vorhanden sind
        if (!permissionHandler.hasAllPermissions()) {
            val missing = permissionHandler.getMissingPermissions()

            LoggingManager.logWarning(
                component = "MainActivity",
                action = "PERMISSIONS_REVOKED",
                message = "Berechtigungen wurden während der Laufzeit widerrufen",
                details = mapOf(
                    "missing_count" to missing.size,
                    "missing_permissions" to missing.joinToString()
                )
            )

            // Zeige kritischen Dialog
            _missingPermissions.value = missing
            _showCriticalPermissionsDialog.value = true
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
        //val topBarTitle by navigationViewModel.topBarTitle.collectAsState()
        val navigationTarget by navigationViewModel.navigationTarget.collectAsState()
        val showExitDialog by navigationViewModel.showExitDialog.collectAsState()
        val showProgressDialog by viewModel.showProgressDialog.collectAsState()
        val errorState by navigationViewModel.errorState.collectAsState()
        // showOwnNumberMissingDialog StateFlow entfernt
        val showSimNumbersDialog by simManagementViewModel.showSimNumbersDialog.collectAsState()
        val missingSims by simManagementViewModel.missingSims.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        // Critical Permissions Dialog State
        val showCriticalPermissionsDialog by _showCriticalPermissionsDialog.collectAsState()
        val missingPermissions by _missingPermissions.collectAsState()

        // MMI Warning Dialog State
        val showMmiWarningDialog by _showMmiWarningDialog.collectAsState()

        // Cleanup Effect
        LaunchedEffect(Unit) {
            viewModel.cleanupCompleted.collect {
                finish()
            }
        }

        // Initialisieren Sie den SnackbarManager mit dem State und Scope
        LaunchedEffect(snackbarHostState) {
            SnackbarManager.setSnackbarState(snackbarHostState, this)
        }

        // State für Swipe-Navigation
        val mailScreenVisible by viewModel.mailScreenVisible.collectAsState()
        val screens = remember(mailScreenVisible) {
            if (mailScreenVisible) {
                listOf("start", "mail", "setup", "log", "info")
            } else {
                listOf("start", "setup", "log", "info")
            }
        }

        val pagerState = rememberPagerState(pageCount = { screens.size })
        val coroutineScope = rememberCoroutineScope()
        var showHelpScreen by remember { mutableStateOf(false) }

        // Navigation Effect - navigate to page when navigationTarget changes
        LaunchedEffect(navigationTarget, screens) {
            navigationTarget?.let { target ->
                showHelpScreen = false
                val targetIndex = screens.indexOf(target)
                if (targetIndex >= 0) {
                    pagerState.animateScrollToPage(targetIndex)
                }
                navigationViewModel.onNavigated()
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {  // Äußere Box für absolutes Positioning
            Scaffold(
                topBar = { CustomTopAppBar(title = "") },
                bottomBar = {
                    BottomNavigationBar(
                        screens = screens,
                        currentPage = pagerState.currentPage,
                        onPageSelected = { page ->
                            showHelpScreen = false
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(page)
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (screens[page]) {
                            "start" -> {
                                val currentCallState = callState.collectAsState()
                                HomeScreen(
                                    viewModel = viewModel,
                                    emailViewModel = emailViewModel,
                                    testUtilsViewModel = testUtilsViewModel,
                                    callState = currentCallState,
                                    onNavigateToHelp = { showHelpScreen = true }
                                )
                            }
                            "mail" -> MailScreen(emailViewModel)
                            "setup" -> SettingsScreen(viewModel, emailViewModel, testUtilsViewModel, navigationViewModel)
                            "log" -> LogScreen(logViewModel)
                            "info" -> InfoScreen()
                        }
                    }

                    // Help Screen als Overlay innerhalb des nutzbaren Inhaltsbereichs (Top/Bottom Bar bleiben sichtbar)
                    if (showHelpScreen) {
                        HelpScreen(
                            modifier = Modifier.fillMaxSize(),
                            onNavigateBack = { showHelpScreen = false }
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

            // Critical Permissions Dialog
            if (showCriticalPermissionsDialog) {
                CriticalPermissionsDialog(
                    missingPermissions = missingPermissions,
                    onRequestPermissions = {
                        _showCriticalPermissionsDialog.value = false
                        permissionHandler.recheckAndRequest(
                            onAllGranted = {
                                LoggingManager.logInfo(
                                    component = "MainActivity",
                                    action = "PERMISSIONS_REGRANTED",
                                    message = "Berechtigungen wurden erfolgreich wieder erteilt"
                                )
                                SnackbarManager.showSuccess(getString(R.string.snackbar_permissions_granted_all))

                                // Führe vollständige Initialisierung durch (inkl. Battery Optimization)
                                completeInitializationAfterPermissions()
                            },
                            onStillMissing = { stillMissing ->
                                LoggingManager.logError(
                                    component = "MainActivity",
                                    action = "PERMISSIONS_FINAL_DENY",
                                    message = "App wird beendet - kritische Berechtigungen verweigert",
                                    details = mapOf(
                                        "missing_permissions" to stillMissing.joinToString()
                                    )
                                )
                                SnackbarManager.showError(getString(R.string.snackbar_permissions_missing_exit))
                                lifecycleScope.launch {
                                    delay(2000) // Zeige Snackbar für 2 Sekunden
                                    finish()
                                }
                            }
                        )
                    },
                    onExitApp = {
                        LoggingManager.logInfo(
                            component = "MainActivity",
                            action = "USER_EXIT_NO_PERMISSIONS",
                            message = "User hat App beendet wegen fehlender Berechtigungen"
                        )
                        finish()
                    }
                )
            }

            // MMI Warning Dialog - Beautiful overlay shown before dialing MMI codes
            if (showMmiWarningDialog) {
                MmiWarningDialog(
                    onDismiss = {
                        _showMmiWarningDialog.value = false
                    }
                )
            }
        }
    }

    /**
     * Startet die App-Initialisierung nach akzeptierter Privacy Policy.
     * Prüft Berechtigungen und zeigt bei Bedarf den CriticalPermissionsDialog.
     */
    private fun initializeApp() {
        lifecycleScope.launch {
            try {
                _loadingError.value = getString(R.string.msg_requesting_permissions)

                // Warte auf vollständige Basis-Initialisierung des AppContainers
                AppContainer.isInitialized.first { it }

                permissionHandler.checkPermissions(
                    onGranted = {
                        completeInitializationAfterPermissions()
                    },
                    onDenied = {
                        val missing = permissionHandler.getMissingPermissions()
                        _isLoading.value = false
                        _missingPermissions.value = missing
                        _showCriticalPermissionsDialog.value = true
                    }
                )
            } catch (e: Exception) {
                _loadingError.value = getString(R.string.error_initialization_generic, e.message ?: "")
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
     * Führt notwendige Schritte nach erteilten Berechtigungen aus.
     */
    private fun completeInitializationAfterPermissions() {
        SmsForegroundService.startService(this@MainActivity)
        setupPhoneStateListener()
        lifecycleScope.launch {
            delay(500)
            viewModel.initialize()
        }
        _isLoading.value = false
    }

    /**
     * Setzt Fehlerzustände zurück und versucht Initialisierung erneut.
     */
    private fun retryInitialization() {
        _loadingError.value = null
        _isLoading.value = true
        initializeApp()
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

