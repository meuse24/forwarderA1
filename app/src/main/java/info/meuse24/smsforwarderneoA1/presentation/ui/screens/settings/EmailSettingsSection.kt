package info.meuse24.smsforwarderneoA1.presentation.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import info.meuse24.smsforwarderneoA1.R
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import info.meuse24.smsforwarderneoA1.domain.model.EmailTransportSecurity
import info.meuse24.smsforwarderneoA1.presentation.viewmodel.EmailViewModel
import info.meuse24.smsforwarderneoA1.SnackbarManager
import info.meuse24.smsforwarderneoA1.AppContainer

@Composable
fun EmailSettingsSection(
    emailViewModel: EmailViewModel,
    sectionTitleStyle: TextStyle,
    onMailTabVisibilityChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val smtpHost by emailViewModel.smtpHost.collectAsState()
    val smtpUsername by emailViewModel.smtpUsername.collectAsState()
    val smtpPassword by emailViewModel.smtpPassword.collectAsState()
    val smtpPort by emailViewModel.smtpPort.collectAsState()
    val smtpPortInput by emailViewModel.smtpPortInput.collectAsState()
    val smtpPortError by emailViewModel.smtpPortError.collectAsState()
    val smtpSecurity by emailViewModel.smtpSecurity.collectAsState()
    val smtpFromAddress by emailViewModel.smtpFromAddress.collectAsState()
    val smtpFromError by emailViewModel.smtpFromError.collectAsState()
    var isPasswordVisible by remember { mutableStateOf(false) }
    var mailScreenVisible by remember { mutableStateOf(AppContainer.requirePrefsManager().isMailScreenVisible()) }
    val smtpSettingsComplete = smtpHost.isNotBlank() && smtpUsername.isNotBlank() && smtpPassword.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.label_mail_tab_visible),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.desc_show_mail_tab),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = mailScreenVisible,
                onCheckedChange = { newValue ->
                    onMailTabVisibilityChanged(newValue)
                    mailScreenVisible = newValue
                    if (newValue && !smtpSettingsComplete) {
                        SnackbarManager.showWarning(context.getString(R.string.warning_incomplete_smtp_settings))
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = smtpHost,
            onValueChange = { emailViewModel.updateSmtpSettings(it, smtpPort, smtpUsername, smtpPassword) },
            label = { Text(stringResource(R.string.label_smtp_host)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Der Verschluesselungsmodus ist explizit, weil er sich technisch unterscheidet: STARTTLS
        // hebt eine Klartextverbindung an, SSL/TLS verschluesselt ab dem ersten Byte. Frueher war
        // nur STARTTLS implementiert - ein eingetragener Port 465 konnte nie funktionieren.
        Text(
            text = stringResource(R.string.label_smtp_security),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            EmailTransportSecurity.entries.forEachIndexed { index, security ->
                SegmentedButton(
                    selected = smtpSecurity == security,
                    onClick = { emailViewModel.updateSmtpSecurity(security) },
                    shape = SegmentedButtonDefaults.itemShape(index, EmailTransportSecurity.entries.size)
                ) {
                    Text(
                        stringResource(
                            when (security) {
                                EmailTransportSecurity.STARTTLS -> R.string.option_smtp_starttls
                                EmailTransportSecurity.IMPLICIT_TLS -> R.string.option_smtp_implicit_tls
                            }
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = smtpPortInput,
            onValueChange = emailViewModel::updateSmtpPortInput,
            label = { Text(stringResource(R.string.label_smtp_port)) },
            isError = smtpPortError != null,
            supportingText = smtpPortError?.let { { Text(stringResource(it)) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = smtpUsername,
            onValueChange = { emailViewModel.updateSmtpSettings(smtpHost, smtpPort, it, smtpPassword) },
            label = { Text(stringResource(R.string.label_smtp_username)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = smtpPassword,
            onValueChange = { emailViewModel.updateSmtpSettings(smtpHost, smtpPort, smtpUsername, it) },
            label = { Text(stringResource(R.string.label_smtp_password)) },
            visualTransformation = if (isPasswordVisible)
                VisualTransformation.None
            else
                PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible)
                            Icons.Default.Visibility
                        else
                            Icons.Default.VisibilityOff,
                        contentDescription = if (isPasswordVisible)
                            stringResource(R.string.desc_hide_password)
                        else
                            stringResource(R.string.desc_show_password)
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Getrennt vom Benutzernamen, weil beides bei vielen Anbietern nicht dasselbe ist: Ein
        // Login wie `u1234567` ergibt keine gueltige Absenderadresse.
        OutlinedTextField(
            value = smtpFromAddress,
            onValueChange = emailViewModel::updateSmtpFromAddress,
            label = { Text(stringResource(R.string.label_smtp_from_address)) },
            isError = smtpFromError != null,
            supportingText = {
                Text(stringResource(smtpFromError ?: R.string.desc_smtp_from_address))
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    emailViewModel.updateSmtpSettings(
                        "smtp.gmail.com",
                        587,
                        "",
                        ""
                    )
                    SnackbarManager.showSuccess(context.getString(R.string.msg_gmail_setup_info))
                }
            ) {
                Text(stringResource(R.string.btn_preset_gmail))
            }
            Button(
                onClick = {
                    emailViewModel.updateSmtpSettings(
                        "mail.gmx.net",
                        587,
                        "",
                        ""
                    )
                    SnackbarManager.showSuccess(context.getString(R.string.msg_gmx_setup_info))
                }
            ) {
                Text(stringResource(R.string.btn_preset_gmx))
            }
        }

        Text(
            text = stringResource(R.string.note_email_setup_info),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
