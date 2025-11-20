package info.meuse24.smsforwarderneoA1

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.i18n.phonenumbers.PhoneNumberUtil
import info.meuse24.smsforwarderneoA1.AppContainer.prefsManager
import info.meuse24.smsforwarderneoA1.data.local.Logger
import info.meuse24.smsforwarderneoA1.data.local.PermissionHandler
import info.meuse24.smsforwarderneoA1.data.local.SharedPreferencesManager
import info.meuse24.smsforwarderneoA1.data.repository.ContactsRepositoryImpl
import info.meuse24.smsforwarderneoA1.domain.model.Contact
import info.meuse24.smsforwarderneoA1.domain.model.LogEntry
import info.meuse24.smsforwarderneoA1.presentation.state.ContactsState
import info.meuse24.smsforwarderneoA1.presentation.viewmodel.NavigationViewModel
import info.meuse24.smsforwarderneoA1.service.SmsForegroundService
import info.meuse24.smsforwarderneoA1.util.email.EmailResult
import info.meuse24.smsforwarderneoA1.util.email.EmailSender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ViewModel für die Verwaltung von Kontakten und SMS-Weiterleitungsfunktionen.
 * Implementiert DefaultLifecycleObserver für Lifecycle-bezogene Aktionen.
 */
@OptIn(FlowPreview::class)

