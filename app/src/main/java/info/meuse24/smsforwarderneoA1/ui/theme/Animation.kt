package info.meuse24.smsforwarderneoA1.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember

/**
 * Animation specifications and constants for SMS Forwarder Neo
 * Provides reusable animation configurations for consistent motion design
 */

// Animation Durations (in milliseconds)
object AnimationDuration {
    const val FAST = 150
    const val NORMAL = 300
    const val SLOW = 500
    const val VERY_SLOW = 800
}

// Animation Delays
object AnimationDelay {
    const val SHORT = 50
    const val MEDIUM = 100
    const val LONG = 200
}

// Scale Values
object ScaleValues {
    const val PRESSED = 0.95f
    const val NORMAL = 1f
    const val SLIGHTLY_LARGER = 1.05f
}

// Alpha Values
object AlphaValues {
    const val DISABLED = 0.38f
    const val MEDIUM = 0.6f
    const val VISIBLE = 1f
}

// Standard Animation Specs
object AnimationSpecs {
    /**
     * Spring animation for natural motion
     */
    val spring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    /**
     * Tween animation for controlled motion
     */
    fun <T> tween(duration: Int = AnimationDuration.NORMAL) = tween<T>(
        durationMillis = duration,
        easing = FastOutSlowInEasing
    )

    /**
     * Fast tween for quick transitions
     */
    val fastTween = tween<Float>(
        durationMillis = AnimationDuration.FAST,
        easing = LinearOutSlowInEasing
    )

    /**
     * Slow tween for emphasis
     */
    val slowTween = tween<Float>(
        durationMillis = AnimationDuration.SLOW,
        easing = FastOutSlowInEasing
    )

    /**
     * Infinite repeating animation
     */
    val infiniteRepeatable = infiniteRepeatable<Float>(
        animation = tween(
            durationMillis = AnimationDuration.VERY_SLOW,
            easing = LinearEasing
        ),
        repeatMode = RepeatMode.Restart
    )

    /**
     * Pulse animation (for icons, indicators)
     */
    val pulse = infiniteRepeatable<Float>(
        animation = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        repeatMode = RepeatMode.Reverse
    )

    /**
     * Shimmer animation (for loading states)
     */
    val shimmer = infiniteRepeatable<Float>(
        animation = tween(
            durationMillis = 1200,
            easing = LinearEasing
        ),
        repeatMode = RepeatMode.Restart
    )
}

// Helper functions for animations
object AnimationHelpers {
    /**
     * Remembers an animatable float value
     */
    @Composable
    fun rememberAnimatedFloat(initialValue: Float = 1f): Animatable<Float, AnimationVector1D> {
        return remember { Animatable(initialValue) }
    }

    /**
     * Creates an infinite transition for continuous animations
     */
    @Composable
    fun rememberInfiniteTransition(): InfiniteTransition {
        return rememberInfiniteTransition(label = "infinite_transition")
    }

    /**
     * Animated float value that pulses between two values
     */
    @Composable
    fun animatePulse(
        targetValue: Float = 1.2f,
        initialValue: Float = 1f
    ): State<Float> {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        return infiniteTransition.animateFloat(
            initialValue = initialValue,
            targetValue = targetValue,
            animationSpec = AnimationSpecs.pulse,
            label = "pulse_value"
        )
    }

    /**
     * Animated value for shimmer effect
     */
    @Composable
    fun animateShimmer(): State<Float> {
        val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
        return infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = AnimationSpecs.shimmer,
            label = "shimmer_offset"
        )
    }
}

// Cascade animation for staggered item appearances
object CascadeAnimation {
    /**
     * Calculate delay for cascade effect
     */
    fun getDelay(index: Int, baseDelay: Int = AnimationDelay.SHORT): Int {
        return baseDelay * index
    }
}
