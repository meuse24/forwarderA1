package info.meuse24.smsforwarderneoA1.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import info.meuse24.smsforwarderneoA1.LogLevel
import info.meuse24.smsforwarderneoA1.data.local.Logger
import info.meuse24.smsforwarderneoA1.LoggingManager
import info.meuse24.smsforwarderneoA1.LogMetadata
import info.meuse24.smsforwarderneoA1.data.local.SharedPreferencesManager
import info.meuse24.smsforwarderneoA1.SnackbarManager
import info.meuse24.smsforwarderneoA1.util.email.EmailResult
import info.meuse24.smsforwarderneoA1.util.email.EmailSender
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
 * @param logger Logger instance for structured logging
 */
class EmailViewModel(
    private val prefsManager: SharedPreferencesManager,
    private val logger: Logger
) : ViewModel() {

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
                        SnackbarManager.showSuccess("E-Mail-Adresse hinzugefügt")
                    } else {
                        SnackbarManager.showWarning("E-Mail-Adresse existiert bereits")
                    }
                } catch (e: Exception) {
                    LoggingManager.logError(
                        component = "EmailViewModel",
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
                    LoggingManager.log(
                        LogLevel.INFO,
                        LogMetadata(
                            component = "EmailViewModel",
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
                        component = "EmailViewModel",
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
                    component = "EmailViewModel",
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

    /**
     * Update test email text.
     */
    fun updateTestEmailText(newText: String) {
        _testEmailText.value = newText
        prefsManager.saveTestEmailText(newText)

        LoggingManager.log(
            LogLevel.DEBUG,
            LogMetadata(
                component = "EmailViewModel",
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
        prefsManager.saveSmtpSettings(host, port, username, password)
    }

    /**
     * Send a test email to verify SMTP configuration.
     *
     * @param mailrecipient Email address to send test email to
     */
    fun sendTestEmail(mailrecipient: String) {
        viewModelScope.launch {
            try {
                // Get SMTP settings from SharedPreferences
                val host = prefsManager.getSmtpHost()
                val port = prefsManager.getSmtpPort()
                val username = prefsManager.getSmtpUsername()
                val password = prefsManager.getSmtpPassword()
                val testEmailText = prefsManager.getTestEmailText()

                // Check if all required SMTP settings are present
                if (host.isEmpty() || username.isEmpty() || password.isEmpty()) {
                    LoggingManager.log(
                        LogLevel.WARNING,
                        LogMetadata(
                            component = "EmailViewModel",
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

                // Create formatted email text with timestamp
                val emailBody = buildString {
                    append("Test-Email von SMS Forwarder\n\n")
                    append("Zeitpunkt: ${getCurrentTimestamp()}\n\n")
                    append("Nachricht:\n")
                    append(testEmailText)
                    append("\n\nDies ist eine Test-Email zur Überprüfung der Email-Weiterleitungsfunktion.")
                }

                when (val result = emailSender.sendEmail(
                    to = listOf(mailrecipient),
                    subject = "SMS Forwarder Test E-Mail",
                    body = emailBody
                )) {
                    is EmailResult.Success -> {
                        LoggingManager.log(
                            LogLevel.INFO,
                            LogMetadata(
                                component = "EmailViewModel",
                                action = "TEST_EMAIL_SENT",
                                details = mapOf(
                                    "recipient" to mailrecipient,
                                    "smtp_host" to host,
                                    "text_length" to testEmailText.length
                                )
                            ),
                            "Test-E-Mail wurde versendet"
                        )
                        SnackbarManager.showSuccess("Test-E-Mail wurde an $mailrecipient versendet")
                    }

                    is EmailResult.Error -> {
                        LoggingManager.log(
                            LogLevel.ERROR,
                            LogMetadata(
                                component = "EmailViewModel",
                                action = "TEST_EMAIL_FAILED",
                                details = mapOf(
                                    "error" to result.message,
                                    "smtp_host" to host,
                                    "recipient" to mailrecipient
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
                        component = "EmailViewModel",
                        action = "TEST_EMAIL_ERROR",
                        details = mapOf(
                            "error" to e.message,
                            "recipient" to mailrecipient
                        )
                    ),
                    "Unerwarteter Fehler beim E-Mail-Versand"
                )
                SnackbarManager.showError("E-Mail-Versand fehlgeschlagen: ${e.message}")
            }
        }
    }

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
        private val prefsManager: SharedPreferencesManager,
        private val logger: Logger
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EmailViewModel::class.java)) {
                return EmailViewModel(prefsManager, logger) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
