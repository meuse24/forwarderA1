package info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog

/**
 * Fullscreen dialog wrapper with default gradient background.
 */
@Composable
fun AppFullscreenDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: androidx.compose.ui.window.DialogProperties = DialogDefaults.FullscreenDialogProperties,
    backgroundGradient: Brush = Brush.radialGradient(
        colors = listOf(
            Color.Black.copy(alpha = 0.7f),
            Color.Black.copy(alpha = 0.95f)
        )
    ),
    content: @Composable BoxScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundGradient),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
