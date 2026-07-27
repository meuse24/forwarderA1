package info.meuse24.smsforwarderneoA1.util

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Shared animation utilities for dialog components.
 *
 * Provides reusable animation composables to eliminate code duplication
 * across different dialog implementations (for example, LoopProtectionDialog).
 */

/**
 * Pulsating scale animation for icons and visual elements.
 *
 * Creates a smooth scale animation that oscillates between minScale and maxScale,
 * typically used for attention-grabbing effects on warning icons or important UI elements.
 *
 * @param minScale The minimum scale value (default: 1.0 = original size)
 * @param maxScale The maximum scale value (default: 1.15 = 15% larger)
 * @param duration Animation duration in milliseconds for one cycle (default: 800ms)
 * @return State<Float> containing the current scale value
 *
 * @sample
 * ```kotlin
 * val scale by rememberPulseAnimation()
 * Icon(
 *     imageVector = Icons.Default.Warning,
 *     modifier = Modifier.scale(scale)
 * )
 * ```
 */
@Composable
fun rememberPulseAnimation(
    minScale: Float = 1f,
    maxScale: Float = 1.15f,
    duration: Int = 800
): State<Float> {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    return infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
}

/**
 * Glow alpha animation for background effects.
 *
 * Creates a smooth alpha (transparency) animation that oscillates between minAlpha and maxAlpha,
 * typically used for glowing background circles behind icons.
 *
 * @param minAlpha The minimum alpha value (default: 0.3 = 30% opacity)
 * @param maxAlpha The maximum alpha value (default: 0.7 = 70% opacity)
 * @param duration Animation duration in milliseconds for one cycle (default: 1200ms)
 * @return State<Float> containing the current alpha value
 *
 * @sample
 * ```kotlin
 * val alpha by rememberGlowAnimation()
 * Box(
 *     modifier = Modifier
 *         .size(140.dp)
 *         .background(Color.Red.copy(alpha = alpha))
 * )
 * ```
 */
@Composable
fun rememberGlowAnimation(
    minAlpha: Float = 0.3f,
    maxAlpha: Float = 0.7f,
    duration: Int = 1200
): State<Float> {
    val infiniteTransition = rememberInfiniteTransition(label = "glow_transition")
    return infiniteTransition.animateFloat(
        initialValue = minAlpha,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    ),
    label = "glow_alpha"
)
}

@Preview(showBackground = true)
@Composable
private fun PulseAnimationPreview() {
    val scale by rememberPulseAnimation()
    Column(
        modifier = Modifier
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Pulse Animation", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .scale(scale),
            tint = MaterialTheme.colorScheme.error
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GlowAnimationPreview() {
    val alpha by rememberGlowAnimation()
    Card(
        modifier = Modifier.padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = alpha))
            )
            Spacer(modifier = Modifier.size(16.dp))
            Text("Glow Animation", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
