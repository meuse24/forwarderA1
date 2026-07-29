package info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import info.meuse24.smsforwarderneoA1.R
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.AppAlertDialog
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.DialogConfirmButton
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.DialogDefaults
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.DialogDestructiveButton

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

    AppAlertDialog(
        onDismissRequest = { /* Verhindere Schließen durch Tippen außerhalb */ },
        properties = DialogDefaults.CriticalDialogProperties,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = stringResource(R.string.cd_warning_icon),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(DialogDefaults.StandardIconSize)
            )
        },
        title = stringResource(R.string.dialog_title_permissions_required),
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(DialogDefaults.StandardSpacing)
            ) {
                Text(
                    text = stringResource(R.string.msg_permissions_required),
                    style = MaterialTheme.typography.bodyMedium
                )

                // Liste der fehlenden Berechtigungen
                Column(
                    verticalArrangement = Arrangement.spacedBy(DialogDefaults.CompactSpacing)
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

                Text(
                    text = stringResource(R.string.msg_permissions_required_explanation),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            DialogConfirmButton(
                text = stringResource(R.string.btn_grant_permissions),
                onClick = onRequestPermissions
            )
        },
        dismissButton = {
            DialogDestructiveButton(
                text = stringResource(R.string.btn_exit_app),
                onClick = onExitApp
            )
        }
    )
}

/**
 * Konvertiert Android Permission-Namen in lesbare Beschreibungen.
 */
private fun String.toReadableName(context: android.content.Context): String {
    return when (this) {
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
