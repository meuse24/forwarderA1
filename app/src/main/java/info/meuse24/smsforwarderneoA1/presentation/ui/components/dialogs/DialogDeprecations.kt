package info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * Deprecated shims to ease migration from legacy dialog calls to the new components.
 */
@Deprecated(
    message = "Use AppAlertDialog with DialogDefaults instead",
    replaceWith = ReplaceWith("AppAlertDialog(onDismissRequest, title, icon = icon, text = text, confirmButton = confirmButton, dismissButton = dismissButton, properties = properties)"),
    level = DeprecationLevel.WARNING
)
@Composable
fun LegacySimpleDialog(
    title: String,
    onDismissRequest: () -> Unit,
    text: (@Composable () -> Unit)? = null,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    properties: androidx.compose.ui.window.DialogProperties = DialogDefaults.StandardDialogProperties
) {
    AppAlertDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        icon = icon,
        text = text,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        properties = properties
    )
}

@Deprecated(
    message = "Use AppFullscreenDialog instead",
    replaceWith = ReplaceWith("AppFullscreenDialog(onDismissRequest, modifier, properties, backgroundGradient) { content() }"),
    level = DeprecationLevel.WARNING
)
@Composable
fun LegacyFullscreenDialog(
    onDismissRequest: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    properties: androidx.compose.ui.window.DialogProperties = DialogDefaults.FullscreenDialogProperties,
    backgroundGradient: androidx.compose.ui.graphics.Brush = androidx.compose.ui.graphics.Brush.radialGradient(
        colors = listOf(
            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f),
            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.95f)
        )
    ),
    content: @Composable () -> Unit
) {
    AppFullscreenDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        properties = properties,
        backgroundGradient = backgroundGradient
    ) {
        content()
    }
}
