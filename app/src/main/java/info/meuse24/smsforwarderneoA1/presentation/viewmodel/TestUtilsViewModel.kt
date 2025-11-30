package info.meuse24.smsforwarderneoA1.presentation.viewmodel

import android.app.Application
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import info.meuse24.smsforwarderneoA1.LoggingManager
import info.meuse24.smsforwarderneoA1.PhoneSmsUtils
import info.meuse24.smsforwarderneoA1.SnackbarManager
import info.meuse24.smsforwarderneoA1.data.local.Logger.LogLevel
import info.meuse24.smsforwarderneoA1.data.local.Logger.LogMetadata
import info.meuse24.smsforwarderneoA1.data.local.SharedPreferencesManager
import info.meuse24.smsforwarderneoA1.domain.model.Contact
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/**
 * ViewModel für Test- und Utility-Funktionen.
 *
 * Verantwortlichkeiten:
 * - Test-SMS Text Management
 * - Test-SMS Versand
 * - Test-Kontakte setzen (für Unit Tests)
 *
 * Note: Filter Text wurde zurück nach ContactsViewModel verschoben,
 * da es für die Core-Filterlogik benötigt wird.
 */
class TestUtilsViewModel(
    private val application: Application,
    private val prefsManager: SharedPreferencesManager,
    private val contactsStore: Any // ContactsStore für setTestContacts
) : AndroidViewModel(application) {

    // StateFlows
    private val _testSmsText = MutableStateFlow("")
    val testSmsText: StateFlow<String> = _testSmsText.asStateFlow()

    init {
        // Lade gespeicherte Werte
        _testSmsText.value = prefsManager.getTestSmsText()
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
                    component = "TestUtilsViewModel",
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

    /**
     * Sendet eine Test-SMS an die eigene Nummer (aus SIM-Verwaltung).
     */
    fun sendTestSms(selectedContact: Contact?) {
        if (selectedContact == null) {
            LoggingManager.log(
                LogLevel.WARNING,
                LogMetadata(
                    component = "TestUtilsViewModel",
                    action = "TEST_SMS_FAILED",
                    details = mapOf("reason" to "no_contact_selected")
                ),
                "Test-SMS konnte nicht gesendet werden - kein Kontakt ausgewählt"
            )
            SnackbarManager.showError("Kein Kontakt ausgewählt")
            return
        }

        // Verwendung der eigenen Telefonnummer aus SIM-Verwaltung
        val simNumbers = prefsManager.getSimPhoneNumbers()
        val receiver = simNumbers.values.firstOrNull()

        if (receiver.isNullOrEmpty()) {
            LoggingManager.log(
                LogLevel.WARNING,
                LogMetadata(
                    component = "TestUtilsViewModel",
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
                    component = "TestUtilsViewModel",
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
                    component = "TestUtilsViewModel",
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

    /**
     * Setzt Test-Kontakte (nur für Unit Tests).
     */
    @VisibleForTesting
    fun setTestContacts(contacts: List<Contact>, onComplete: suspend () -> Unit) = viewModelScope.launch {
        try {
            // Callback zum Setzen der Kontakte im ContactsStore
            onComplete()

            // Warten bis die Änderungen übernommen wurden
            delay(500)

            LoggingManager.log(
                LogLevel.DEBUG,
                LogMetadata(
                    component = "TestUtilsViewModel",
                    action = "SET_TEST_CONTACTS",
                    details = mapOf(
                        "contacts_count" to contacts.size
                    )
                ),
                "Test-Kontakte wurden gesetzt"
            )
        } catch (e: Exception) {
            LoggingManager.logError(
                component = "TestUtilsViewModel",
                action = "SET_TEST_CONTACTS_ERROR",
                message = "Fehler beim Setzen der Test-Kontakte",
                error = e,
                details = mapOf(
                    "contacts_count" to contacts.size
                )
            )
        }
    }

    /**
     * Factory für TestUtilsViewModel.
     */
    class Factory(
        private val application: Application,
        private val prefsManager: SharedPreferencesManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TestUtilsViewModel::class.java)) {
                // contactsStore wird als dummy Any() übergeben, da es nur für Test-Callbacks verwendet wird
                return TestUtilsViewModel(application, prefsManager, Any()) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
