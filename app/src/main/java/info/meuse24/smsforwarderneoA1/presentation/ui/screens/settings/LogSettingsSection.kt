package info.meuse24.smsforwarderneoA1.presentation.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
fun LogSettingsSection(
    sectionTitleStyle: TextStyle,
    onDeleteLogs: () -> Unit,
    onChangePin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Log-Einstellungen",
            style = sectionTitleStyle,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Log-Datei löschen",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Löscht alle Protokolleinträge",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDeleteLogs) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Logs löschen",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .alpha(0.5f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PIN ändern",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "PIN für Löschfunktion ändern",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onChangePin) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "PIN ändern",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
