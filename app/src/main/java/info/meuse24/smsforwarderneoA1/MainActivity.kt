package info.meuse24.smsforwarderneoA1

// Removed unsafe direct import - use AppContainer.requirePrefsManager() instead
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.os.PowerManager
import android.provider.Settings
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import info.meuse24.smsforwarderneoA1.domain.model.MmiSimSelectionMode
import info.meuse24.smsforwarderneoA1.domain.model.MmiExecutionMode
import info.meuse24.smsforwarderneoA1.domain.model.ForwardingVerification
import info.meuse24.smsforwarderneoA1.domain.model.MmiOperationPolicy
import info.meuse24.smsforwarderneoA1.domain.model.DialPath
import info.meuse24.smsforwarderneoA1.util.MmiCodeMasker
import info.meuse24.smsforwarderneoA1.domain.model.SimInfo
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.CriticalPermissionsDialog
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.ExitDialog
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.LoadingScreen
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.LoopProtectionDialog
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.SimNumbersDialog
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.UssdProgressDialog
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.CleanupErrorDialog
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.CleanupProgressDialog
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.EditSimNumberDialog
import info.meuse24.smsforwarderneoA1.presentation.ui.components.navigation.BottomNavigationBar
import info.meuse24.smsforwarderneoA1.presentation.ui.components.navigation.CustomTopAppBar
import info.meuse24.smsforwarderneoA1.presentation.ui.screens.help.HelpScreen
import info.meuse24.smsforwarderneoA1.presentation.ui.screens.help.HelpSection
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
import info.meuse24.smsforwarderneoA1.util.phone.PickPhoneNumber
import info.meuse24.smsforwarderneoA1.UssdRequestResult
import info.meuse24.smsforwarderneoA1.UssdRequestType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

