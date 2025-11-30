package info.meuse24.smsforwarderneoA1.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Enhanced Dark Color Scheme with Blue/Cyan/Amber palette
 */
private val DarkColorScheme = darkColorScheme(
    // Primary colors
    primary = Blue200,
    onPrimary = Blue900,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,

    // Secondary colors
    secondary = Cyan200,
    onSecondary = Cyan900,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,

    // Tertiary colors
    tertiary = Amber200,
    onTertiary = Amber900,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,

    // Error colors
    error = Red200,
    onError = Red900,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,

    // Background colors
    background = BackgroundDark,
    onBackground = OnBackgroundDark,

    // Surface colors
    surface = SurfaceDark,
    onSurface = White,
    surfaceVariant = SurfaceDarkDim,
    onSurfaceVariant = Grey300,
    surfaceTint = Blue200,

    // Outline
    outline = OutlineDark,
    outlineVariant = Grey700,

    // Inverse colors
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    inversePrimary = InversePrimaryDark,

    // Scrim
    scrim = Scrim,
)

/**
 * Enhanced Light Color Scheme with Blue/Cyan/Amber palette
 */
private val LightColorScheme = lightColorScheme(
    // Primary colors
    primary = Blue500,
    onPrimary = White,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,

    // Secondary colors
    secondary = Cyan500,
    onSecondary = White,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,

    // Tertiary colors
    tertiary = Amber500,
    onTertiary = Black,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,

    // Error colors
    error = Red500,
    onError = White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,

    // Background colors
    background = BackgroundLight,
    onBackground = OnBackgroundLight,

    // Surface colors
    surface = SurfaceLight,
    onSurface = Black,
    surfaceVariant = SurfaceDim,
    onSurfaceVariant = Grey700,
    surfaceTint = Blue500,

    // Outline
    outline = OutlineLight,
    outlineVariant = Grey300,

    // Inverse colors
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    inversePrimary = InversePrimaryLight,

    // Scrim
    scrim = Scrim,
)

@Composable
fun SMSForwarderNeoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,  // Disabled to use our custom colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
