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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import info.meuse24.smsforwarderneoA1.presentation.viewmodel.EmailViewModel
import info.meuse24.smsforwarderneoA1.SnackbarManager

@Composable
fun EmailSettingsSection(
    emailViewModel: EmailViewModel,
    sectionTitleStyle: TextStyle
) {
    val smtpHost by emailViewModel.smtpHost.collectAsState()
    val smtpPort by emailViewModel.smtpPort.collectAsState()
    val smtpUsername by emailViewModel.smtpUsername.collectAsState()
    val smtpPassword by emailViewModel.smtpPassword.collectAsState()
    var isPasswordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "E-Mail-Einstellungen",
            style = sectionTitleStyle,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = smtpHost,
            onValueChange = { emailViewModel.updateSmtpSettings(it, smtpPort, smtpUsername, smtpPassword) },
            label = { Text("SMTP Server") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = smtpPort.toString(),
            onValueChange = {
                val newPort = it.toIntOrNull() ?: smtpPort
                emailViewModel.updateSmtpSettings(smtpHost, newPort, smtpUsername, smtpPassword)
            },
            label = { Text("TLS Port") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = smtpUsername,
            onValueChange = { emailViewModel.updateSmtpSettings(smtpHost, smtpPort, it, smtpPassword) },
            label = { Text("Benutzername/Email-Adresse") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = smtpPassword,
            onValueChange = { emailViewModel.updateSmtpSettings(smtpHost, smtpPort, smtpUsername, it) },
            label = { Text("Passwort") },
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
                            "Passwort verbergen"
                        else
                            "Passwort anzeigen"
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
                    SnackbarManager.showSuccess("Benutzername und App-spezifisches Passwort eingeben.")
                }
            ) {
                Text("Gmail")
            }
            Button(
                onClick = {
                    emailViewModel.updateSmtpSettings(
                        "mail.gmx.net",
                        587,
                        "",
                        ""
                    )
                    SnackbarManager.showSuccess("Email-Adresse und Passwort eingeben.")
                }
            ) {
                Text("GMX")
            }
        }

        Text(
            text = "Hinweis: Für Gmail wird ein App-spezifisches Passwort benötigt und für GMX muss IMAP oder POP3 in den Einstellungen aktiviert werden.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
