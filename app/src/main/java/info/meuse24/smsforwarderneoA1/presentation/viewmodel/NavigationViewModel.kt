package info.meuse24.smsforwarderneoA1.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import info.meuse24.smsforwarderneoA1.data.local.SharedPreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel für Navigation und UI-State-Management.
 *
 * Verantwortlichkeiten:
 * - Exit-Dialog State
 * - Navigation zwischen Screens
 * - Error State Management
 */
class NavigationViewModel(
    private val prefsManager: SharedPreferencesManager
) : ViewModel() {

    /**
     * Sealed class für Error-Dialog States.
     */
    sealed class ErrorDialogState {
        data class DeactivationError(val message: String) : ErrorDialogState()
        data object TimeoutError : ErrorDialogState()
        data class GeneralError(val error: Exception) : ErrorDialogState()
    }

    // StateFlows
    private val _showExitDialog = MutableStateFlow(false)
    val showExitDialog: StateFlow<Boolean> = _showExitDialog.asStateFlow()

    private val _navigationTarget = MutableStateFlow<String?>(null)
    val navigationTarget: StateFlow<String?> = _navigationTarget.asStateFlow()

    private val _errorState = MutableStateFlow<ErrorDialogState?>(null)
    val errorState: StateFlow<ErrorDialogState?> = _errorState.asStateFlow()

    /**
     * Zeigt den Exit-Dialog an.
     */
    fun onShowExitDialog() {
        _showExitDialog.value = true
    }

    /**
     * Versteckt den Exit-Dialog.
     */
    fun hideExitDialog() {
        _showExitDialog.value = false
    }

    /**
     * Navigiert zur Settings-Seite.
     */
    fun navigateToSettings() {
        _navigationTarget.value = "setup"
    }

    /**
     * Setzt Navigation-State zurück nach erfolgreicher Navigation.
     */
    fun onNavigated() {
        _navigationTarget.value = null
    }

    /**
     * Löscht Error-State.
     */
    fun clearErrorState() {
        _errorState.value = null
    }

    /**
     * Setzt Error-State (für Error-Dialogs).
     */
    fun setErrorState(error: ErrorDialogState) {
        _errorState.value = error
    }

    /**
     * Factory für NavigationViewModel.
     */
    class Factory(
        private val prefsManager: SharedPreferencesManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NavigationViewModel::class.java)) {
                return NavigationViewModel(prefsManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
