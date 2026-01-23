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

    // Edit Single SIM Dialog State
    private val _editingSim = MutableStateFlow<SimInfo?>(null)
    val editingSim: StateFlow<SimInfo?> = _editingSim.asStateFlow()
    
    private val _editingSimNumber = MutableStateFlow("")
    val editingSimNumber: StateFlow<String> = _editingSimNumber.asStateFlow()

    private val _storedNumbers = MutableStateFlow<Map<Int, String>>(emptyMap())
    val storedNumbers: StateFlow<Map<Int, String>> = _storedNumbers.asStateFlow()

    init {
        loadStoredNumbers()
    }

    fun loadStoredNumbers() {
        try {
            _storedNumbers.value = prefsManager.getSimPhoneNumbers()
        } catch (e: Exception) {
            LoggingManager.logError(
                component = "SimManagementViewModel",
                action = "LOAD_NUMBERS_ERROR",
                message = "Fehler beim Laden der gespeicherten SIM-Nummern",
                error = e
            )
            _storedNumbers.value = emptyMap()
        }
    }

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
     * Show dialog to edit a specific SIM number.
     */
    fun showEditSimDialog(sim: SimInfo, currentNumber: String) {
        _editingSim.value = sim
        _editingSimNumber.value = currentNumber
    }

    /**
     * Hide the edit SIM dialog.
     */
    fun hideEditSimDialog() {
        _editingSim.value = null
        _editingSimNumber.value = ""
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
                loadStoredNumbers()
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
