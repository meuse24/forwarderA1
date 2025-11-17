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
import info.meuse24.smsforwarderneoA1.domain.model.Contact
import info.meuse24.smsforwarderneoA1.domain.model.LogEntry
import info.meuse24.smsforwarderneoA1.presentation.state.ContactsState
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

    // Noise Actions: Log-Einträge, die standardmäßig ausgeblendet werden
    companion object {
        private val NOISE_ACTIONS = setOf(
            "HEARTBEAT",
            "UPDATE_NOTIFICATION",
            "LOAD_CONTACTS",
            "LOAD_CONTACTS_START",
            "CONTACTS_RELOAD",
            "FILTER_CONTACTS",
            "FILTER_APPLIED",
            "GET_PREFERENCE",
            "SET_PREFERENCE",
            "SAVE_STATE",
            "VALIDATE_STATE",
            "REGISTER_OBSERVER",
            "UNREGISTER_OBSERVER",
            "WAKE_LOCK_ACQUIRED",
            "WAKE_LOCK_RELEASED",
            "LOW_MEMORY",
            "TRIM_MEMORY",
            "CONFIG_CHANGED",
            "CONTACTS_CHANGED_SKIPPED",
            "APPLY_FILTER"
        )
    }

    // Callback for dialing MMI codes with speakerphone
    var onDialMmiCode: ((String) -> Unit)? = null

    // StateFlows with thread-safe access
    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _selectedContact = MutableStateFlow<Contact?>(null)
    val selectedContact: StateFlow<Contact?> = _selectedContact.asStateFlow()

    private val contactsStore = ContactsStore()
    private val _state = MutableStateFlow(ContactsState())

    // StateFlows für verschiedene UI-Zustände
    private val _isLoading = MutableStateFlow(false)

    private val _forwardingActive = MutableStateFlow(false)
    val forwardingActive: StateFlow<Boolean> = _forwardingActive.asStateFlow()

    private val _forwardingPhoneNumber = MutableStateFlow("")

    private val _filterText = MutableStateFlow("")
    val filterText: StateFlow<String> = _filterText

    private val _logEntriesHtml = MutableStateFlow("")
    val logEntriesHtml: StateFlow<String> = _logEntriesHtml

    private val _logEntries = MutableStateFlow<List<LogEntry>>(emptyList())
    val logEntries: StateFlow<List<LogEntry>> = _logEntries

    private val _testSmsText = MutableStateFlow("")
    val testSmsText: StateFlow<String> = _testSmsText

    // Eigene Telefonnummer StateFlows entfernt - wird jetzt über SIM-spezifische Verwaltung abgewickelt

    private val _topBarTitle = MutableStateFlow("")
    val topBarTitle: StateFlow<String> = _topBarTitle

    private val _navigationTarget = MutableStateFlow<String?>(null)
    val navigationTarget: StateFlow<String?> = _navigationTarget.asStateFlow()

    private val _countryCode = MutableStateFlow("")
    val countryCode: StateFlow<String> = _countryCode.asStateFlow()

    private val _countryCodeSource = MutableStateFlow("")
    val countryCodeSource: StateFlow<String> = _countryCodeSource.asStateFlow()

    private val _showExitDialog = MutableStateFlow(false)
    val showExitDialog: StateFlow<Boolean> = _showExitDialog.asStateFlow()

    private val _showProgressDialog = MutableStateFlow(false)
    val showProgressDialog: StateFlow<Boolean> = _showProgressDialog.asStateFlow()

    private val _emailAddresses = MutableStateFlow<List<String>>(emptyList())
    val emailAddresses: StateFlow<List<String>> = _emailAddresses.asStateFlow()

    private val _newEmailAddress = MutableStateFlow("")
    val newEmailAddress: StateFlow<String> = _newEmailAddress.asStateFlow()

    private val _errorState = MutableStateFlow<ErrorDialogState?>(null)
    val errorState: StateFlow<ErrorDialogState?> = _errorState.asStateFlow()

    private val _isCleaningUp = MutableStateFlow(false)

    private val _cleanupCompleted = MutableSharedFlow<Unit>()
    val cleanupCompleted = _cleanupCompleted.asSharedFlow()

    // showOwnNumberMissingDialog StateFlows entfernt - wird jetzt über SIM-Verwaltung abgewickelt

    // SIM-Nummern Dialog State
    private val _missingSims = MutableStateFlow<List<PhoneSmsUtils.SimInfo>>(emptyList())
    val missingSims: StateFlow<List<PhoneSmsUtils.SimInfo>> = _missingSims.asStateFlow()

    private val _showSimNumbersDialog = MutableStateFlow(false)
    val showSimNumbersDialog: StateFlow<Boolean> = _showSimNumbersDialog.asStateFlow()

    private val _forwardSmsToEmail = MutableStateFlow(prefsManager.isForwardSmsToEmail())
    val forwardSmsToEmail: StateFlow<Boolean> = _forwardSmsToEmail.asStateFlow()

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

    private val _showAllLogs = MutableStateFlow(false)  // Default: nur wichtige Logs
    val showAllLogs: StateFlow<Boolean> = _showAllLogs.asStateFlow()

    private val _mmiStatusCode = MutableStateFlow(prefsManager.getMmiStatusCode())
    val mmiStatusCode: StateFlow<String> = _mmiStatusCode.asStateFlow()

    private val _keepForwardingOnExit = MutableStateFlow(false)
    //val keepForwardingOnExit: StateFlow<Boolean> = _keepForwardingOnExit.asStateFlow()

    private val filterMutex = Mutex() // Verhindert parallele Filteroperationen

    private val _smtpHost = MutableStateFlow(prefsManager.getSmtpHost())
    val smtpHost: StateFlow<String> = _smtpHost.asStateFlow()

    private val _smtpPort = MutableStateFlow(prefsManager.getSmtpPort())
    val smtpPort: StateFlow<Int> = _smtpPort.asStateFlow()

    private val _smtpUsername = MutableStateFlow(prefsManager.getSmtpUsername())
    val smtpUsername: StateFlow<String> = _smtpUsername.asStateFlow()

    private val _smtpPassword = MutableStateFlow(prefsManager.getSmtpPassword())
    val smtpPassword: StateFlow<String> = _smtpPassword.asStateFlow()

    private val _testEmailText = MutableStateFlow("")
    val testEmailText: StateFlow<String> = _testEmailText.asStateFlow()

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

    fun onShowExitDialog() {
        _showExitDialog.value = true
    }

    fun updateNewEmailAddress(email: String) {
        _newEmailAddress.value = email
    }

    fun addEmailAddress() {
        val email = _newEmailAddress.value.trim()
        if (email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            viewModelScope.launch {
                try {
                    val currentList = _emailAddresses.value.toMutableList()
                    if (!currentList.contains(email)) {
                        currentList.add(email)
                        _emailAddresses.value = currentList
                        prefsManager.saveEmailAddresses(currentList)
                        _newEmailAddress.value = "" // Reset input field
                        SnackbarManager.showSuccess("E-Mail-Adresse hinzugefügt")
                    } else {
                        SnackbarManager.showWarning("E-Mail-Adresse existiert bereits")
                    }
                } catch (e: Exception) {
                    LoggingManager.logError(
                        component = "ContactsViewModel",
                        action = "ADD_EMAIL_ERROR",
                        message = "Fehler beim Hinzufügen der E-Mail-Adresse",
                        error = e,
                        details = mapOf("email" to email)
                    )
                    SnackbarManager.showError("Fehler beim Hinzufügen der E-Mail-Adresse: ${e.message}")
                }
            }
        } else {
            SnackbarManager.showError("Ungültige E-Mail-Adresse")
        }
    }

    fun removeEmailAddress(email: String) {
        viewModelScope.launch {
            try {
                val currentList = _emailAddresses.value.toMutableList()
                currentList.remove(email)
                _emailAddresses.value = currentList
                prefsManager.saveEmailAddresses(currentList)

                // Wenn die Liste leer ist, deaktiviere die SMS-Email-Weiterleitung
                if (currentList.isEmpty() && _forwardSmsToEmail.value) {
                    _forwardSmsToEmail.value = false
                    prefsManager.setForwardSmsToEmail(false)
                    LoggingManager.log(
                        LogLevel.INFO,
                        LogMetadata(
                            component = "ContactsViewModel",
                            action = "SMS_EMAIL_FORWARD_AUTO_DISABLE",
                            details = mapOf(
                                "reason" to "no_email_addresses"
                            )
                        ),
                        "SMS-E-Mail-Weiterleitung automatisch deaktiviert (keine E-Mail-Adressen vorhanden)"
                    )
                    SnackbarManager.showInfo("SMS-E-Mail-Weiterleitung wurde deaktiviert, da keine E-Mail-Adressen mehr vorhanden sind")
                }

                LoggingManager.log(
                    LogLevel.INFO,
                    LogMetadata(
                        component = "ContactsViewModel",
                        action = "REMOVE_EMAIL",
                        details = mapOf(
                            "remaining_emails" to currentList.size,
                            "forwarding_status" to _forwardSmsToEmail.value
                        )
                    ),
                    "E-Mail-Adresse entfernt"
                )
                SnackbarManager.showSuccess("E-Mail-Adresse entfernt")
            } catch (e: Exception) {
                LoggingManager.logError(
                    component = "ContactsViewModel",
                    action = "REMOVE_EMAIL_ERROR",
                    message = "Fehler beim Entfernen der E-Mail-Adresse",
                    error = e,
                    details = mapOf(
                        "email" to email,
                        "current_list_size" to _emailAddresses.value.size
                    )
                )
                SnackbarManager.showError("Fehler beim Entfernen der E-Mail-Adresse: ${e.message}")
            }
        }
    }

    fun updateTestEmailText(newText: String) {
        _testEmailText.value = newText
        prefsManager.saveTestEmailText(newText)

        LoggingManager.log(
            LogLevel.DEBUG,
            LogMetadata(
                component = "ContactsViewModel",
                action = "UPDATE_TEST_EMAIL",
                details = mapOf(
                    "old_length" to _testEmailText.value.length,
                    "new_length" to newText.length,
                    "is_empty" to newText.isEmpty()
                )
            ),
            "Test-Email Text aktualisiert"
        )
    }

    fun updateSmtpSettings(
        host: String,
        port: Int,
        username: String,
        password: String
    ) {
        _smtpHost.value = host
        _smtpPort.value = port
        _smtpUsername.value = username
        _smtpPassword.value = password
        prefsManager.saveSmtpSettings(host, port, username, password)
    }

    fun sendTestEmail(mailrecipent: String) {
        viewModelScope.launch {
            try {
                // Hole SMTP-Einstellungen aus SharedPreferences
                val host = prefsManager.getSmtpHost()
                val port = prefsManager.getSmtpPort()
                val username = prefsManager.getSmtpUsername()
                val password = prefsManager.getSmtpPassword()
                val testEmailText = prefsManager.getTestEmailText()

                // Prüfe ob alle erforderlichen SMTP-Einstellungen vorhanden sind
                if (host.isEmpty() || username.isEmpty() || password.isEmpty()) {
                    LoggingManager.log(
                        LogLevel.WARNING,
                        LogMetadata(
                            component = "ContactsViewModel",
                            action = "TEST_EMAIL",
                            details = mapOf(
                                "error" to "incomplete_smtp_settings",
                                "has_host" to host.isNotEmpty(),
                                "has_username" to username.isNotEmpty()
                            )
                        ),
                        "Unvollständige SMTP-Einstellungen"
                    )
                    SnackbarManager.showError("SMTP-Einstellungen sind unvollständig")
                    return@launch
                }

                val emailSender = EmailSender(host, port, username, password)

                // Erstelle formatierten Email-Text mit Timestamp
                val emailBody = buildString {
                    append("Test-Email von SMS Forwarder\n\n")
                    append("Zeitpunkt: ${getCurrentTimestamp()}\n\n")
                    append("Nachricht:\n")
                    append(testEmailText)
                    append("\n\nDies ist eine Test-Email zur Überprüfung der Email-Weiterleitungsfunktion.")
                }

                when (val result = emailSender.sendEmail(
                    to = listOf(mailrecipent),
                    subject = "SMS Forwarder Test E-Mail",
                    body = emailBody
                )) {
                    is EmailResult.Success -> {
                        LoggingManager.log(
                            LogLevel.INFO,
                            LogMetadata(
                                component = "ContactsViewModel",
                                action = "TEST_EMAIL_SENT",
                                details = mapOf(
                                    "recipient" to mailrecipent,
                                    "smtp_host" to host,
                                    "text_length" to testEmailText.length
                                )
                            ),
                            "Test-E-Mail wurde versendet"
                        )
                        SnackbarManager.showSuccess("Test-E-Mail wurde an $mailrecipent versendet")
                    }

                    is EmailResult.Error -> {
                        LoggingManager.log(
                            LogLevel.ERROR,
                            LogMetadata(
                                component = "ContactsViewModel",
                                action = "TEST_EMAIL_FAILED",
                                details = mapOf(
                                    "error" to result.message,
                                    "smtp_host" to host,
                                    "recipient" to mailrecipent
                                )
                            ),
                            "Fehler beim Versenden der Test-E-Mail"
                        )
                        SnackbarManager.showError("E-Mail-Versand fehlgeschlagen: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                LoggingManager.log(
                    LogLevel.ERROR,
                    LogMetadata(
                        component = "ContactsViewModel",
                        action = "TEST_EMAIL_ERROR",
                        details = mapOf(
                            "error" to e.message,
                            "recipient" to mailrecipent
                        )
                    ),
                    "Unerwarteter Fehler beim E-Mail-Versand"
                )
                SnackbarManager.showError("E-Mail-Versand fehlgeschlagen: ${e.message}")
            }
        }
    }

    private fun getCurrentTimestamp(): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }

    @VisibleForTesting
    fun setTestContacts(contacts: List<Contact>) = viewModelScope.launch {
        try {
            // Warten bis der Store initialisiert ist
            contactsStore.setTestContacts(contacts)
            // Warten bis die Änderungen übernommen wurden
            delay(500)
            withContext(Dispatchers.IO) {
                // Verify contacts are set
                val contactsList = contactsStore.contacts.first()
                if (contactsList.isEmpty()) {
                    throw AssertionError("Failed to initialize test contacts")
                }
            }
        } catch (e: Exception) {
            LoggingManager.logError(
                component = "ContactsViewModel",
                action = "SET_TEST_CONTACTS_ERROR",
                message = "Fehler beim Setzen der Test-Kontakte",
                error = e,
                details = mapOf(
                    "contacts_count" to contacts.size
                )
            )
            throw e // Re-throw for test visibility
        }
    }

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
                                _errorState.value =
                                    ErrorDialogState.DeactivationError(result.message)
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
                                    _errorState.value =
                                        ErrorDialogState.DeactivationError(result.message)
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
                _errorState.value = ErrorDialogState.GeneralError(e)
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

    // SIM-Nummern Dialog Funktionen
    fun requestMissingSimNumbers(sims: List<PhoneSmsUtils.SimInfo>) {
        _missingSims.value = sims
        _showSimNumbersDialog.value = true
    }

    fun hideSimNumbersDialog() {
        _showSimNumbersDialog.value = false
        _missingSims.value = emptyList()
    }

    fun saveSimNumber(subscriptionId: Int, phoneNumber: String) {
        try {
            if (phoneNumber.isNotBlank()) {
                prefsManager.setSimPhoneNumber(subscriptionId, phoneNumber.trim())
                LoggingManager.logInfo(
                    component = "ContactsViewModel",
                    action = "SAVE_SIM_NUMBER",
                    message = "SIM-Nummer vom User gespeichert",
                    details = mapOf(
                        "subscription_id" to subscriptionId,
                        "number_length" to phoneNumber.length
                    )
                )
            }
        } catch (e: Exception) {
            LoggingManager.logError(
                component = "ContactsViewModel",
                action = "SAVE_SIM_NUMBER",
                message = "Fehler beim Speichern der SIM-Nummer",
                error = e,
                details = mapOf("subscription_id" to subscriptionId)
            )
        }
    }

    sealed class ErrorDialogState {
        data class DeactivationError(val message: String) : ErrorDialogState()
        data object TimeoutError : ErrorDialogState()
        data class GeneralError(val error: Exception) : ErrorDialogState()
    }

    fun hideExitDialog() {
        _showExitDialog.value = false
    }

    fun clearErrorState() {
        _errorState.value = null
    }


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

    fun updateFilterText(newFilter: String) {
        _filterText.value = newFilter
        viewModelScope.launch {
            try {
                applyCurrentFilter()  // Direkter Aufruf für sofortige Aktualisierung
            } catch (e: Exception) {
                LoggingManager.logError(
                    component = "ContactsViewModel",
                    action = "FILTER_UPDATE_ERROR",
                    message = "Fehler beim Aktualisieren des Kontaktfilters",
                    error = e,
                    details = mapOf(
                        "filter_text" to newFilter
                    )
                )
            }
        }
        prefsManager.saveFilterText(newFilter)
    }

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
        _testSmsText.value = prefsManager.getTestSmsText()
        _testEmailText.value = prefsManager.getTestEmailText()
        // _ownPhoneNumber.value = prefsManager.getOwnPhoneNumber() // entfernt
        _topBarTitle.value = prefsManager.getTopBarTitle()
        _smtpHost.value = prefsManager.getSmtpHost()
        _smtpPort.value = prefsManager.getSmtpPort()
        _smtpUsername.value = prefsManager.getSmtpUsername()
        _smtpPassword.value = prefsManager.getSmtpPassword()
        _countryCode.value = prefsManager.getCountryCode()
        _forwardSmsToEmail.value = prefsManager.isForwardSmsToEmail()
        _emailAddresses.value = prefsManager.getEmailAddresses()
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

    fun navigateToSettings() {
        _navigationTarget.value = "setup"
    }

    fun onNavigated() {
        _navigationTarget.value = null
    }

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

    fun reloadLogs() {
        viewModelScope.launch {
            try {
                val showAll = _showAllLogs.value
                _logEntriesHtml.value = logger.getLogEntriesHtml(
                    filterNoise = !showAll,
                    noiseActions = NOISE_ACTIONS
                )
                _logEntries.value = logger.getLogEntriesAsList()
            } catch (e: Exception) {
                LoggingManager.logError(
                    component = "ContactsViewModel",
                    action = "RELOAD_LOGS_ERROR",
                    message = "Fehler beim Neuladen der Log-Einträge",
                    error = e
                )
            }
        }
    }

    fun toggleLogFilter() {
        _showAllLogs.value = !_showAllLogs.value
        reloadLogs()  // Logs mit neuem Filter neu laden
    }

    fun updateTopBarTitle(title: String) {
        _topBarTitle.value = title
        prefsManager.saveTopBarTitle(title)
    }

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
                prefsManager.saveTestSmsText(_testSmsText.value)
                prefsManager.saveEmailAddresses(_emailAddresses.value)
                prefsManager.setForwardSmsToEmail(_forwardSmsToEmail.value)
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

        val currentSelected = _selectedContact.value

        viewModelScope.launch {
            try {
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

    /**
     * Aktualisiert den Text für Test-SMS.
     */
    fun updateTestSmsText(newText: String) {
        val oldText = _testSmsText.value
        _testSmsText.value = newText
        prefsManager.saveTestSmsText(newText)

        if (oldText != newText) {
            LoggingManager.log(
                LogLevel.DEBUG,
                LogMetadata(
                    component = "ContactsViewModel",
                    action = "UPDATE_TEST_SMS",
                    details = mapOf(
                        "old_length" to oldText.length,
                        "new_length" to newText.length,
                        "is_empty" to newText.isEmpty()
                    )
                ),
                "Test-SMS Text aktualisiert"
            )
        }
    }

    fun updateForwardSmsToEmail(enabled: Boolean) {
        _forwardSmsToEmail.value = enabled
        prefsManager.setForwardSmsToEmail(enabled)
        updateServiceNotification()

        LoggingManager.logInfo(
            component = "ContactsViewModel",
            action = if (enabled) "ENABLE_EMAIL_FORWARDING" else "DISABLE_EMAIL_FORWARDING",
            message = "Email-Weiterleitung ${if (enabled) "aktiviert" else "deaktiviert"}",
            details = mapOf(
                "sms_forwarding_active" to _forwardingActive.value,
                "email_addresses_count" to _emailAddresses.value.size
            )
        )
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

    private fun updateServiceNotification() {
        val status = buildString {
            val hasForwarding = _forwardingActive.value
            val hasEmail = _forwardSmsToEmail.value

            when {
                // Beide aktiv
                hasForwarding && hasEmail -> {
                    append("SMS-Weiterleitung aktiv")
                    _selectedContact.value?.let { contact ->
                        append(" zu ${contact.name}")
                    }
                    append("\nEmail-Weiterleitung aktiv")
                    val emailCount = _emailAddresses.value.size
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
                    val emailCount = _emailAddresses.value.size
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

    /**
     * Sendet eine Test-SMS.
     */
    fun sendTestSms() {
        val contact = _selectedContact.value
        if (contact != null) {
            // Verwendung der eigenen Telefonnummer aus SIM-Verwaltung
            val simNumbers = prefsManager.getSimPhoneNumbers()
            val receiver = simNumbers.values.firstOrNull()

            if (receiver.isNullOrEmpty()) {
                LoggingManager.log(
                    LogLevel.WARNING,
                    LogMetadata(
                        component = "ContactsViewModel",
                        action = "TEST_SMS_FAILED",
                        details = mapOf("reason" to "no_sim_number_available")
                    ),
                    "Test-SMS konnte nicht gesendet werden - keine SIM-Nummer verfügbar"
                )
                SnackbarManager.showError("Keine SIM-Telefonnummer verfügbar")
                return
            }
            if (PhoneSmsUtils.sendTestSms(
                    application,
                    receiver,
                    prefsManager.getTestSmsText()
                )
            ) {
                LoggingManager.log(
                    LogLevel.INFO,
                    LogMetadata(
                        component = "ContactsViewModel",
                        action = "TEST_SMS_SENT",
                        details = mapOf(
                            "receiver" to receiver,
                            "text" to prefsManager.getTestSmsText()
                        )
                    ),
                    "Test-SMS wurde versendet"
                )
            } else {
                LoggingManager.log(
                    LogLevel.ERROR,
                    LogMetadata(
                        component = "ContactsViewModel",
                        action = "TEST_SMS_FAILED",
                        details = mapOf(
                            "receiver" to receiver,
                            "text" to prefsManager.getTestSmsText()
                        )
                    ),
                    "Fehler beim Versenden der Test-SMS"
                )
            }
        }
    }
    }

class ContactsStore {

    private var contentObserver: ContentObserver? = null
    private var contentResolver: ContentResolver? = null
    private var updateJob: Job? = null
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    // Lifecycle-aware Handler für ContentObserver
    private var observerHandler: Handler? = null

    // Flag um zu verhindern dass Observer nach cleanup noch aktiv ist
    private val isActive = AtomicBoolean(true)

    // Koroutinen-Scope für dieses Objekt statt statisch
    private val storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Mutex für thread-safe Zugriff auf die Listen
    private val contactsMutex = Mutex()

    // MutableStateFlow für die Kontaktliste
    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val allContacts = mutableListOf<Contact>()
    private val searchIndex = HashMap<String, MutableSet<Contact>>()
    private var currentCountryCode: String = "+43"

    private var isUpdating = AtomicBoolean(false)

    fun initialize(contentResolver: ContentResolver, countryCode: String) {
        this.currentCountryCode = countryCode
        this.contentResolver = contentResolver
        scope.launch { setupContentObserver(contentResolver) }

        // Initial load
        storeScope.launch {
            loadContacts(contentResolver)
        }
    }

    @VisibleForTesting
    suspend fun setTestContacts(contacts: List<Contact>) {
        contactsMutex.withLock {
            _contacts.value = contacts
            allContacts.clear()
            allContacts.addAll(contacts)
            rebuildSearchIndex()
        }
    }


    private fun setupContentObserver(contentResolver: ContentResolver) {
        contentObserver?.let {
            contentResolver.unregisterContentObserver(it)
            LoggingManager.log(
                LogLevel.INFO,
                LogMetadata(
                    component = "ContactsStore",
                    action = "UNREGISTER_OBSERVER",
                    details = emptyMap()
                ),
                "Alter ContentObserver wurde entfernt"
            )
        }

        // Erstelle lifecycle-aware Handler
        if (observerHandler == null) {
            observerHandler = Handler(Looper.getMainLooper())
        }

        contentObserver = object : ContentObserver(observerHandler) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)

                // Prüfe ob Store noch aktiv ist (Memory-Leak-Prävention)
                if (!isActive.get()) {
                    return
                }

                if (!isUpdating.compareAndSet(false, true)) {
                    scope.launch {
                        LoggingManager.log(
                            LogLevel.INFO,
                            LogMetadata(
                                component = "ContactsStore",
                                action = "CONTACTS_CHANGED_SKIPPED",
                                details = mapOf("reason" to "update_in_progress")
                            ),
                            "Update übersprungen - bereits in Bearbeitung"
                        )
                    }
                    return
                }

                updateJob?.cancel()
                updateJob = storeScope.launch {
                    try {
                        delay(500) // Debouncing
                        val startTime = System.currentTimeMillis()

                        contentResolver.let { resolver ->
                            withContext(Dispatchers.IO) {
                                loadContacts(resolver)
                            }

                            val duration = System.currentTimeMillis() - startTime
                            LoggingManager.log(
                                LogLevel.INFO,
                                LogMetadata(
                                    component = "ContactsStore",
                                    action = "CONTACTS_RELOAD",
                                    details = mapOf(
                                        "duration_ms" to duration,
                                        "contacts_count" to allContacts.size
                                    )
                                ),
                                "Kontakte erfolgreich neu geladen"
                            )
                        }
                    } catch (e: Exception) {
                        LoggingManager.log(
                            LogLevel.ERROR,
                            LogMetadata(
                                component = "ContactsStore",
                                action = "CONTACTS_RELOAD_ERROR",
                                details = mapOf(
                                    "error" to e.message,
                                    "error_type" to e.javaClass.simpleName
                                )
                            ),
                            "Fehler beim Neuladen der Kontakte",
                            e
                        )
                    } finally {
                        isUpdating.set(false)
                    }
                }
            }
        }

        try {
            contentObserver?.let { observer ->
                contentResolver.registerContentObserver(
                    ContactsContract.Contacts.CONTENT_URI,
                    true,
                    observer
                )
                contentResolver.registerContentObserver(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    true,
                    observer
                )
                contentResolver.registerContentObserver(
                    ContactsContract.Groups.CONTENT_URI,
                    true,
                    observer
                )

                LoggingManager.log(
                    LogLevel.INFO,
                    LogMetadata(
                        component = "ContactsStore",
                        action = "REGISTER_OBSERVER",
                        details = mapOf("status" to "success")
                    ),
                    "ContentObserver erfolgreich registriert"
                )
            }
        } catch (e: Exception) {
            LoggingManager.log(
                LogLevel.ERROR,
                LogMetadata(
                    component = "ContactsStore",
                    action = "OBSERVER_REGISTRATION_ERROR",
                    details = mapOf(
                        "error" to e.message,
                        "error_type" to e.javaClass.simpleName
                    )
                ),
                "Fehler bei der Observer-Registrierung",
                e
            )
        }
    }

    suspend fun loadContacts(contentResolver: ContentResolver) {
        contactsMutex.withLock {
            val startTime = System.currentTimeMillis()
            try {
                LoggingManager.logInfo(
                    component = "ContactsStore",
                    action = "LOAD_CONTACTS_START",
                    message = "Starte Laden der Kontakte"
                )

                val contacts = readContactsFromProvider(contentResolver)
                allContacts.clear()
                allContacts.addAll(contacts)
                rebuildSearchIndex()
                _contacts.value = allContacts.toList()

                LoggingManager.logInfo(
                    component = "ContactsStore",
                    action = "LOAD_CONTACTS",
                    details = mapOf(
                        "duration_ms" to (System.currentTimeMillis() - startTime),
                        "contacts_count" to contacts.size,
                        "index_size" to searchIndex.size
                    ),
                    message = "Kontakte erfolgreich geladen"
                )
            } catch (e: Exception) {
                LoggingManager.logError(
                    component = "ContactsStore",
                    action = "LOAD_CONTACTS_ERROR",
                    details = mapOf(
                        "error" to e.message,
                        "duration_ms" to (System.currentTimeMillis() - startTime)
                    ),
                    message = "Fehler beim Laden der Kontakte",
                    error = e
                )
                throw e
            }
        }
    }

    suspend fun filterContacts(query: String): List<Contact> {
        return contactsMutex.withLock {
            val startTime = System.currentTimeMillis()
            val results = if (query.isBlank()) {
                allContacts.toList()
            } else {
                val searchTerms = query.lowercase().split(" ")
                var filteredResults = mutableSetOf<Contact>()

                // Für den ersten Suchbegriff
                searchTerms.firstOrNull()?.let { firstTerm ->
                    filteredResults = searchIndex.entries
                        .filter { (key, _) -> key.contains(firstTerm) }
                        .flatMap { it.value }
                        .toMutableSet()
                }

                // Für weitere Suchbegriffe (AND-Verknüpfung)
                searchTerms.drop(1).forEach { term ->
                    val termResults = searchIndex.entries
                        .filter { (key, _) -> key.contains(term) }
                        .flatMap { it.value }
                        .toSet()
                    filteredResults.retainAll(termResults)
                }

                filteredResults.sortedBy { it.name }
            }

            val duration = System.currentTimeMillis() - startTime
            LoggingManager.log(
                LogLevel.DEBUG,
                LogMetadata(
                    component = "ContactsStore",
                    action = "FILTER_CONTACTS",
                    details = mapOf(
                        "query" to query,
                        "duration_ms" to duration,
                        "results_count" to results.size,
                        "total_contacts" to allContacts.size
                    )
                ),
                "Kontakte gefiltert (${duration}ms)"
            )

            results
        }
    }


    fun cleanup() {
        val startTime = System.currentTimeMillis()
        try {
            // Zuerst isActive auf false setzen um weitere Observer-Callbacks zu verhindern
            isActive.set(false)

            updateJob?.cancel()
            contentObserver?.let { observer ->
                contentResolver?.unregisterContentObserver(observer)
            }

            // Cleanup vor Scope-Cancellation
            runBlocking {
                contactsMutex.withLock {
                    allContacts.clear()
                    searchIndex.clear()
                    _contacts.value = emptyList()
                }
            }

            // Handler cleanup für Memory-Leak-Prävention
            observerHandler?.removeCallbacksAndMessages(null)
            observerHandler = null

            contentObserver = null
            contentResolver = null

            // Scope als letztes canceln
            storeScope.cancel()
            scope.cancel()

            LoggingManager.log(
                LogLevel.INFO,
                LogMetadata(
                    component = "ContactsStore",
                    action = "CLEANUP",
                    details = mapOf(
                        "duration_ms" to (System.currentTimeMillis() - startTime)
                    )
                ),
                "ContactsStore erfolgreich bereinigt"
            )
        } catch (e: Exception) {
            LoggingManager.log(
                LogLevel.ERROR,
                LogMetadata(
                    component = "ContactsStore",
                    action = "CLEANUP_ERROR",
                    details = mapOf(
                        "error" to e.message,
                        "duration_ms" to (System.currentTimeMillis() - startTime)
                    )
                ),
                "Fehler bei der Bereinigung des ContactsStore",
                e
            )
        }
    }


    fun updateCountryCode(newCode: String) {
        if (currentCountryCode != newCode) {
            LoggingManager.log(
                LogLevel.INFO,
                LogMetadata(
                    component = "ContactsStore",
                    action = "UPDATE_COUNTRY_CODE",
                    details = mapOf(
                        "old_code" to currentCountryCode,
                        "new_code" to newCode
                    )
                ),
                "Ländervorwahl wird aktualisiert"
            )

            currentCountryCode = newCode

            storeScope.launch {
                try {
                    contentResolver?.let {
                        loadContacts(it)
                        LoggingManager.log(
                            LogLevel.INFO,
                            LogMetadata(
                                component = "ContactsStore",
                                action = "COUNTRY_CODE_UPDATE_COMPLETE",
                                details = mapOf(
                                    "contacts_count" to allContacts.size
                                )
                            ),
                            "Kontakte mit neuer Ländervorwahl geladen"
                        )
                    }
                } catch (e: Exception) {
                    LoggingManager.log(
                        LogLevel.ERROR,
                        LogMetadata(
                            component = "ContactsStore",
                            action = "COUNTRY_CODE_UPDATE_ERROR",
                            details = mapOf(
                                "error" to e.message,
                                "error_type" to e.javaClass.simpleName
                            )
                        ),
                        "Fehler beim Aktualisieren der Kontakte mit neuer Ländervorwahl",
                        e
                    )
                }
            }
        }
    }

    private fun rebuildSearchIndex() {
        searchIndex.clear()
        allContacts.forEach { contact ->
            // Indexiere nach Namen
            contact.name.lowercase().split(" ").forEach { term ->
                searchIndex.getOrPut(term) { mutableSetOf() }.add(contact)
            }
            // Indexiere nach Telefonnummer
            contact.phoneNumber.filter { it.isDigit() }.windowed(3, 1).forEach { numberPart ->
                searchIndex.getOrPut(numberPart) { mutableSetOf() }.add(contact)
            }
        }
    }

    private fun readContactsFromProvider(contentResolver: ContentResolver): List<Contact> {
        // HashMap zur Gruppierung von Kontakten mit gleicher Nummer
        val contactGroups = mutableMapOf<String, MutableList<Contact>>()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.IS_PRIMARY
        )

        val phoneFormatter = PhoneNumberFormatter()

        val defaultRegion = when (currentCountryCode) {
            "+49" -> "DE"
            "+41" -> "CH"
            else -> "AT"
        }

        try {
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val nameIndex =
                    cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex =
                    cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val typeIndex =
                    cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE)

                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIndex).orEmpty().trim()
                    val phoneNumber = cursor.getString(numberIndex).orEmpty()
                    val type = cursor.getInt(typeIndex)

                    // Prüfe ob Telefonnummern-Formatierung aktiviert ist
                    val isFormattingEnabled = prefsManager.isPhoneNumberFormattingEnabled()

                    val finalPhoneNumber: String
                    val description: String
                    val normalizedNumber: String

                    if (isFormattingEnabled) {
                        // Formatiert die Telefonnummer
                        val numberResult = phoneFormatter.formatPhoneNumber(phoneNumber, defaultRegion)

                        if (name.isNotBlank() && numberResult.formattedNumber != null) {
                            finalPhoneNumber = numberResult.formattedNumber.replace(" ", "")
                            // Normalisierte Nummer für Map-Key
                            normalizedNumber = finalPhoneNumber.filter { it.isDigit() }

                            // Erstellt die beschreibende Information mit Formatierung
                            description = buildString {
                                append(numberResult.formattedNumber)
                                numberResult.carrierInfo?.let { carrier ->
                                    append(" | ")
                                    append(carrier)
                                }
                                // Optional: Typ der Telefonnummer hinzufügen
                                append(" | ")
                                append(getPhoneTypeLabel(type))
                            }
                        } else {
                            continue // Überspringe ungültige Nummern
                        }
                    } else {
                        // Verwende die Originalnummer ohne Formatierung
                        if (name.isNotBlank() && phoneNumber.isNotBlank()) {
                            finalPhoneNumber = phoneNumber.trim()
                            normalizedNumber = finalPhoneNumber.filter { it.isDigit() }

                            // Einfache Beschreibung ohne Formatierung
                            description = buildString {
                                append(phoneNumber.trim())
                                append(" | ")
                                append(getPhoneTypeLabel(type))
                            }
                        } else {
                            continue // Überspringe leere Nummern
                        }
                    }

                    val contact = Contact(
                        name = name,
                        phoneNumber = finalPhoneNumber,
                        description = description
                    )

                    // Füge den Kontakt zur entsprechenden Gruppe hinzu
                    contactGroups.getOrPut(normalizedNumber) { mutableListOf() }.add(contact)
                }
            }
        } catch (e: Exception) {
            Log.e("ContactsStore", "Error reading contacts", e)
        }

        // Wähle aus jeder Gruppe den "besten" Kontakt aus
        val finalContacts = mutableListOf<Contact>()
        for (contacts in contactGroups.values) {
            if (contacts.size == 1) {
                // Wenn nur ein Kontakt, füge ihn direkt hinzu
                finalContacts.add(contacts.first())
            } else {
                // Bei mehreren Kontakten mit der gleichen Nummer, wähle den besten aus
                val bestContact = contacts.maxWithOrNull(::compareContacts)
                bestContact?.let { finalContacts.add(it) }
            }
        }

        return finalContacts.sortedBy { it.name }
    }

    // Hilfsfunktion zum Vergleichen von Kontakten
    private fun compareContacts(a: Contact, b: Contact): Int {
        // Längere Namen bevorzugen (oft enthalten diese mehr Informationen)
        val lengthComparison = a.name.length.compareTo(b.name.length)
        if (lengthComparison != 0) return lengthComparison

        // Bei gleicher Länge alphabetisch sortieren
        return a.name.compareTo(b.name)
    }

    private fun getPhoneTypeLabel(type: Int): String {
        return when (type) {
            ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Privat"
            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "Mobil"
            ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Geschäftlich"
            else -> "Sonstige"
        }
    }

}

