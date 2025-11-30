package info.meuse24.smsforwarderneoA1.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Gradient definitions for SMS Forwarder Neo
 * Provides reusable gradient brushes for backgrounds, buttons, and cards
 */

// Primary Gradients
val PrimaryGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF2196F3), // Material Blue 500
        Color(0xFF1976D2)  // Material Blue 700
    ),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
)

val PrimaryGradientVertical = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF2196F3),
        Color(0xFF1976D2)
    )
)

// Secondary Gradients
val SecondaryGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF00BCD4), // Cyan 500
        Color(0xFF0097A7)  // Cyan 700
    ),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
)

// Tertiary/Accent Gradients
val AccentGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFFC107), // Amber 500
        Color(0xFFFFA000)  // Amber 700
    ),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
)

// Semantic Gradients
val SuccessGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF4CAF50), // Green 500
        Color(0xFF388E3C)  // Green 700
    ),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
)

val ErrorGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFF44336), // Red 500
        Color(0xFFD32F2F)  // Red 700
    ),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
)

val WarningGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFF9800), // Orange 500
        Color(0xFFF57C00)  // Orange 700
    ),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
)

// Background Gradients
val BackgroundGradientLight = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFE3F2FD), // Blue 50
        Color(0xFFBBDEFB), // Blue 100
        Color(0xFF90CAF9)  // Blue 200
    )
)

val BackgroundGradientDark = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0D47A1), // Blue 900
        Color(0xFF1565C0), // Blue 800
        Color(0xFF1976D2)  // Blue 700
    )
)

// Card Gradients
val CardGradientLight = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFFFFFF).copy(alpha = 0.95f),
        Color(0xFFE3F2FD).copy(alpha = 0.7f)
    ),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
)

val CardGradientDark = Brush.linearGradient(
    colors = listOf(
        Color(0xFF1E1E1E).copy(alpha = 0.95f),
        Color(0xFF0D47A1).copy(alpha = 0.3f)
    ),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
)

// Gradient Border (for cards)
val BorderGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF2196F3).copy(alpha = 0.6f),
        Color(0xFF00BCD4).copy(alpha = 0.6f),
        Color(0xFF2196F3).copy(alpha = 0.6f)
    ),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
)

// Shimmer Gradient (for loading states)
val ShimmerGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFE0E0E0),
        Color(0xFFF5F5F5),
        Color(0xFFE0E0E0)
    ),
    start = Offset(0f, 0f),
    end = Offset(1000f, 1000f)
)

// Animated background gradient (subtle)
val AnimatedBackgroundGradient = Brush.radialGradient(
    colors = listOf(
        Color(0xFFE3F2FD).copy(alpha = 0.3f),
        Color(0xFFBBDEFB).copy(alpha = 0.2f),
        Color(0xFF90CAF9).copy(alpha = 0.1f)
    )
)
