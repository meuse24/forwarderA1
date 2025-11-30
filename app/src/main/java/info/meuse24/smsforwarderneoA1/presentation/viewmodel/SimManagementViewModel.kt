package info.meuse24.smsforwarderneoA1.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import info.meuse24.smsforwarderneoA1.LoggingManager
import info.meuse24.smsforwarderneoA1.data.local.SharedPreferencesManager
import info.meuse24.smsforwarderneoA1.domain.model.SimInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for managing SIM card phone numbers and SIM number input dialog.
 *
 * Handles:
 * - Requesting missing SIM numbers from user
 * - Showing/hiding SIM numbers dialog
 * - Saving SIM phone numbers to SharedPreferences
 *
 * Extracted from ContactsViewModel as part of Phase 5 refactoring.
 *
 * @param prefsManager SharedPreferencesManager for persisting SIM numbers
 */
class SimManagementViewModel(
    private val prefsManager: SharedPreferencesManager
) : ViewModel() {

    // SIM Numbers Dialog State
    private val _missingSims = MutableStateFlow<List<SimInfo>>(emptyList())
    val missingSims: StateFlow<List<SimInfo>> = _missingSims.asStateFlow()

    private val _showSimNumbersDialog = MutableStateFlow(false)
    val showSimNumbersDialog: StateFlow<Boolean> = _showSimNumbersDialog.asStateFlow()

    /**
     * Request user to input phone numbers for missing SIM cards.
     *
     * Opens the SIM numbers dialog with the list of SIM cards that need phone numbers.
     *
     * @param sims List of SIM cards (SimInfo) that are missing phone numbers
     */
    fun requestMissingSimNumbers(sims: List<SimInfo>) {
        _missingSims.value = sims
        _showSimNumbersDialog.value = true
    }

    /**
     * Hide the SIM numbers dialog and clear the missing SIMs list.
     */
    fun hideSimNumbersDialog() {
        _showSimNumbersDialog.value = false
        _missingSims.value = emptyList()
    }

    /**
     * Save a phone number for a specific SIM card.
     *
     * Persists the phone number to SharedPreferences using the subscription ID as key.
     * Logs the operation for debugging and monitoring.
     *
     * @param subscriptionId The subscription ID of the SIM card
     * @param phoneNumber The phone number to save (will be trimmed)
     */
    fun saveSimNumber(subscriptionId: Int, phoneNumber: String) {
        try {
            if (phoneNumber.isNotBlank()) {
                prefsManager.setSimPhoneNumber(subscriptionId, phoneNumber.trim())
                LoggingManager.logInfo(
                    component = "SimManagementViewModel",
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
                component = "SimManagementViewModel",
                action = "SAVE_SIM_NUMBER",
                message = "Fehler beim Speichern der SIM-Nummer",
                error = e,
                details = mapOf("subscription_id" to subscriptionId)
            )
        }
    }

    /**
     * Factory for creating SimManagementViewModel instances.
     *
     * Provides SharedPreferencesManager dependency from AppContainer.
     */
    class Factory(
        private val prefsManager: SharedPreferencesManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SimManagementViewModel::class.java)) {
                return SimManagementViewModel(prefsManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
