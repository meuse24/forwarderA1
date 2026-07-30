package info.meuse24.smsforwarderneoA1.presentation.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import info.meuse24.smsforwarderneoA1.ContactsViewModel
import info.meuse24.smsforwarderneoA1.R
import info.meuse24.smsforwarderneoA1.domain.model.inSlot

/**
 * Bestimmt, von welchen SIM-Karten eingehende SMS überhaupt verarbeitet werden.
 *
 * **Eigener Abschnitt, nicht Teil der SMS-Weiterleitung.** Der Filter greift im `SmsReceiver`,
 * also vor der Aufteilung in SMS- und E-Mail-Kanal: Eine nicht ausgewählte SIM-Karte wird
 * verworfen, bevor der Dienst überhaupt startet. Solange die Einstellung unter
 * "SMS-Weiterleitung" stand, las sie sich als deren Unterpunkt - und dass sie auch die
 * E-Mail-Weiterleitung abschaltet, war daraus nicht zu erraten.
 */
@Composable
fun SmsReceiveFilterSection(
    viewModel: ContactsViewModel,
    modifier: Modifier = Modifier
) {
    val availableSims by viewModel.availableSimCards.collectAsState()
    val sim1ReceiveEnabled by viewModel.sim1ReceiveEnabled.collectAsState()
    val sim2ReceiveEnabled by viewModel.sim2ReceiveEnabled.collectAsState()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.desc_sms_receive_filter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                // Hervorgehoben, weil die Reichweite die eigentliche Stolperstelle ist.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(top = 2.dp, end = 6.dp)
                            .size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.hint_sms_receive_filter_scope),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                SimReceiveCheckbox(
                    slotLabel = 1,
                    carrierName = availableSims.inSlot(0)?.carrierName,
                    phoneNumber = availableSims.inSlot(0)?.phoneNumber,
                    checked = sim1ReceiveEnabled,
                    onCheckedChange = viewModel::setSim1ReceiveEnabled
                )

                SimReceiveCheckbox(
                    slotLabel = 2,
                    carrierName = availableSims.inSlot(1)?.carrierName,
                    phoneNumber = availableSims.inSlot(1)?.phoneNumber,
                    checked = sim2ReceiveEnabled,
                    onCheckedChange = viewModel::setSim2ReceiveEnabled
                )
            }
        }
    }
}

@Composable
private fun SimReceiveCheckbox(
    slotLabel: Int,
    carrierName: String?,
    phoneNumber: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val label = if (carrierName != null) {
        "SIM $slotLabel: $carrierName" + (phoneNumber?.let { " ($it)" } ?: "")
    } else {
        "SIM $slotLabel: ${stringResource(R.string.suffix_not_available)}"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}