class ContactsViewModel(
    private val application: Application,
    private val prefsManager: SharedPreferencesManager,
    private val logger: Logger
) : AndroidViewModel(application) {
    private val contactsMutex = Mutex()
    private val stateMutex = Mutex()

    // Callback for dialing MMI codes with speakerphone
    var onDialMmiCode: ((String) -> Unit)? = null

    // Callback for error notifications (NavigationViewModel integration)
    var onErrorOccurred: ((NavigationViewModel.ErrorDialogState) -> Unit)? = null

    // StateFlows with thread-safe access
    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _selectedContact = MutableStateFlow<Contact?>(null)
    val selectedContact: StateFlow<Contact?> = _selectedContact.asStateFlow()

    private val contactsStore = ContactsRepositoryImpl(prefsManager)
    private val _state = MutableStateFlow(ContactsState())

    // StateFlows für verschiedene UI-Zustände
    private val _isLoading = MutableStateFlow(false)

    private val _forwardingActive = MutableStateFlow(false)
    val forwardingActive: StateFlow<Boolean> = _forwardingActive.asStateFlow()

    private val _forwardingPhoneNumber = MutableStateFlow("")

    // filterText kept in ContactsViewModel (needed for core filter logic)
    private val _filterText = MutableStateFlow("")
    val filterText: StateFlow<String> = _filterText.asStateFlow()

    // testSmsText moved to TestUtilsViewModel (Phase 1 Step 1.1)
    // topBarTitle, navigationTarget moved to NavigationViewModel (Phase 1 Step 1.2)

    // Eigene Telefonnummer StateFlows entfernt - wird jetzt über SIM-spezifische Verwaltung abgewickelt

    private val _countryCode = MutableStateFlow("")
    val countryCode: StateFlow<String> = _countryCode.asStateFlow()

    private val _countryCodeSource = MutableStateFlow("")
    val countryCodeSource: StateFlow<String> = _countryCodeSource.asStateFlow()

    // showExitDialog, errorState moved to NavigationViewModel (Phase 1 Step 1.2)

    private val _showProgressDialog = MutableStateFlow(false)
    val showProgressDialog: StateFlow<Boolean> = _showProgressDialog.asStateFlow()

    private val _isCleaningUp = MutableStateFlow(false)

    private val _cleanupCompleted = MutableSharedFlow<Unit>()
    val cleanupCompleted = _cleanupCompleted.asSharedFlow()

    // showOwnNumberMissingDialog StateFlows entfernt - wird jetzt über SIM-Verwaltung abgewickelt
    // SIM-Nummern Dialog State moved to SimManagementViewModel (Phase 5 Step 3)

    private val _mailScreenVisible = MutableStateFlow(prefsManager.isMailScreenVisible())
    val mailScreenVisible: StateFlow<Boolean> = _mailScreenVisible.asStateFlow()

    private val _phoneNumberFormatting = MutableStateFlow(prefsManager.isPhoneNumberFormattingEnabled())
    val phoneNumberFormatting: StateFlow<Boolean> = _phoneNumberFormatting.asStateFlow()

    private val _mmiActivatePrefix = MutableStateFlow(prefsManager.getMmiActivatePrefix())
    val mmiActivatePrefix: StateFlow<String> = _mmiActivatePrefix.asStateFlow()

    private val _mmiActivateSuffix = MutableStateFlow(prefsManager.getMmiActivateSuffix())
    val mmiActivateSuffix: StateFlow<String> = _mmiActivateSuffix.asStateFlow()

    private val _mmiDeactivateCode = MutableStateFlow(prefsManager.getMmiDeactivateCode())
    val mmiDeactivateCode: StateFlow<String> = _mmiDeactivateCode.asStateFlow()

    private val _mmiStatusCode = MutableStateFlow(prefsManager.getMmiStatusCode())
    val mmiStatusCode: StateFlow<String> = _mmiStatusCode.asStateFlow()

    private val _keepForwardingOnExit = MutableStateFlow(false)
    //val keepForwardingOnExit: StateFlow<Boolean> = _keepForwardingOnExit.asStateFlow()

    private val filterMutex = Mutex() // Verhindert parallele Filteroperationen

    class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ContactsViewModel::class.java)) {
                val app = AppContainer.getApplication()
                @Suppress("UNCHECKED_CAST")
                return ContactsViewModel(
                    application = app,
                    prefsManager = AppContainer.requirePrefsManager(),
                    logger = AppContainer.requireLogger()
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    /**
     * Status der Weiterleitung
     */
    sealed class ForwardingResult {
        data object Success : ForwardingResult()
        data class Error(val message: String, val technical: String? = null) : ForwardingResult()
    }

    enum class ForwardingAction {
        ACTIVATE, DEACTIVATE, TOGGLE
    }

    suspend fun updateContacts(newContacts: List<Contact>) {
        contactsMutex.withLock {
            _contacts.value = newContacts
        }
    }
    suspend fun selectContact(contact: Contact) {
        stateMutex.withLock {
            if (_selectedContact.value != contact) {
                _selectedContact.value = contact
                // Update other dependent state
                _forwardingActive.value = true
                _forwardingPhoneNumber.value = contact.phoneNumber
                prefsManager.saveSelectedPhoneNumber(contact.phoneNumber)
            }
        }
    }

    suspend fun applyFilter(filterText: String) {
        contactsMutex.withLock {
            val filteredContacts = contactsStore.filterContacts(filterText)
            _contacts.value = filteredContacts

            // Update selected contact if necessary
            stateMutex.withLock {
                _selectedContact.value?.let { currentSelected ->
                    _selectedContact.value = filteredContacts.find {
                        it.phoneNumber == currentSelected.phoneNumber
                    }
                }
            }
        }
    }

    // Thread-safe state updates
    suspend fun updateForwardingState(active: Boolean) {
        stateMutex.withLock {
            _forwardingActive.value = active
            prefsManager.saveForwardingStatus(active)
            if (!active) {
                _selectedContact.value = null
                _forwardingPhoneNumber.value = ""
            }
        }
    }


    /**
     * Zentrale Funktion zum Verwalten des Weiterleitungsstatus
     * @param action Die gewünschte Aktion (ACTIVATE, DEACTIVATE, TOGGLE)
     * @param contact Optional: Der Kontakt für die Aktivierung
     * @param onResult Callback für das Ergebnis der Operation
     */
    private fun manageForwardingStatus(
        action: ForwardingAction,
        contact: Contact? = null,
        onResult: (ForwardingResult) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = when (action) {
                    ForwardingAction.ACTIVATE -> {
                        if (contact == null) {
                            ForwardingResult.Error("Kein Kontakt für Aktivierung ausgewählt")
                        } else {
                            // Eigenweiterleitung-Prüfung entfernt - wird jetzt über SIM-Verwaltung abgewickelt
                            withContext(Dispatchers.IO) {
                                activateForwardingInternal(contact)
                            }
                        }
                    }

                    ForwardingAction.DEACTIVATE -> {
                        withContext(Dispatchers.IO) {
                            deactivateForwardingInternal()
                        }
                    }

                    ForwardingAction.TOGGLE -> {
                        if (_forwardingActive.value) {
                            withContext(Dispatchers.IO) {
                                deactivateForwardingInternal()
                            }
                        } else if (contact != null) {
                            withContext(Dispatchers.IO) {
                                activateForwardingInternal(contact)
                            }
                        } else {
                            ForwardingResult.Error("Kein Kontakt für Toggle-Aktivierung ausgewählt")
                        }
                    }
                }

                onResult(result)

            } catch (e: Exception) {
                LoggingManager.logError(
                    component = "ContactsViewModel",
                    action = "FORWARDING_ERROR",
                    message = "Fehler beim ${action.name.lowercase()} der Weiterleitung",
                    error = e,
                    details = mapOf(
                        "action" to action.name,
                        "contact" to (contact?.name ?: "none"),
                        "error_type" to e.javaClass.simpleName
                    )
                )
                onResult(
                    ForwardingResult.Error(
                        "Fehler bei der Weiterleitung: ${e.message}",
                        e.stackTraceToString()
                    )
                )
            }
        }
    }

    /**
     * Interne Hilfsfunktion für die Aktivierung der Weiterleitung
     */
    private suspend fun activateForwardingInternal(contact: Contact): ForwardingResult {
        // Aktiviere Weiterleitung via MMI-Code mit Lautsprecher
        val activateCode = "${prefsManager.getMmiActivatePrefix()}${contact.phoneNumber}${prefsManager.getMmiActivateSuffix()}"

        // Use callback to dial code with speakerphone via MainActivity
        onDialMmiCode?.invoke(activateCode) ?: run {
            LoggingManager.logError(
                component = "ContactsViewModel",
                action = "ACTIVATE_FORWARDING",
                message = "Kein Callback für MMI-Code-Wahl gesetzt"
            )
            return ForwardingResult.Error("MMI-Code konnte nicht gesendet werden")
        }

        // Setze Status und speichere Kontakt
        withContext(Dispatchers.Main) {
            _selectedContact.value = contact
            _forwardingPhoneNumber.value = contact.phoneNumber
            _forwardingActive.value = true
        }

        prefsManager.saveSelectedPhoneNumber(contact.phoneNumber)
        prefsManager.saveForwardingStatus(true)

        // Aktualisiere Service-Notification
        updateNotification("Weiterleitung angefordert zu ${contact.name} (${contact.phoneNumber})")

        LoggingManager.logInfo(
            component = "ContactsViewModel",
            action = "REQUEST_ACTIVATE_FORWARDING",
            message = "Aktivierung der Weiterleitung angefordert",
            details = mapOf(
                "contact" to contact.name,
                "number" to contact.phoneNumber
            )
        )

        return ForwardingResult.Success
    }

    private fun updateNotification(message: String) {
        viewModelScope.launch {
            val intent = Intent(AppContainer.getApplication(), SmsForegroundService::class.java)
            intent.action = "UPDATE_NOTIFICATION"
            intent.putExtra("contentText", message)
            AppContainer.getApplication().startService(intent)
        }
    }

    /**
     * Interne Hilfsfunktion für die Deaktivierung der Weiterleitung
     */
    private suspend fun deactivateForwardingInternal(): ForwardingResult {
        val prevContact = _selectedContact.value

        // Deaktiviere Weiterleitung via MMI-Code mit Lautsprecher
        val deactivateCode = prefsManager.getMmiDeactivateCode()

        // Use callback to dial code with speakerphone via MainActivity
        onDialMmiCode?.invoke(deactivateCode) ?: run {
            LoggingManager.logError(
                component = "ContactsViewModel",
                action = "DEACTIVATE_FORWARDING",
                message = "Kein Callback für MMI-Code-Wahl gesetzt"
            )
            return ForwardingResult.Error("MMI-Code konnte nicht gesendet werden")
        }

        // Setze Status zurück
        withContext(Dispatchers.Main) {
            _selectedContact.value = null
            _forwardingActive.value = false
            _forwardingPhoneNumber.value = ""
        }

        prefsManager.clearSelection()

        // Aktualisiere Service-Notification
        updateNotification("Weiterleitung-Deaktivierung angefordert")

        LoggingManager.logInfo(
            component = "ContactsViewModel",
            action = "REQUEST_DEACTIVATE_FORWARDING",
            message = "Deaktivierung der Weiterleitung angefordert",
            details = mapOf(
                "previous_contact" to (prevContact?.name ?: "none"),
                "previous_number" to (prevContact?.phoneNumber ?: "none")
            )
        )

        return ForwardingResult.Success
    }

    private fun activateForwarding(contact: Contact, onResult: (ForwardingResult) -> Unit = {}) {
        manageForwardingStatus(ForwardingAction.ACTIVATE, contact, onResult)
    }

    private fun deactivateForwarding(onResult: (ForwardingResult) -> Unit = {}) {
        manageForwardingStatus(ForwardingAction.DEACTIVATE, onResult = onResult)
    }

    private fun toggleForwarding(contact: Contact? = null, onResult: (ForwardingResult) -> Unit = {}) {
        manageForwardingStatus(ForwardingAction.TOGGLE, contact, onResult)
    }

    // onShowExitDialog moved to NavigationViewModel (Phase 1 Step 1.2)
    // setTestContacts moved to TestUtilsViewModel (Phase 1 Step 1.1)

    fun updateKeepForwardingOnExit(keep: Boolean) {
        _keepForwardingOnExit.value = keep
        prefsManager.setKeepForwardingOnExit(keep)
    }

    fun startCleanup(keepForwarding: Boolean) {
        viewModelScope.launch {
            try {
                _isCleaningUp.value = true
                _showProgressDialog.value = true

                if (!keepForwarding) {
                    deactivateForwarding { result ->
                        when (result) {
                            is ForwardingResult.Error -> {
                                onErrorOccurred?.invoke(
                                    NavigationViewModel.ErrorDialogState.DeactivationError(result.message)
                                )
                            }

                            ForwardingResult.Success -> {
                                prefsManager.setKeepForwardingOnExit(false)
                            }
                        }
                    }
                } else {
                    _selectedContact.value?.let { contact ->
                        activateForwarding(contact) { result ->
                            when (result) {
                                is ForwardingResult.Error -> {
                                    onErrorOccurred?.invoke(
                                        NavigationViewModel.ErrorDialogState.DeactivationError(result.message)
                                    )
                                }

                                ForwardingResult.Success -> {
                                    prefsManager.setKeepForwardingOnExit(true)
                                }
                            }
                        }
                    }
                }

                _cleanupCompleted.emit(Unit)
            } catch (e: Exception) {
                onErrorOccurred?.invoke(NavigationViewModel.ErrorDialogState.GeneralError(e))
            } finally {
                _isCleaningUp.value = false
                _showProgressDialog.value = false
            }
        }
    }

    fun deactivateForwarding() {
        if (!_keepForwardingOnExit.value) {
            deactivateForwarding { result ->
                when (result) {
                    is ForwardingResult.Error -> {
                        LoggingManager.logError(
                            component = "ContactsViewModel",
                            action = "DEACTIVATE_FORWARDING",
                            message = "Fehler beim Deaktivieren der Weiterleitung",
                            details = mapOf("error" to result.message)
                        )
                    }
                    ForwardingResult.Success -> {
                        updateServiceNotification() // Hier hinzugefügt
                    }
                }
            }
        }
    }

    // showOwnNumberMissingDialog und hideOwnNumberMissingDialog Funktionen entfernt - werden über SIM-Verwaltung abgewickelt
    // SIM-Nummern Dialog Funktionen moved to SimManagementViewModel (Phase 5 Step 3)
    // ErrorDialogState, hideExitDialog, clearErrorState moved to NavigationViewModel (Phase 1 Step 1.2)


    init {
        viewModelScope.launch {
            try {
                // Beobachte Filtertext-Änderungen
                _filterText
                    .debounce(300) // Wartet 300ms nach der letzten Änderung
                    .collect { filterText ->
                        applyCurrentFilter()
                        LoggingManager.logInfo(
                            component = "ContactsViewModel",
                            action = "FILTER_APPLIED",
                            message = "Kontaktfilter angewendet",
                            details = mapOf(
                                "filter_text" to filterText,
                                "results_count" to _contacts.value.size
                            )
                        )
                    }
            } catch (e: Exception) {
                LoggingManager.logError(
                    component = "ContactsViewModel",
                    action = "FILTER_INIT_ERROR",
                    message = "Fehler bei der Initialisierung der Kontaktfilterung",
                    error = e
                )
            }
        }

        // Bestehende Initialisierung
        initialize()
    }

    // updateFilterText moved to TestUtilsViewModel (Phase 1 Step 1.1)

    fun initialize() {
        viewModelScope.launch {
            try {
                stateMutex.withLock {
                    _isLoading.value = true
                }

                // Lade gespeicherte Einstellungen
                loadSavedState()

                // Telefonnummer-Ermittlung entfernt - wird jetzt in SIM-Karten-Übersicht verwaltet

                // Initialisiere Ländercode zuerst
                initializeCountryCode()

                // Initialisiere ContactsStore
                contactsStore.initialize(
                    contentResolver = application.contentResolver,
                    countryCode = _countryCode.value
                )

                // Starte Collection in separatem Launch-Block
                viewModelScope.launch {
                    try {
                        contactsStore.contacts.collect { contactsList ->
                            updateContacts(contactsList)
                        }
                    } catch (e: Exception) {
                        LoggingManager.logError(
                            component = "ContactsViewModel",
                            action = "CONTACTS_COLLECTION_ERROR",
                            message = "Fehler beim Überwachen der Kontaktliste",
                            error = e
                        )
                    }
                }
                LoggingManager.logInfo(
                    component = "ContactsViewModel",
                    action = "INIT",
                    message = "ViewModel erfolgreich initialisiert"
                )
            } catch (e: Exception) {
                LoggingManager.logError(
                    component = "ContactsViewModel",
                    action = "INIT_ERROR",
                    message = "ViewModel Initialisierung fehlgeschlagen",
                    error = e
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadSavedState() {
        _state.update { currentState ->
            currentState.copy(
                forwardingActive = prefsManager.isForwardingActive(),
                selectedPhoneNumber = prefsManager.getSelectedPhoneNumber(),
                emailForwardingEnabled = prefsManager.isForwardSmsToEmail(),
                emailAddresses = prefsManager.getEmailAddresses()
            )
        }
        _filterText.value = prefsManager.getFilterText()
        // testSmsText now managed by TestUtilsViewModel
        // Email-related StateFlows moved to EmailViewModel
        // topBarTitle moved to NavigationViewModel
        _countryCode.value = prefsManager.getCountryCode()
        _forwardingActive.value = prefsManager.isForwardingActive()
        val savedPhoneNumber = prefsManager.getSelectedPhoneNumber()
        _forwardingPhoneNumber.value = savedPhoneNumber

        // Starte einen Coroutine-Scope um auf die Kontaktliste zu warten
        viewModelScope.launch {
            try {
                contacts.first { it.isNotEmpty() }.find { contact ->
                    PhoneSmsUtils.standardizePhoneNumber(contact.phoneNumber, _countryCode.value) ==
                            PhoneSmsUtils.standardizePhoneNumber(savedPhoneNumber, _countryCode.value)
                }?.let { foundContact ->
                    _selectedContact.value = foundContact
                    LoggingManager.logInfo(
                        component = "ContactsViewModel",
                        action = "RESTORE_CONTACT",
                        message = "Gespeicherter Kontakt wiederhergestellt",
                        details = mapOf(
                            "contact" to foundContact.name,
                            "number" to foundContact.phoneNumber,
                            "forwarding_active" to _forwardingActive.value
                        )
                    )
                }

                // Validiere den wiederhergestellten Zustand
                validateRestoredState()
            } catch (e: Exception) {
                LoggingManager.logError(
                    component = "ContactsViewModel",
                    action = "RESTORE_CONTACT_ERROR",
                    message = "Fehler beim Wiederherstellen des gespeicherten Kontakts",
                    error = e,
                    details = mapOf(
                        "saved_phone_number" to savedPhoneNumber
                    )
                )
            }
        }
    }

    private fun validateRestoredState() {
        viewModelScope.launch {
            try {
                val hasSelectedContact = _selectedContact.value != null
                val isForwarding = _forwardingActive.value

                when {
                    isForwarding && !hasSelectedContact -> {
                        _forwardingActive.value = false
                        prefsManager.saveForwardingStatus(false)
                        LoggingManager.logWarning(
                            component = "ContactsViewModel",
                            action = "VALIDATE_STATE",
                            message = "Inkonsistenter Status korrigiert",
                            details = mapOf(
                                "reason" to "no_contact_but_active"
                            )
                        )
                    }
                    !isForwarding && hasSelectedContact -> {
                        LoggingManager.logInfo(
                            component = "ContactsViewModel",
                            action = "VALIDATE_STATE",
                            message = "Kontakt beibehalten, Weiterleitung inaktiv"
                        )
                    }
                }
            } catch (e: Exception) {
                LoggingManager.logError(
                    component = "ContactsViewModel",
                    action = "VALIDATE_STATE_ERROR",
                    message = "Fehler bei der Zustandsvalidierung",
                    error = e
                )
            }
        }
    }

    private fun initializeCountryCode() {

        viewModelScope.launch {
            try {
                // 1. Erste Priorität: SIM-Karte
                val simCode = PhoneSmsUtils.getSimCardCountryCode(application)
                if (simCode.isNotEmpty()) {
                    updateCountryCode(simCode)
                    _countryCodeSource.value = "SIM-Karte"
                    LoggingManager.log(
                        LogLevel.INFO,
                        LogMetadata(
                            component = "ContactsViewModel",
                            action = "COUNTRY_CODE_INIT",
                            details = mapOf(
                                "source" to "sim",
                                "code" to simCode
                            )
                        ),
                        "Ländercode von SIM-Karte ermittelt"
                    )
                    return@launch
                }

                /*// 2. Zweite Priorität: Eigene Telefonnummer
                val ownNumber = prefsManager.getOwnPhoneNumber()
                if (ownNumber.isNotEmpty()) {
                    try {
                        val phoneUtil = PhoneNumberUtil.getInstance()
                        val number = phoneUtil.parse(ownNumber, "")
                        val detectedCode = "+${number.countryCode}"
                        if (isValidCountryCode(detectedCode)) {
                            updateCountryCode(detectedCode)
                            _countryCodeSource.value = "Eigene Telefonnummer"
                            LoggingManager.log(
                                LogLevel.INFO,
                                LogMetadata(
                                    component = "ContactsViewModel",
                                    action = "COUNTRY_CODE_INIT",
                                    details = mapOf(
                                        "source" to "own_number",
                                        "code" to detectedCode
                                    )
                                ),
                                "Ländercode aus eigener Nummer ermittelt"
                            )
                            return@launch
                        }
                    } catch (e: Exception) {
                        LoggingManager.log(
                            LogLevel.WARNING,
                            LogMetadata(
                                component = "ContactsViewModel",
                                action = "COUNTRY_CODE_DETECTION_FAILED",
                                details = mapOf(
                                    "number" to ownNumber,
                                    "error" to e.message
                                )
                            ),
                            "Fehler bei der Erkennung des Ländercodes aus eigener Nummer"
                        )
                    }
                }*/

                // 3. Fallback auf Österreich
                updateCountryCode("+43")
                _countryCodeSource.value = "Standard (Österreich)"
                LoggingManager.log(
                    LogLevel.INFO,
                    LogMetadata(
                        component = "ContactsViewModel",
                        action = "COUNTRY_CODE_INIT",
                        details = mapOf(
                            "source" to "default",
                            "code" to "+43"
                        )
                    ),
                    "Verwende Default-Ländercode: Österreich"
                )

            } catch (e: Exception) {
                LoggingManager.log(
                    LogLevel.ERROR,
                    LogMetadata(
                        component = "ContactsViewModel",
                        action = "COUNTRY_CODE_INIT_ERROR",
                        details = mapOf(
                            "error" to e.message,
                            "error_type" to e.javaClass.simpleName
                        )
                    ),
                    "Fehler bei der Ländercode-Initialisierung",
                    e
                )
                updateCountryCode("+43")
                _countryCodeSource.value = "Standard (Österreich) nach Fehler"
            }
        }
    }

    private fun isValidCountryCode(code: String): Boolean {
        return when (code) {
            "+43", // Österreich
            "+49", // Deutschland
            "+41"  // Schweiz
                -> true

            else -> false
        }
    }

    private fun updateCountryCode(code: String) {
        _countryCode.value = code
        prefsManager.saveCountryCode(code)

        // Aktualisiere auch den ContactsStore
        contactsStore.updateCountryCode(code)
    }

    // navigateToSettings, onNavigated moved to NavigationViewModel (Phase 1 Step 1.2)

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            try {
                contactsStore.cleanup()
                saveCurrentState()
                LoggingManager.log(
                    LogLevel.INFO,
                    LogMetadata(
                        component = "ContactsViewModel",
                        action = "VIEWMODEL_CLEARED",
                        details = mapOf("state" to "saved")
                    ),
                    "ViewModel wurde bereinigt"
                )
            } catch (e: Exception) {
                LoggingManager.logError(
                    component = "ContactsViewModel",
                    action = "VIEWMODEL_CLEAR_ERROR",
                    message = "Fehler beim Bereinigen des ViewModels",
                    error = e
                )
            }
        }
    }

    // updateTopBarTitle moved to NavigationViewModel (Phase 1 Step 1.2)

    // updateOwnPhoneNumber Funktion entfernt - wird jetzt über SIM-spezifische Verwaltung abgewickelt

    // loadOwnPhoneNumber Funktion entfernt - Telefonnummer wird jetzt in SIM-Karten-Übersicht verwaltet

    /**
     * Speichert den aktuellen Zustand der App.
     */
    fun saveCurrentState() {
        viewModelScope.launch {
            try {
                val currentContact = _selectedContact.value
                val isActive = _forwardingActive.value

                // Speichere Status und Nummer zusammen
                if (currentContact != null && isActive) {
                    LoggingManager.logInfo(
                        component = "ContactsViewModel",
                        action = "SAVE_STATE",
                        message = "Speichere aktiven Weiterleitungskontakt",
                        details = mapOf(
                            "contact" to currentContact.name,
                            "number" to currentContact.phoneNumber,
                            "is_active" to true
                        )
                    )
                    prefsManager.saveSelectedPhoneNumber(currentContact.phoneNumber)
                    prefsManager.saveForwardingStatus(true)
                } else {
                    LoggingManager.logInfo(
                        component = "ContactsViewModel",
                        action = "SAVE_STATE",
                        message = "Keine aktive Weiterleitung zu speichern",
                        details = mapOf(
                            "has_contact" to (currentContact != null),
                            "is_active" to isActive
                        )
                    )
                    prefsManager.clearSelection()
                    prefsManager.saveForwardingStatus(false)
                }

                // Rest der Einstellungen speichern
                prefsManager.saveFilterText(_filterText.value)
                // testSmsText now managed by TestUtilsViewModel
                // Email settings now saved in EmailViewModel
            } catch (e: Exception) {
                LoggingManager.logError(
                    component = "ContactsViewModel",
                    action = "SAVE_STATE_ERROR",
                    message = "Fehler beim Speichern des aktuellen Zustands",
                    error = e,
                    details = mapOf(
                        "has_selected_contact" to (_selectedContact.value != null),
                        "forwarding_active" to _forwardingActive.value
                    )
                )
            }
        }
    }

    /**
     * Wechselt die Auswahl eines Kontakts mit Prüfung auf Eigenweiterleitung.
     */
    fun toggleContactSelection(contact: Contact) {
        // Eigentelefonnummer-Prüfung entfernt - wird jetzt über SIM-Verwaltung abgewickelt

        viewModelScope.launch {
            try {
                // Thread-safe Zugriff auf selectedContact innerhalb der Coroutine
                val currentSelected = stateMutex.withLock {
                    _selectedContact.value
                }

                // Vergleich der normalisierten Nummern statt der Kontaktobjekte
                if (currentSelected != null &&
                    contact.phoneNumber.filter { it.isDigit() } ==
                    currentSelected.phoneNumber.filter { it.isDigit() }
                ) {

                    LoggingManager.logInfo(
                        component = "ContactsViewModel",
                        action = "TOGGLE_CONTACT",
                        message = "Toggle bestehende Weiterleitung",
                        details = mapOf(
                            "contact" to contact.name,
                            "number" to contact.phoneNumber,
                            "current_state" to _forwardingActive.value
                        )
                    )

                    // Nutze toggleForwarding für konsistente Status-Verwaltung
                    toggleForwarding(contact) { result ->
                        when (result) {
                            is ForwardingResult.Success -> {
                                LoggingManager.logInfo(
                                    component = "ContactsViewModel",
                                    action = "TOGGLE_SUCCESS",
                                    message = "Weiterleitung erfolgreich umgeschaltet",
                                    details = mapOf(
                                        "new_state" to _forwardingActive.value
                                    )
                                )
                            }

                            is ForwardingResult.Error -> {
                                LoggingManager.logError(
                                    component = "ContactsViewModel",
                                    action = "TOGGLE_ERROR",
                                    message = "Fehler beim Umschalten der Weiterleitung",
                                    details = mapOf(
                                        "error" to result.message,
                                        "contact" to contact.name
                                    )
                                )
                                SnackbarManager.showError(result.message)
                            }
                        }
                    }
                    updateServiceNotification()
                    return@launch
                }

                // Neuen Kontakt aktivieren
                activateForwarding(contact) { result ->
                    when (result) {
                        is ForwardingResult.Success -> {
                            LoggingManager.logInfo(
                                component = "ContactsViewModel",
                                action = "REQUEST_SWITCH_CONTACT",
                                message = "Umschaltung der Weiterleitung angefordert",
                                details = mapOf(
                                    "previous_contact" to (currentSelected?.name ?: "none"),
                                    "new_contact" to contact.name
                                )
                            )
                            if (currentSelected != null) {
                                SnackbarManager.showSuccess(
                                    "Umschaltung der Weiterleitung von ${currentSelected.name} zu ${contact.name} wurde angefordert"
                                )
                            } else {
                                SnackbarManager.showSuccess(
                                    "Aktivierung der Weiterleitung zu ${contact.name} wurde angefordert"
                                )
                            }
                        }

                        is ForwardingResult.Error -> {
                            LoggingManager.logError(
                                component = "ContactsViewModel",
                                action = "SWITCH_CONTACT",
                                message = "Fehler beim Umschalten der Weiterleitung",
                                details = mapOf(
                                    "previous_contact" to (currentSelected?.name ?: "none"),
                                    "new_contact" to contact.name,
                                    "error" to result.message
                                )
                            )
                            SnackbarManager.showError(
                                "Fehler beim ${if (currentSelected != null) "Umschalten" else "Aktivieren"} " +
                                        "der Weiterleitung: ${result.message}"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                LoggingManager.logError(
                    component = "ContactsViewModel",
                    action = "TOGGLE_CONTACT_ERROR",
                    message = "Unerwarteter Fehler beim Umschalten der Kontaktauswahl",
                    error = e,
                    details = mapOf(
                        "contact" to contact.name,
                        "contact_number" to contact.phoneNumber
                    )
                )
                SnackbarManager.showError("Fehler beim Umschalten der Kontaktauswahl: ${e.message}")
            }
        }
    }

    // Optimierte applyCurrentFilter mit Mutex
    suspend fun applyCurrentFilter() {
        filterMutex.withLock {
            val startTime = System.currentTimeMillis()
            _isLoading.value = true
            try {
                val filteredContacts = contactsStore.filterContacts(_filterText.value)
                _contacts.value = filteredContacts

                LoggingManager.log(
                    LogLevel.DEBUG,
                    LogMetadata(
                        component = "ContactsViewModel",
                        action = "APPLY_FILTER",
                        details = mapOf(
                            "duration_ms" to (System.currentTimeMillis() - startTime),
                            "filter_text" to _filterText.value,
                            "results_count" to filteredContacts.size,
                            "total_contacts" to contactsStore.contacts.value.size
                        )
                    ),
                    "Kontaktfilter angewendet"
                )

                // Update selected contact if necessary
                _selectedContact.value?.let { tempContact ->
                    _selectedContact.value = filteredContacts.find {
                        it.phoneNumber == tempContact.phoneNumber
                    }
                    if (_selectedContact.value == null) {
                        LoggingManager.log(
                            LogLevel.INFO,
                            LogMetadata(
                                component = "ContactsViewModel",
                                action = "SELECTED_CONTACT_FILTERED",
                                details = mapOf(
                                    "contact_number" to tempContact.phoneNumber
                                )
                            ),
                            "Ausgewählter Kontakt nicht mehr in gefilterter Liste"
                        )
                    }
                }
            } catch (e: Exception) {
                LoggingManager.log(
                    LogLevel.ERROR,
                    LogMetadata(
                        component = "ContactsViewModel",
                        action = "FILTER_ERROR",
                        details = mapOf(
                            "error" to e.message,
                            "filter_text" to _filterText.value
                        )
                    ),
                    "Fehler bei Anwendung des Kontaktfilters",
                    e
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    // updateTestSmsText moved to TestUtilsViewModel (Phase 1 Step 1.1)

    /**
     * Aktualisiert den Filter-Text für die Kontaktsuche.
     * Triggert automatisch die Filterung durch den debounce-Mechanismus im init Block.
     */
    fun updateFilterText(newFilter: String) {
        _filterText.value = newFilter
        prefsManager.saveFilterText(newFilter)
    }

    fun updateMailScreenVisibility(visible: Boolean) {
        _mailScreenVisible.value = visible
        prefsManager.setMailScreenVisible(visible)

        LoggingManager.logInfo(
            component = "ContactsViewModel",
            action = if (visible) "SHOW_MAIL_SCREEN" else "HIDE_MAIL_SCREEN",
            message = "Mail Screen ${if (visible) "angezeigt" else "ausgeblendet"}",
            details = mapOf(
                "mail_screen_visible" to visible
            )
        )
    }

    fun updatePhoneNumberFormatting(enabled: Boolean) {
        _phoneNumberFormatting.value = enabled
        prefsManager.setPhoneNumberFormatting(enabled)

        LoggingManager.logInfo(
            component = "ContactsViewModel",
            action = if (enabled) "ENABLE_PHONE_FORMATTING" else "DISABLE_PHONE_FORMATTING",
            message = "Telefonnummern-Formatierung ${if (enabled) "aktiviert" else "deaktiviert"}",
            details = mapOf(
                "phone_formatting_enabled" to enabled
            )
        )

        // Kontakte nach Änderung der Formatierung neu einlesen
        viewModelScope.launch {
            try {
                LoggingManager.logInfo(
                    component = "ContactsViewModel",
                    action = "RELOAD_CONTACTS_AFTER_FORMATTING_CHANGE",
                    message = "Kontakte werden nach Formatierungs-Änderung neu geladen"
                )

                // Kontakte neu laden mit aktuellem ContentResolver
                contactsStore.loadContacts(application.contentResolver)
            } catch (e: Exception) {
                LoggingManager.logError(
                    component = "ContactsViewModel",
                    action = "RELOAD_CONTACTS_ERROR",
                    message = "Fehler beim Neuladen der Kontakte nach Formatierungs-Änderung",
                    details = mapOf(
                        "error" to e.message,
                        "error_type" to e.javaClass.simpleName
                    )
                )
            }
        }
    }

    fun updateMmiActivatePrefix(prefix: String) {
        _mmiActivatePrefix.value = prefix
        prefsManager.setMmiActivatePrefix(prefix)

        LoggingManager.logInfo(
            component = "ContactsViewModel",
            action = "UPDATE_MMI_ACTIVATE_PREFIX",
            message = "MMI Aktivierungscode geändert",
            details = mapOf(
                "new_prefix" to prefix
            )
        )
    }

    fun updateMmiActivateSuffix(suffix: String) {
        _mmiActivateSuffix.value = suffix
        prefsManager.setMmiActivateSuffix(suffix)

        LoggingManager.logInfo(
            component = "ContactsViewModel",
            action = "UPDATE_MMI_ACTIVATE_SUFFIX",
            message = "MMI Aktivierungssuffix geändert",
            details = mapOf(
                "new_suffix" to suffix
            )
        )
    }

    fun updateMmiDeactivateCode(code: String) {
        _mmiDeactivateCode.value = code
        prefsManager.setMmiDeactivateCode(code)

        LoggingManager.logInfo(
            component = "ContactsViewModel",
            action = "UPDATE_MMI_DEACTIVATE_CODE",
            message = "MMI Deaktivierungscode geändert",
            details = mapOf(
                "new_code" to code
            )
        )
    }

    fun updateMmiStatusCode(code: String) {
        _mmiStatusCode.value = code
        prefsManager.setMmiStatusCode(code)

        LoggingManager.logInfo(
            component = "ContactsViewModel",
            action = "UPDATE_MMI_STATUS_CODE",
            message = "MMI Statusabfrage-Code geändert",
            details = mapOf(
                "new_code" to code
            )
        )
    }

    fun queryForwardingStatus() {
        val statusCode = prefsManager.getMmiStatusCode()

        // Use callback to dial status code with speakerphone via MainActivity
        onDialMmiCode?.invoke(statusCode) ?: run {
            LoggingManager.logError(
                component = "ContactsViewModel",
                action = "QUERY_FORWARDING_STATUS",
                message = "Kein Callback für MMI-Code-Wahl gesetzt"
            )
            SnackbarManager.showError("Statusabfrage konnte nicht gesendet werden")
            return
        }

        LoggingManager.logInfo(
            component = "ContactsViewModel",
            action = "QUERY_FORWARDING_STATUS",
            message = "Statusabfrage angefordert",
            details = mapOf(
                "status_code" to statusCode
            )
        )

        SnackbarManager.showInfo("Statusabfrage wird gesendet")
    }

    fun resetMmiCodesToDefault() {
        _mmiActivatePrefix.value = prefsManager.run {
            resetMmiCodesToDefault()
            getMmiActivatePrefix()
        }
        _mmiActivateSuffix.value = prefsManager.getMmiActivateSuffix()
        _mmiDeactivateCode.value = prefsManager.getMmiDeactivateCode()
        _mmiStatusCode.value = prefsManager.getMmiStatusCode()

        LoggingManager.logInfo(
            component = "ContactsViewModel",
            action = "RESET_MMI_CODES",
            message = "MMI-Codes auf Standardwerte zurückgesetzt",
            details = mapOf(
                "activate_prefix" to _mmiActivatePrefix.value,
                "activate_suffix" to _mmiActivateSuffix.value,
                "deactivate_code" to _mmiDeactivateCode.value
            )
        )
    }

    fun updateServiceNotification() {
        val status = buildString {
            val hasForwarding = _forwardingActive.value
            val hasEmail = prefsManager.isForwardSmsToEmail()  // Get from prefs (EmailViewModel manages this)

            when {
                // Beide aktiv
                hasForwarding && hasEmail -> {
                    append("SMS-Weiterleitung aktiv")
                    _selectedContact.value?.let { contact ->
                        append(" zu ${contact.name}")
                    }
                    append("\nEmail-Weiterleitung aktiv")
                    val emailCount = prefsManager.getEmailAddresses().size  // Get from prefs
                    append(" an $emailCount Email(s)")
                }
                // Nur SMS-Weiterleitung
                hasForwarding -> {
                    append("SMS-Weiterleitung aktiv")
                    _selectedContact.value?.let { contact ->
                        append(" zu ${contact.name}")
                    }
                }
                // Nur Email-Weiterleitung
                hasEmail -> {
                    append("Email-Weiterleitung aktiv")
                    val emailCount = prefsManager.getEmailAddresses().size  // Get from prefs
                    append(" an $emailCount Email(s)")
                }
                // Keine Weiterleitung aktiv
                else -> {
                    append("TEL/SMS Forwarder läuft im Hintergrund.")
                }
            }
        }

        viewModelScope.launch {
            try {
                val intent = Intent(AppContainer.getApplication(), SmsForegroundService::class.java)
                intent.action = "UPDATE_NOTIFICATION"
                intent.putExtra("contentText", status)
                AppContainer.getApplication().startService(intent)
            } catch (e: Exception) {
                LoggingManager.logError(
                    component = "ContactsViewModel",
                    action = "UPDATE_NOTIFICATION_ERROR",
                    message = "Fehler beim Aktualisieren der Service-Benachrichtigung",
                    error = e,
                    details = mapOf(
                        "status_text" to status
                    )
                )
            }
        }
    }

    // sendTestSms moved to TestUtilsViewModel (Phase 1 Step 1.1)
    // ContactsStore extracted to data/repository/ContactsRepositoryImpl.kt (Phase 1 Step 1.3)
}

