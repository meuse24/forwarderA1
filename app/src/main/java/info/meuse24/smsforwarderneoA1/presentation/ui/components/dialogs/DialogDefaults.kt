package info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * Centralized design defaults for dialogs to ensure consistent spacing,
 * sizing, and behavior across the app.
 */
@Immutable
object DialogDefaults {
    // Spacing
    val CompactSpacing = 8.dp
    val StandardSpacing = 12.dp
    val LargeSpacing = 16.dp
    val ExtraLargeSpacing = 24.dp

    // Padding
    val CompactPadding = 12.dp
    val StandardPadding = 16.dp
    val LargePadding = 24.dp
    val ExtraLargePadding = 32.dp

    // Corner Radius
    val CornerRadius = 16.dp
    val LargeCornerRadius = 24.dp
    val FullscreenCornerRadius = 32.dp
    val CornerShape = RoundedCornerShape(CornerRadius)
    val FullscreenCornerShape = RoundedCornerShape(FullscreenCornerRadius)

    // Icon Sizes
    val SmallIconSize = 24.dp
    val StandardIconSize = 48.dp
    val LargeIconSize = 80.dp
    val FullscreenIconSize = 100.dp

    // Elevation
    val StandardElevation = 8.dp
    val FullscreenElevation = 24.dp

    // Animations
    val StandardAnimationDuration = 300
    val CountdownDuration = 4000 // 4 seconds

    // Dialog Properties
    val CriticalDialogProperties = DialogProperties(
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
        usePlatformDefaultWidth = true
    )

    val StandardDialogProperties = DialogProperties(
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        usePlatformDefaultWidth = true
    )

    val FullscreenDialogProperties = DialogProperties(
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
        usePlatformDefaultWidth = false
    )
}
