package info.meuse24.smsforwarderneoA1.presentation.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import info.meuse24.smsforwarderneoA1.AppContainer
import info.meuse24.smsforwarderneoA1.ContactsViewModel
import info.meuse24.smsforwarderneoA1.LoggingManager
import info.meuse24.smsforwarderneoA1.SnackbarManager
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.ChangePinDialog
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.PinDialog

@Composable
fun SettingsScreen(viewModel: ContactsViewModel) {
    val scrollState = rememberScrollState()
    LocalFocusManager.current
    var isAnyFieldFocused by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    val sectionTitleStyle = MaterialTheme.typography.titleMedium

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {

        PhoneSettingsSection(
            viewModel = viewModel,
            onFocusChanged = { isAnyFieldFocused = it },
            sectionTitleStyle = sectionTitleStyle)

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        SimManagementSection(
            viewModel = viewModel,
            onFocusChanged = { isAnyFieldFocused = it },
            sectionTitleStyle = sectionTitleStyle
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        AppSettingsSection(
            viewModel = viewModel,
            onFocusChanged = { isAnyFieldFocused = it },
            sectionTitleStyle = sectionTitleStyle
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        MmiCodeSettingsSection(
            viewModel = viewModel,
            onFocusChanged = { isAnyFieldFocused = it },
            sectionTitleStyle = sectionTitleStyle
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        EmailSettingsSection(
            viewModel = viewModel,
            sectionTitleStyle = sectionTitleStyle
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // Neue Log Settings Section
        LogSettingsSection(
            sectionTitleStyle = sectionTitleStyle,
            onDeleteLogs = { showPinDialog = true },
            onChangePin = { showChangePinDialog = true }
        )

        // Die existierenden PIN-Dialoge aus dem LogScreen
        if (showPinDialog) {
            PinDialog(
                storedPin = AppContainer.requirePrefsManager().getLogPIN(),
                onPinCorrect = {
                    AppContainer.requireLogger().clearLog()
                    LoggingManager.logInfo(
                        component = "SettingsScreen",
                        action = "CLEAR_LOGS",
                        message = "Log-Einträge wurden gelöscht"
                    )
                    SnackbarManager.showSuccess("Logs wurden gelöscht")
                    showPinDialog = false
                },
                onDismiss = { showPinDialog = false }
            )
        }

        if (showChangePinDialog) {
            ChangePinDialog(
                storedPin = AppContainer.requirePrefsManager().getLogPIN(),
                onPinChanged = { newPin ->
                    AppContainer.requirePrefsManager().setLogPIN(newPin)
                    LoggingManager.logInfo(
                        component = "SettingsScreen",
                        action = "CHANGE_PIN",
                        message = "Log-PIN wurde geändert"
                    )
                    SnackbarManager.showSuccess("PIN wurde geändert")
                    showChangePinDialog = false
                },
                onDismiss = { showChangePinDialog = false }
            )
        }

    }
}
