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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import info.meuse24.smsforwarderneoA1.R

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
    val context = LocalContext.current

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
                text = stringResource(R.string.dialog_title_permissions_required),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.msg_permissions_required),
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
                                text = permission.toReadableName(context),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.msg_permissions_required_explanation),
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
                Text(stringResource(R.string.btn_grant_permissions))
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
                    Text(stringResource(R.string.btn_exit_app))
                }
            }
        }
    )
}

/**
 * Konvertiert Android Permission-Namen in lesbare Beschreibungen.
 */
private fun String.toReadableName(context: android.content.Context): String {
    return when (this) {
        Manifest.permission.READ_CONTACTS ->
            context.getString(R.string.perm_contacts_desc)
        Manifest.permission.SEND_SMS ->
            context.getString(R.string.perm_send_sms_desc)
        Manifest.permission.RECEIVE_SMS ->
            context.getString(R.string.perm_receive_sms_desc)
        Manifest.permission.CALL_PHONE ->
            context.getString(R.string.perm_call_forwarding_desc)
        Manifest.permission.READ_PHONE_STATE ->
            context.getString(R.string.perm_phone_state_desc)
        Manifest.permission.READ_PHONE_NUMBERS ->
            context.getString(R.string.perm_phone_number_desc)
        Manifest.permission.POST_NOTIFICATIONS ->
            context.getString(R.string.perm_notifications_desc)
        else -> this.substringAfterLast('.').replace('_', ' ')
    }
}
