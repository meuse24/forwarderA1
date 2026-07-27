package info.meuse24.smsforwarderneoA1.presentation.ui.components.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Schlanke Kopfleiste ohne Titel.
 *
 * Bewusst kein Material3-TopAppBar, deshalb muessen die WindowInsets selbst behandelt
 * werden: Seit targetSdk 36 erzwingt Android 16 Edge-to-Edge, sodass die Leiste sonst
 * unter der Statusleiste liegen wuerde. Die Surface faerbt den Statusleistenbereich mit,
 * der 56-dp-Inhalt beginnt darunter.
 */
@Composable
fun CustomTopAppBar(title: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(56.dp)
        ) {
            // Kein Text mehr - nur farbige TopAppBar
        }
    }
}
