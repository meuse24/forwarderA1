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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.meuse24.smsforwarderneoA1.R
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.AppFullscreenDialog
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.DialogConfirmButton
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.DialogDefaults
import info.meuse24.smsforwarderneoA1.util.rememberCountdown
import info.meuse24.smsforwarderneoA1.util.rememberGlowAnimation
import info.meuse24.smsforwarderneoA1.util.rememberPulseAnimation

/**
 * Loop Protection Warning Dialog - Critical warning when user tries to select own SIM as target.
 *
 * Features:
 * - Full-screen overlay with dark gradient background
 * - Warning icon with pulsating animation
 * - Critical error message
 * - Interactive countdown button (4 seconds)
 * - Auto-dismiss after countdown completes
 *
 * @param targetNumber The number that was blocked (for display)
 * @param ownNumber The own SIM number that was detected (for display)
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
fun LoopProtectionDialog(
    targetNumber: String,
    ownNumber: String,
    onDismiss: () -> Unit
) {
    // Use shared countdown utility (eliminates code duplication)
    val (countdown, manualDismiss) = rememberCountdown(seconds = 4, onFinish = onDismiss)

    // Use shared animation utilities (eliminates code duplication)
    // Note: Slightly different parameters for warning intensity (faster pulse, higher opacity)
    val scale by rememberPulseAnimation(minScale = 1f, maxScale = 1.2f, duration = 600)
    val alpha by rememberGlowAnimation(minAlpha = 0.4f, maxAlpha = 0.8f, duration = 1000)

    AppFullscreenDialog(
        onDismissRequest = { /* Prevent dismissal during countdown */ },
        properties = DialogDefaults.FullscreenDialogProperties
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF3D0000).copy(alpha = 0.85f),  // Dark red center
                            Color.Black.copy(alpha = 0.95f)          // Black edges
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
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
                    // Pulsating warning icon with glow effect
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
                                    MaterialTheme.colorScheme.error.copy(alpha = alpha * 0.4f)
                                )
                        )

                        // Warning icon
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = stringResource(R.string.cd_warning_icon),
                            modifier = Modifier
                                .size(100.dp)
                                .scale(scale),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    // Error title
                    Text(
                        text = stringResource(R.string.loop_protection_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.5.sp,
                        lineHeight = 32.sp
                    )

                    // Error message with details
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.loop_protection_message),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )

                        // Number details
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .padding(top = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        ) {
                            Column(
                                modifier = Modifier.padding(DialogDefaults.StandardPadding),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(R.string.loop_protection_target),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = targetNumber,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(R.string.loop_protection_own_sim),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = ownNumber,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    // Countdown button - auto-dismiss
                    Button(
                        onClick = manualDismiss,
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(64.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
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
                                color = MaterialTheme.colorScheme.onError.copy(alpha = 0.2f)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = countdown.value.toString(),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            // Button text
                            Text(
                                text = stringResource(R.string.btn_understand),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                }
            }
        }
    }
}
