package info.meuse24.smsforwarderneoA1

import android.app.Application
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import info.meuse24.smsforwarderneoA1.data.local.Logger
import info.meuse24.smsforwarderneoA1.data.local.SharedPreferencesManager
import info.meuse24.smsforwarderneoA1.domain.model.Contact
import info.meuse24.smsforwarderneoA1.presentation.viewmodel.NavigationViewModel
import info.meuse24.smsforwarderneoA1.service.SmsForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Simplified ViewModel for contact selection and forwarding using Android Contact Picker.
 * Replaces complex contact list management with simple picker-based selection.
 */
class ContactsViewModel(
    private val application: Application,
    private val prefsManager: SharedPreferencesManager,
    private val logger: Logger
) : AndroidViewModel(application) {

    private val stateMutex = Mutex()

    // Callbacks
    var onDialMmiCode: ((String) -> Unit)? = null
    var onLaunchContactPicker: (() -> Unit)? = null
    var onErrorOccurred: ((NavigationViewModel.ErrorDialogState) -> Unit)? = null

    // Call state from MainActivity
    var callStateFlow: StateFlow<Int>? = null

    // State
    private val _selectedContact = MutableStateFlow<Contact?>(null)
    val selectedContact: StateFlow<Contact?> = _selectedContact.asStateFlow()

    private val _forwardingActive = MutableStateFlow(false)
    val forwardingActive: StateFlow<Boolean> = _forwardingActive.asStateFlow()

    private val _countryCode = MutableStateFlow("")
    val countryCode: StateFlow<String> = _countryCode.asStateFlow()

    private val _countryCodeSource = MutableStateFlow("")
    val countryCodeSource: StateFlow<String> = _countryCodeSource.asStateFlow()

    private val _showProgressDialog = MutableStateFlow(false)
    val showProgressDialog: StateFlow<Boolean> = _showProgressDialog.asStateFlow()

    private val _isCleaningUp = MutableStateFlow(false)

    private val _cleanupCompleted = MutableSharedFlow<Unit>()
    val cleanupCompleted = _cleanupCompleted.asSharedFlow()

    // Settings StateFlows
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

    sealed class ForwardingResult {
        data object Success : ForwardingResult()
        data class Error(val message: String, val technical: String? = null) : ForwardingResult()
    }

    enum class ForwardingAction {
        ACTIVATE, DEACTIVATE, TOGGLE
    }

    init {
        initialize()
    }

    fun initialize() {
        viewModelScope.launch {
            try {
                // Initialize country code
                initializeCountryCode()

                // Load saved state
                loadSavedState()

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
            }
        }
    }

    private fun loadSavedState() {
        _countryCode.value = prefsManager.getCountryCode()
        _forwardingActive.value = prefsManager.isForwardingActive()
        val savedPhoneNumber = prefsManager.getSelectedPhoneNumber()
        val savedContactName = prefsManager.getContactName() // Neue Methode in SharedPreferences

        if (savedPhoneNumber.isNotEmpty() && _forwardingActive.value) {
            _selectedContact.value = Contact(
                name = savedContactName.ifEmpty { "Unbekannt" },
                phoneNumber = savedPhoneNumber,
                description = savedPhoneNumber
            )

            LoggingManager.logInfo(
                component = "ContactsViewModel",
                action = "RESTORE_CONTACT",
                message = "Gespeicherter Kontakt wiederhergestellt",
                details = mapOf(
                    "contact" to savedContactName,
                    "number" to savedPhoneNumber,
                    "forwarding_active" to _forwardingActive.value
                )
            )
        }
    }

    private fun initializeCountryCode() {
        viewModelScope.launch {
            try {
                // 1. Priority: SIM card
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

                // 2. Fallback to Austria
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

    private fun updateCountryCode(code: String) {
        _countryCode.value = code
        prefsManager.saveCountryCode(code)
    }

    /**
     * Launch Android Contact Picker to select a contact
     */
    fun launchContactPicker() {
        onLaunchContactPicker?.invoke()
    }

    /**
     * Handle contact picker result - extracts contact info from URI and activates forwarding
     */
    suspend fun handleContactPickerResult(uri: Uri) {
        viewModelScope.launch {
            try {
                val contact = extractContactFromUri(uri)
                if (contact != null) {
                    // Activate forwarding to selected contact
                    activateForwarding(contact) { result ->
                        when (result) {
                            is ForwardingResult.Success -> {
                                SnackbarManager.showSuccess(
                                    "Weiterleitung zu ${contact.name} (${contact.phoneNumber}) aktiviert"
                                )
                            }
                            is ForwardingResult.Error -> {
                                SnackbarManager.showError(
                                    "Fehler beim Aktivieren der Weiterleitung: ${result.message}"
                                )
                            }
                        }
                    }
                } else {
                    SnackbarManager.showError("Kontakt konnte nicht geladen werden")
                }
            } catch (e: Exception) {
                LoggingManager.logError(
                    component = "ContactsViewModel",
                    action = "CONTACT_PICKER_ERROR",
                    message = "Fehler beim Verarbeiten des ausgewählten Kontakts",
                    error = e
                )
                SnackbarManager.showError("Fehler beim Laden des Kontakts: ${e.message}")
            }
        }
    }

    /**
     * Extract contact information from Contact Picker URI
     */
    private suspend fun extractContactFromUri(uri: Uri): Contact? = withContext(Dispatchers.IO) {
        try {
            val contentResolver: ContentResolver = application.contentResolver

            // Query contact ID from picker URI
            val contactCursor = contentResolver.query(uri, null, null, null, null)
            contactCursor?.use {
                if (it.moveToFirst()) {
                    val contactId = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                    val displayName = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))

                    // Query phone numbers for this contact
                    val phoneCursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        arrayOf(
                            ContactsContract.CommonDataKinds.Phone.NUMBER,
                            ContactsContract.CommonDataKinds.Phone.TYPE
                        ),
                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(contactId),
                        null
                    )

                    phoneCursor?.use { phoneCurs ->
                        if (phoneCurs.moveToFirst()) {
                            val phoneNumber = phoneCurs.getString(
                                phoneCurs.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            )
                            val phoneType = phoneCurs.getInt(
                                phoneCurs.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE)
                            )

                            val typeLabel = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                                application.resources,
                                phoneType,
                                ""
                            ).toString()

                            LoggingManager.logInfo(
                                component = "ContactsViewModel",
                                action = "CONTACT_EXTRACTED",
                                message = "Kontakt aus Picker extrahiert",
                                details = mapOf(
                                    "name" to displayName,
                                    "number" to phoneNumber,
                                    "type" to typeLabel
                                )
                            )

                            return@withContext Contact(
                                name = displayName,
                                phoneNumber = phoneNumber,
                                description = "$phoneNumber ($typeLabel)"
                            )
                        }
                    }
                }
            }

            LoggingManager.logWarning(
                component = "ContactsViewModel",
                action = "CONTACT_EXTRACTION_FAILED",
                message = "Keine Telefonnummer für ausgewählten Kontakt gefunden"
            )
            null
        } catch (e: Exception) {
            LoggingManager.logError(
                component = "ContactsViewModel",
                action = "EXTRACT_CONTACT_ERROR",
                message = "Fehler beim Extrahieren der Kontaktdaten",
                error = e
            )
            null
        }
    }

    /**
     * Deactivate current forwarding
     */
    fun deactivateCurrentForwarding() {
        deactivateForwarding { result ->
            when (result) {
                is ForwardingResult.Success -> {
                    SnackbarManager.showSuccess("Weiterleitung deaktiviert")
                }
                is ForwardingResult.Error -> {
                    SnackbarManager.showError("Fehler beim Deaktivieren: ${result.message}")
                }
            }
        }
    }

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

    private suspend fun activateForwardingInternal(contact: Contact): ForwardingResult {
        val activateCode = "${prefsManager.getMmiActivatePrefix()}${contact.phoneNumber}${prefsManager.getMmiActivateSuffix()}"

        onDialMmiCode?.invoke(activateCode) ?: run {
            LoggingManager.logError(
                component = "ContactsViewModel",
                action = "ACTIVATE_FORWARDING",
                message = "Kein Callback für MMI-Code-Wahl gesetzt"
            )
            return ForwardingResult.Error("MMI-Code konnte nicht gesendet werden")
        }

        withContext(Dispatchers.Main) {
            _selectedContact.value = contact
            _forwardingActive.value = true
        }

        prefsManager.saveSelectedPhoneNumber(contact.phoneNumber)
        prefsManager.saveContactName(contact.name) // Neue Methode
        prefsManager.saveForwardingStatus(true)

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

    private suspend fun deactivateForwardingInternal(): ForwardingResult {
        val prevContact = _selectedContact.value
        val deactivateCode = prefsManager.getMmiDeactivateCode()

        onDialMmiCode?.invoke(deactivateCode) ?: run {
            LoggingManager.logError(
                component = "ContactsViewModel",
                action = "DEACTIVATE_FORWARDING",
                message = "Kein Callback für MMI-Code-Wahl gesetzt"
            )
            return ForwardingResult.Error("MMI-Code konnte nicht gesendet werden")
        }

        withContext(Dispatchers.Main) {
            _selectedContact.value = null
            _forwardingActive.value = false
        }

        prefsManager.clearSelection()

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

    private fun updateNotification(message: String) {
        viewModelScope.launch {
            val intent = Intent(AppContainer.getApplication(), SmsForegroundService::class.java)
            intent.action = "UPDATE_NOTIFICATION"
            intent.putExtra("contentText", message)
            AppContainer.getApplication().startService(intent)
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
                        updateServiceNotification()
                    }
                }
            }
        }
    }

    fun saveCurrentState() {
        viewModelScope.launch {
            try {
                val currentContact = _selectedContact.value
                val isActive = _forwardingActive.value

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
                    prefsManager.saveContactName(currentContact.name)
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

    // Settings management methods
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

    /**
     * Resets forwarding and queries status after call completes.
     * Waits for the deactivation call to finish before querying status.
     */
    fun resetForwardingWithStatusQuery() {
        // Deactivate forwarding (triggers MMI call)
        deactivateCurrentForwarding()

        // Launch coroutine to wait for call completion
        viewModelScope.launch {
            val callState = callStateFlow
            if (callState != null) {
                // Wait until call is idle using Flow.first
                callState.first { it == android.telephony.TelephonyManager.CALL_STATE_IDLE }

                // Add buffer after call ends
                kotlinx.coroutines.delay(500)

                // Query status
                queryForwardingStatus()

                LoggingManager.logInfo(
                    component = "ContactsViewModel",
                    action = "RESET_WITH_STATUS_QUERY",
                    message = "Reset completed, status query triggered"
                )
            } else {
                // Fallback if callState not available
                LoggingManager.logWarning(
                    component = "ContactsViewModel",
                    action = "RESET_WITH_STATUS_QUERY",
                    message = "CallState not available, using fallback delay"
                )
                kotlinx.coroutines.delay(3000)
                queryForwardingStatus()
            }
        }
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
            val hasEmail = prefsManager.isForwardSmsToEmail()

            when {
                hasForwarding && hasEmail -> {
                    append("SMS-Weiterleitung aktiv")
                    _selectedContact.value?.let { contact ->
                        append(" zu ${contact.name}")
                    }
                    append("\nEmail-Weiterleitung aktiv")
                    val emailCount = prefsManager.getEmailAddresses().size
                    append(" an $emailCount Email(s)")
                }
                hasForwarding -> {
                    append("SMS-Weiterleitung aktiv")
                    _selectedContact.value?.let { contact ->
                        append(" zu ${contact.name}")
                    }
                }
                hasEmail -> {
                    append("Email-Weiterleitung aktiv")
                    val emailCount = prefsManager.getEmailAddresses().size
                    append(" an $emailCount Email(s)")
                }
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

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            try {
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
}
