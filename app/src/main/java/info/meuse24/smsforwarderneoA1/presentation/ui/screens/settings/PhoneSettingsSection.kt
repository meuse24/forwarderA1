package info.meuse24.smsforwarderneoA1.presentation.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import info.meuse24.smsforwarderneoA1.ContactsViewModel
import info.meuse24.smsforwarderneoA1.PhoneSmsUtils

@Composable
fun PhoneSettingsSection(
    viewModel: ContactsViewModel,
    onFocusChanged: (Boolean) -> Unit,
    sectionTitleStyle: TextStyle
) {
    val context = LocalContext.current
    val isForwardingActive by viewModel.forwardingActive.collectAsState()
    val countryCode by viewModel.countryCode.collectAsState()
    val countryCodeSource by viewModel.countryCodeSource.collectAsState()

    // LaunchedEffect für eigene Telefonnummer entfernt

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Telefon-Einstellungen",
            style = sectionTitleStyle,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Eigene Telefonnummer Feld entfernt - wird jetzt in SIM-Karten-Übersicht verwaltet

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isForwardingActive,
                    onCheckedChange = null,
                    enabled = false
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Weiterleitung aktiv")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column {
            Text(
                text = "Erkannte Ländervorwahl",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(text = PhoneSmsUtils.getCountryNameForCode(countryCode))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Quelle: $countryCodeSource",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
