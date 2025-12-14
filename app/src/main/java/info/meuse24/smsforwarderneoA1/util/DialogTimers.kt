package info.meuse24.smsforwarderneoA1.util

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Countdown timer utilities for dialog components.
 *
 * Provides reusable countdown functionality to eliminate code duplication
 * across different dialog implementations.
 */

/**
 * Auto-countdown timer with manual dismiss option.
 *
 * Creates a countdown that automatically calls onFinish when reaching 0,
 * but can also be dismissed manually before completion. The countdown state
 * is exposed for UI display (e.g., showing remaining seconds in a button).
 *
 * Handles cleanup properly when the composable is disposed or when dismissed manually,
 * preventing memory leaks or callbacks firing after dismissal.
 *
 * @param seconds Countdown duration in seconds (default: 4)
 * @param onFinish Callback invoked when countdown reaches 0 OR when manually dismissed
 * @return Pair of (countdown State, manualDismiss function)
 *   - countdown: Current countdown value (seconds to Int)
 *   - manualDismiss: Function to call for early dismissal
 *
 * @sample
 * ```kotlin
 * val (countdown, manualDismiss) = rememberCountdown(4) {
 *     // Dialog finished - dismiss or perform action
 *     onDismiss()
 * }
 *
 * Button(onClick = manualDismiss) {
 *     Row {
 *         // Show countdown badge
 *         Text(countdown.value.toString())
 *         Text("Skip")
 *     }
 * }
 * ```
 */
@Composable
fun rememberCountdown(
    seconds: Int = 4,
    onFinish: () -> Unit
): Pair<State<Int>, () -> Unit> {
    val countdown = remember { mutableIntStateOf(seconds) }
    val isDismissed = remember { mutableStateOf(false) }
    val onFinishState by rememberUpdatedState(onFinish)

    // Countdown coroutine
    LaunchedEffect(Unit) {
        repeat(seconds) { i ->
            // Exit early if manually dismissed
            if (isDismissed.value) return@LaunchedEffect

            countdown.intValue = seconds - i
            delay(1000)
        }

        // Only call onFinish if not already dismissed
        if (!isDismissed.value) {
            onFinishState()
        }
    }

    // Manual dismiss function
    val manualDismiss: () -> Unit = {
        if (!isDismissed.value) {
            isDismissed.value = true
            onFinishState()
        }
    }

    return countdown to manualDismiss
}

@Preview(showBackground = true)
@Composable
private fun CountdownPreview() {
    var finished by remember { mutableStateOf(false) }
    val (countdown, manualDismiss) = rememberCountdown(seconds = 4) {
        finished = true
    }

    Surface {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (finished) "Finished" else "Countdown: ${countdown.value}",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = manualDismiss) {
                Text("Dismiss")
            }
        }
    }
}
