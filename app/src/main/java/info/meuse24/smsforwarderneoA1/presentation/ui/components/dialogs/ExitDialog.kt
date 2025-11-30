package info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import info.meuse24.smsforwarderneoA1.domain.model.Contact

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

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text("App beenden")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                contact?.let {
                    Text(
                        text = "Aktive Weiterleitung zu: ${it.name}",
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
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Weiterleitung beim Beenden beibehalten",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(keepForwarding) }
            ) {
                Text("Beenden")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onSettings) {
                    Text("Einstellungen")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("Abbrechen")
                }
            }
        }
    )
}
