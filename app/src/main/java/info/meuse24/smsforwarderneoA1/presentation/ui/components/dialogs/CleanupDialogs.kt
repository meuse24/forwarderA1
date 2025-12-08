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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import info.meuse24.smsforwarderneoA1.ContactsViewModel
import info.meuse24.smsforwarderneoA1.R
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
        title = { Text(stringResource(R.string.msg_exit_app)) },
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
                    text = stringResource(R.string.msg_please_wait),
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
    val context = LocalContext.current

    val (title, message) = when (error) {
        is NavigationViewModel.ErrorDialogState.DeactivationError ->
            Pair(context.getString(R.string.error_deactivation_failed), error.message)

        is NavigationViewModel.ErrorDialogState.TimeoutError ->
            Pair(
                context.getString(R.string.error_timeout),
                context.getString(R.string.error_timeout_message)
            )

        is NavigationViewModel.ErrorDialogState.GeneralError ->
            Pair(
                context.getString(R.string.error_general),
                context.getString(R.string.error_general_message, error.error.message ?: "Unknown error")
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
                Text(stringResource(R.string.btn_retry))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onIgnore) {
                    Text(stringResource(R.string.btn_ignore))
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        }
    )
}
