package info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import info.meuse24.smsforwarderneoA1.R
import info.meuse24.smsforwarderneoA1.domain.model.Contact
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.AppAlertDialog
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.DialogConfirmButton
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.DialogDefaults
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.DialogDismissButton
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.DialogButtonSpacerVertical

/**
 * Exit confirmation dialog.
 *
 * Shows when the user attempts to exit the app. If forwarding is active,
 * allows the user to choose whether to keep forwarding active or deactivate it.
 *
 * @param contact Currently active forwarding contact, or null if no forwarding is active
 * @param initialKeepForwarding Initial state of the "keep forwarding on exit" preference
 * @param onDismiss Callback when dialog is dismissed without action
 * @param onConfirm Callback when user confirms exit (receives keepForwarding flag)
 * @param onSettings Callback when user wants to open settings instead
 * @param updateKeepForwardingOnExit Callback to update the "keep forwarding on exit" preference
 */
@Composable
fun ExitDialog(
    contact: Contact?,
    initialKeepForwarding: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Boolean) -> Unit,
    onSettings: () -> Unit,
    updateKeepForwardingOnExit: (Boolean) -> Unit
) {
    var keepForwarding by remember { mutableStateOf(initialKeepForwarding) }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = stringResource(R.string.cd_exit_icon),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = stringResource(R.string.dialog_title_exit_app),
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = DialogDefaults.StandardPadding),
                verticalArrangement = Arrangement.spacedBy(DialogDefaults.StandardSpacing)
            ) {
                contact?.let {
                    Text(
                        text = stringResource(R.string.msg_active_forwarding_to, it.name),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (contact != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = keepForwarding,
                            onCheckedChange = {
                                keepForwarding = it
                                updateKeepForwardingOnExit(it)
                            }
                        )
                        Spacer(modifier = Modifier.width(DialogDefaults.CompactSpacing))
                        Text(
                            text = stringResource(R.string.toggle_keep_forwarding_on_exit),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(DialogDefaults.CompactSpacing),
                horizontalAlignment = Alignment.End
            ) {
                DialogDismissButton(
                    text = stringResource(R.string.btn_settings),
                    onClick = onSettings,
                    modifier = Modifier.defaultMinSize(minWidth = 140.dp)
                )
                DialogDismissButton(
                    text = stringResource(R.string.btn_cancel),
                    onClick = onDismiss,
                    modifier = Modifier.defaultMinSize(minWidth = 140.dp)
                )
                DialogConfirmButton(
                    text = stringResource(R.string.btn_exit),
                    onClick = { onConfirm(keepForwarding) },
                    modifier = Modifier.defaultMinSize(minWidth = 140.dp)
                )
            }
        },
        dismissButton = {}
    )
}
