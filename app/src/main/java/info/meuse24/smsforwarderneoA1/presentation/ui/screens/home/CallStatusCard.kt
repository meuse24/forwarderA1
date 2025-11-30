package info.meuse24.smsforwarderneoA1.presentation.ui.screens.home

import android.telephony.TelephonyManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import info.meuse24.smsforwarderneoA1.ui.theme.AnimationDuration
import info.meuse24.smsforwarderneoA1.ui.theme.AnimationHelpers
import info.meuse24.smsforwarderneoA1.ui.theme.Blue200
import info.meuse24.smsforwarderneoA1.ui.theme.Cyan200

@Composable
fun CallStatusCard(callState: Int) {
    val isVisible = callState != TelephonyManager.CALL_STATE_IDLE

    val statusText = when (callState) {
        TelephonyManager.CALL_STATE_OFFHOOK -> "MMI-Code wird ausgeführt..."
        TelephonyManager.CALL_STATE_RINGING -> "Eingehender Anruf..."
        else -> ""
    }

    val containerColor = when (callState) {
        TelephonyManager.CALL_STATE_OFFHOOK -> MaterialTheme.colorScheme.primaryContainer
        TelephonyManager.CALL_STATE_RINGING -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val contentColor = when (callState) {
        TelephonyManager.CALL_STATE_OFFHOOK -> MaterialTheme.colorScheme.onPrimaryContainer
        TelephonyManager.CALL_STATE_RINGING -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val borderColor = when (callState) {
        TelephonyManager.CALL_STATE_OFFHOOK -> Blue200
        TelephonyManager.CALL_STATE_RINGING -> Cyan200
        else -> Color.Transparent
    }

    // Pulse animation for active call state
    val pulseScale by AnimationHelpers.animatePulse(
        targetValue = 1.02f,
        initialValue = 1f
    )

    // Scale animation
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = tween(durationMillis = AnimationDuration.NORMAL),
        label = "call_status_scale"
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(durationMillis = AnimationDuration.NORMAL)
        ) + fadeIn(animationSpec = tween(durationMillis = AnimationDuration.NORMAL)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(durationMillis = AnimationDuration.FAST)
        ) + fadeOut(animationSpec = tween(durationMillis = AnimationDuration.FAST))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .scale(if (callState == TelephonyManager.CALL_STATE_OFFHOOK) pulseScale else scale)
                .border(
                    border = BorderStroke(2.dp, borderColor),
                    shape = RoundedCornerShape(12.dp)
                ),
            colors = CardDefaults.cardColors(
                containerColor = containerColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 3.dp,
                    color = contentColor
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor
                )
            }
        }
    }
}
