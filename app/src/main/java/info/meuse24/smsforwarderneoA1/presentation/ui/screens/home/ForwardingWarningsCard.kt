package info.meuse24.smsforwarderneoA1.presentation.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import info.meuse24.smsforwarderneoA1.AppContainer
import info.meuse24.smsforwarderneoA1.R
import info.meuse24.smsforwarderneoA1.domain.model.DroppedForwardingWarning
import info.meuse24.smsforwarderneoA1.domain.model.QueueCorruptionWarning
import info.meuse24.smsforwarderneoA1.presentation.ui.components.AnimatedCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Zustaende, die die Weiterleitung beeintraechtigen - **ohne** sie zu stoppen.
 *
 * Alle vier sind reine Anzeige. Keiner von ihnen darf den Betrieb blockieren; das war die
 * eigentliche Ursache des frueheren Totalausfalls bei fehlender Benachrichtigungsberechtigung.
 */
data class ForwardingWarnings(
    val notificationsSuppressed: Boolean = false,
    val batteryOptimizationActive: Boolean = false,
    val corruption: QueueCorruptionWarning? = null,
    val dropped: DroppedForwardingWarning? = null,
    val serviceTimeoutAtMillis: Long? = null,
    val problemCount: Int = 0
) {
    val hasAny: Boolean
        get() = notificationsSuppressed || batteryOptimizationActive || corruption != null ||
            dropped != null || serviceTimeoutAtMillis != null || problemCount > 0

    /** Die unterdrueckte Statusanzeige verschwindet erst mit der Berechtigung, nicht per Klick. */
    val hasAcknowledgeable: Boolean
        get() = corruption != null || dropped != null || serviceTimeoutAtMillis != null || problemCount > 0
}

/**
 * Zeigt Betriebswarnungen auf der Startseite.
 *
 * Stateless, damit alle Faelle ohne Geraet darstellbar bleiben.
 */
@Composable
fun ForwardingWarningsCard(
    state: ForwardingWarnings,
    onAcknowledge: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.hasAny) return

    val messages = buildList {
        if (state.notificationsSuppressed) add(stringResource(R.string.warning_notifications_suppressed))
        if (state.batteryOptimizationActive) add(stringResource(R.string.warning_battery_optimization))
        state.corruption?.let { corruption ->
            val time = formatTimestamp(corruption.timestampMillis)
            add(
                if (corruption.lostEntries == QueueCorruptionWarning.UNKNOWN_COUNT) {
                    stringResource(R.string.warning_queue_corruption_unknown, time)
                } else {
                    stringResource(R.string.warning_queue_corruption, corruption.lostEntries, time)
                }
            )
        }
        state.dropped?.let { dropped ->
            add(
                stringResource(
                    R.string.warning_dropped_forwardings,
                    dropped.count,
                    formatTimestamp(dropped.timestampMillis)
                )
            )
        }
        state.serviceTimeoutAtMillis?.let {
            add(stringResource(R.string.warning_service_timeout, formatTimestamp(it)))
        }
        if (state.problemCount > 0) {
            add(stringResource(R.string.warning_failed_operations, state.problemCount))
        }
    }

    AnimatedCard(visible = true, modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Rein dekorativ: die Aussage steht vollstaendig im Text daneben.
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.warning_card_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                    messages.forEach { message ->
                        Text(text = message, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (state.hasAcknowledgeable) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onAcknowledge) {
                        Text(stringResource(R.string.warning_acknowledge))
                    }
                }
            }
        }
    }
}

/**
 * Bindet [ForwardingWarningsCard] an Preferences und Queue an.
 *
 * Der Zustand der Benachrichtigungsberechtigung wird live geprueft und der gespeicherte Wert
 * nachgezogen: Erteilt der Nutzer die Berechtigung nachtraeglich, soll der Hinweis auch ohne
 * Neustart des Dienstes verschwinden.
 */
@Composable
fun ForwardingWarningsCardHost(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(ForwardingWarnings()) }

    suspend fun reload() {
        state = withContext(Dispatchers.IO) {
            val prefs = runCatching { AppContainer.requirePrefsManager() }.getOrNull()
                ?: return@withContext ForwardingWarnings()
            val queue = AppContainer.getForwardingQueueSafe()

            val suppressed = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            if (suppressed != prefs.areNotificationsSuppressed()) {
                prefs.setNotificationsSuppressed(suppressed)
            }

            val powerManager = context.getSystemService(PowerManager::class.java)
            val batteryOptimizationActive =
                powerManager?.isIgnoringBatteryOptimizations(context.packageName) == false

            ForwardingWarnings(
                notificationsSuppressed = suppressed,
                batteryOptimizationActive = batteryOptimizationActive,
                corruption = prefs.getQueueCorruptionWarning(),
                dropped = prefs.getDroppedForwardingWarning(),
                serviceTimeoutAtMillis = prefs.getServiceTimeoutAt(),
                problemCount = queue?.unacknowledgedProblems()?.size ?: 0
            )
        }
    }

    LaunchedEffect(Unit) { reload() }

    ForwardingWarningsCard(
        state = state,
        onAcknowledge = {
            scope.launch {
                withContext(Dispatchers.IO) {
                    runCatching {
                        AppContainer.requirePrefsManager().acknowledgeQueueCorruption()
                        AppContainer.requirePrefsManager().acknowledgeDroppedForwardings()
                        AppContainer.requirePrefsManager().acknowledgeServiceTimeout()
                        AppContainer.getForwardingQueueSafe()?.acknowledgeProblems()
                    }
                }
                reload()
            }
        },
        modifier = modifier
    )
}

private fun formatTimestamp(millis: Long): String =
    SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(millis))
