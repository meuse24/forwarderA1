package info.meuse24.smsforwarderneoA1.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import info.meuse24.smsforwarderneoA1.LoggingManager
import info.meuse24.smsforwarderneoA1.data.local.SharedPreferencesManager
import info.meuse24.smsforwarderneoA1.R
import info.meuse24.smsforwarderneoA1.SnackbarManager
import info.meuse24.smsforwarderneoA1.AppContainer
import info.meuse24.smsforwarderneoA1.domain.model.EmailPortPolicy
import info.meuse24.smsforwarderneoA1.domain.model.EmailTransportSecurity
import info.meuse24.smsforwarderneoA1.util.messageRes
import info.meuse24.smsforwarderneoA1.util.email.EmailSender
import info.meuse24.smsforwarderneoA1.util.email.SmtpConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ViewModel for managing email addresses and SMTP settings.
 *
 * Handles:
 * - Email address list management (add/remove)
 * - SMTP server configuration (host, port, username, password)
 * - Email forwarding toggle
 * - Test email sending
 *
 * Extracted from ContactsViewModel as part of Phase 5 refactoring.
 *
 * @param prefsManager SharedPreferencesManager for persisting settings
 */
class EmailViewModel(
    private val prefsManager: SharedPreferencesManager
) : ViewModel() {
    private val appContext = AppContainer.getApplication()

    // Callback for notifying when email forwarding state changes (triggers service notification update)
    var onForwardingStateChanged: (() -> Unit)? = null

    // Email addresses
    private val _emailAddresses = MutableStateFlow<List<String>>(emptyList())
    val emailAddresses: StateFlow<List<String>> = _emailAddresses.asStateFlow()

    private val _newEmailAddress = MutableStateFlow("")
    val newEmailAddress: StateFlow<String> = _newEmailAddress.asStateFlow()

    // Email forwarding toggle
    private val _forwardSmsToEmail = MutableStateFlow(prefsManager.isForwardSmsToEmail())
    val forwardSmsToEmail: StateFlow<Boolean> = _forwardSmsToEmail.asStateFlow()

    // SMTP settings
    private val _smtpHost = MutableStateFlow(prefsManager.getSmtpHost())
    val smtpHost: StateFlow<String> = _smtpHost.asStateFlow()

    private val _smtpPort = MutableStateFlow(prefsManager.getSmtpPort())
    val smtpPort: StateFlow<Int> = _smtpPort.asStateFlow()

    private val _smtpUsername = MutableStateFlow(prefsManager.getSmtpUsername())
    val smtpUsername: StateFlow<String> = _smtpUsername.asStateFlow()

    private val _smtpPassword = MutableStateFlow(prefsManager.getSmtpPassword())
    val smtpPassword: StateFlow<String> = _smtpPassword.asStateFlow()

    private val _smtpSecurity = MutableStateFlow(prefsManager.getSmtpSecurity())
    val smtpSecurity: StateFlow<EmailTransportSecurity> = _smtpSecurity.asStateFlow()

    private val _smtpFromAddress = MutableStateFlow(prefsManager.getSmtpFromAddress())
    val smtpFromAddress: StateFlow<String> = _smtpFromAddress.asStateFlow()

    /** Rohtext des Portfeldes. Eine unbrauchbare Eingabe bleibt sichtbar, statt still zu verschwinden. */
    private val _smtpPortInput = MutableStateFlow(prefsManager.getSmtpPort().toString())
    val smtpPortInput: StateFlow<String> = _smtpPortInput.asStateFlow()

    private val _smtpPortError = MutableStateFlow<Int?>(null)
    val smtpPortError: StateFlow<Int?> = _smtpPortError.asStateFlow()

    private val _smtpFromError = MutableStateFlow<Int?>(null)
    val smtpFromError: StateFlow<Int?> = _smtpFromError.asStateFlow()

    // Test email text
    private val _testEmailText = MutableStateFlow("")
    val testEmailText: StateFlow<String> = _testEmailText.asStateFlow()

    init {
        // Load email addresses from preferences
        viewModelScope.launch {
            try {
                _emailAddresses.value = prefsManager.getEmailAddresses()
                _testEmailText.value = prefsManager.getTestEmailText()
            } catch (e: Exception) {
                LoggingManager.logError(
                    component = "EmailViewModel",
                    action = "INIT_ERROR",
                    message = "Fehler beim Initialisieren von EmailViewModel",
                    error = e
                )
            }
        }
    }

    /**
     * Update the new email address input field.
     */
    fun updateNewEmailAddress(email: String) {
        _newEmailAddress.value = email
    }

    /**
     * Add a new email address to the list.
     *
     * Validates email format, checks for duplicates, and persists to preferences.
     */
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
                        SnackbarManager.showSuccess(appContext.getString(R.string.snackbar_email_added))
                    } else {
                        SnackbarManager.showWarning(appContext.getString(R.string.snackbar_email_exists))
                    }
                } catch (e: Exception) {
                    LoggingManager.logError(
                        component = "EmailViewModel",
                        action = "ADD_EMAIL_ERROR",
                        message = "Fehler beim Hinzufügen der E-Mail-Adresse",
                        error = e,
                        details = mapOf("email" to email)
                    )
                    SnackbarManager.showError(appContext.getString(R.string.snackbar_email_add_error, e.message ?: ""))
                }
            }
        } else {
            SnackbarManager.showError(appContext.getString(R.string.snackbar_email_invalid))
        }
    }

    /**
     * Remove an email address from the list.
     *
     * If list becomes empty, automatically disables SMS-to-email forwarding.
     */
    fun removeEmailAddress(email: String) {
        viewModelScope.launch {
            try {
                val currentList = _emailAddresses.value.toMutableList()
                currentList.remove(email)
                _emailAddresses.value = currentList
                prefsManager.saveEmailAddresses(currentList)

                // If list is empty, disable SMS-email forwarding
                if (currentList.isEmpty() && _forwardSmsToEmail.value) {
                    _forwardSmsToEmail.value = false
                    prefsManager.setForwardSmsToEmail(false)
                    onForwardingStateChanged?.invoke()
                    LoggingManager.logInfo(
                        component = "EmailViewModel",
                        action = "SMS_EMAIL_FORWARD_AUTO_DISABLE",
                        message = "SMS-E-Mail-Weiterleitung automatisch deaktiviert (keine E-Mail-Adressen vorhanden)",
                        details = mapOf(
                            "reason" to "no_email_addresses"
                        )
                    )
                    SnackbarManager.showInfo(appContext.getString(R.string.snackbar_email_forwarding_disabled_no_addresses))
                }

                LoggingManager.logInfo(
                    component = "EmailViewModel",
                    action = "REMOVE_EMAIL",
                    message = "E-Mail-Adresse entfernt",
                    details = mapOf(
                        "remaining_emails" to currentList.size,
                        "forwarding_status" to _forwardSmsToEmail.value
                    )
                )
                SnackbarManager.showSuccess(appContext.getString(R.string.snackbar_email_removed))
            } catch (e: Exception) {
                LoggingManager.logError(
                    component = "EmailViewModel",
                    action = "REMOVE_EMAIL_ERROR",
                    message = "Fehler beim Entfernen der E-Mail-Adresse",
                    error = e,
                    details = mapOf(
                        "email" to email,
                        "current_list_size" to _emailAddresses.value.size
                    )
                )
                SnackbarManager.showError(appContext.getString(R.string.snackbar_email_remove_error, e.message ?: ""))
            }
        }
    }

    /**
     * Update test email text.
     */
    fun updateTestEmailText(newText: String) {
        _testEmailText.value = newText
        prefsManager.saveTestEmailText(newText)

        LoggingManager.logDebug(
            component = "EmailViewModel",
            action = "UPDATE_TEST_EMAIL",
            message = "Test-Email Text aktualisiert",
            details = mapOf(
                "old_length" to _testEmailText.value.length,
                "new_length" to newText.length,
                "is_empty" to newText.isEmpty()
            )
        )
    }

    /**
     * Update SMTP server settings.
     */
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
        persistSmtpSettings()
    }

    /**
     * Uebernimmt die Porteingabe.
     *
     * Frueher wurde eine unbrauchbare Eingabe stillschweigend auf den alten Wert zurueckgesetzt.
     * Jetzt bleibt sie im Feld stehen und wird als Fehler ausgewiesen; gespeichert wird nur ein
     * gueltiger Wert.
     */
    fun updateSmtpPortInput(input: String) {
        _smtpPortInput.value = input
        when (val result = EmailPortPolicy.validate(input)) {
            is EmailPortPolicy.Result.Valid -> {
                _smtpPortError.value = null
                _smtpPort.value = result.port
                persistSmtpSettings()
            }

            EmailPortPolicy.Result.Empty -> _smtpPortError.value = R.string.error_smtp_port_empty
            EmailPortPolicy.Result.NotANumber -> _smtpPortError.value = R.string.error_smtp_port_not_a_number
            EmailPortPolicy.Result.OutOfRange -> _smtpPortError.value = R.string.error_smtp_port_out_of_range
        }
    }

    /**
     * Wechselt die Transportverschluesselung und schlaegt den passenden Port vor.
     *
     * Ein abweichender Port wird nur ersetzt, wenn er der Standardport des bisherigen Modus war -
     * eine bewusst eingetragene Sonderkonfiguration bleibt erhalten.
     */
    fun updateSmtpSecurity(security: EmailTransportSecurity) {
        val previous = _smtpSecurity.value
        _smtpSecurity.value = security
        if (_smtpPort.value == previous.defaultPort) {
            _smtpPort.value = security.defaultPort
            _smtpPortInput.value = security.defaultPort.toString()
            _smtpPortError.value = null
        }
        persistSmtpSettings()
    }

    /** Leer bedeutet „wie Benutzername" und ist ausdruecklich zulaessig. */
    fun updateSmtpFromAddress(address: String) {
        _smtpFromAddress.value = address
        val valid = address.isBlank() ||
            android.util.Patterns.EMAIL_ADDRESS.matcher(address.trim()).matches()
        _smtpFromError.value = if (valid) null else R.string.error_smtp_from_invalid
        if (valid) persistSmtpSettings()
    }

    private fun persistSmtpSettings() {
        prefsManager.saveSmtpSettings(
            host = _smtpHost.value,
            port = _smtpPort.value,
            username = _smtpUsername.value,
            password = _smtpPassword.value,
            security = _smtpSecurity.value,
            fromAddress = _smtpFromAddress.value.trim()
        )
    }

    /**
     * Send a test email to verify SMTP configuration.
     *
     * @param mailrecipient Email address to send test email to
     */
    fun sendTestEmail(mailrecipient: String) {
        viewModelScope.launch {
            try {
                val host = prefsManager.getSmtpHost()
                val username = prefsManager.getSmtpUsername()
                val password = prefsManager.getSmtpPassword()
                val fromAddress = prefsManager.getEffectiveSmtpFromAddress()
                val testEmailText = prefsManager.getTestEmailText()

                if (host.isEmpty() || username.isEmpty() || password.isEmpty() || fromAddress.isEmpty()) {
                    LoggingManager.logWarning(
                        component = "EmailViewModel",
                        action = "TEST_EMAIL",
                        message = "Unvollständige SMTP-Einstellungen",
                        details = mapOf(
                            "error" to "incomplete_smtp_settings",
                            "has_host" to host.isNotEmpty(),
                            "has_username" to username.isNotEmpty(),
                            "has_from" to fromAddress.isNotEmpty()
                        )
                    )
                    SnackbarManager.showError(appContext.getString(R.string.snackbar_smtp_incomplete))
                    return@launch
                }

                // Derselbe Pfad wie die Weiterleitung - Verschluesselungsmodus, Absenderadresse und
                // Fehlerklassifikation inbegriffen. Sonst sagt der Test nicht aus, was er verspricht.
                val emailSender = EmailSender(
                    SmtpConfig(
                        host = host,
                        port = prefsManager.getSmtpPort(),
                        security = prefsManager.getSmtpSecurity(),
                        username = username,
                        password = password,
                        fromAddress = fromAddress
                    )
                )

                val emailBody = buildString {
                    append("Test-Email von SMS Forwarder\n\n")
                    append("Zeitpunkt: ${getCurrentTimestamp()}\n\n")
                    append("Nachricht:\n")
                    append(testEmailText)
                    append("\n\nDies ist eine Test-Email zur Überprüfung der Email-Weiterleitungsfunktion.")
                }

                // Die Test-E-Mail laeuft bewusst **nicht** ueber die Queue: Sie ist eine Diagnose,
                // kein Weiterleitungsauftrag, und darf nicht spaeter erneut versucht werden.
                val failure = emailSender.sendSingle(
                    recipient = mailrecipient,
                    subject = "SMS Forwarder Test E-Mail",
                    body = emailBody
                )

                if (failure == null) {
                    LoggingManager.logInfo(
                        component = "EmailViewModel",
                        action = "TEST_EMAIL_SENT",
                        message = "Test-E-Mail wurde versendet",
                        details = mapOf(
                            "recipient" to maskAddress(mailrecipient),
                            "smtp_host" to host,
                            "text_length" to testEmailText.length
                        )
                    )
                    SnackbarManager.showSuccess(
                        appContext.getString(R.string.snackbar_test_email_sent, mailrecipient)
                    )
                } else {
                    LoggingManager.logError(
                        component = "EmailViewModel",
                        action = "TEST_EMAIL_FAILED",
                        message = "Fehler beim Versenden der Test-E-Mail",
                        details = mapOf(
                            "failure_kind" to failure.kind.name,
                            "return_code" to failure.returnCode,
                            "detail" to failure.detail,
                            "smtp_host" to host,
                            "recipient" to maskAddress(mailrecipient)
                        )
                    )
                    SnackbarManager.showError(
                        appContext.getString(
                            R.string.snackbar_test_email_failed,
                            appContext.getString(failure.kind.messageRes())
                        )
                    )
                }
            } catch (e: Exception) {
                LoggingManager.logError(
                    component = "EmailViewModel",
                    action = "TEST_EMAIL_ERROR",
                    message = "Unerwarteter Fehler beim E-Mail-Versand",
                    error = e,
                    details = mapOf("recipient" to maskAddress(mailrecipient))
                )
                SnackbarManager.showError(appContext.getString(R.string.snackbar_test_email_failed, e.message ?: ""))
            }
        }
    }

    /** Adressen gehoeren nicht vollstaendig ins Protokoll - es ist per PIN einsehbar und exportierbar. */
    private fun maskAddress(address: String): String =
        address.substringAfter('@', "").let { domain -> if (domain.isBlank()) "***" else "***@$domain" }

    /**
     * Toggle SMS-to-email forwarding.
     *
     * Triggers service notification update via callback.
     */
    fun updateForwardSmsToEmail(enabled: Boolean) {
        _forwardSmsToEmail.value = enabled
        prefsManager.setForwardSmsToEmail(enabled)
        onForwardingStateChanged?.invoke()

        LoggingManager.logInfo(
            component = "EmailViewModel",
            action = if (enabled) "ENABLE_EMAIL_FORWARDING" else "DISABLE_EMAIL_FORWARDING",
            message = "Email-Weiterleitung ${if (enabled) "aktiviert" else "deaktiviert"}",
            details = mapOf(
                "email_addresses_count" to _emailAddresses.value.size
            )
        )
    }

    /**
     * Get current timestamp formatted as "dd.MM.yyyy HH:mm:ss".
     */
    private fun getCurrentTimestamp(): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }

    /**
     * Factory for creating EmailViewModel instances.
     */
    class Factory(
        private val prefsManager: SharedPreferencesManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EmailViewModel::class.java)) {
                return EmailViewModel(prefsManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
