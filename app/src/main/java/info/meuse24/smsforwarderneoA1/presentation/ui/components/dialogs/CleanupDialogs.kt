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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import info.meuse24.smsforwarderneoA1.ContactsViewModel
import info.meuse24.smsforwarderneoA1.R
import info.meuse24.smsforwarderneoA1.presentation.viewmodel.NavigationViewModel
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.AppAlertDialog
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.DialogButtonRow
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.DialogButtonSpacer
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.DialogConfirmButton
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.DialogDefaults
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.DialogDismissButton

/**
 * Progress dialog shown during app cleanup/exit process.
 *
 * This dialog is non-dismissible and shows a loading indicator while the app
 * performs cleanup operations (e.g., deactivating forwarding) before exiting.
 */
@Composable
fun CleanupProgressDialog() {
    AppAlertDialog(
        onDismissRequest = { /* Nicht abbrechbar */ },
        properties = DialogDefaults.CriticalDialogProperties,
        title = stringResource(R.string.msg_exit_app),
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(DialogDefaults.StandardPadding),
                horizontalArrangement = Arrangement.spacedBy(DialogDefaults.StandardSpacing),
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

    AppAlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = stringResource(R.string.cd_info_icon),
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = title,
        text = { Text(message) },
        confirmButton = {
            DialogButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                DialogDismissButton(
                    text = stringResource(R.string.btn_ignore),
                    onClick = onIgnore
                )
                DialogButtonSpacer()
                DialogDismissButton(
                    text = stringResource(R.string.btn_cancel),
                    onClick = onDismiss
                )
                DialogButtonSpacer()
                DialogConfirmButton(
                    text = stringResource(R.string.btn_retry),
                    onClick = onRetry
                )
            }
        },
        dismissButton = {}
    )
}
