package info.meuse24.smsforwarderneoA1.presentation.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import info.meuse24.smsforwarderneoA1.presentation.viewmodel.EmailViewModel
import info.meuse24.smsforwarderneoA1.presentation.viewmodel.NavigationViewModel
import info.meuse24.smsforwarderneoA1.presentation.viewmodel.TestUtilsViewModel
import info.meuse24.smsforwarderneoA1.ui.theme.BackgroundGradientLight

@Composable
fun SettingsScreen(
    viewModel: ContactsViewModel,
    emailViewModel: EmailViewModel,
    testUtilsViewModel: TestUtilsViewModel,
    navigationViewModel: NavigationViewModel
) {
    val scrollState = rememberScrollState()
    LocalFocusManager.current
    var isAnyFieldFocused by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    val sectionTitleStyle = MaterialTheme.typography.titleMedium

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGradientLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PhoneSettingsSection(
                viewModel = viewModel,
                onFocusChanged = { isAnyFieldFocused = it },
                sectionTitleStyle = sectionTitleStyle
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp
            )

            SimManagementSection(
                viewModel = viewModel,
                onFocusChanged = { isAnyFieldFocused = it },
                sectionTitleStyle = sectionTitleStyle
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp
            )

            SimSelectionSection(
                viewModel = viewModel,
                sectionTitleStyle = sectionTitleStyle
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp
            )

            AppSettingsSection(
                viewModel = viewModel,
                emailViewModel = emailViewModel,
                testUtilsViewModel = testUtilsViewModel,
                navigationViewModel = navigationViewModel,
                onFocusChanged = { isAnyFieldFocused = it },
                sectionTitleStyle = sectionTitleStyle
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp
            )

            MmiCodeSettingsSection(
                viewModel = viewModel,
                onFocusChanged = { isAnyFieldFocused = it },
                sectionTitleStyle = sectionTitleStyle
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp
            )

            EmailSettingsSection(
                emailViewModel = emailViewModel,
                sectionTitleStyle = sectionTitleStyle
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp
            )

            LogSettingsSection(
                sectionTitleStyle = sectionTitleStyle,
                onDeleteLogs = { showPinDialog = true },
                onChangePin = { showChangePinDialog = true },
                viewModel = viewModel,
                onFocusChanged = { isAnyFieldFocused = it }
            )

        }

        // PIN Dialoge
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
