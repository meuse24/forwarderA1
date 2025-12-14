package info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.meuse24.smsforwarderneoA1.R
import info.meuse24.smsforwarderneoA1.util.rememberCountdown
import info.meuse24.smsforwarderneoA1.util.rememberGlowAnimation
import info.meuse24.smsforwarderneoA1.util.rememberPulseAnimation
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.AppFullscreenDialog
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.DialogDefaults

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
    // Use shared countdown utility (eliminates code duplication)
    val (countdown, manualDismiss) = rememberCountdown(seconds = 4, onFinish = onDismiss)

    // Use shared animation utilities (eliminates code duplication)
    val scale by rememberPulseAnimation(minScale = 1f, maxScale = 1.15f, duration = 800)
    val alpha by rememberGlowAnimation(minAlpha = 0.3f, maxAlpha = 0.7f, duration = 1200)

    AppFullscreenDialog(
        onDismissRequest = { /* Prevent dismissal */ },
        properties = DialogDefaults.FullscreenDialogProperties
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
                        AsyncImage(
                            model = R.drawable.officer,
                            contentDescription = stringResource(R.string.cd_officer_logo),
                            modifier = Modifier
                                .size(120.dp)
                                .scale(scale)
                                .clip(CircleShape)
                        )
                    }

                    // Warning text - compact and impactful
                    Text(
                        text = stringResource(R.string.warning_await_dialing),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.5.sp,
                        lineHeight = 32.sp
                    )

                    // Countdown button - clickable to skip countdown
                    Button(
                        onClick = manualDismiss,
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
                                        text = countdown.value.toString(),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Button text
                            Text(
                                text = stringResource(R.string.btn_execute_now),
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
