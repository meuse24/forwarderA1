package info.meuse24.smsforwarderneoA1.presentation.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import info.meuse24.smsforwarderneoA1.ui.theme.AnimationDuration
import info.meuse24.smsforwarderneoA1.ui.theme.AnimationSpecs
import info.meuse24.smsforwarderneoA1.ui.theme.PrimaryGradient
import info.meuse24.smsforwarderneoA1.ui.theme.ScaleValues

/**
 * Animated button with scale-on-press effect
 */
@Composable
fun AnimatedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) ScaleValues.PRESSED else ScaleValues.NORMAL,
        animationSpec = AnimationSpecs.spring,
        label = "button_scale"
    )

    Button(
        onClick = onClick,
        modifier = modifier.scale(scale),
        enabled = enabled,
        interactionSource = interactionSource,
        content = content
    )
}

/**
 * Animated outlined button with scale-on-press effect
 */
@Composable
fun AnimatedOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) ScaleValues.PRESSED else ScaleValues.NORMAL,
        animationSpec = AnimationSpecs.spring,
        label = "outlined_button_scale"
    )

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.scale(scale),
        enabled = enabled,
        interactionSource = interactionSource,
        content = content
    )
}

/**
 * Gradient button with animation
 */
@Composable
fun GradientButton(
    onClick: () -> Unit,
    gradient: Brush = PrimaryGradient,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) ScaleValues.PRESSED else ScaleValues.NORMAL,
        animationSpec = AnimationSpecs.spring,
        label = "gradient_button_scale"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .scale(scale)
            .background(
                brush = if (enabled) gradient else Brush.linearGradient(
                    listOf(Color.Gray, Color.DarkGray)
                ),
                shape = RoundedCornerShape(12.dp)
            ),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}