class MainActivity : ComponentActivity() {
    private val viewModel: ContactsViewModel by viewModels { ContactsViewModel.Factory() }
    private val logViewModel: LogViewModel by viewModels {
        LogViewModel.Factory()
    }
    private val emailViewModel: EmailViewModel by viewModels {
        EmailViewModel.Factory(
            AppContainer.requirePrefsManager()
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


    // Contact Picker Launcher - liefert die Datenzeile der gewaehlten Rufnummer
    private val contactPickerLauncher = registerForActivityResult(
        PickPhoneNumber()
    ) { uri: Uri? ->
        uri?.let { viewModel.handleContactPickerResult(it) }
    }

    // Call state management for MMI codes
    private val _callState = MutableStateFlow(TelephonyManager.CALL_STATE_IDLE)
    val callState = _callState
    private var telephonyManager: TelephonyManager? = null
    private var telephonyCallback: TelephonyCallback? = null
    @Suppress("DEPRECATION")
    private var phoneStateListener: PhoneStateListener? = null
    private var mmiOffHookAtMillis: Long? = null

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
        viewModel.onDialMmiCode = { code -> dialCode(code, viewModel.pendingForwardingRequest.value?.id) }
        viewModel.onDialStatusCode = { code -> dialCode(code, null) }

        // Set the contact picker launcher callback
        viewModel.onLaunchContactPicker = { contactPickerLauncher.launch(Unit) }

        // Set EmailViewModel callback to update service notification when forwarding state changes
        emailViewModel.onForwardingStateChanged = {
            viewModel.updateServiceNotification()
        }

        // Set ContactsViewModel callback to forward errors to NavigationViewModel
        viewModel.onErrorOccurred = { errorState ->
            navigationViewModel.setErrorState(errorState)
        }

        // Edge-to-Edge: Ab targetSdk 36 erzwingt Android 16 die randlose Darstellung;
        // setDecorFitsSystemWindows(true) wird dort ignoriert. Das Wallpaper zeichnet
        // deshalb bewusst unter Status- und Navigationsleiste, waehrend die Bedienelemente
        // ueber WindowInsets (Scaffold bzw. safeDrawing) eingerueckt bleiben.
        enableEdgeToEdge()

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
                val showCriticalPermissionsDialog by navigationViewModel.showCriticalPermissionsDialog.collectAsState()
        val criticalMissingPermissions by navigationViewModel.missingPermissions.collectAsState()
                val showPrivacyPolicy by navigationViewModel.showPrivacyPolicy.collectAsState()

                when {
                    // Zeige Privacy Policy Screen als allererstes (wenn noch nicht akzeptiert)
                    showPrivacyPolicy -> {
                        PrivacyPolicyScreen(
                            onAccept = {
                                AppContainer.requirePrefsManager().setPrivacyPolicyAccepted(true)
                                navigationViewModel.hidePrivacyPolicy()

                                LoggingManager.logInfo(
                                    component = "MainActivity",
                                    action = "PRIVACY_POLICY_ACCEPTED",
                                    message = "Datenschutzerklärung akzeptiert"
                                )

                                // Zeige LoadingScreen und starte normale Initialisierung
                                _isLoading.value = true

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
                        val permissionAvailable = AppContainer.getPermissionHandlerSafe() != null

                        if (prefsAvailable && permissionAvailable) {
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
                    navigationViewModel.showPrivacyPolicy()

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

        // A callback only reports future transitions. Seed the UI state from the
        // current platform state so an Activity recreated after a call cannot
        // keep showing a stale OFFHOOK indicator.
        _callState.value = try {
            telephonyManager?.callState ?: TelephonyManager.CALL_STATE_IDLE
        } catch (e: SecurityException) {
            LoggingManager.logWarning(
                component = "MainActivity",
                action = "PHONE_STATE_INITIAL_READ_FAILED",
                message = "Telefonstatus konnte nicht initial gelesen werden"
            )
            TelephonyManager.CALL_STATE_IDLE
        }

        // Use TelephonyCallback for API 31+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    _callState.value = state
                    recordMmiCallEvidence(state)
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
                    recordMmiCallEvidence(state)
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
    fun dialCode(code: String, operationId: String? = null) {
        if (code.isBlank()) {
            SnackbarManager.showWarning(getString(R.string.snackbar_mmi_code_empty))
            operationId?.let {
                viewModel.resolvePendingForwardingResult(it, false, ForwardingVerification.DIAL_FAILED, "DIAL_EMPTY_CODE", "MMI-Code ist leer")
            }
            return
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            LoggingManager.logWarning(
                component = "MainActivity",
                action = "DIAL_PERMISSION_MISSING",
                message = "MMI-Code wegen fehlender CALL_PHONE-Berechtigung nicht gewählt",
                details = mapOf("code" to MmiCodeMasker.mask(code))
            )
            SnackbarManager.showError(getString(R.string.snackbar_call_permission_missing))
            operationId?.let {
                viewModel.resolvePendingForwardingResult(it, false, ForwardingVerification.DIAL_FAILED, "DIAL_PERMISSION_MISSING", "CALL_PHONE fehlt")
            }
            return
        }

        if (Settings.Global.getInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1) {
            LoggingManager.logWarning(
                component = "MainActivity",
                action = "DIAL_AIRPLANE_MODE",
                message = "MMI-Code wegen aktivem Flugmodus nicht gewählt",
                details = mapOf("code" to MmiCodeMasker.mask(code))
            )
            SnackbarManager.showError(getString(R.string.snackbar_airplane_mode_active))
            operationId?.let {
                viewModel.resolvePendingForwardingResult(it, false, ForwardingVerification.DIAL_FAILED, "DIAL_AIRPLANE_MODE", "Flugmodus ist aktiv")
            }
            return
        }

        // Internationales Anschaltzeichen "+" durch konfigurierte Anschaltziffernfolge ersetzen
        val dialPrefix = prefsManager.getInternationalDialPrefix()
        val normalizedCode = if (code.contains("+")) {
            code.replace("+", dialPrefix)
        } else {
            code
        }
        // A pending deactivation can be bound to the profile used at activation.
        // Its mode must not be recomputed from settings changed in the meantime.
        val isUssdCode = viewModel.executionModeForOperation(operationId, code) == MmiExecutionMode.USSD_CALLBACK

        // Launch coroutine to wait if call is active
        lifecycleScope.launch {
            val currentCallState = callState.value
            // Wait if call is active (not IDLE)
            if (currentCallState != TelephonyManager.CALL_STATE_IDLE) {
                LoggingManager.logInfo(
                    component = "MainActivity",
                    action = "DIAL_MMI_WAITING",
                    message = "Warte bis aktueller Anruf beendet ist",
                    details = mapOf("code" to MmiCodeMasker.mask(normalizedCode), "callState" to currentCallState)
                )
                SnackbarManager.showInfo(getString(R.string.snackbar_wait_for_call_end))

                // Wait until call is idle
                val idleReached = withTimeoutOrNull(30_000) { callState.first { it == TelephonyManager.CALL_STATE_IDLE } }
                if (!MmiOperationPolicy.shouldDialAfterWaitingForCall(idleReached != null)) {
                    operationId?.let { viewModel.resolvePendingForwardingResult(it, false, ForwardingVerification.DIAL_FAILED, "CALL_BUSY_TIMEOUT", "Laufender Anruf nicht beendet") }
                    return@launch
                }

                // Add buffer after call ends
                delay(500)

                LoggingManager.logInfo(
                    component = "MainActivity",
                    action = "DIAL_MMI_READY",
                    message = "Anruf beendet, wähle MMI-Code",
                    details = mapOf("code" to MmiCodeMasker.mask(normalizedCode))
                )
            }

            // Proceed with dialing
            dialCodeNow(normalizedCode, code, isUssdCode, operationId)
        }
    }

    /**
     * Internal function to actually dial the MMI code (called after waiting if needed)
     */
    private fun dialCodeNow(normalizedCode: String, originalCode: String, isUssdCode: Boolean, operationId: String?) {
        try {
            // Determine which SIM to use for MMI code
            val mmiSimMode = prefsManager.getMmiSimSelectionMode()
            val selectedSubscriptionId = PhoneSmsUtils.determineTargetSubscriptionIdForMmi(
                this,
                mmiSimMode
            )
            val targetSubscriptionId = viewModel.targetSubscriptionForOperation(operationId, selectedSubscriptionId)

            if (isUssdCode) {
                // USSD-Code (z.B. ##21#, *#21#): Direkt senden, kein Dialer
                LoggingManager.logInfo(
                    component = "MainActivity",
                    action = "DIAL_USSD_DETECTED",
                    message = "USSD-Code erkannt (endet mit #), verwende direkten USSD-Request",
                    details = mapOf(
                        "code" to MmiCodeMasker.mask(normalizedCode),
                        "mmi_sim_mode" to mmiSimMode.name,
                        "target_sub_id" to targetSubscriptionId
                    )
                )

                viewModel.showUssdProgressDialog()
                operationId?.let { viewModel.recordDialDispatch(it, targetSubscriptionId, DialPath.USSD_REQUEST) }

                val success = PhoneSmsUtils.sendUssdCode(
                    this,
                    normalizedCode,
                    targetSubscriptionId
                ) { result ->
                    handleUssdResult(result, operationId)
                }

                LoggingManager.logInfo(
                    component = "MainActivity",
                    action = "DIAL_USSD_CODE",
                    message = if (success) "USSD-Code erfolgreich gesendet" else "USSD-Code senden fehlgeschlagen",
                    details = mapOf(
                        "original_code" to MmiCodeMasker.mask(originalCode),
                        "normalized_code" to MmiCodeMasker.mask(normalizedCode),
                        "mmi_sim_mode" to mmiSimMode.name,
                        "target_sub_id" to targetSubscriptionId,
                        "success" to success
                    )
                )
                if (!success) {
                    viewModel.dismissUssdProgressDialog()
                    operationId?.let {
                        viewModel.resolvePendingForwardingResult(it, false, ForwardingVerification.DIAL_FAILED, "USSD_SEND_FAILED", getString(R.string.snackbar_ussd_general_error, getString(R.string.snackbar_forwarding_unknown_reason)))
                    }
                }
                return // Beende Funktion nach USSD-Request
            }

            // Ab hier: MMI-Code (endet mit * oder **), verwende Dialer-Ansatz
            LoggingManager.logInfo(
                component = "MainActivity",
                action = "DIAL_MMI_DETECTED",
                message = "MMI-Code erkannt (endet mit *), verwende Dialer",
                details = mapOf(
                    "code" to MmiCodeMasker.mask(normalizedCode),
                    "mmi_sim_mode" to mmiSimMode.name,
                    "target_sub_id" to targetSubscriptionId
                )
            )

            // Get PhoneAccountHandle - IMMER holen, auch für DEFAULT_VOICE_SIM
            val phoneAccountHandle = if (targetSubscriptionId != -1) {
                PhoneSmsUtils.getPhoneAccountHandleForSubscription(this, targetSubscriptionId)
            } else {
                null
            }

            val intent = Intent(Intent.ACTION_CALL).apply {
                data = "tel:${Uri.encode(normalizedCode)}".toUri()

                // Add PhoneAccountHandle if available - verwende ALLE möglichen Keys für maximale Kompatibilität
                if (phoneAccountHandle != null) {
                    putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE", phoneAccountHandle as Parcelable)
                    // Zusätzliche Keys für verschiedene Android-Versionen/Hersteller
                    putExtra("com.android.phone.extra.PHONE_ACCOUNT_HANDLE", phoneAccountHandle as Parcelable)
                    putExtra("phone_account_handle", phoneAccountHandle as Parcelable)

                    LoggingManager.logInfo(
                        component = "MainActivity",
                        action = "DIAL_MMI_PHONE_ACCOUNT",
                        message = "PhoneAccountHandle zum Intent hinzugefügt",
                        details = mapOf(
                            "subscription_id" to targetSubscriptionId,
                            "mode" to mmiSimMode.name
                        )
                    )
                } else if (targetSubscriptionId != -1) {
                    // Fallback: Versuche mit subscription_id
                    putExtra("subscription", targetSubscriptionId)
                    putExtra("com.android.phone.extra.slot", if (targetSubscriptionId > 0) 1 else 0)

                    LoggingManager.logWarning(
                        component = "MainActivity",
                        action = "DIAL_MMI_FALLBACK",
                        message = "PhoneAccountHandle nicht verfügbar, verwende subscription_id Fallback",
                        details = mapOf("subscription_id" to targetSubscriptionId)
                    )
                }

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

            // Versuche ERST mit TelecomManager direkt zu wählen (funktioniert besser für MMI-Codes)
            var callPlaced = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val telecomManager = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                    if (telecomManager != null) {
                        val uri = "tel:${Uri.encode(normalizedCode)}".toUri()
                        telecomManager.placeCall(uri, intent.extras)
                        callPlaced = true

                        LoggingManager.logInfo(
                            component = "MainActivity",
                            action = "DIAL_MMI_TELECOM",
                            message = "MMI-Code direkt über TelecomManager gewählt",
                            details = mapOf(
                                "original_code" to MmiCodeMasker.mask(originalCode),
                                "normalized_code" to MmiCodeMasker.mask(normalizedCode),
                                "method" to "TelecomManager.placeCall"
                            )
                        )
                    }
                } catch (e: Exception) {
                    LoggingManager.logWarning(
                        component = "MainActivity",
                        action = "DIAL_MMI_TELECOM_FAILED",
                        message = "TelecomManager.placeCall fehlgeschlagen, verwende Intent Fallback",
                        details = mapOf("error" to e.message.orEmpty())
                    )
                }
            }

            // Fallback: Verwende Intent.ACTION_CALL wenn TelecomManager nicht funktioniert hat
            if (!callPlaced) {
                startActivity(intent)

                LoggingManager.logInfo(
                    component = "MainActivity",
                    action = "DIAL_MMI_INTENT",
                    message = "MMI-Code über Intent.ACTION_CALL gewählt",
                    details = mapOf(
                        "original_code" to MmiCodeMasker.mask(originalCode),
                        "normalized_code" to MmiCodeMasker.mask(normalizedCode),
                        "method" to "Intent Fallback"
                    )
                )
            }

            // Finales Logging mit allen Details (nur für MMI, nicht USSD)
            LoggingManager.logInfo(
                component = "MainActivity",
                action = "DIAL_MMI_CODE",
                message = "MMI-Code gewählt mit Lautsprecher (endet mit *)",
                details = mapOf(
                    "original_code" to MmiCodeMasker.mask(originalCode),
                    "normalized_code" to MmiCodeMasker.mask(normalizedCode),
                    "code_type" to "MMI (Dialer)",
                    "speakerphone" to true,
                    "plus_replaced" to (originalCode != normalizedCode),
                    "mmi_sim_mode" to mmiSimMode.name,
                    "target_sub_id" to targetSubscriptionId,
                    "phone_account_handle" to (phoneAccountHandle != null),
                    "call_placed_via" to if (callPlaced) "TelecomManager" else "Intent"
                )
            )

            operationId?.let {
                viewModel.recordDialDispatch(
                    it,
                    targetSubscriptionId,
                    if (callPlaced) DialPath.TELECOM_MANAGER else DialPath.ACTION_CALL
                )
            }

            // Telecom accepts the request asynchronously. For A1 voice MMI this is the
            // strongest machine-readable signal available, so SMS forwarding proceeds
            // immediately while retaining the honest ASSUMED_SUCCESS verification state.
            viewModel.pendingForwardingRequest.value?.takeIf { it.id == operationId }?.let { pending ->
                viewModel.resolvePendingForwardingResult(
                    operationId = pending.id,
                    success = true,
                    verification = ForwardingVerification.ASSUMED_SUCCESS,
                    source = "MMI_ASSUMED_SUCCESS"
                )
            }

        } catch (e: Exception) {
            LoggingManager.logError(
                component = "MainActivity",
                action = "DIAL_MMI_CODE",
                message = "Fehler beim Wählen des MMI-Codes",
                error = e,
                details = mapOf("code" to MmiCodeMasker.mask(originalCode))
            )
            SnackbarManager.showError(getString(R.string.snackbar_dial_error, e.message ?: ""))
            operationId?.let {
                viewModel.resolvePendingForwardingResult(it, false, ForwardingVerification.DIAL_FAILED, "DIAL_ERROR", e.message)
            }
        }
    }

    private fun recordMmiCallEvidence(state: Int) {
        val now = System.currentTimeMillis()
        when (state) {
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                mmiOffHookAtMillis = now
                viewModel.recordVoiceMmiEvidence(mmiOffHookAtMillis, now)
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                if (mmiOffHookAtMillis != null) {
                    viewModel.recordVoiceMmiEvidence(mmiOffHookAtMillis, now)
                    mmiOffHookAtMillis = null
                    // Start the short "report failure" window only after the
                    // carrier announcement/call has actually ended.
                    viewModel.showVoiceMmiCompletionHint()
                }
            }
        }
    }

    private fun handleUssdResult(result: UssdRequestResult, operationId: String?) {
        viewModel.dismissUssdProgressDialog()

        val sourceLabel = "USSD_${result.type.name}_${if (result.success) "SUCCESS" else "FAILED"}"
        operationId?.let {
            viewModel.recordUssdEvidence(it, result.message.orEmpty(), result.success)
            viewModel.resolvePendingForwardingResult(
                operationId = it,
                success = result.success,
                verification = viewModel.pendingForwardingRequest.value?.verification
                    ?: if (result.success) ForwardingVerification.CONFIRMED_SUCCESS else ForwardingVerification.DIAL_FAILED,
                source = sourceLabel,
                message = result.message
            )
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
            navigationViewModel.showPrivacyPolicy.value) {
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
            navigationViewModel.showCriticalPermissions(missing)
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
        val editingSim by simManagementViewModel.editingSim.collectAsState()
        val editingSimNumber by simManagementViewModel.editingSimNumber.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        // Critical Permissions Dialog State
        val showCriticalPermissionsDialog by navigationViewModel.showCriticalPermissionsDialog.collectAsState()
        val missingPermissions by navigationViewModel.missingPermissions.collectAsState()

        val showUssdInProgress by viewModel.showUssdInProgress.collectAsState()

        // Cleanup Effect
        LaunchedEffect(Unit) {
            viewModel.cleanupCompleted.collect {
                finish()
            }
        }

        // Der Scope muss die Registrierung ueberleben: `this` waere der Scope der
        // LaunchedEffect-Coroutine, und die ist mit dem Ende ihres Blocks abgeschlossen. Ein
        // abgeschlossener Job nimmt keine Kinder mehr an - jedes spaetere launch() zum Anzeigen
        // einer Meldung lief damit ins Leere, und die App blieb bei Fehlern stumm.
        val snackbarScope = rememberCoroutineScope()
        LaunchedEffect(snackbarHostState, snackbarScope) {
            SnackbarManager.setSnackbarState(snackbarHostState, snackbarScope)
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
        // Bestimmt, welcher Hilfe-Abschnitt zuerst gezeigt wird (RCS-Hinweis vs. Hilfe-Button)
        var helpInitialSection by remember { mutableStateOf(HelpSection.OVERVIEW) }

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
                                    onNavigateToHelp = {
                                        helpInitialSection = HelpSection.OVERVIEW
                                        showHelpScreen = true
                                    },
                                    onNavigateToRcsHelp = {
                                        helpInitialSection = HelpSection.RCS
                                        showHelpScreen = true
                                    }
                                )
                            }
                            "mail" -> MailScreen(emailViewModel)
                            "setup" -> SettingsScreen(viewModel, emailViewModel, testUtilsViewModel, navigationViewModel, simManagementViewModel)
                            "log" -> LogScreen(logViewModel)
                            "info" -> InfoScreen()
                        }
                    }

                    // Help Screen als Overlay innerhalb des nutzbaren Inhaltsbereichs (Top/Bottom Bar bleiben sichtbar)
                    if (showHelpScreen) {
                        HelpScreen(
                            modifier = Modifier.fillMaxSize(),
                            onNavigateBack = { showHelpScreen = false },
                            initialSection = helpInitialSection
                        )
                    }
                }
            }

            // Snackbar außerhalb des Scaffolds aber innerhalb der Box.
            // Da er nicht im Scaffold liegt, bekommt er dessen Insets nicht - unter
            // Edge-to-Edge (targetSdk 36) muss die Statusleiste selbst abgezogen werden.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)  // Ausrichtung oben
                    .windowInsetsPadding(WindowInsets.statusBars)
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

            // Edit Single SIM Dialog - Shows dialog to manually edit SIM phone number
            // Triggered by clicking on a SIM card in SimManagementSection
            editingSim?.let { sim ->
                EditSimNumberDialog(
                    simInfo = sim,
                    currentNumber = editingSimNumber,
                    onDismiss = { simManagementViewModel.hideEditSimDialog() },
                    onSave = { number ->
                        simManagementViewModel.saveSimNumber(sim.subscriptionId, number)
                        simManagementViewModel.hideEditSimDialog()
                        SnackbarManager.showSuccess(getString(R.string.msg_edit_sim_number_saved))
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
                    missingPermissions = navigationViewModel.missingPermissions.value,
                    onRequestPermissions = {
                        navigationViewModel.hideCriticalPermissions()
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

            // Loop Protection Dialog - Critical warning when selecting own SIM as target
            val loopProtectionDialogData by viewModel.showLoopProtectionDialog.collectAsState()
            loopProtectionDialogData?.let { data ->
                LoopProtectionDialog(
                    targetNumber = data.targetNumber,
                    ownNumber = data.ownNumber,
                    onDismiss = {
                        viewModel.dismissLoopProtectionDialog()
                    }
                )
            }

            if (showUssdInProgress) {
                UssdProgressDialog()
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
                // Warte auf vollständige Basis-Initialisierung des AppContainers
                AppContainer.isInitialized.first { it }

                permissionHandler.checkPermissions(
                    onGranted = {
                        completeInitializationAfterPermissions()
                    },
                    onDenied = {
                        val missing = permissionHandler.getMissingPermissions()
                        _isLoading.value = false
                        navigationViewModel.showCriticalPermissions(missing)
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

            _isLoading.value = false
        }
    }

    /**
     * Setzt Fehlerzustände zurück und versucht Initialisierung erneut.
     */
    private fun retryInitialization() {
        _loadingError.value = null
        _isLoading.value = true
        initializeApp()
    }

    /**
     * Prüft den Battery Optimization Status und zeigt bei Bedarf einen Dialog.
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
     * Zeigt einen Dialog zur Deaktivierung der Battery Optimization.
     */
    private fun showBatteryOptimizationDialog() {
        lifecycleScope.launch(Dispatchers.Main) {
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setTitle(getString(R.string.dialog_battery_opt_title))
            builder.setMessage(getString(R.string.dialog_battery_opt_message))
            builder.setPositiveButton(getString(R.string.btn_open_settings)) { dialog, _ ->
                requestBatteryOptimizationExemption()
                dialog.dismiss()
            }
            builder.setNegativeButton(getString(R.string.btn_later)) { dialog, _ ->
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
     * Öffnet die System-Einstellungen zur Deaktivierung der Battery Optimization.
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
            SnackbarManager.showError(getString(R.string.snackbar_error_open_settings))
        }
    }

    /**
     * Prüft alle SIM-Karten auf fehlende Telefonnummern und fordert diese bei Bedarf an.
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

            LoggingManager.logInfo(
                component = "MainActivity",
                action = "CHECK_SIM_NUMBERS_DEBUG",
                message = "SIM-Nummern-Prüfung gestartet",
                details = mapOf(
                    "stored_numbers" to storedNumbers.toString(),
                    "sim_count" to simInfoList.size
                )
            )

            // Prüfe jede SIM auf fehlende Telefonnummern
            simInfoList.forEach { simInfo ->
                val stored = storedNumbers[simInfo.subscriptionId]

                LoggingManager.logInfo(
                    component = "MainActivity",
                    action = "CHECK_SIM_DEBUG",
                    message = "Prüfe SIM-Karte",
                    details = mapOf(
                        "subscription_id" to simInfo.subscriptionId,
                        "slot" to simInfo.slotIndex,
                        "auto_detected" to (simInfo.phoneNumber ?: "null"),
                        "stored" to (stored ?: "null"),
                        "carrier" to (simInfo.carrierName ?: "Unknown")
                    )
                )

                if (stored.isNullOrEmpty() && simInfo.phoneNumber.isNullOrEmpty()) {
                    missingSims.add(simInfo)
                    LoggingManager.logInfo(
                        component = "MainActivity",
                        action = "SIM_MISSING",
                        message = "SIM-Nummer fehlt - Dialog wird angezeigt",
                        details = mapOf("subscription_id" to simInfo.subscriptionId)
                    )
                } else if (!simInfo.phoneNumber.isNullOrEmpty()) {
                    // Auto-detected number available
                    // Do NOT save to preferences automatically to distinguish from manual entry
                    LoggingManager.logInfo(
                        component = "MainActivity",
                        action = "SIM_DETECTED",
                        message = "SIM-Nummer automatisch erkannt",
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
        } catch (e: Exception) {
            LoggingManager.logError(
                component = "MainActivity",
                action = "CHECK_SIM_NUMBERS",
                message = "Fehler beim Prüfen der SIM-Nummern",
                error = e
            )
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

