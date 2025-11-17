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
import info.meuse24.smsforwarderneoA1.presentation.ui.screens.settings.SettingsScreen
import info.meuse24.smsforwarderneoA1.presentation.ui.screens.home.HomeScreen
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
                        val currentCallState = callState.collectAsState()
                        HomeScreen(viewModel, currentCallState)
                    }
                    composable("mail") {
                        MailScreen(viewModel)
                    }
                    composable("setup") {
                        SettingsScreen(viewModel)
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

    // getPhoneNumber Funktion entfernt - Telefonnummer wird jetzt in SIM-Karten-Übersicht verwaltet
    // LogScreen, LogTable, LogButtons moved to presentation.ui.screens.logs/
    // InfoScreen moved to presentation.ui.screens.info/
    // PinDialog moved to presentation.ui.components.dialogs.PinDialogs
    // ChangePinDialog moved to presentation.ui.components.dialogs.PinDialogs
}

