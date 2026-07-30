package info.meuse24.smsforwarderneoA1

import android.app.Application
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import info.meuse24.smsforwarderneoA1.data.local.SharedPreferencesManager
import info.meuse24.smsforwarderneoA1.domain.model.Contact
import info.meuse24.smsforwarderneoA1.domain.model.MmiSimSelectionMode
import info.meuse24.smsforwarderneoA1.domain.model.MmiExecutionMode
import info.meuse24.smsforwarderneoA1.domain.model.ForwardingVerification
import info.meuse24.smsforwarderneoA1.domain.model.PersistedMmiOperation
import info.meuse24.smsforwarderneoA1.domain.model.MmiOperationState
import info.meuse24.smsforwarderneoA1.domain.model.MmiEvidence
import info.meuse24.smsforwarderneoA1.domain.model.MmiOperationReducer
import info.meuse24.smsforwarderneoA1.domain.model.MmiOperationPolicy
import info.meuse24.smsforwarderneoA1.domain.model.ForwardingResolutionReducer
import info.meuse24.smsforwarderneoA1.domain.model.DialPath
import info.meuse24.smsforwarderneoA1.domain.model.MmiAuditEntry
import info.meuse24.smsforwarderneoA1.util.MmiCodeMasker
import java.util.UUID
import info.meuse24.smsforwarderneoA1.domain.model.SimInfo
import info.meuse24.smsforwarderneoA1.domain.model.SimSelectionMode
import info.meuse24.smsforwarderneoA1.domain.model.MmiCodeProfile
import info.meuse24.smsforwarderneoA1.domain.model.ForwardingCodeSnapshot
import info.meuse24.smsforwarderneoA1.domain.model.A1Detection
import info.meuse24.smsforwarderneoA1.domain.model.A1Detector
import info.meuse24.smsforwarderneoA1.domain.model.MmiExecutionModeResolver
import info.meuse24.smsforwarderneoA1.presentation.viewmodel.NavigationViewModel
import info.meuse24.smsforwarderneoA1.service.SmsForegroundService
import info.meuse24.smsforwarderneoA1.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Simplified ViewModel for contact selection and forwarding using Android Contact Picker.
 * Replaces complex contact list management with simple picker-based selection.
 */
