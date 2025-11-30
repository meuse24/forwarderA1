package info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import info.meuse24.smsforwarderneoA1.ContactsViewModel
import info.meuse24.smsforwarderneoA1.presentation.viewmodel.NavigationViewModel

/**
 * Progress dialog shown during app cleanup/exit process.
 *
 * This dialog is non-dismissible and shows a loading indicator while the app
 * performs cleanup operations (e.g., deactivating forwarding) before exiting.
 */
@Composable
fun CleanupProgressDialog() {
    AlertDialog(
        onDismissRequest = { /* Nicht abbrechbar */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        title = { Text("Beende App") },
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Bitte warten...",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = { /* Keine Buttons während des Cleanups */ }
    )
}

/**
 * Error dialog shown when cleanup/exit process fails.
 *
 * Displays the specific error and provides options to retry, ignore the error,
 * or cancel the exit operation.
 *
 * @param error The error state containing error details
 * @param onRetry Callback to retry the failed operation
 * @param onIgnore Callback to ignore the error and proceed with exit
 * @param onDismiss Callback to cancel the exit operation
 */
@Composable
fun CleanupErrorDialog(
    error: NavigationViewModel.ErrorDialogState,
    onRetry: () -> Unit,
    onIgnore: () -> Unit,
    onDismiss: () -> Unit
) {
    val (title, message) = when (error) {
        is NavigationViewModel.ErrorDialogState.DeactivationError ->
            Pair("Deaktivierung fehlgeschlagen", error.message)

        is NavigationViewModel.ErrorDialogState.TimeoutError ->
            Pair(
                "Zeitüberschreitung",
                "Die Deaktivierung der Weiterleitung dauert zu lange."
            )

        is NavigationViewModel.ErrorDialogState.GeneralError ->
            Pair(
                "Fehler",
                "Ein unerwarteter Fehler ist aufgetreten: ${error.error.message}"
            )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onRetry) {
                Text("Wiederholen")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onIgnore) {
                    Text("Ignorieren")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("Abbrechen")
                }
            }
        }
    )
}
