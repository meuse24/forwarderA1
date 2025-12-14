package info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Standardized AlertDialog wrapper using DialogDefaults for shape and elevation.
 */
@Composable
fun AppAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    properties: androidx.compose.ui.window.DialogProperties = DialogDefaults.StandardDialogProperties
 ) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        icon = icon,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = text,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        properties = properties,
        shape = DialogDefaults.CornerShape,
        tonalElevation = DialogDefaults.StandardElevation
    )
}
