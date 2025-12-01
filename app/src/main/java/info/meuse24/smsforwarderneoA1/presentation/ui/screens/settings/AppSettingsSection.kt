package info.meuse24.smsforwarderneoA1.presentation.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import info.meuse24.smsforwarderneoA1.ContactsViewModel
import info.meuse24.smsforwarderneoA1.LoggingManager
import info.meuse24.smsforwarderneoA1.SnackbarManager
import info.meuse24.smsforwarderneoA1.presentation.viewmodel.EmailViewModel
import info.meuse24.smsforwarderneoA1.presentation.viewmodel.TestUtilsViewModel
import info.meuse24.smsforwarderneoA1.presentation.viewmodel.NavigationViewModel

@Composable
fun AppSettingsSection(
    viewModel: ContactsViewModel,
    emailViewModel: EmailViewModel,
    testUtilsViewModel: TestUtilsViewModel,
    navigationViewModel: NavigationViewModel,
    onFocusChanged: (Boolean) -> Unit,
    sectionTitleStyle: TextStyle
) {
    val testSmsText by testUtilsViewModel.testSmsText.collectAsState()
    val testEmailText by emailViewModel.testEmailText.collectAsState()
    val mailScreenVisible by viewModel.mailScreenVisible.collectAsState()
    val internationalDialPrefix by viewModel.internationalDialPrefix.collectAsState()

    // SMTP settings for validation
    val smtpHost by emailViewModel.smtpHost.collectAsState()
    val smtpUsername by emailViewModel.smtpUsername.collectAsState()
    val smtpPassword by emailViewModel.smtpPassword.collectAsState()

    var isTestSmsTextFocused by remember { mutableStateOf(false) }
    var isTestEmailTextFocused by remember { mutableStateOf(false) }
    var isDialPrefixFocused by remember { mutableStateOf(false) }

    // Check if SMTP settings are complete
    val smtpSettingsComplete = remember(smtpHost, smtpUsername, smtpPassword) {
        smtpHost.isNotBlank() && smtpUsername.isNotBlank() && smtpPassword.isNotBlank()
    }

    LaunchedEffect(
        isTestSmsTextFocused,
        isTestEmailTextFocused,
        isDialPrefixFocused
    ) {
        onFocusChanged(
            isTestSmsTextFocused || isTestEmailTextFocused || isDialPrefixFocused
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "App-Einstellungen",
            style = sectionTitleStyle,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = testSmsText,
            onValueChange = { testUtilsViewModel.updateTestSmsText(it) },
            label = { Text("Text der Test-SMS") },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isTestSmsTextFocused = it.isFocused }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = testEmailText,
            onValueChange = { emailViewModel.updateTestEmailText(it) },
            label = { Text("Text der Test-Email") },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isTestEmailTextFocused = it.isFocused }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = internationalDialPrefix,
            onValueChange = { newValue ->
                // Nur Ziffern erlauben (0-9)
                if (newValue.all { it.isDigit() } && newValue.length <= 3) {
                    viewModel.updateInternationalDialPrefix(newValue)
                }
            },
            label = { Text("Internationale Anschaltziffernfolge") },
            supportingText = {
                Text("Ersetzt '+' in Telefonnummern (z.B. '00' für Österreich)")
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isDialPrefixFocused = it.isFocused },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Mail Screen Visibility Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Mail-Tab anzeigen",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (smtpSettingsComplete)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = if (smtpSettingsComplete)
                        "Zeigt den Mail-Tab in der unteren Navigation an"
                    else
                        "SMTP-Einstellungen müssen vollständig sein",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (smtpSettingsComplete)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.error
                )
            }
            Switch(
                checked = mailScreenVisible,
                enabled = smtpSettingsComplete,
                onCheckedChange = { checked ->
                    if (smtpSettingsComplete) {
                        viewModel.updateMailScreenVisibility(checked)
                    } else {
                        SnackbarManager.showWarning(
                            "Bitte konfigurieren Sie zuerst die SMTP-Einstellungen " +
                            "(Server, Benutzername, Passwort) im Abschnitt 'E-Mail-Einstellungen'"
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Battery Optimization Status Indicator
        BatteryOptimizationStatusCard()
    }
}

@Composable
fun BatteryOptimizationStatusCard() {
    val context = LocalContext.current
    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    val isIgnoringBatteryOptimizations = remember(powerManager) {
        powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isIgnoringBatteryOptimizations) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isIgnoringBatteryOptimizations) {
                        Icons.Default.BatteryFull
                    } else {
                        Icons.Default.BatteryAlert
                    },
                    contentDescription = null,
                    tint = if (isIgnoringBatteryOptimizations) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (isIgnoringBatteryOptimizations) {
                            "Akku-Optimierung deaktiviert"
                        } else {
                            "Akku-Optimierung aktiv"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isIgnoringBatteryOptimizations) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                    Text(
                        text = if (isIgnoringBatteryOptimizations) {
                            "Die App kann zuverlässig im Hintergrund arbeiten"
                        } else {
                            "SMS-Weiterleitung könnte im Hintergrund eingeschränkt werden"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isIgnoringBatteryOptimizations) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
            }

            if (!isIgnoringBatteryOptimizations) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)

                            LoggingManager.logInfo(
                                component = "AppSettingsSection",
                                action = "BATTERY_OPT_SETTINGS",
                                message = "Battery Optimization Einstellungen geöffnet"
                            )
                        } catch (e: Exception) {
                            LoggingManager.logError(
                                component = "AppSettingsSection",
                                action = "BATTERY_OPT_SETTINGS_ERROR",
                                message = "Fehler beim Öffnen der Einstellungen",
                                error = e
                            )
                            SnackbarManager.showError("Einstellungen konnten nicht geöffnet werden")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Jetzt deaktivieren")
                }
            }
        }
    }
}
