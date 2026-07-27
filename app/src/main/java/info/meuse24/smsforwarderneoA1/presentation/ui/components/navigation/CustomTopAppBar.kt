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
 * Kopfleiste ohne eigenen Inhalt.
 *
 * Sie faerbt ausschliesslich den Bereich hinter der Statusleiste ein, damit deren Icons
 * lesbar bleiben - das Wallpaper darunter ist stellenweise dunkel. Bewusst kein
 * Material3-TopAppBar, deshalb werden die WindowInsets selbst behandelt: Seit targetSdk 36
 * erzwingt Android 16 Edge-to-Edge, sonst laege die Leiste unter der Statusleiste.
 *
 * Eine feste Zusatzhoehe gibt es absichtlich nicht mehr: Da die Leiste keinen Titel und
 * keine Aktionen enthaelt, waeren das 56 dp toter Raum. Der Platz fehlt auf der Startseite
 * genau dann, wenn zusaetzliche Statuskarten eingeblendet werden und der Inhalt sonst
 * unten aus dem sichtbaren Bereich geschoben wuerde.
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
        )
    }
}
