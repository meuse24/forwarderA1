package info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import info.meuse24.smsforwarderneoA1.R
import kotlinx.coroutines.delay

/**
 * MMI Warning Dialog - Modern, compact overlay shown before dialing MMI codes.
 *
 * Features:
 * - Full-screen overlay with gradient background
 * - Officer logo with pulsating animation
 * - Minimal, impactful warning text
 * - Interactive countdown button
 * - Auto-dismiss after 4 seconds or manual skip
 *
 * @param onDismiss Callback when dialog is dismissed (after countdown or manual skip)
 */
@Composable
fun MmiWarningDialog(
    onDismiss: () -> Unit
) {
    var countdown by remember { mutableStateOf(4) }
    var isDismissed by remember { mutableStateOf(false) }

    // Countdown timer
    LaunchedEffect(Unit) {
        repeat(4) { i ->
            if (isDismissed) return@LaunchedEffect
            countdown = 4 - i
            delay(1000)
        }
        if (!isDismissed) {
            onDismiss()
        }
    }

    // Pulsating animation for officer icon
    val infiniteTransition = rememberInfiniteTransition(label = "officer_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_animation"
    )

    // Glow animation
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_animation"
    )

    Dialog(
        onDismissRequest = { /* Prevent dismissal */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0.95f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Pulsating officer logo with glow effect
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        // Glow background
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .scale(scale)
                                .clip(CircleShape)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = alpha * 0.3f)
                                )
                        )

                        // Officer logo
                        Image(
                            painter = painterResource(id = R.drawable.officer),
                            contentDescription = "Officer",
                            modifier = Modifier
                                .size(120.dp)
                                .scale(scale)
                                .clip(CircleShape)
                        )
                    }

                    // Warning text - compact and impactful
                    Text(
                        text = "Automatischer Wählvorgang abwarten!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.5.sp,
                        lineHeight = 32.sp
                    )

                    // Countdown button - clickable to skip countdown
                    Button(
                        onClick = {
                            isDismissed = true
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(64.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 12.dp
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Countdown badge
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = countdown.toString(),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Button text
                            Text(
                                text = "Jetzt wählen",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
