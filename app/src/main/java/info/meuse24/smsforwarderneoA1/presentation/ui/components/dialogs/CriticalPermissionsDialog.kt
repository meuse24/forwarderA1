package info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Dialog der angezeigt wird wenn kritische Berechtigungen fehlen.
 *
 * Zeigt eine Liste fehlender Berechtigungen und bietet dem User zwei Optionen:
 * 1. Berechtigungen erteilen (öffnet Permission-Request)
 * 2. App beenden
 *
 * @param missingPermissions Liste der fehlenden Berechtigungen
 * @param onRequestPermissions Callback wenn User "Berechtigungen erteilen" wählt
 * @param onExitApp Callback wenn User "App beenden" wählt
 */
@Composable
fun CriticalPermissionsDialog(
    missingPermissions: List<String>,
    onRequestPermissions: () -> Unit,
    onExitApp: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Verhindere Schließen durch Tippen außerhalb */ },
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Erforderliche Berechtigungen fehlen",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Diese App benötigt zwingend folgende Berechtigungen um zu funktionieren:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Liste der fehlenden Berechtigungen
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    missingPermissions.forEach { permission ->
                        Row(
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = permission.toReadableName(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Ohne diese Berechtigungen kann die App SMS nicht empfangen oder weiterleiten.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onRequestPermissions,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Berechtigungen erteilen")
            }
        },
        dismissButton = {
            Button(
                onClick = onExitApp,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("App beenden")
                }
            }
        }
    )
}

/**
 * Konvertiert Android Permission-Namen in lesbare deutsche Beschreibungen.
 */
private fun String.toReadableName(): String {
    return when (this) {
        Manifest.permission.READ_CONTACTS ->
            "Kontakte lesen - Für die Auswahl des Weiterleitungsziels"
        Manifest.permission.SEND_SMS ->
            "SMS senden - Zum Weiterleiten von Nachrichten"
        Manifest.permission.RECEIVE_SMS ->
            "SMS empfangen - Zum Empfangen eingehender Nachrichten"
        Manifest.permission.CALL_PHONE ->
            "Telefon - Für die Anrufweiterleitung (MMI-Codes)"
        Manifest.permission.READ_PHONE_STATE ->
            "Telefonzustand - Zum Lesen von SIM-Informationen"
        Manifest.permission.READ_PHONE_NUMBERS ->
            "Telefonnummern - Zum Erkennen der eigenen Nummer"
        Manifest.permission.POST_NOTIFICATIONS ->
            "Benachrichtigungen - Für den Hintergrunddienst"
        else -> this.substringAfterLast('.').replace('_', ' ')
    }
}
