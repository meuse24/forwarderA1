package info.meuse24.smsforwarderneoA1

// Removed unsafe direct import - use AppContainer.requirePrefsManager() instead
import android.content.Context
import info.meuse24.smsforwarderneoA1.data.local.PermissionHandler
import info.meuse24.smsforwarderneoA1.domain.model.Contact
import info.meuse24.smsforwarderneoA1.domain.model.LogEntry
import info.meuse24.smsforwarderneoA1.domain.model.SimInfo
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.LoadingScreen
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.ExitDialog
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.CleanupProgressDialog
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.CleanupErrorDialog
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.SimNumbersDialog
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.PinDialog
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.ChangePinDialog
import info.meuse24.smsforwarderneoA1.presentation.ui.screens.logs.LogScreen
import info.meuse24.smsforwarderneoA1.presentation.ui.screens.info.InfoScreen
import info.meuse24.smsforwarderneoA1.presentation.ui.screens.mail.MailScreen
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.icu.text.SimpleDateFormat
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Textsms
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: ContactsViewModel by viewModels { ContactsViewModel.Factory() }
    private val _isLoading = MutableStateFlow(true)
    private val _loadingError = MutableStateFlow<String?>(null)
    private lateinit var permissionHandler: PermissionHandler

    // Call state management for MMI codes
    private val _callState = MutableStateFlow(TelephonyManager.CALL_STATE_IDLE)
    val callState = _callState
    private var telephonyManager: TelephonyManager? = null
    private var telephonyCallback: TelephonyCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the MMI code dial callback in ViewModel
        viewModel.onDialMmiCode = { code -> dialCode(code) }

        // Normale Statusleiste - kein Edge-to-Edge
        WindowCompat.setDecorFitsSystemWindows(window, true)

        onBackPressedDispatcher.addCallback(this) {
            // Prüfe ob irgendeine Art der Weiterleitung aktiv ist
            if (viewModel.forwardingActive.value || viewModel.forwardSmsToEmail.value) {
                // Zeige Exit-Dialog mit Optionen zum Deaktivieren/Beibehalten
                viewModel.onShowExitDialog()
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
                            UI(viewModel)
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
                    viewModel.requestMissingSimNumbers(missingSims)
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

            // Warnung bei mehreren SIM-Karten
            if (simInfoList.size > 1) {
                withContext(Dispatchers.Main) {
                    SnackbarManager.showWarning(
                        "Diese App wurde nur mit einer SIM-Karte getestet. Bei ${simInfoList.size} SIM-Karten können unerwartete Probleme auftreten.",
                        duration = SnackbarManager.Duration.LONG
                    )
                }
                LoggingManager.logWarning(
                    component = "MainActivity",
                    action = "MULTI_SIM_WARNING",
                    message = "Multi-SIM Warnung angezeigt",
                    details = mapOf(
                        "sim_count" to simInfoList.size,
                        "sim_slots" to simInfoList.map { "Slot ${it.slotIndex}: ${it.carrierName ?: "Unknown"}" }
                    )
                )
            }

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
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
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
                telephonyManager?.registerTelephonyCallback(
                    mainExecutor,
                    telephonyCallback!!
                )
            } else {
                // For older versions use PhoneStateListener
                @Suppress("DEPRECATION")
                val phoneStateListener = object : PhoneStateListener() {
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
                @Suppress("DEPRECATION")
                telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
            }
        }
    }

    /**
     * Dial MMI code with speakerphone enabled and audio focus management
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

        // Internationales Anschaltzeichen "+" durch österreichische Anschaltzeichenfolge "00" ersetzen
        val normalizedCode = if (code.contains("+")) {
            code.replace("+", "00")
        } else {
            code
        }

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

                audioManager.requestAudioFocus(focusRequest)
            }

            startActivity(intent)

            LoggingManager.logInfo(
                component = "MainActivity",
                action = "DIAL_MMI_CODE",
                message = "MMI-Code gewählt mit Lautsprecher",
                details = mapOf(
                    "original_code" to code,
                    "normalized_code" to normalizedCode,
                    "speakerphone" to true,
                    "plus_replaced" to (code != normalizedCode)
                )
            )

            SnackbarManager.showInfo("MMI-Code wird gewählt: $normalizedCode")

        } catch (e: Exception) {
            LoggingManager.logError(
                component = "MainActivity",
                action = "DIAL_MMI_CODE",
                message = "Fehler beim Wählen des MMI-Codes",
                error = e,
                details = mapOf("code" to code)
            )
            SnackbarManager.showError("Fehler beim Wählen: ${e.message}")
        }
    }

    override fun onDestroy() {
        //viewModel.deactivateForwarding()
        //viewModel.saveCurrentState() // Neue Methode, die wir im ViewModel hinzufügen werden

        // Unregister telephony callback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let {
                telephonyManager?.unregisterTelephonyCallback(it)
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
    fun UI(viewModel: ContactsViewModel) {
        val navController = rememberNavController()
        val topBarTitle by viewModel.topBarTitle.collectAsState()
        val navigationTarget by viewModel.navigationTarget.collectAsState()
        val showExitDialog by viewModel.showExitDialog.collectAsState()
        val showProgressDialog by viewModel.showProgressDialog.collectAsState()
        val errorState by viewModel.errorState.collectAsState()
        // showOwnNumberMissingDialog StateFlow entfernt
        val showSimNumbersDialog by viewModel.showSimNumbersDialog.collectAsState()
        val missingSims by viewModel.missingSims.collectAsState()
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
                viewModel.onNavigated()
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {  // Äußere Box für absolutes Positioning
            Scaffold(
                topBar = { CustomTopAppBar(title = topBarTitle) },
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
                        HomeScreen(viewModel)
                    }
                    composable("mail") {
                        MailScreen(viewModel)
                    }
                    composable("setup") {
                        SettingsScreen()
                    }
                    composable("log") {
                        LogScreen(viewModel)
                    }
                    composable("info") {
                        InfoScreen()
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
                    onDismiss = { viewModel.hideSimNumbersDialog() },
                    onSaveNumber = { subscriptionId, phoneNumber ->
                        viewModel.saveSimNumber(subscriptionId, phoneNumber)
                    }
                )
            }

            // Exit Dialog
            if (showExitDialog) {
                val selectedContact by viewModel.selectedContact.collectAsState() // Hier collectAsState verwenden
                ExitDialog(
                    contact = selectedContact,
                    initialKeepForwarding = AppContainer.requirePrefsManager().getKeepForwardingOnExit(),
                    onDismiss = { viewModel.hideExitDialog() },
                    onConfirm = { keepForwarding ->
                        viewModel.hideExitDialog()
                        viewModel.startCleanup(keepForwarding)
                    },
                    onSettings = {
                        viewModel.hideExitDialog()
                        viewModel.navigateToSettings()
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
                        viewModel.clearErrorState()
                        viewModel.startCleanup(false)
                    },
                    onIgnore = {
                        viewModel.clearErrorState()
                        finish()
                    },
                    onDismiss = {
                        viewModel.clearErrorState()
                    }
                )
            }
        }
    }

    // OwnNumberMissingDialog entfernt - Telefonnummer wird jetzt in SIM-Karten-Übersicht verwaltet

    // ExitDialog moved to presentation.ui.components.dialogs.ExitDialog

    // CleanupProgressDialog moved to presentation.ui.components.dialogs.CleanupDialogs
    // SimNumbersDialog moved to presentation.ui.components.dialogs.SimNumbersDialog
    // CleanupErrorDialog moved to presentation.ui.components.dialogs.CleanupDialogs

    @Composable
    fun CustomTopAppBar(title: String) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                // Kein Text mehr - nur farbige TopAppBar
            }
        }
    }

    @Composable
    fun BottomNavigationBar(navController: NavController, viewModel: ContactsViewModel) {
        val mailScreenVisible by viewModel.mailScreenVisible.collectAsState()
        val items = if (mailScreenVisible) {
            listOf("start", "mail", "setup", "log", "info")
        } else {
            listOf("start", "setup", "log", "info")
        }
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        // Navigate away from mail screen if it becomes hidden
        LaunchedEffect(mailScreenVisible, currentRoute) {
            if (!mailScreenVisible && currentRoute == "mail") {
                navController.navigate("start") {
                    popUpTo("start") { inclusive = true }
                    launchSingleTop = true
                }
            }
        }

        NavigationBar(
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            items.forEach { screen ->
                NavigationBarItem(
                    icon = {
                        when (screen) {
                            "setup" -> Icon(
                                Icons.Filled.Settings,
                                contentDescription = "Setup"
                            )

                            "mail" -> Icon(
                                Icons.Filled.Email,
                                contentDescription = "Mail"
                            )

                            "log" -> Icon(
                                Icons.AutoMirrored.Filled.List,
                                contentDescription = "Log"
                            )

                            "info" -> Icon(
                                Icons.Filled.Info,
                                contentDescription = "Info"
                            )

                            else -> Icon(
                                Icons.Filled.Home,
                                contentDescription = "Start"
                            )
                        }
                    },
                    label = {
                        Text(
                            when (screen) {
                                "start" -> "Start"
                                "mail" -> "Mail"
                                "setup" -> "Setup"
                                "log" -> "Log"
                                else -> "Info"
                            }
                        )
                    },
                    selected = currentRoute == screen,
                    onClick = {
                        navController.navigate(screen) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                        }
                    }
                )
            }
        }
    }

    @Composable
    fun HomeScreen(viewModel: ContactsViewModel) {
        val contacts by viewModel.contacts.collectAsState()
        val selectedContact by viewModel.selectedContact.collectAsState()
        val forwardingActive by viewModel.forwardingActive.collectAsState()
        val filterText by viewModel.filterText.collectAsState()
        val forwardSmsToEmail by viewModel.forwardSmsToEmail.collectAsState()
        val emailAddresses by viewModel.emailAddresses.collectAsState()
        val currentCallState by callState.collectAsState()

        // Check if call is active (for button disabling)
        val isCallActive = currentCallState == TelephonyManager.CALL_STATE_OFFHOOK

        // Initialisierung beim ersten Laden
        LaunchedEffect(Unit) {
            viewModel.initialize()
        }

        // Filter neu anwenden beim Betreten des Screens
        LaunchedEffect(Unit) {
            if (filterText.isNotEmpty()) {
                viewModel.applyCurrentFilter()
            }
        }

        BoxWithConstraints {
            @Suppress("UNUSED_EXPRESSION")
            val isLandscape = this.maxWidth > this.maxHeight

            if (isLandscape) {
                LandscapeLayout(
                    viewModel = viewModel,
                    contacts = contacts,
                    selectedContact = selectedContact,
                    forwardingActive = forwardingActive,
                    filterText = filterText,
                    forwardSmsToEmail = forwardSmsToEmail,
                    emailAddresses = emailAddresses,
                    isCallActive = isCallActive,
                    callState = currentCallState
                )
            } else {
                PortraitLayout(
                    viewModel = viewModel,
                    contacts = contacts,
                    selectedContact = selectedContact,
                    forwardingActive = forwardingActive,
                    filterText = filterText,
                    forwardSmsToEmail = forwardSmsToEmail,
                    emailAddresses = emailAddresses,
                    isCallActive = isCallActive,
                    callState = currentCallState
                )
            }
        }
    }

    @Composable
    fun LandscapeLayout(
        viewModel: ContactsViewModel,
        contacts: List<Contact>,
        selectedContact: Contact?,
        forwardingActive: Boolean,
        filterText: String,
        forwardSmsToEmail: Boolean,
        emailAddresses: List<String>,
        isCallActive: Boolean,
        callState: Int
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp)
        ) {
            ContactListBox(
                contacts = contacts,
                selectedContact = selectedContact,
                onSelectContact = viewModel::toggleContactSelection,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                isCallActive = isCallActive
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                FilterAndLogo(
                    filterText = filterText,
                    onFilterTextChange = {
                        viewModel.updateFilterText(it)
                    },
                    forwardingActive = forwardingActive,
                    onDeactivateForwarding = viewModel::deactivateForwarding
                )

                CallStatusCard(callState = callState)

                ForwardingStatus(
                    forwardingActive = forwardingActive,
                    selectedContact = selectedContact,
                    forwardSmsToEmail = forwardSmsToEmail,
                    emailAddresses = emailAddresses,
                    onQueryStatus = viewModel::queryForwardingStatus
                )

                ControlButtons(
                    onDeactivateForwarding = viewModel::deactivateForwarding,
                    onSendTestSms = viewModel::sendTestSms,
                    isEnabled = selectedContact != null
                )
            }
        }
    }

    @Composable
    fun PortraitLayout(
        viewModel: ContactsViewModel,
        contacts: List<Contact>,
        selectedContact: Contact?,
        forwardingActive: Boolean,
        filterText: String,
        forwardSmsToEmail: Boolean,
        emailAddresses: List<String>,
        isCallActive: Boolean,
        callState: Int
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            FilterAndLogo(
                filterText = filterText,
                onFilterTextChange = {
                    viewModel.updateFilterText(it)
                },
                forwardingActive = forwardingActive,
                onDeactivateForwarding = viewModel::deactivateForwarding
            )

            ContactListBox(
                contacts = contacts,
                selectedContact = selectedContact,
                onSelectContact = viewModel::toggleContactSelection,
                modifier = Modifier.weight(1f),
                isCallActive = isCallActive
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CallStatusCard(callState = callState)
                ForwardingStatus(
                    forwardingActive = forwardingActive,
                    selectedContact = selectedContact,
                    forwardSmsToEmail = forwardSmsToEmail,
                    emailAddresses = emailAddresses,
                    onQueryStatus = viewModel::queryForwardingStatus
                )
                Spacer(modifier = Modifier.height(4.dp))
                ControlButtons(
                    onDeactivateForwarding = viewModel::deactivateForwarding,
                    onSendTestSms = viewModel::sendTestSms,
                    isEnabled = selectedContact != null
                )
            }
        }
    }

    @Composable
    fun FilterAndLogo(
        filterText: String,
        onFilterTextChange: (String) -> Unit,
        forwardingActive: Boolean,
        onDeactivateForwarding: () -> Unit
    ) {
        val rotation = remember { Animatable(0f) }
        var hasAnimated by remember { mutableStateOf(false) }
        val focusManager = LocalFocusManager.current
        var isFilterFocused by remember { mutableStateOf(false) }

        // Animiere nur beim ersten Start, nicht bei jedem Recompose
        LaunchedEffect(Unit) {
            if (!hasAnimated) {
                rotation.animateTo(
                    targetValue = 360f,
                    animationSpec = tween(
                        durationMillis = 2000,
                        easing = LinearEasing
                    )
                )
                hasAnimated = true
            }
        }

// Speichere den Orientierungszustand
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 4.dp,
                    horizontal = if (isLandscape) 8.dp else 4.dp
                )
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .graphicsLayer(rotationZ = rotation.value)  // Keine Animation, nur der Wert
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logofwd2),
                        contentDescription = "App Icon",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(scaleX = 1.5f, scaleY = 1.5f)
                            .align(Alignment.Center)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = filterText,
                        onValueChange = onFilterTextChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged {
                                isFilterFocused = it.isFocused
                            },
                        label = { Text("Kontakt suchen") },
                        placeholder = { Text("Namen oder Nummer eingeben") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                            }
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Suchen",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (filterText.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        onFilterTextChange("")
                                        focusManager.clearFocus()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Filter löschen",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }
    }

    @Composable
    fun ContactListBox(
        contacts: List<Contact>,
        selectedContact: Contact?,
        onSelectContact: (Contact) -> Unit,
        modifier: Modifier = Modifier,
        isCallActive: Boolean = false
    ) {
        Box(modifier = modifier) {
            if (contacts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Keine Kontakte gefunden",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    items(contacts) { contact ->
                        ContactItem(
                            contact = contact,
                            isSelected = contact == selectedContact,
                            onSelect = { onSelectContact(contact) },
                            isCallActive = isCallActive
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun ContactItem(
        contact: Contact,
        isSelected: Boolean,
        onSelect: () -> Unit,
        isCallActive: Boolean = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isCallActive) {
                    // Eigentelefonnummer-Prüfung entfernt - wird über SIM-Verwaltung abgewickelt
                    onSelect()
                }
                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                .padding(vertical = 4.dp, horizontal = 16.dp)
        ) {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = contact.description,
                style = MaterialTheme.typography.bodySmall
            )
        }
        HorizontalDivider()
    }

    @Composable
    fun CallStatusCard(callState: Int) {
        val statusText = when (callState) {
            TelephonyManager.CALL_STATE_OFFHOOK -> "MMI-Code wird ausgeführt..."
            TelephonyManager.CALL_STATE_RINGING -> "Eingehender Anruf..."
            else -> return // Don't show card if IDLE
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (callState == TelephonyManager.CALL_STATE_OFFHOOK)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (callState == TelephonyManager.CALL_STATE_OFFHOOK)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }

    @Composable
    fun ForwardingStatus(
        forwardingActive: Boolean,
        selectedContact: Contact?,
        forwardSmsToEmail: Boolean,
        emailAddresses: List<String>,
        onQueryStatus: () -> Unit
    ) {
        val hasEmailForwarding = forwardSmsToEmail && emailAddresses.isNotEmpty()
        val hasAnyForwarding = forwardingActive || hasEmailForwarding

        Surface(
            color = if (hasAnyForwarding) Color.Green else Color.Red,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Text(s)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (forwardingActive) {
                        Text(
                            text = "SMS-Weiterleitung aktiv zu ${selectedContact?.phoneNumber}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (hasEmailForwarding) {
                        Text(
                            text = "Email-Weiterleitung aktiv an ${emailAddresses.size} Adresse(n)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (!hasAnyForwarding) {
                        Text(
                            text = "Weiterleitung inaktiv",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Status Query Icon Button
                IconButton(
                    onClick = onQueryStatus,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Status abfragen",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    @Composable
    fun ControlButtons(
        onDeactivateForwarding: () -> Unit,
        onSendTestSms: () -> Unit,
        isEnabled: Boolean
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // First row: Deactivate and Test SMS
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
            Button(
                onClick = onDeactivateForwarding,
                modifier = Modifier.weight(1f),
                enabled = isEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Deaktivieren",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Button(
                onClick = onSendTestSms,
                modifier = Modifier.weight(1f),
                enabled = isEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Textsms,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Test-SMS",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            }
        }
    }

    @Composable
    fun SettingsScreen() {
        val scrollState = rememberScrollState()
        LocalFocusManager.current
        var isAnyFieldFocused by remember { mutableStateOf(false) }
        var showPinDialog by remember { mutableStateOf(false) }
        var showChangePinDialog by remember { mutableStateOf(false) }
        val sectionTitleStyle = MaterialTheme.typography.titleMedium

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {

            PhoneSettingsSection(
                viewModel = viewModel,
                onFocusChanged = { isAnyFieldFocused = it },
                sectionTitleStyle = sectionTitleStyle)

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            SimManagementSection(
                viewModel = viewModel,
                onFocusChanged = { isAnyFieldFocused = it },
                sectionTitleStyle = sectionTitleStyle
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            AppSettingsSection(
                viewModel = viewModel,
                onFocusChanged = { isAnyFieldFocused = it },
                sectionTitleStyle = sectionTitleStyle
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            MmiCodeSettingsSection(
                viewModel = viewModel,
                onFocusChanged = { isAnyFieldFocused = it },
                sectionTitleStyle = sectionTitleStyle
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            EmailSettingsSection(
                viewModel = viewModel,
                sectionTitleStyle = sectionTitleStyle
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Neue Log Settings Section
            LogSettingsSection(
                sectionTitleStyle = sectionTitleStyle,
                onDeleteLogs = { showPinDialog = true },
                onChangePin = { showChangePinDialog = true }
            )

            // Die existierenden PIN-Dialoge aus dem LogScreen
            if (showPinDialog) {
                PinDialog(
                    storedPin = AppContainer.requirePrefsManager().getLogPIN(),
                    onPinCorrect = {
                        AppContainer.requireLogger().clearLog()
                        LoggingManager.logInfo(
                            component = "SettingsScreen",
                            action = "CLEAR_LOGS",
                            message = "Log-Einträge wurden gelöscht"
                        )
                        SnackbarManager.showSuccess("Logs wurden gelöscht")
                        showPinDialog = false
                    },
                    onDismiss = { showPinDialog = false }
                )
            }

            if (showChangePinDialog) {
                ChangePinDialog(
                    storedPin = AppContainer.requirePrefsManager().getLogPIN(),
                    onPinChanged = { newPin ->
                        AppContainer.requirePrefsManager().setLogPIN(newPin)
                        LoggingManager.logInfo(
                            component = "SettingsScreen",
                            action = "CHANGE_PIN",
                            message = "Log-PIN wurde geändert"
                        )
                        SnackbarManager.showSuccess("PIN wurde geändert")
                        showChangePinDialog = false
                    },
                    onDismiss = { showChangePinDialog = false }
                )
            }

        }
    }

    @Composable
    private fun LogSettingsSection(
        sectionTitleStyle: TextStyle,
        onDeleteLogs: () -> Unit,
        onChangePin: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Log-Einstellungen",
                style = sectionTitleStyle,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Log-Datei löschen",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Löscht alle Protokolleinträge",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDeleteLogs) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Logs löschen",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .alpha(0.5f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PIN ändern",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "PIN für Löschfunktion ändern",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onChangePin) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "PIN ändern",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    @Composable
    private fun PhoneSettingsSection(
        viewModel: ContactsViewModel,
        onFocusChanged: (Boolean) -> Unit,
        sectionTitleStyle: TextStyle
    ) {
        val context = LocalContext.current
        val isForwardingActive by viewModel.forwardingActive.collectAsState()
        val countryCode by viewModel.countryCode.collectAsState()
        val countryCodeSource by viewModel.countryCodeSource.collectAsState()

        // LaunchedEffect für eigene Telefonnummer entfernt

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Telefon-Einstellungen",
                style = sectionTitleStyle,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Eigene Telefonnummer Feld entfernt - wird jetzt in SIM-Karten-Übersicht verwaltet

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isForwardingActive,
                        onCheckedChange = null,
                        enabled = false
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Weiterleitung aktiv")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column {
                Text(
                    text = "Erkannte Ländervorwahl",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(text = PhoneSmsUtils.getCountryNameForCode(countryCode))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Quelle: $countryCodeSource",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun SimManagementSection(
        viewModel: ContactsViewModel,
        onFocusChanged: (Boolean) -> Unit,
        sectionTitleStyle: TextStyle
    ) {
        val context = LocalContext.current
        var simInfoList by remember { mutableStateOf<List<SimInfo>>(emptyList()) }
        var storedNumbers by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
        var defaultSimIds by remember { mutableStateOf<Pair<Int, Int>?>(null) }
        var isLoading by remember { mutableStateOf(true) }

        // Load SIM info on first composition
        LaunchedEffect(Unit) {
            try {
                val sims = PhoneSmsUtils.getAllSimInfo(context)
                val stored = AppContainer.getPrefsManagerSafe()?.getSimPhoneNumbers() ?: emptyMap()
                val defaults = PhoneSmsUtils.getDefaultSimIds(context)
                simInfoList = sims
                storedNumbers = stored
                defaultSimIds = defaults
                isLoading = false
            } catch (e: Exception) {
                LoggingManager.logError(
                    component = "SimManagementSection",
                    action = "LOAD_SIM_INFO",
                    message = "Fehler beim Laden der SIM-Informationen",
                    error = e
                )
                isLoading = false
            }
        }

        // No focus tracking needed for read-only display
        LaunchedEffect(Unit) {
            onFocusChanged(false)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "SIM-Karten Übersicht",
                style = sectionTitleStyle,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            } else if (simInfoList.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Keine SIM-Karten gefunden",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Stellen Sie sicher, dass Berechtigungen erteilt wurden",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            } else {
                // Show SIM cards (read-only)
                simInfoList.forEach { sim ->
                    val currentNumber = storedNumbers[sim.subscriptionId] ?: sim.phoneNumber ?: ""

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // SIM Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "SIM ${sim.slotIndex + 1}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (!sim.carrierName.isNullOrEmpty()) {
                                        Text(
                                            text = sim.carrierName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (!sim.displayName.isNullOrEmpty() && sim.displayName != sim.carrierName) {
                                        Text(
                                            text = sim.displayName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Check if this SIM is default for SMS or Voice
                                    val isDefaultSms = defaultSimIds?.first == sim.subscriptionId
                                    val isDefaultVoice = defaultSimIds?.second == sim.subscriptionId

                                    // Default SIM Badge
                                    if (isDefaultSms || isDefaultVoice) {
                                        Surface(
                                            shape = MaterialTheme.shapes.small,
                                            color = MaterialTheme.colorScheme.tertiaryContainer
                                        ) {
                                            Text(
                                                text = when {
                                                    isDefaultSms && isDefaultVoice -> "Standard"
                                                    isDefaultSms -> "Standard SMS"
                                                    else -> "Standard Voice"
                                                },
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                        }
                                    }

                                    if (sim.isAutoDetected) {
                                        Surface(
                                            shape = MaterialTheme.shapes.small,
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "Auto",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }

                                    // Status Badge
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = if (currentNumber.isNotEmpty()) {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.errorContainer
                                        }
                                    ) {
                                        Text(
                                            text = if (currentNumber.isNotEmpty()) "Konfiguriert" else "Fehlend",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (currentNumber.isNotEmpty()) {
                                                MaterialTheme.colorScheme.onSecondaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onErrorContainer
                                            }
                                        )
                                    }
                                }
                            }

                            // Phone Number Display
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (currentNumber.isNotEmpty()) {
                                        currentNumber
                                    } else {
                                        "Nicht konfiguriert"
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (currentNumber.isNotEmpty()) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    fontFamily = if (currentNumber.isNotEmpty()) {
                                        FontFamily.Monospace
                                    } else {
                                        FontFamily.Default
                                    }
                                )
                            }

                            // Source Info
                            if (currentNumber.isNotEmpty()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (sim.isAutoDetected) Icons.Default.Visibility else Icons.Default.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (sim.isAutoDetected) {
                                                "Automatisch erkannt"
                                            } else {
                                                "Manuell eingegeben"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Summary info
                if (simInfoList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "Zusammenfassung:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "• ${simInfoList.size} SIM-Karte(n) erkannt",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val autoDetected = simInfoList.count { !it.phoneNumber.isNullOrEmpty() }
                            Text(
                                text = "• $autoDetected automatisch erkannt",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val withStoredNumbers = storedNumbers.values.count { it.isNotEmpty() }
                            Text(
                                text = "• $withStoredNumbers Nummer(n) konfiguriert",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Hinweis für fehlende Nummern
                            if (withStoredNumbers < simInfoList.size) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "ℹ️ Fehlende Nummern werden beim App-Start abgefragt",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun AppSettingsSection(
        viewModel: ContactsViewModel,
        onFocusChanged: (Boolean) -> Unit,
        sectionTitleStyle: TextStyle
    ) {
        val filterText by viewModel.filterText.collectAsState()
        val testSmsText by viewModel.testSmsText.collectAsState()
        val testEmailText by viewModel.testEmailText.collectAsState()
        val topBarTitle by viewModel.topBarTitle.collectAsState()
        val mailScreenVisible by viewModel.mailScreenVisible.collectAsState()
        val phoneNumberFormatting by viewModel.phoneNumberFormatting.collectAsState()

        var isFilterTextFocused by remember { mutableStateOf(false) }
        var isTestSmsTextFocused by remember { mutableStateOf(false) }
        var isTestEmailTextFocused by remember { mutableStateOf(false) }
        var isTopBarTitleFocused by remember { mutableStateOf(false) }

        LaunchedEffect(
            isFilterTextFocused,
            isTestSmsTextFocused,
            isTestEmailTextFocused,
            isTopBarTitleFocused
        ) {
            onFocusChanged(
                isFilterTextFocused || isTestSmsTextFocused ||
                        isTestEmailTextFocused || isTopBarTitleFocused
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "App-Einstellungen",
                style = sectionTitleStyle,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = filterText,
                onValueChange = { viewModel.updateFilterText(it) },
                label = { Text("Kontakte - Suchfilter") },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFilterTextFocused = it.isFocused }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = testSmsText,
                onValueChange = { viewModel.updateTestSmsText(it) },
                label = { Text("Text der Test-SMS") },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isTestSmsTextFocused = it.isFocused }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = testEmailText,
                onValueChange = { viewModel.updateTestEmailText(it) },
                label = { Text("Text der Test-Email") },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isTestEmailTextFocused = it.isFocused }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = topBarTitle,
                onValueChange = { viewModel.updateTopBarTitle(it) },
                label = { Text("App Titel") },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isTopBarTitleFocused = it.isFocused }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Mail Screen Visibility Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Mail-Tab anzeigen",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Zeigt den Mail-Tab in der unteren Navigation an",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = mailScreenVisible,
                    onCheckedChange = { viewModel.updateMailScreenVisibility(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Phone Number Formatting Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Telefonnummern formatieren",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Formatiert Telefonnummern beim Einlesen der Kontakte",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = phoneNumberFormatting,
                    onCheckedChange = { viewModel.updatePhoneNumberFormatting(it) }
                )
            }
        }
    }

    @Composable
    private fun MmiCodeSettingsSection(
        viewModel: ContactsViewModel,
        onFocusChanged: (Boolean) -> Unit,
        sectionTitleStyle: TextStyle
    ) {
        val mmiActivatePrefix by viewModel.mmiActivatePrefix.collectAsState()
        val mmiActivateSuffix by viewModel.mmiActivateSuffix.collectAsState()
        val mmiDeactivateCode by viewModel.mmiDeactivateCode.collectAsState()
        val mmiStatusCode by viewModel.mmiStatusCode.collectAsState()

        var isActivateFocused by remember { mutableStateOf(false) }
        var isActivateSuffixFocused by remember { mutableStateOf(false) }
        var isDeactivateFocused by remember { mutableStateOf(false) }
        var isStatusFocused by remember { mutableStateOf(false) }

        LaunchedEffect(isActivateFocused, isActivateSuffixFocused, isDeactivateFocused, isStatusFocused) {
            onFocusChanged(isActivateFocused || isActivateSuffixFocused || isDeactivateFocused || isStatusFocused)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "MMI-Codes (Anrufweiterleitung)",
                style = sectionTitleStyle,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = mmiActivatePrefix,
                onValueChange = { viewModel.updateMmiActivatePrefix(it) },
                label = { Text("Aktivierungscode Prefix (z.B. *21*)") },
                placeholder = { Text("*21*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isActivateFocused = it.isFocused }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = mmiActivateSuffix,
                onValueChange = { viewModel.updateMmiActivateSuffix(it) },
                label = { Text("Aktivierungscode Suffix (z.B. #)") },
                placeholder = { Text("#") },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isActivateSuffixFocused = it.isFocused }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = mmiDeactivateCode,
                onValueChange = { viewModel.updateMmiDeactivateCode(it) },
                label = { Text("Deaktivierungscode (z.B. ##21#)") },
                placeholder = { Text("##21#") },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isDeactivateFocused = it.isFocused }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = mmiStatusCode,
                onValueChange = { viewModel.updateMmiStatusCode(it) },
                label = { Text("Statusabfrage (z.B. *#21#)") },
                placeholder = { Text("*#21#") },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isStatusFocused = it.isFocused }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        viewModel.updateMmiActivatePrefix("*21*")
                        viewModel.updateMmiActivateSuffix("**")
                        viewModel.updateMmiDeactivateCode("**21**")
                        viewModel.updateMmiStatusCode("*021**")
                    }
                ) {
                    Text("BMI/A1-Codes")
                }

                Button(
                    onClick = { viewModel.resetMmiCodesToDefault() }
                ) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = "Reset",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Standard")
                }
            }
        }
    }

    @Composable
    private fun EmailSettingsSection(
        viewModel: ContactsViewModel,
        sectionTitleStyle: TextStyle
    ) {
        val smtpHost by viewModel.smtpHost.collectAsState()
        val smtpPort by viewModel.smtpPort.collectAsState()
        val smtpUsername by viewModel.smtpUsername.collectAsState()
        val smtpPassword by viewModel.smtpPassword.collectAsState()
        var isPasswordVisible by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "E-Mail-Einstellungen",
                style = sectionTitleStyle,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = smtpHost,
                onValueChange = { viewModel.updateSmtpSettings(it, smtpPort, smtpUsername, smtpPassword) },
                label = { Text("SMTP Server") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = smtpPort.toString(),
                onValueChange = {
                    val newPort = it.toIntOrNull() ?: smtpPort
                    viewModel.updateSmtpSettings(smtpHost, newPort, smtpUsername, smtpPassword)
                },
                label = { Text("TLS Port") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = smtpUsername,
                onValueChange = { viewModel.updateSmtpSettings(smtpHost, smtpPort, it, smtpPassword) },
                label = { Text("Benutzername/Email-Adresse") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (!smtpHost.equals("smtp.world4you.com", ignoreCase = true)) {
                OutlinedTextField(
                    value = smtpPassword,
                    onValueChange = { viewModel.updateSmtpSettings(smtpHost, smtpPort, smtpUsername, it) },
                    label = { Text("Passwort") },
                    visualTransformation = if (isPasswordVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible)
                                    Icons.Default.Visibility
                                else
                                    Icons.Default.VisibilityOff,
                                contentDescription = if (isPasswordVisible)
                                    "Passwort verbergen"
                                else
                                    "Passwort anzeigen"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        viewModel.updateSmtpSettings(
                            "smtp.gmail.com",
                            587,
                            "",
                            ""
                        )
                        SnackbarManager.showSuccess("Benutzername und App-spezifisches Passwort eingeben.")
                    }
                ) {
                    Text("Gmail")
                }
                Button(
                    onClick = {
                        viewModel.updateSmtpSettings(
                            "mail.gmx.net",
                            587,
                            "",
                            ""
                        )
                        SnackbarManager.showSuccess("Email-Adresse und Passwort eingeben.")
                    }
                ) {
                    Text("GMX")
                }
            }

            Text(
                text = "Hinweis: Für Gmail wird ein App-spezifisches Passwort benötigt und für GMX muss IMAP oder POP3 in den Einstellungen aktiviert werden.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }


    // getPhoneNumber Funktion entfernt - Telefonnummer wird jetzt in SIM-Karten-Übersicht verwaltet
    // LogScreen, LogTable, LogButtons moved to presentation.ui.screens.logs/
    // InfoScreen moved to presentation.ui.screens.info/
    // PinDialog moved to presentation.ui.components.dialogs.PinDialogs
    // ChangePinDialog moved to presentation.ui.components.dialogs.PinDialogs
}