class ContactsViewModel(
    private val application: Application,
    private val prefsManager: SharedPreferencesManager
) : AndroidViewModel(application) {
    private companion object {
        const val VOICE_MMI_EVIDENCE_TIMEOUT_MS = 20_000L
    }

    // Callbacks
    var onDialMmiCode: ((String) -> Unit)? = null
    var onDialStatusCode: ((String) -> Unit)? = null
    var onLaunchContactPicker: (() -> Unit)? = null
    var onErrorOccurred: ((NavigationViewModel.ErrorDialogState) -> Unit)? = null

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

    private val _mmiActivatePrefix = MutableStateFlow(prefsManager.getMmiActivatePrefix())
    val mmiActivatePrefix: StateFlow<String> = _mmiActivatePrefix.asStateFlow()

    private val _mmiActivateSuffix = MutableStateFlow(prefsManager.getMmiActivateSuffix())
    val mmiActivateSuffix: StateFlow<String> = _mmiActivateSuffix.asStateFlow()

    private val _mmiDeactivateCode = MutableStateFlow(prefsManager.getMmiDeactivateCode())
    val mmiDeactivateCode: StateFlow<String> = _mmiDeactivateCode.asStateFlow()

    private val _mmiStatusCode = MutableStateFlow(prefsManager.getMmiStatusCode())
    val mmiStatusCode: StateFlow<String> = _mmiStatusCode.asStateFlow()

    private val _mmiCodeProfile = MutableStateFlow(prefsManager.getMmiCodeProfile())
    val mmiCodeProfile: StateFlow<MmiCodeProfile> = _mmiCodeProfile.asStateFlow()

    private val _internationalDialPrefix = MutableStateFlow(prefsManager.getInternationalDialPrefix())
    val internationalDialPrefix: StateFlow<String> = _internationalDialPrefix.asStateFlow()

    private val _maxLogSizeMB = MutableStateFlow(prefsManager.getMaxLogSizeMB())
    val maxLogSizeMB: StateFlow<Int> = _maxLogSizeMB.asStateFlow()

    private val _keepForwardingOnExit = MutableStateFlow(false)

    // Loop Protection Dialog State
    private val _showLoopProtectionDialog = MutableStateFlow<LoopProtectionDialogData?>(null)
    val showLoopProtectionDialog: StateFlow<LoopProtectionDialogData?> = _showLoopProtectionDialog.asStateFlow()

    // USSD Progress Dialog State
    private val _showUssdInProgress = MutableStateFlow(false)
    val showUssdInProgress: StateFlow<Boolean> = _showUssdInProgress.asStateFlow()

    // SIM Selection StateFlows
    private val _simSelectionMode = MutableStateFlow(SimSelectionMode.SAME_AS_INCOMING)
    val simSelectionMode: StateFlow<SimSelectionMode> = _simSelectionMode.asStateFlow()

    private val _availableSimCards = MutableStateFlow<List<SimInfo>>(emptyList())
    val availableSimCards: StateFlow<List<SimInfo>> = _availableSimCards.asStateFlow()

    private val _defaultSmsSubscriptionId = MutableStateFlow(-1)
    val defaultSmsSubscriptionId: StateFlow<Int> = _defaultSmsSubscriptionId.asStateFlow()

    // MMI SIM Selection StateFlows
    private val _mmiSimSelectionMode = MutableStateFlow(MmiSimSelectionMode.DEFAULT_VOICE_SIM)
    val mmiSimSelectionMode: StateFlow<MmiSimSelectionMode> = _mmiSimSelectionMode.asStateFlow()

    private val _defaultVoiceSubscriptionId = MutableStateFlow(-1)
    val defaultVoiceSubscriptionId: StateFlow<Int> = _defaultVoiceSubscriptionId.asStateFlow()

    private val _mmiSimA1Detection = MutableStateFlow(A1Detection.UNKNOWN)
    val mmiSimA1Detection: StateFlow<A1Detection> = _mmiSimA1Detection.asStateFlow()

    private val _showA1ProfileChoice = MutableStateFlow(false)
    val showA1ProfileChoice: StateFlow<Boolean> = _showA1ProfileChoice.asStateFlow()
    private var currentA1HintScope: String? = null

    // SMS Receive Filter StateFlows
    private val _sim1ReceiveEnabled = MutableStateFlow(true)
    val sim1ReceiveEnabled: StateFlow<Boolean> = _sim1ReceiveEnabled.asStateFlow()

    private val _sim2ReceiveEnabled = MutableStateFlow(true)
    val sim2ReceiveEnabled: StateFlow<Boolean> = _sim2ReceiveEnabled.asStateFlow()

    private val _pendingForwardingRequest = MutableStateFlow<PendingForwardingRequest?>(null)
    val pendingForwardingRequest: StateFlow<PendingForwardingRequest?> = _pendingForwardingRequest.asStateFlow()
    private var pendingMmiWatchdog: Job? = null
    private var voiceMmiEvidenceWatchdog: Job? = null
    private var settledVoiceMmiOperation: PendingForwardingRequest? = null
    private var mmiReservationInProgress = false
    private var lastCompletedForwardingRequest: PendingForwardingRequest? = null

    private val _forwardingVerification = MutableStateFlow(prefsManager.getForwardingVerification())
    val forwardingVerification: StateFlow<ForwardingVerification> = _forwardingVerification.asStateFlow()

    /**
     * The "report failure" hint is deliberately session-local.  The persisted
     * verification is status information, not an instruction to re-show a
     * completed-operation hint after an app restart.
     */
    private val _showTransientForwardingHint = MutableStateFlow(false)
    val showTransientForwardingHint: StateFlow<Boolean> = _showTransientForwardingHint.asStateFlow()

    class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ContactsViewModel::class.java)) {
                val app = AppContainer.getApplication()
                @Suppress("UNCHECKED_CAST")
                return ContactsViewModel(
                    application = app,
                    prefsManager = AppContainer.requirePrefsManager()
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

    data class PendingForwardingRequest(
        val id: String = UUID.randomUUID().toString(),
        val action: ForwardingAction,
        val contact: Contact?,
        val code: String,
        val executionMode: MmiExecutionMode,
        val dialedAtMillis: Long = System.currentTimeMillis(),
        val state: MmiOperationState = MmiOperationState.DIALING,
        val verification: ForwardingVerification = ForwardingVerification.NOT_CHECKED,
        val evidence: MmiEvidence = MmiEvidence(),
        val targetSubscriptionId: Int? = null,
        val dialPath: DialPath = DialPath.NOT_DISPATCHED,
        val userMessage: String? = null
    ) {
        val isUssd: Boolean get() = executionMode == MmiExecutionMode.USSD_CALLBACK
    }

    private fun PendingForwardingRequest.toPersistedOperation() = PersistedMmiOperation(
        id, action.name, code, executionMode, dialedAtMillis,
        contact?.name, contact?.phoneNumber, state, verification, evidence,
        targetSubscriptionId, dialPath, userMessage
    )

    data class LoopProtectionDialogData(
        val targetNumber: String,
        val ownNumber: String
    )

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

                // Initialize SIM selection
                initializeSimSelection()

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
                phoneNumber = savedPhoneNumber
            )

            LoggingManager.logInfo(
                component = "ContactsViewModel",
                action = "RESTORE_CONTACT",
                message = "Gespeicherter Kontakt wiederhergestellt",
                details = mapOf(
                    "contact" to savedContactName,
                    "number" to MmiCodeMasker.maskNumber(savedPhoneNumber),
                    "forwarding_active" to _forwardingActive.value
                )
            )
        }
        restorePendingForwardingRequest()
    }

    private fun restorePendingForwardingRequest() {
        val stored = prefsManager.getPendingMmiRequest() ?: return
        if (stored.state == MmiOperationState.SETTLED) {
            prefsManager.clearPendingMmiRequest()
            return
        }
        val action = runCatching { ForwardingAction.valueOf(stored.action) }.getOrElse {
            prefsManager.clearPendingMmiRequest()
            LoggingManager.logWarning(component = "ContactsViewModel", action = "MMI_OPERATION_INVALID", message = "Ungültigen persistierten MMI-Vorgang verworfen")
            return
        }
        val pending = PendingForwardingRequest(
            id = stored.id,
            action = action,
            contact = stored.contactNumber?.let { Contact(stored.contactName.takeIf { !it.isNullOrBlank() } ?: "Unbekannt", it) },
            code = stored.code,
            executionMode = stored.mode,
            dialedAtMillis = stored.dialedAtMillis,
            state = stored.state,
            verification = stored.verification,
            evidence = stored.evidence,
            targetSubscriptionId = stored.targetSubscriptionId,
            dialPath = stored.dialPath,
            userMessage = stored.userMessage
        )
        if (MmiOperationPolicy.isExpired(pending.executionMode, pending.dialedAtMillis, System.currentTimeMillis())) {
            prefsManager.clearPendingMmiRequest()
            _forwardingVerification.value = ForwardingVerification.DIAL_FAILED
            prefsManager.saveForwardingVerification(ForwardingVerification.DIAL_FAILED)
            LoggingManager.logWarning(component = "ContactsViewModel", action = "MMI_OPERATION_EXPIRED", message = "Wiederhergestellter MMI-Vorgang ist abgelaufen")
        } else {
            _pendingForwardingRequest.value = pending
            schedulePendingMmiWatchdog(pending)
            LoggingManager.logInfo(component = "ContactsViewModel", action = "MMI_OPERATION_RESTORED", message = "Laufender MMI-Vorgang wiederhergestellt")
        }
    }

    private fun schedulePendingMmiWatchdog(pending: PendingForwardingRequest) {
        pendingMmiWatchdog?.cancel()
        pendingMmiWatchdog = viewModelScope.launch {
            val timeout = MmiOperationPolicy.timeoutFor(pending.executionMode)
            delay((pending.dialedAtMillis + timeout - System.currentTimeMillis()).coerceAtLeast(0L))
            if (_pendingForwardingRequest.value?.id == pending.id) {
                if (pending.isUssd) {
                    val reduced = MmiOperationReducer.withTimeout(pending.toPersistedOperation())
                    val updated = pending.copy(
                        state = reduced.state,
                        verification = reduced.verification,
                        evidence = reduced.evidence
                    )
                    _pendingForwardingRequest.value = updated
                    prefsManager.savePendingMmiRequest(updated.toPersistedOperation())
                    dismissUssdProgressDialog()
                    resolvePendingForwardingResult(pending.id, MmiOperationPolicy.shouldContinueSmsForwardingAfterTimeout(pending.executionMode), reduced.verification, "USSD_TIMEOUT_UNKNOWN_NO_RESPONSE", "Keine USSD-Antwort empfangen")
                } else {
                    val reduced = MmiOperationReducer.withTimeout(pending.toPersistedOperation())
                    val updated = pending.copy(
                        state = reduced.state,
                        verification = reduced.verification,
                        evidence = reduced.evidence
                    )
                    _pendingForwardingRequest.value = updated
                    prefsManager.savePendingMmiRequest(updated.toPersistedOperation())
                    resolvePendingForwardingResult(pending.id, MmiOperationPolicy.shouldContinueSmsForwardingAfterTimeout(pending.executionMode), reduced.verification, "MMI_OPERATION_TIMEOUT", "MMI-Vorgang ohne Ergebnis abgelaufen")
                }
            }
        }
    }

    fun recordUssdEvidence(operationId: String, response: String, success: Boolean) {
        val current = _pendingForwardingRequest.value
            ?.takeIf { MmiOperationPolicy.matchesPendingOperation(it.id, operationId) } ?: return
        val reduced = MmiOperationReducer.withUssdResponse(
            current.toPersistedOperation(), MmiCodeMasker.maskFreeText(response), success
        )
        val updated = current.copy(
            state = reduced.state,
            verification = reduced.verification,
            evidence = reduced.evidence
        )
        _pendingForwardingRequest.value = updated
        prefsManager.savePendingMmiRequest(updated.toPersistedOperation())
    }

    fun recordDialDispatch(operationId: String, subscriptionId: Int, path: DialPath, message: String? = null) {
        val current = _pendingForwardingRequest.value?.takeIf { it.id == operationId } ?: return
        val updated = current.copy(targetSubscriptionId = subscriptionId, dialPath = path, userMessage = message)
        _pendingForwardingRequest.value = updated
        prefsManager.savePendingMmiRequest(updated.toPersistedOperation())
    }

    fun targetSubscriptionForOperation(operationId: String?, fallback: Int): Int {
        val pending = _pendingForwardingRequest.value?.takeIf { it.id == operationId }
        return if (pending?.action == ForwardingAction.DEACTIVATE) {
            prefsManager.getForwardingCodeSnapshot()?.subscriptionId ?: fallback
        } else fallback
    }

    /** The pending operation is authoritative: its mode may originate from an active-forwarding snapshot. */
    fun executionModeForOperation(operationId: String?, code: String): MmiExecutionMode =
        MmiExecutionModeResolver.resolve(
            _pendingForwardingRequest.value?.takeIf { it.id == operationId }?.executionMode,
            prefsManager.getMmiExecutionMode(code),
        )

    fun recordVoiceMmiEvidence(offHookAtMillis: Long?, idleAtMillis: Long) {
        val current = _pendingForwardingRequest.value?.takeIf { !it.isUssd }
            ?: settledVoiceMmiOperation ?: return
        val reduced = MmiOperationReducer.withCallState(current.toPersistedOperation(), offHookAtMillis, idleAtMillis)
        val updated = current.copy(evidence = reduced.evidence)
        if (_pendingForwardingRequest.value?.id == current.id) {
            _pendingForwardingRequest.value = updated
            prefsManager.savePendingMmiRequest(updated.toPersistedOperation())
        } else {
            settledVoiceMmiOperation = updated
        }
        LoggingManager.logInfo(
            component = "ContactsViewModel",
            action = "MMI_CALL_EVIDENCE",
            message = "MMI-Anrufstatus als Evidenz erfasst",
            details = mapOf(
                "call_observed" to updated.evidence.callObserved,
                "call_duration_ms" to (updated.evidence.callDurationMs ?: "unknown")
            )
        )
    }

    /**
     * Shows the optional failure-report hint only after the observed voice-MMI
     * call has ended.  The forwarding workflow itself has already continued
     * when Telecom accepted the request; this only controls the UI timing.
     */
    fun showVoiceMmiCompletionHint() {
        val settled = settledVoiceMmiOperation
            ?.takeIf { it.verification == ForwardingVerification.ASSUMED_SUCCESS }
            ?: return
        _showTransientForwardingHint.value = true
        LoggingManager.logInfo(
            component = "ContactsViewModel",
            action = "MMI_COMPLETION_HINT_SHOWN",
            message = "Hinweis zur MMI-Rückmeldung nach Anrufende aktiviert",
            details = mapOf("operation_id" to settled.id)
        )
    }

    private fun startVoiceMmiEvidenceWatchdog(operation: PendingForwardingRequest) {
        voiceMmiEvidenceWatchdog?.cancel()
        settledVoiceMmiOperation = operation
        voiceMmiEvidenceWatchdog = viewModelScope.launch {
            delay(VOICE_MMI_EVIDENCE_TIMEOUT_MS)
            val current = settledVoiceMmiOperation?.takeIf { it.id == operation.id } ?: return@launch
            if (!current.evidence.callObserved) {
                val reduced = MmiOperationReducer.withMissingCallObservation(current.toPersistedOperation())
                settledVoiceMmiOperation = current.copy(evidence = reduced.evidence)
                LoggingManager.logWarning(
                    component = "ContactsViewModel",
                    action = "MMI_CALL_EVIDENCE_TIMEOUT",
                    message = "Kein OFFHOOK innerhalb des Evidenzfensters beobachtet",
                    details = mapOf("operation_id" to operation.id)
                )
            }
            // Call-state callbacks are global. Do not attribute later, unrelated calls
            // to this MMI operation once its short evidence window has elapsed.
            if (settledVoiceMmiOperation?.id == operation.id) {
                settledVoiceMmiOperation = null
            }
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
                    LoggingManager.logInfo(
                        component = "ContactsViewModel",
                        action = "COUNTRY_CODE_INIT",
                        message = "Ländercode von SIM-Karte ermittelt",
                        details = mapOf(
                            "source" to "sim",
                            "code" to simCode
                        )
                    )
                    return@launch
                }

                // 2. Fallback to Austria
                updateCountryCode("+43")
                _countryCodeSource.value = "Standard (Österreich)"
                LoggingManager.logInfo(
                    component = "ContactsViewModel",
                    action = "COUNTRY_CODE_INIT",
                    message = "Verwende Default-Ländercode: Österreich",
                    details = mapOf(
                        "source" to "default",
                        "code" to "+43"
                    )
                )

            } catch (e: Exception) {
                LoggingManager.logError(
                    component = "ContactsViewModel",
                    action = "COUNTRY_CODE_INIT_ERROR",
                    message = "Fehler bei der Ländercode-Initialisierung",
                    error = e,
                    details = mapOf(
                        "error" to e.message,
                        "error_type" to e.javaClass.simpleName
                    )
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
     * Ergebnis der Umwandlung einer Picker-URI in ein Weiterleitungsziel.
     *
     * Die Fälle werden getrennt gehalten, weil sie für den Nutzer verschiedene Handlungen
     * bedeuten: eine fehlende Nummer erfordert einen anderen Kontakt, eine entzogene
     * Berechtigung einen Griff in die Systemeinstellungen.
     */
    private sealed interface ContactPickResult {
        data class Selected(val contact: Contact) : ContactPickResult
        data object NoPhoneNumber : ContactPickResult
        data class NotDialable(val reason: String) : ContactPickResult

        /**
         * Das Auswahlmenü hat die Datenzeile der gewählten Rufnummer nicht freigegeben.
         *
         * Kein erwarteter Zustand: `ACTION_PICK` auf `Phone.CONTENT_URI` delegiert den
         * Lesezugriff auf das Ergebnis. Tritt das auf, weicht die Picker-Implementierung des
         * Geräts vom Vertrag ab - eine Kontakteberechtigung würde die App deshalb hier nicht
         * anfordern.
         */
        data object PermissionDenied : ContactPickResult
        data class ReadFailed(val reason: String) : ContactPickResult
    }

    /**
     * Handle contact picker result - extracts contact info from URI and activates forwarding
     */
    fun handleContactPickerResult(uri: Uri) {
        viewModelScope.launch {
            when (val picked = extractContactFromUri(uri)) {
                is ContactPickResult.Selected -> activateForwarding(picked.contact) { result ->
                    when (result) {
                        is ForwardingResult.Success -> {
                            // Erfolgsmeldung entfernt - Status ist im Log sichtbar
                        }
                        is ForwardingResult.Error -> SnackbarManager.showError(
                            application.getString(R.string.snackbar_forwarding_activation_error, result.message)
                        )
                    }
                }

                ContactPickResult.NoPhoneNumber ->
                    SnackbarManager.showError(application.getString(R.string.snackbar_contact_no_number))

                is ContactPickResult.NotDialable -> SnackbarManager.showError(
                    application.getString(R.string.snackbar_contact_number_invalid, picked.reason)
                )

                ContactPickResult.PermissionDenied ->
                    SnackbarManager.showError(application.getString(R.string.snackbar_contact_picker_denied))

                is ContactPickResult.ReadFailed -> SnackbarManager.showError(
                    application.getString(R.string.snackbar_error_loading_contact, picked.reason)
                )
            }
        }
    }

    /**
     * Liest die gewählte Rufnummer aus und macht sie wählbar.
     *
     * [uri] ist die Datenzeile **einer** Rufnummer (siehe `PickPhoneNumber`), nicht ein
     * Kontakt. Deshalb genügt eine einzige Abfrage genau dieser Zeile: Der Nutzer hat die
     * Nummer bereits ausgewählt, und die URI trägt die dafür nötige temporäre
     * Leseberechtigung. Ein Zugriff auf die allgemeine Nummerntabelle entfällt damit - die
     * App kommt ohne Kontakteberechtigung aus.
     *
     * Die Rufnummer wird auf E.164 normalisiert, **bevor** sie irgendwo gespeichert oder in
     * einen MMI-Code eingesetzt wird. Adressbucheinträge enthalten regelmäßig Trennzeichen
     * und Schreibweisen wie "+43 (0)664 …"; deren Null überlebt das Entfernen der
     * Trennzeichen im Telefonie-Stack und würde beim Netz eine falsche Zielrufnummer
     * registrieren. Eine Festnetznummer wird bewusst akzeptiert - die Netzrufumleitung
     * dorthin ist gültig, auch wenn die SMS-Weiterleitung sie nicht erreicht.
     */
    private suspend fun extractContactFromUri(uri: Uri): ContactPickResult = withContext(Dispatchers.IO) {
        try {
            val contentResolver: ContentResolver = application.contentResolver
            val cursor = contentResolver.query(
                uri,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.TYPE
                ),
                null,
                null,
                null
            )

            cursor?.use {
                if (!it.moveToFirst()) return@use

                val rawNumber = it.getString(
                    it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                ).orEmpty()
                if (rawNumber.isBlank()) return@withContext ContactPickResult.NoPhoneNumber

                val displayName = it.getString(
                    it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                ).orEmpty().ifBlank { rawNumber }
                val phoneType = it.getInt(
                    it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE)
                )
                val typeLabel = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                    application.resources,
                    phoneType,
                    ""
                ).toString()

                val region = PhoneSmsUtils.getSimRegionIso(application)
                val validated = PhoneNumberValidator(application)
                    .validatePhoneNumber(rawNumber, region, requireMobile = false)
                val normalized = validated.normalizedNumber
                if (!validated.isValid || normalized.isNullOrBlank()) {
                    LoggingManager.logWarning(
                        component = "ContactsViewModel",
                        action = "CONTACT_NUMBER_REJECTED",
                        message = "Rufnummer des Kontakts ist nicht als Weiterleitungsziel verwendbar",
                        details = mapOf(
                            "number" to MmiCodeMasker.maskNumber(rawNumber),
                            "region" to region,
                            "error_type" to (validated.errorType?.name ?: "unknown")
                        )
                    )
                    return@withContext ContactPickResult.NotDialable(
                        validated.errorMessage
                            ?: application.getString(R.string.phone_error_invalid_number)
                    )
                }

                LoggingManager.logInfo(
                    component = "ContactsViewModel",
                    action = "CONTACT_EXTRACTED",
                    message = "Rufnummer aus Picker übernommen",
                    details = mapOf(
                        "name" to displayName,
                        "number" to MmiCodeMasker.maskNumber(normalized),
                        "type" to typeLabel,
                        "region" to region,
                        "normalized" to (normalized != rawNumber)
                    )
                )

                return@withContext ContactPickResult.Selected(
                    Contact(name = displayName, phoneNumber = normalized)
                )
            }

            LoggingManager.logWarning(
                component = "ContactsViewModel",
                action = "CONTACT_EXTRACTION_FAILED",
                message = "Ausgewählte Rufnummer konnte nicht gelesen werden"
            )
            ContactPickResult.NoPhoneNumber
        } catch (e: SecurityException) {
            // Vertragsbruch des Pickers: ACTION_PICK delegiert den Lesezugriff auf sein Ergebnis.
            LoggingManager.logError(
                component = "ContactsViewModel",
                action = "EXTRACT_CONTACT_URI_DENIED",
                message = "Auswahlmenü hat die Datenzeile der gewählten Rufnummer nicht freigegeben",
                error = e
            )
            ContactPickResult.PermissionDenied
        } catch (e: Exception) {
            LoggingManager.logError(
                component = "ContactsViewModel",
                action = "EXTRACT_CONTACT_ERROR",
                message = "Fehler beim Extrahieren der Kontaktdaten",
                error = e
            )
            ContactPickResult.ReadFailed(e.message ?: e.javaClass.simpleName)
        }
    }

    /**
     * Deactivate current forwarding
     */
    fun deactivateCurrentForwarding() {
        deactivateForwarding { result ->
            when (result) {
                is ForwardingResult.Success -> {
                    // Erfolgsmeldung entfernt - Status ist im Log sichtbar
                }
                is ForwardingResult.Error -> {
                    SnackbarManager.showError(
                        application.getString(R.string.snackbar_forwarding_deactivation_error, result.message)
                    )
                }
            }
        }
    }

    fun resolvePendingForwardingResult(operationId: String, success: Boolean, verification: ForwardingVerification, source: String, message: String? = null) {
        val pending = _pendingForwardingRequest.value ?: run {
            LoggingManager.logWarning(
                component = "ContactsViewModel",
                action = "FORWARDING_RESULT",
                message = "Kein ausstehender Forwarding-Request bei Ergebnisrückmeldung",
                details = mapOf(
                    "success" to success,
                    "source" to source,
                    "reason" to (message ?: "none")
                )
            )
            return
        }
        if (!MmiOperationPolicy.matchesPendingOperation(pending.id, operationId)) {
            LoggingManager.logWarning(component = "ContactsViewModel", action = "FORWARDING_RESULT_IGNORED", message = "Verspätetes Ergebnis für anderen MMI-Vorgang ignoriert", details = mapOf("operation_id" to operationId))
            return
        }

        if (!pending.isUssd && verification == ForwardingVerification.ASSUMED_SUCCESS) {
            startVoiceMmiEvidenceWatchdog(
                pending.copy(
                    state = MmiOperationState.SETTLED,
                    verification = verification
                )
            )
        }

        lastCompletedForwardingRequest = pending

        _pendingForwardingRequest.value = null
        pendingMmiWatchdog?.cancel()
        mmiReservationInProgress = false
        prefsManager.clearPendingMmiRequest()
        _forwardingVerification.value = verification
        // For voice MMI, wait for the observed call end before showing the
        // short hint. USSD has a real callback and can show it immediately.
        _showTransientForwardingHint.value = pending.isUssd &&
            verification == ForwardingVerification.ASSUMED_SUCCESS
        prefsManager.saveForwardingVerification(verification)
        val reason = message?.takeIf { it.isNotBlank() }
            ?: application.getString(R.string.snackbar_forwarding_unknown_reason)
        val resolution = ForwardingResolutionReducer.resolve(pending.action.name, success)
        prefsManager.appendMmiAudit(
            MmiAuditEntry(
                timestampMillis = System.currentTimeMillis(), operationId = pending.id, action = pending.action.name,
                executionMode = pending.executionMode, targetSubscriptionId = pending.targetSubscriptionId,
                dialPath = pending.dialPath, verification = verification, evidence = pending.evidence,
                message = MmiCodeMasker.maskFreeText(reason)
            )
        )

        when (pending.action) {
            ForwardingAction.ACTIVATE -> {
                if (resolution.forwardingActive == true) {
                    val contact = pending.contact
                    if (resolution.contactHandling == ForwardingResolutionReducer.ContactHandling.SAVE_PENDING_CONTACT) contact?.let {
                        viewModelScope.launch {
                            _selectedContact.value = it
                            _forwardingActive.value = resolution.forwardingActive
                        }
                        prefsManager.saveSelectedPhoneNumber(it.phoneNumber)
                        prefsManager.saveContactName(it.name)
                    } ?: run {
                        viewModelScope.launch {
                            _forwardingActive.value = resolution.forwardingActive
                        }
                    }
                    prefsManager.saveForwardingStatus(resolution.forwardingActive)
                    prefsManager.saveForwardingCodeSnapshot(
                        ForwardingCodeSnapshot(
                            deactivateCode = prefsManager.getMmiDeactivateCode(),
                            executionMode = prefsManager.getMmiExecutionMode(prefsManager.getMmiDeactivateCode()),
                            subscriptionId = pending.targetSubscriptionId,
                        )
                    )
                    val notificationMessage = if (contact != null) {
                        "Weiterleitung aktiviert zu ${contact.name} (${contact.phoneNumber})"
                    } else {
                        "Weiterleitung aktiviert"
                    }
                    updateNotification(notificationMessage)

                    LoggingManager.logInfo(
                        component = "ContactsViewModel",
                        action = if (verification == ForwardingVerification.CONFIRMED_SUCCESS) "FORWARDING_CONFIRMED" else "FORWARDING_ASSUMED",
                        message = if (verification == ForwardingVerification.CONFIRMED_SUCCESS) "Aktivierung der Weiterleitung bestätigt" else "Aktivierung der Weiterleitung angenommen",
                        details = mapOf(
                            "source" to source,
                            "code" to MmiCodeMasker.mask(pending.code),
                            "execution_mode" to pending.executionMode.name,
                            "contact" to (pending.contact?.name ?: "unknown"),
                            "number" to (pending.contact?.phoneNumber?.takeIf { it.isNotBlank() }
                                ?.let(MmiCodeMasker::maskNumber) ?: "unknown")
                        )
                    )
                } else {
                    LoggingManager.logWarning(
                        component = "ContactsViewModel",
                        action = "FORWARDING_FAILED",
                        message = "Aktivierung der Weiterleitung nicht bestätigt",
                        details = mapOf(
                            "source" to source,
                            "code" to MmiCodeMasker.mask(pending.code),
                            "execution_mode" to pending.executionMode.name,
                            "reason" to reason
                        )
                    )
                    SnackbarManager.showError(
                        application.getString(R.string.snackbar_forwarding_activation_failed, reason)
                    )
                }
            }

            ForwardingAction.DEACTIVATE -> {
                if (resolution.forwardingActive == false) {
                    viewModelScope.launch {
                        _selectedContact.value = null
                        _forwardingActive.value = resolution.forwardingActive
                    }
                    if (resolution.contactHandling == ForwardingResolutionReducer.ContactHandling.CLEAR) {
                        prefsManager.clearSelection()
                    }
                    prefsManager.saveForwardingStatus(resolution.forwardingActive)
                    prefsManager.clearForwardingCodeSnapshot()
                    updateNotification("Weiterleitung deaktiviert")

                    LoggingManager.logInfo(
                        component = "ContactsViewModel",
                        action = if (verification == ForwardingVerification.CONFIRMED_SUCCESS) "FORWARDING_CONFIRMED" else "FORWARDING_ASSUMED",
                        message = if (verification == ForwardingVerification.CONFIRMED_SUCCESS) "Deaktivierung der Weiterleitung bestätigt" else "Deaktivierung der Weiterleitung angenommen",
                        details = mapOf(
                            "source" to source,
                            "code" to MmiCodeMasker.mask(pending.code),
                            "execution_mode" to pending.executionMode.name,
                            "previous_contact" to (pending.contact?.name ?: "unknown")
                        )
                    )
                } else {
                    LoggingManager.logWarning(
                        component = "ContactsViewModel",
                        action = "FORWARDING_FAILED",
                        message = "Deaktivierung der Weiterleitung nicht bestätigt",
                        details = mapOf(
                            "source" to source,
                            "code" to MmiCodeMasker.mask(pending.code),
                            "execution_mode" to pending.executionMode.name,
                            "reason" to reason
                        )
                    )
                    SnackbarManager.showError(
                        application.getString(R.string.snackbar_forwarding_deactivation_failed, reason)
                    )
                }
            }

            ForwardingAction.TOGGLE -> {
                LoggingManager.logWarning(
                    component = "ContactsViewModel",
                    action = "FORWARDING_RESULT",
                    message = "Unverarbeiteter Toggle-Status - keine Aktion durchgeführt",
                    details = mapOf(
                        "source" to source,
                        "success" to success,
                        "reason" to reason
                    )
                )
            }
        }
    }

    /**
     * Nimmt die Rückmeldung entgegen, dass das Netz die Rufumleitung nicht geschaltet hat,
     * und stellt einen definierten Zustand her, statt weiter eine aktive Umleitung zu behaupten.
     *
     * Dazu wird der Deaktivierungscode gesendet: Er ist unschädlich, wenn ohnehin nichts
     * geschaltet ist, und räumt auf, falls die Aktivierung doch teilweise gegriffen hat.
     *
     * Die Anzeige wird bewusst **nicht** sofort geleert. Das übernimmt erst
     * [resolvePendingForwardingResult], wenn die Deaktivierung durch ist - und nur, wenn sie
     * gelungen ist. "Keine Umleitung" zu behaupten, während das Netz womöglich noch umleitet,
     * wäre der gefährlichere Irrtum: Anrufe gingen dann still woanders hin, während der Nutzer
     * sie bei sich erwartet. Scheitert die Deaktivierung, bleibt der Zustand deshalb stehen
     * und wird gemeldet.
     *
     * Damit endet auch die SMS-/E-Mail-Weiterleitung, denn `forwardingActive` trägt beide
     * Bedeutungen. Wer bewusst weitermachen will, hat dafür [continueWithAssumedForwarding].
     */
    fun reportForwardingFailure() {
        _showTransientForwardingHint.value = false
        _forwardingVerification.value = ForwardingVerification.USER_REPORTED_FAILURE
        prefsManager.saveForwardingVerification(ForwardingVerification.USER_REPORTED_FAILURE)
        LoggingManager.logWarning(
            component = "ContactsViewModel",
            action = "FORWARDING_USER_REPORTED_FAILURE",
            message = "Nutzer meldet Fehler der Netzrufumleitung - schalte zur Sicherheit ab"
        )
        SnackbarManager.showInfo(application.getString(R.string.snackbar_forwarding_safety_deactivation))
        deactivateForwarding { result ->
            if (result is ForwardingResult.Error) {
                SnackbarManager.showError(
                    application.getString(R.string.snackbar_forwarding_deactivation_error, result.message)
                )
            }
        }
    }

    /** User explicitly accepts continuing the independent SMS forwarding despite a carrier-side warning. */
    fun continueWithAssumedForwarding() {
        _forwardingVerification.value = ForwardingVerification.ASSUMED_SUCCESS
        _showTransientForwardingHint.value = true
        prefsManager.saveForwardingVerification(ForwardingVerification.ASSUMED_SUCCESS)
        LoggingManager.logInfo(component = "ContactsViewModel", action = "FORWARDING_USER_CONTINUED", message = "Nutzer setzt SMS-Weiterleitung trotz MMI-Warnung bewusst fort")
    }

    fun dismissTransientForwardingHint() {
        _showTransientForwardingHint.value = false
    }

    /** Repeats the last completed MMI request with a new correlation id. */
    fun retryLastForwardingOperation() {
        val previous = lastCompletedForwardingRequest ?: run {
            SnackbarManager.showError(application.getString(R.string.snackbar_no_operation_to_retry))
            return
        }
        if (!MmiOperationPolicy.mayStartOperation(_pendingForwardingRequest.value != null, mmiReservationInProgress)) return
        val retry = previous.copy(
            id = UUID.randomUUID().toString(),
            dialedAtMillis = System.currentTimeMillis(),
            state = MmiOperationState.DIALING,
            verification = ForwardingVerification.NOT_CHECKED,
            evidence = MmiEvidence(),
            targetSubscriptionId = null,
            dialPath = DialPath.NOT_DISPATCHED,
            userMessage = "Wiederholung"
        )
        mmiReservationInProgress = true
        _pendingForwardingRequest.value = retry
        prefsManager.savePendingMmiRequest(retry.toPersistedOperation())
        schedulePendingMmiWatchdog(retry)
        onDialMmiCode?.invoke(retry.code) ?: resolvePendingForwardingResult(
            retry.id, false, ForwardingVerification.DIAL_FAILED, "MMI_RETRY_NO_CALLBACK",
            application.getString(R.string.error_mmi_code_not_sent)
        )
    }

    private fun manageForwardingStatus(
        action: ForwardingAction,
        contact: Contact? = null,
        onResult: (ForwardingResult) -> Unit
    ) {
        viewModelScope.launch {
            if (!MmiOperationPolicy.mayStartOperation(_pendingForwardingRequest.value != null, mmiReservationInProgress)) {
                LoggingManager.logWarning(
                    component = "ContactsViewModel",
                    action = "FORWARDING_REQUEST_BLOCKED",
                    message = "MMI-Vorgang läuft bereits; weitere Aktion verworfen"
                )
                onResult(ForwardingResult.Error(application.getString(R.string.snackbar_forwarding_operation_running)))
                return@launch
            }
            // Reserve before switching to Dispatchers.IO so a second tap cannot pass
            // the check while the first operation is still being prepared.
            mmiReservationInProgress = true
            try {
                val result = when (action) {
                    ForwardingAction.ACTIVATE -> {
                        if (contact == null) {
                            ForwardingResult.Error(application.getString(R.string.error_no_contact_selected))
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
                            ForwardingResult.Error(application.getString(R.string.error_no_contact_selected))
                        }
                    }
                }

                if (result is ForwardingResult.Error && _pendingForwardingRequest.value == null) {
                    mmiReservationInProgress = false
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
                        application.getString(R.string.error_forwarding_generic, e.message ?: e.javaClass.simpleName),
                        e.stackTraceToString()
                    )
                )
                mmiReservationInProgress = false
            }
        }
    }

    private suspend fun activateForwardingInternal(contact: Contact): ForwardingResult {
        if (!prefsManager.isMmiConfigurationValid()) {
            return ForwardingResult.Error(application.getString(R.string.mmi_configuration_invalid))
        }
        // Loop Protection: Check if target number matches any known SIM number (manual OR auto-detected)
        val storedNumbers = prefsManager.getSimPhoneNumbers().values.toSet()
        val autoDetectedNumbers = PhoneSmsUtils.getAllSimInfo(application).mapNotNull { it.phoneNumber }.toSet()
        
        val ownNumbers = storedNumbers + autoDetectedNumbers
        val validator = PhoneNumberValidator(application)

        for (ownNumber in ownNumbers) {
            if (validator.areSameNumber(contact.phoneNumber, ownNumber)) {
                LoggingManager.logError(
                    component = "ContactsViewModel",
                    action = "LOOP_PROTECTION_TRIGGERED",
                    message = "Kontaktauswahl blockiert: Zielnummer ist eigene SIM-Karte",
                    details = mapOf(
                        "target" to MmiCodeMasker.maskNumber(contact.phoneNumber),
                        "own_number" to MmiCodeMasker.maskNumber(ownNumber)
                    )
                )

                // Trigger Loop Protection Dialog
                _showLoopProtectionDialog.value = LoopProtectionDialogData(
                    targetNumber = contact.phoneNumber,
                    ownNumber = ownNumber
                )

                return ForwardingResult.Error(application.getString(R.string.snackbar_loop_protection_own_sim))
            }
        }

        val activateCode = "${prefsManager.getMmiActivatePrefix()}${contact.phoneNumber}${prefsManager.getMmiActivateSuffix()}"
        val pendingRequest = PendingForwardingRequest(
            action = ForwardingAction.ACTIVATE,
            contact = contact,
            code = activateCode,
            executionMode = prefsManager.getMmiExecutionMode(activateCode)
        )
        _pendingForwardingRequest.value = pendingRequest
        schedulePendingMmiWatchdog(pendingRequest)
        prefsManager.savePendingMmiRequest(pendingRequest.toPersistedOperation())

        onDialMmiCode?.invoke(activateCode) ?: run {
            _pendingForwardingRequest.value = null
            prefsManager.clearPendingMmiRequest()
            LoggingManager.logError(
                component = "ContactsViewModel",
                action = "ACTIVATE_FORWARDING",
                message = "Kein Callback für MMI-Code-Wahl gesetzt"
            )
            return ForwardingResult.Error(application.getString(R.string.error_mmi_code_not_sent))
        }

        LoggingManager.logInfo(
            component = "ContactsViewModel",
            action = "REQUEST_ACTIVATE_FORWARDING",
            message = "Aktivierung der Weiterleitung angefordert",
            details = mapOf(
                "contact" to contact.name,
                "number" to MmiCodeMasker.maskNumber(contact.phoneNumber)
            )
        )

        return ForwardingResult.Success
    }

    private suspend fun deactivateForwardingInternal(): ForwardingResult {
        if (!prefsManager.isMmiConfigurationValid() && prefsManager.getForwardingCodeSnapshot() == null) {
            return ForwardingResult.Error(application.getString(R.string.mmi_configuration_invalid))
        }
        val prevContact = _selectedContact.value
        val snapshot = prefsManager.getForwardingCodeSnapshot()
        val deactivateCode = snapshot?.deactivateCode ?: prefsManager.getMmiDeactivateCode()

        val pendingRequest = PendingForwardingRequest(
            action = ForwardingAction.DEACTIVATE,
            contact = prevContact,
            code = deactivateCode,
            executionMode = snapshot?.executionMode ?: prefsManager.getMmiExecutionMode(deactivateCode)
        )
        _pendingForwardingRequest.value = pendingRequest
        schedulePendingMmiWatchdog(pendingRequest)
        prefsManager.savePendingMmiRequest(pendingRequest.toPersistedOperation())

        onDialMmiCode?.invoke(deactivateCode) ?: run {
            _pendingForwardingRequest.value = null
            prefsManager.clearPendingMmiRequest()
            LoggingManager.logError(
                component = "ContactsViewModel",
                action = "DEACTIVATE_FORWARDING",
                message = "Kein Callback für MMI-Code-Wahl gesetzt"
            )
            return ForwardingResult.Error(application.getString(R.string.error_mmi_code_not_sent))
        }

        LoggingManager.logInfo(
            component = "ContactsViewModel",
            action = "REQUEST_DEACTIVATE_FORWARDING",
            message = "Deaktivierung der Weiterleitung angefordert",
            details = mapOf(
                "previous_contact" to (prevContact?.name ?: "none"),
                "previous_number" to (prevContact?.phoneNumber?.takeIf { it.isNotBlank() }
                    ?.let(MmiCodeMasker::maskNumber) ?: "none")
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
            intent.action = SmsForegroundService.ACTION_UPDATE_NOTIFICATION
            intent.putExtra("contentText", message)
            AppContainer.getApplication().startService(intent)
        }
    }

    fun dismissLoopProtectionDialog() {
        _showLoopProtectionDialog.value = null
    }

    // USSD Progress Dialog Management
    fun showUssdProgressDialog() {
        _showUssdInProgress.value = true
    }

    fun dismissUssdProgressDialog() {
        _showUssdInProgress.value = false
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
                    // Deaktiviere Weiterleitung beim App-Beenden
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
                    // Weiterleitung ist bereits aktiv - speichere nur Preference
                    // Kein erneuter MMI-Code nötig, da Dialog nur bei aktiver Weiterleitung erscheint
                    prefsManager.setKeepForwardingOnExit(true)

                    LoggingManager.logInfo(
                        component = "ContactsViewModel",
                        action = "KEEP_FORWARDING_ON_EXIT",
                        message = "Weiterleitung bleibt beim App-Beenden aktiv",
                        details = mapOf(
                            "contact" to (_selectedContact.value?.name ?: "unknown"),
                            "number" to (_selectedContact.value?.phoneNumber?.takeIf { it.isNotBlank() }
                                ?.let(MmiCodeMasker::maskNumber) ?: "unknown")
                        )
                    )
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
                            "number" to MmiCodeMasker.maskNumber(currentContact.phoneNumber),
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

    fun updateInternationalDialPrefix(prefix: String) {
        _internationalDialPrefix.value = prefix
        prefsManager.setInternationalDialPrefix(prefix)

        LoggingManager.logInfo(
            component = "ContactsViewModel",
            action = "UPDATE_DIAL_PREFIX",
            message = "Internationale Anschaltziffernfolge aktualisiert",
            details = mapOf(
                "dial_prefix" to prefix
            )
        )
    }

    fun updateMmiActivatePrefix(prefix: String) {
        prefsManager.setMmiActivatePrefix(prefix)
        _mmiActivatePrefix.value = prefsManager.getMmiActivatePrefix()
        _mmiCodeProfile.value = prefsManager.getMmiCodeProfile()

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
        prefsManager.setMmiActivateSuffix(suffix)
        _mmiActivateSuffix.value = prefsManager.getMmiActivateSuffix()
        _mmiCodeProfile.value = prefsManager.getMmiCodeProfile()

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
        prefsManager.setMmiDeactivateCode(code)
        _mmiDeactivateCode.value = prefsManager.getMmiDeactivateCode()
        _mmiCodeProfile.value = prefsManager.getMmiCodeProfile()

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
        prefsManager.setMmiStatusCode(code)
        _mmiStatusCode.value = prefsManager.getMmiStatusCode()
        _mmiCodeProfile.value = prefsManager.getMmiCodeProfile()

        LoggingManager.logInfo(
            component = "ContactsViewModel",
            action = "UPDATE_MMI_STATUS_CODE",
            message = "MMI Statusabfrage-Code geändert",
            details = mapOf(
                "new_code" to code
            )
        )
    }

    fun updateMaxLogSizeMB(sizeMB: Int) {
        _maxLogSizeMB.value = sizeMB
        prefsManager.setMaxLogSizeMB(sizeMB)

        LoggingManager.logInfo(
            component = "ContactsViewModel",
            action = "UPDATE_MAX_LOG_SIZE",
            message = "Maximale Log-Größe geändert",
            details = mapOf(
                "size_mb" to sizeMB
            )
        )
    }

    fun queryForwardingStatus() {
        if (!MmiOperationPolicy.mayStartOperation(_pendingForwardingRequest.value != null, mmiReservationInProgress)) {
            LoggingManager.logWarning(
                component = "ContactsViewModel",
                action = "FORWARDING_STATUS_BLOCKED",
                message = "Statusabfrage während laufendem MMI-Vorgang verworfen"
            )
            SnackbarManager.showInfo(application.getString(R.string.snackbar_forwarding_operation_running))
            return
        }
        val statusCode = prefsManager.getMmiStatusCode()

        onDialStatusCode?.invoke(statusCode) ?: run {
            LoggingManager.logError(
                component = "ContactsViewModel",
                action = "QUERY_FORWARDING_STATUS",
                message = "Kein Callback für MMI-Code-Wahl gesetzt"
            )
            SnackbarManager.showError(application.getString(R.string.snackbar_status_query_failed))
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
    }

    /**
     * Resets all forwarding (call forwarding via MMI, SMS, and email).
     * Status can be queried separately using the Info button.
     */
    fun resetAllForwarding() {
        // Deactivate call forwarding via MMI code
        deactivateCurrentForwarding()

        LoggingManager.logInfo(
            component = "ContactsViewModel",
            action = "RESET_ALL_FORWARDING",
            message = "All forwarding deactivated (call, SMS, email)"
        )
    }

    fun resetMmiCodesToDefault() {
        _mmiActivatePrefix.value = prefsManager.run {
            resetMmiCodesToDefault()
            getMmiActivatePrefix()
        }
        _mmiActivateSuffix.value = prefsManager.getMmiActivateSuffix()
        _mmiDeactivateCode.value = prefsManager.getMmiDeactivateCode()
        _mmiStatusCode.value = prefsManager.getMmiStatusCode()
        _mmiCodeProfile.value = prefsManager.getMmiCodeProfile()

        LoggingManager.logInfo(
            component = "ContactsViewModel",
            action = "RESET_MMI_CODES",
            message = "MMI-Codes auf Standardwerte (BMI/A1) zurückgesetzt",
            details = mapOf(
                "activate_prefix" to _mmiActivatePrefix.value,
                "activate_suffix" to _mmiActivateSuffix.value,
                "deactivate_code" to _mmiDeactivateCode.value
            )
        )
    }

    fun resetMmiCodesToGeneric() {
        _mmiActivatePrefix.value = prefsManager.run {
            resetMmiCodesToGeneric()
            getMmiActivatePrefix()
        }
        _mmiActivateSuffix.value = prefsManager.getMmiActivateSuffix()
        _mmiDeactivateCode.value = prefsManager.getMmiDeactivateCode()
        _mmiStatusCode.value = prefsManager.getMmiStatusCode()
        _mmiCodeProfile.value = prefsManager.getMmiCodeProfile()

        LoggingManager.logInfo(
            component = "ContactsViewModel",
            action = "RESET_MMI_CODES_GENERIC",
            message = "MMI-Codes auf generische Standardwerte zurückgesetzt",
            details = mapOf(
                "activate_prefix" to _mmiActivatePrefix.value,
                "activate_suffix" to _mmiActivateSuffix.value,
                "deactivate_code" to _mmiDeactivateCode.value
            )
        )
    }

    // ==================== SIM Selection Functions ====================

    /**
     * Initialisiert die SIM-Auswahl: Lädt gespeicherten Modus und ermittelt verfügbare SIMs.
     */
    private suspend fun initializeSimSelection() {
        withContext(Dispatchers.IO) {
            try {
                // Lade gespeicherten SIM-Auswahl-Modus
                _simSelectionMode.value = prefsManager.getSimSelectionMode()

                // Lade gespeicherten MMI-SIM-Auswahl-Modus
                _mmiSimSelectionMode.value = prefsManager.getMmiSimSelectionMode()

                applyPlatformSimInfo()

                // Lade SMS-Empfangsfilter-Einstellungen
                _sim1ReceiveEnabled.value = prefsManager.isSim1ReceiveEnabled()
                _sim2ReceiveEnabled.value = prefsManager.isSim2ReceiveEnabled()

                LoggingManager.logInfo(
                    component = "ContactsViewModel",
                    action = "INIT_SIM_SELECTION",
                    message = "SIM-Auswahl initialisiert",
                    details = mapOf(
                        "sms_mode" to _simSelectionMode.value.name,
                        "mmi_mode" to _mmiSimSelectionMode.value.name,
                        "available_sims" to _availableSimCards.value.size,
                        "default_sms_sub_id" to _defaultSmsSubscriptionId.value,
                        "default_voice_sub_id" to _defaultVoiceSubscriptionId.value,
                        "sim1_receive_enabled" to _sim1ReceiveEnabled.value,
                        "sim2_receive_enabled" to _sim2ReceiveEnabled.value
                    )
                )
            } catch (e: Exception) {
                LoggingManager.logError(
                    component = "ContactsViewModel",
                    action = "INIT_SIM_SELECTION",
                    message = "Fehler bei SIM-Auswahl-Initialisierung",
                    error = e
                )
            }
        }
    }

    /**
     * Liest die vom System stammenden SIM-Daten neu ein.
     *
     * Notwendig, weil das ViewModel bereits in `onCreate` erzeugt wird - also **bevor** die
     * Berechtigungen erteilt sind. `getAllSimInfo` liefert dann eine leere Liste, und ohne
     * erneutes Lesen bliebe dieser Zustand die ganze Sitzung stehen: sichtbar als
     * "Unbekannter Anbieter", folgenreich für die A1-Erkennung, die auf `carrierId`/`mccMnc`
     * beruht. Deshalb zusätzlich nach der Berechtigungserteilung und bei jedem `onResume`
     * (SIM-Wechsel, Flugmodus, Rückkehr aus dem Hintergrund).
     *
     * Aus den Einstellungen abgeleitete Werte bleiben unberührt - die halten ihre Setter aktuell.
     */
    fun refreshSimInfo() {
        viewModelScope.launch {
            runCatching { applyPlatformSimInfo() }.onFailure { error ->
                LoggingManager.logError(
                    component = "ContactsViewModel",
                    action = "REFRESH_SIM_INFO",
                    message = "SIM-Daten konnten nicht aktualisiert werden",
                    error = error as? Exception ?: Exception(error)
                )
            }
        }
    }

    private suspend fun applyPlatformSimInfo() = withContext(Dispatchers.IO) {
        val sims = PhoneSmsUtils.getAllSimInfo(application)

        // Eine leere Liste ist nicht davon zu unterscheiden, dass das Lesen fehlgeschlagen ist
        // (fehlende Berechtigung, Binder-Fehler). Bereits bekannte Karten deshalb nicht wegen
        // eines möglichen Leseproblems verwerfen - der nächste erfolgreiche Lauf korrigiert.
        if (sims.isEmpty() && _availableSimCards.value.isNotEmpty()) {
            LoggingManager.logWarning(
                component = "ContactsViewModel",
                action = "SIM_INFO_UNAVAILABLE",
                message = "Keine SIM-Daten lesbar - behalte zuletzt bekannten Stand",
                details = mapOf("known_sims" to _availableSimCards.value.size)
            )
            return@withContext
        }

        val defaultSims = PhoneSmsUtils.getDefaultSimIds(application)
        val smsSubscriptionId = defaultSims?.first ?: -1
        val voiceSubscriptionId = defaultSims?.second ?: -1
        val changed = sims != _availableSimCards.value ||
            smsSubscriptionId != _defaultSmsSubscriptionId.value ||
            voiceSubscriptionId != _defaultVoiceSubscriptionId.value

        _availableSimCards.value = sims
        _defaultSmsSubscriptionId.value = smsSubscriptionId
        _defaultVoiceSubscriptionId.value = voiceSubscriptionId
        updateMmiSimA1Detection(sims)

        // Nur protokollieren, wenn sich etwas geändert hat: onResume feuert häufig, und das
        // Protokoll ist größenbegrenzt und wird exportiert.
        if (changed) {
            LoggingManager.logInfo(
                component = "ContactsViewModel",
                action = "SIM_INFO_REFRESHED",
                message = "SIM-Daten aktualisiert",
                details = mapOf(
                    "available_sims" to sims.size,
                    "carriers" to sims.joinToString { it.carrierName?.takeIf(String::isNotBlank) ?: "unknown" },
                    "default_sms_sub_id" to smsSubscriptionId,
                    "default_voice_sub_id" to voiceSubscriptionId,
                    "a1_detection" to _mmiSimA1Detection.value.name
                )
            )
        }
    }

    fun dismissA1ProfileChoice() {
        currentA1HintScope?.let(prefsManager::setA1HintShownScope)
        _showA1ProfileChoice.value = false
    }

    private fun updateMmiSimA1Detection(sims: List<SimInfo>) {
        val targetId = when (_mmiSimSelectionMode.value) {
            MmiSimSelectionMode.DEFAULT_VOICE_SIM -> _defaultVoiceSubscriptionId.value
            MmiSimSelectionMode.ALWAYS_SIM_1 -> sims.firstOrNull { it.slotIndex == 0 }?.subscriptionId ?: -1
            MmiSimSelectionMode.ALWAYS_SIM_2 -> sims.firstOrNull { it.slotIndex == 1 }?.subscriptionId ?: -1
        }
        val sim = sims.firstOrNull { it.subscriptionId == targetId }
        _mmiSimA1Detection.value = sim?.let {
            A1Detector.detect(it.carrierId, it.mccMnc, it.carrierName, it.displayName)
        } ?: A1Detection.UNKNOWN
        currentA1HintScope = sim?.let {
            listOf(it.mccMnc ?: it.carrierId?.toString() ?: "unknown", _mmiSimSelectionMode.value.name, it.slotIndex)
                .joinToString(":")
        }
        _showA1ProfileChoice.value = _mmiSimA1Detection.value == A1Detection.A1_CONFIRMED &&
            !prefsManager.hasMmiProfileUserDecision() && !_forwardingActive.value &&
            _pendingForwardingRequest.value == null && currentA1HintScope != prefsManager.getA1HintShownScope()
    }

    /**
     * Setzt den SIM-Auswahl-Modus und speichert ihn persistent.
     */
    fun setSimSelectionMode(mode: SimSelectionMode) {
        viewModelScope.launch(Dispatchers.IO) {
            prefsManager.setSimSelectionMode(mode)
            _simSelectionMode.value = mode

            LoggingManager.logInfo(
                component = "ContactsViewModel",
                action = "SET_SIM_SELECTION",
                message = "SIM-Auswahl-Modus geändert",
                details = mapOf("mode" to mode.name)
            )
        }
    }

    /**
     * Setzt den MMI-SIM-Auswahl-Modus und speichert ihn persistent.
     */
    fun setMmiSimSelectionMode(mode: MmiSimSelectionMode) {
        viewModelScope.launch(Dispatchers.IO) {
            prefsManager.setMmiSimSelectionMode(mode)
            _mmiSimSelectionMode.value = mode
            updateMmiSimA1Detection(_availableSimCards.value)

            LoggingManager.logInfo(
                component = "ContactsViewModel",
                action = "SET_MMI_SIM_SELECTION",
                message = "MMI SIM-Auswahl-Modus geändert",
                details = mapOf("mode" to mode.name)
            )
        }
    }

    /**
     * Aktiviert/deaktiviert SMS-Empfang von SIM 1.
     */
    fun setSim1ReceiveEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            prefsManager.setSim1ReceiveEnabled(enabled)
            _sim1ReceiveEnabled.value = enabled

            // Prüfe ob beide SIM-Karten deaktiviert sind und Weiterleitung aktiv ist
            if (!enabled && !_sim2ReceiveEnabled.value && _forwardingActive.value) {
                withContext(Dispatchers.Main) {
                    SnackbarManager.showWarning(application.getString(R.string.snackbar_no_sim_receive_enabled))
                }
                LoggingManager.logWarning(
                    component = "ContactsViewModel",
                    action = "ALL_SIMS_DISABLED",
                    message = "Alle SIM-Karten für Empfang deaktiviert bei aktiver Weiterleitung",
                    details = mapOf(
                        "sim1_enabled" to enabled,
                        "sim2_enabled" to _sim2ReceiveEnabled.value,
                        "forwarding_active" to _forwardingActive.value
                    )
                )
            }

            LoggingManager.logInfo(
                component = "ContactsViewModel",
                action = "UPDATE_SIM1_RECEIVE",
                message = "SIM 1 Empfang geändert",
                details = mapOf("enabled" to enabled)
            )
        }
    }

    /**
     * Aktiviert/deaktiviert SMS-Empfang von SIM 2.
     */
    fun setSim2ReceiveEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            prefsManager.setSim2ReceiveEnabled(enabled)
            _sim2ReceiveEnabled.value = enabled

            // Prüfe ob beide SIM-Karten deaktiviert sind und Weiterleitung aktiv ist
            if (!enabled && !_sim1ReceiveEnabled.value && _forwardingActive.value) {
                withContext(Dispatchers.Main) {
                    SnackbarManager.showWarning(application.getString(R.string.snackbar_no_sim_receive_enabled))
                }
                LoggingManager.logWarning(
                    component = "ContactsViewModel",
                    action = "ALL_SIMS_DISABLED",
                    message = "Alle SIM-Karten für Empfang deaktiviert bei aktiver Weiterleitung",
                    details = mapOf(
                        "sim1_enabled" to _sim1ReceiveEnabled.value,
                        "sim2_enabled" to enabled,
                        "forwarding_active" to _forwardingActive.value
                    )
                )
            }

            LoggingManager.logInfo(
                component = "ContactsViewModel",
                action = "UPDATE_SIM2_RECEIVE",
                message = "SIM 2 Empfang geändert",
                details = mapOf("enabled" to enabled)
            )
        }
    }

    fun updateServiceNotification() {
        val status = buildString {
            val hasForwarding = _forwardingActive.value
            val hasEmail = prefsManager.isForwardSmsToEmail()

            when {
                hasForwarding && hasEmail -> {
                    append(application.getString(R.string.notification_status_sms_active))
                    _selectedContact.value?.let { contact ->
                        append(" " + application.getString(R.string.notification_status_to_number, contact.name))
                    }
                    append("\nEmail-Weiterleitung aktiv")
                    val emailCount = prefsManager.getEmailAddresses().size
                    append(" " + application.getString(R.string.notification_status_email_count, emailCount))
                }
                hasForwarding -> {
                    append(application.getString(R.string.notification_status_sms_active))
                    _selectedContact.value?.let { contact ->
                        append(" " + application.getString(R.string.notification_status_to_number, contact.name))
                    }
                }
                hasEmail -> {
                    append(application.getString(R.string.notification_status_email_active))
                    val emailCount = prefsManager.getEmailAddresses().size
                    append(" " + application.getString(R.string.notification_status_email_count, emailCount))
                }
                else -> {
                    append(application.getString(R.string.notification_running_long))
                }
            }
        }

        viewModelScope.launch {
            try {
                val intent = Intent(AppContainer.getApplication(), SmsForegroundService::class.java)
                intent.action = SmsForegroundService.ACTION_UPDATE_NOTIFICATION
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
                LoggingManager.logInfo(
                    component = "ContactsViewModel",
                    action = "VIEWMODEL_CLEARED",
                    message = "ViewModel wurde bereinigt",
                    details = mapOf("state" to "saved")
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
