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
import info.meuse24.smsforwarderneoA1.presentation.viewmodel.EmailViewModel
import info.meuse24.smsforwarderneoA1.SnackbarManager
import info.meuse24.smsforwarderneoA1.AppContainer

@Composable
fun EmailSettingsSection(
    emailViewModel: EmailViewModel,
    sectionTitleStyle: TextStyle
) {
    val context = LocalContext.current
    val smtpHost by emailViewModel.smtpHost.collectAsState()
    val smtpPort by emailViewModel.smtpPort.collectAsState()
    val smtpUsername by emailViewModel.smtpUsername.collectAsState()
    val smtpPassword by emailViewModel.smtpPassword.collectAsState()
    var isPasswordVisible by remember { mutableStateOf(false) }
    var mailScreenVisible by remember { mutableStateOf(AppContainer.requirePrefsManager().isMailScreenVisible()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.label_mail_tab_visible),
                style = MaterialTheme.typography.bodyMedium
            )
            Switch(
                checked = mailScreenVisible,
                onCheckedChange = { newValue ->
                    AppContainer.requirePrefsManager().setMailScreenVisible(newValue)
                    mailScreenVisible = newValue
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

        OutlinedTextField(
            value = smtpPort.toString(),
            onValueChange = {
                val newPort = it.toIntOrNull() ?: smtpPort
                emailViewModel.updateSmtpSettings(smtpHost, newPort, smtpUsername, smtpPassword)
            },
            label = { Text(stringResource(R.string.label_smtp_port)) },
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
