package info.meuse24.smsforwarderneoA1.presentation.ui.screens.home

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import info.meuse24.smsforwarderneoA1.AppContainer
import info.meuse24.smsforwarderneoA1.R
import info.meuse24.smsforwarderneoA1.data.local.SharedPreferencesManager
import info.meuse24.smsforwarderneoA1.domain.model.GoogleMessagesState
import info.meuse24.smsforwarderneoA1.presentation.ui.components.AnimatedCard
import info.meuse24.smsforwarderneoA1.util.GoogleMessagesDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Sichtbarkeit des RCS-Hinweises als beobachtbarer, prozessweiter Zustand.
 *
 * Notwendig, weil die Startseite in einem Pager liegt und beim Zurueckwechseln nicht neu
 * aufgebaut wird: Ein Schalter in den Einstellungen wuerde sonst erst nach einem
 * App-Neustart wirken. Die verschluesselten Preferences bleiben die Quelle der Wahrheit;
 * dieser Zustand spiegelt sie nur fuer Compose.
 */
object RcsHintVisibility {

    private var dismissedState by mutableStateOf(true)
    private var loaded = false

    /** Einmalig aus den Preferences laden. Weitere Aufrufe sind wirkungslos. */
    fun load(prefsManager: SharedPreferencesManager?) {
        if (loaded) return
        dismissedState = prefsManager?.isRcsHintDismissed() ?: true
        loaded = true
    }

    val isDismissed: Boolean
        get() = dismissedState

    /** Schreibt den Wert persistent und aktualisiert alle beobachtenden Composables. */
    fun setDismissed(prefsManager: SharedPreferencesManager?, dismissed: Boolean) {
        prefsManager?.setRcsHintDismissed(dismissed)
        dismissedState = dismissed
        loaded = true
    }
}

/**
 * Hinweis auf der Startseite, dass RCS-Chats nicht weitergeleitet werden koennen.
 *
 * Die Karte behauptet bewusst NICHT, dass RCS aktiv ist - das ist ohne privilegierte
 * Berechtigung nicht feststellbar. Sie wertet nur aus, ob Google Messages installiert
 * bzw. Standard-SMS-App ist, und formuliert den Hinweis entsprechend als Moeglichkeit.
 *
 * Stateless: Zustand und Aktionen werden von aussen gereicht, damit die drei Faelle
 * ohne Geraet testbar sind.
 */
@Composable
fun RcsHintCard(
    state: GoogleMessagesState,
    onLearnMore: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val message = when (state) {
        GoogleMessagesState.DEFAULT_SMS_APP -> stringResource(R.string.rcs_hint_default_app)
        GoogleMessagesState.INSTALLED_NOT_DEFAULT -> stringResource(R.string.rcs_hint_installed_not_default)
        // Ohne Google Messages besteht kein RCS-Risiko - kein Hinweis.
        GoogleMessagesState.NOT_INSTALLED -> return
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
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.rcs_hint_action_dismiss))
                }
                TextButton(onClick = onLearnMore) {
                    Text(stringResource(R.string.rcs_hint_action_learn_more))
                }
            }
        }
    }
}

/**
 * Bindet [RcsHintCard] an Geraetezustand und Einstellungen an.
 *
 * Die Paketpruefung laeuft einmalig auf dem IO-Dispatcher, damit die Composition nicht
 * blockiert wird. Faellt sie aus, bleibt der Zustand NOT_INSTALLED und es wird nichts
 * angezeigt - ein fehlerhafter Hinweis waere schlechter als gar keiner.
 */
@Composable
fun RcsHintCardHost(
    onNavigateToHelp: () -> Unit,
    modifier: Modifier = Modifier,
    // Als Default-Parameter injizierbar, damit der Host ohne Abhaengigkeit vom real
    // installierten Google Messages getestet werden kann.
    detectState: (Context) -> GoogleMessagesState = GoogleMessagesDetector::detect
) {
    val context = LocalContext.current
    val prefsManager = remember { runCatching { AppContainer.requirePrefsManager() }.getOrNull() }

    var state by remember { mutableStateOf(GoogleMessagesState.NOT_INSTALLED) }

    LaunchedEffect(Unit) {
        RcsHintVisibility.load(prefsManager)
        state = withContext(Dispatchers.IO) { detectState(context) }
    }

    if (RcsHintVisibility.isDismissed) return

    RcsHintCard(
        state = state,
        onLearnMore = onNavigateToHelp,
        onDismiss = { RcsHintVisibility.setDismissed(prefsManager, true) },
        modifier = modifier
    )
}
