package info.meuse24.smsforwarderneoA1.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import info.meuse24.smsforwarderneoA1.ui.theme.AnimationDuration
import info.meuse24.smsforwarderneoA1.ui.theme.AnimationSpecs
import info.meuse24.smsforwarderneoA1.ui.theme.BorderGradient
import kotlinx.coroutines.delay

/**
 * Animated card with slide-in and fade-in entrance
 */
@Composable
fun AnimatedCard(
    visible: Boolean,
    modifier: Modifier = Modifier,
    elevation: Dp = 4.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = AnimationSpecs.tween(AnimationDuration.NORMAL)
        ) + fadeIn(animationSpec = AnimationSpecs.tween(AnimationDuration.NORMAL)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = AnimationSpecs.tween(AnimationDuration.FAST)
        ) + fadeOut(animationSpec = AnimationSpecs.tween(AnimationDuration.FAST))
    ) {
        Card(
            modifier = modifier,
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            content = content
        )
    }
}

/**
 * Card with gradient border
 */
@Composable
fun GradientBorderCard(
    modifier: Modifier = Modifier,
    gradient: Brush = BorderGradient,
    borderWidth: Dp = 2.dp,
    elevation: Dp = 4.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .border(
                width = borderWidth,
                brush = gradient,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            content = content
        )
    }
}

/**
 * Card with cascading content reveal animation
 */
@Composable
fun CascadeCard(
    visible: Boolean,
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    elevation: Dp = 4.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            delay(delayMillis.toLong())
            isVisible = true
        } else {
            isVisible = false
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = AnimationSpecs.spring,
        label = "cascade_card_scale"
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = AnimationSpecs.tween(AnimationDuration.NORMAL)),
        exit = fadeOut(animationSpec = AnimationSpecs.tween(AnimationDuration.FAST))
    ) {
        Card(
            modifier = modifier.scale(scale),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            content = content
        )
    }
}

/**
 * Enhanced card with gradient background
 */
@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    gradient: Brush,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    elevation: Dp = 4.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(16.dp)
        ) {
            content()
        }
    }
}
