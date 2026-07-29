package info.meuse24.smsforwarderneoA1.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import info.meuse24.smsforwarderneoA1.LoggingManager
import info.meuse24.smsforwarderneoA1.PhoneSmsUtils
import info.meuse24.smsforwarderneoA1.SnackbarManager
import info.meuse24.smsforwarderneoA1.data.local.SharedPreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel für Test- und Utility-Funktionen.
 *
 * Verantwortlichkeiten:
 * - Test-SMS Text Management
 * - Test-SMS Versand
 */
class TestUtilsViewModel(
    private val application: Application,
    private val prefsManager: SharedPreferencesManager
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
            LoggingManager.logDebug(
                component = "TestUtilsViewModel",
                action = "UPDATE_TEST_SMS",
                message = "Test-SMS Text aktualisiert",
                details = mapOf(
                    "old_length" to oldText.length,
                    "new_length" to newText.length,
                    "is_empty" to newText.isEmpty()
                )
            )
        }
    }

    /**
     * Sendet eine Test-SMS an die eigene Nummer (aus SIM-Verwaltung).
     *
     * Hinweis: Wenn SMS-Weiterleitung aktiviert ist, wird die Test-SMS durch den
     * regulären Weiterleitungsmechanismus (SmsForegroundService) verarbeitet,
     * was bereits die Email-Weiterleitung beinhaltet.
     */
    fun sendTestSms() {
        // Verwendung der eigenen Telefonnummer aus SIM-Verwaltung
        val simNumbers = prefsManager.getSimPhoneNumbers()
        val receiver = simNumbers.values.firstOrNull()

        if (receiver.isNullOrEmpty()) {
            LoggingManager.logWarning(
                component = "TestUtilsViewModel",
                action = "TEST_SMS_FAILED",
                message = "Test-SMS konnte nicht gesendet werden - keine SIM-Nummer verfügbar",
                details = mapOf("reason" to "no_sim_number_available")
            )
            SnackbarManager.showError("Keine SIM-Telefonnummer verfügbar")
            return
        }

        val testSmsText = prefsManager.getTestSmsText()
        val smsSent = PhoneSmsUtils.sendTestSms(application, receiver, testSmsText)

        if (smsSent) {
            val sanitizedText = sanitizeSmsTextForLogging(testSmsText)
            LoggingManager.logInfo(
                component = "TestUtilsViewModel",
                action = "TEST_SMS_SENT",
                message = "Test-SMS wurde versendet",
                details = mapOf(
                    "receiver" to receiver,
                    "text" to sanitizedText,
                    "note" to "Email-Weiterleitung erfolgt durch regulären SMS-Empfang"
                )
            )
        } else {
            val sanitizedText = sanitizeSmsTextForLogging(testSmsText)
            LoggingManager.logError(
                component = "TestUtilsViewModel",
                action = "TEST_SMS_FAILED",
                message = "Fehler beim Versenden der Test-SMS",
                details = mapOf(
                    "receiver" to receiver,
                    "text" to sanitizedText
                )
            )
        }
    }

    /**
     * Anonymisiert SMS-Text für Logging-Zwecke.
     * Bei Texten über 20 Zeichen werden nur die ersten und letzten 10 Zeichen geloggt.
     */
    private fun sanitizeSmsTextForLogging(text: String): String {
        return if (text.length > 20) {
            "${text.take(10)}...${text.takeLast(10)}"
        } else {
            text
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
                return TestUtilsViewModel(application, prefsManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
