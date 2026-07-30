package info.meuse24.smsforwarderneoA1.presentation.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import info.meuse24.smsforwarderneoA1.ContactsViewModel
import info.meuse24.smsforwarderneoA1.R
import info.meuse24.smsforwarderneoA1.domain.model.SimSelectionMode
import info.meuse24.smsforwarderneoA1.domain.model.inSlot

/**
 * Wählt die **Sende-SIM** für weitergeleitete SMS.
 *
 * Nur das: Welche eingehenden SMS überhaupt verarbeitet werden, entscheidet der getrennte
 * Abschnitt "SMS-Empfangsfilter" ([SmsReceiveFilterSection]). Beides zusammen in einem
 * Abschnitt zu zeigen hatte den Eindruck erzeugt, der Filter sei eine Verfeinerung der
 * Weiterleitung.
 *
 * Zeigt Radio-Buttons für die 3 Modi: SAME_AS_INCOMING, ALWAYS_SIM_1, ALWAYS_SIM_2, und
 * markiert die Standard-SMS-SIM mit "(Standard-SMS)".
 */
@Composable
fun SimSelectionSection(
    viewModel: ContactsViewModel,
    sectionTitleStyle: TextStyle,
    modifier: Modifier = Modifier
) {
    val selectedMode by viewModel.simSelectionMode.collectAsState()
    val availableSims by viewModel.availableSimCards.collectAsState()
    val defaultSmsSubId by viewModel.defaultSmsSubscriptionId.collectAsState()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.section_sms_sim_selection),
            style = sectionTitleStyle,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = stringResource(R.string.desc_sms_send_sim),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        // Card mit Rahmen für die Radio-Buttons
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Radio-Buttons für jeden Modus
                SimSelectionMode.values().forEach { mode ->
                    val label = buildLabel(
                        mode,
                        availableSims.size,
                        defaultSmsSubId,
                        availableSims.inSlot(0)?.subscriptionId,
                        availableSims.inSlot(1)?.subscriptionId
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        RadioButton(
                            selected = (selectedMode == mode),
                            onClick = { viewModel.setSimSelectionMode(mode) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (selectedMode == mode) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

            }
        }
    }
}

/**
 * Erstellt das Label für einen SIM-Auswahl-Modus.
 * Bei ALWAYS_SIM_1/2 wird ein lokalisierter Suffix angehängt (Standard-SMS / nicht verfügbar).
 */
@Composable
private fun buildLabel(
    mode: SimSelectionMode,
    simCount: Int,
    defaultSmsSubId: Int,
    sim1SubId: Int?,
    sim2SubId: Int?
): String {
    val baseLabel = when (mode) {
        SimSelectionMode.SAME_AS_INCOMING -> stringResource(R.string.sim_selection_same_as_incoming)
        SimSelectionMode.ALWAYS_SIM_1 -> stringResource(R.string.sim_selection_always_sim1)
        SimSelectionMode.ALWAYS_SIM_2 -> stringResource(R.string.sim_selection_always_sim2)
    }

    val defaultSuffix = stringResource(R.string.suffix_default_sms)
    val notAvailableSuffix = stringResource(R.string.suffix_not_available)

    return when (mode) {
        SimSelectionMode.SAME_AS_INCOMING -> baseLabel

        SimSelectionMode.ALWAYS_SIM_1 -> {
            val isDefault = sim1SubId == defaultSmsSubId && sim1SubId != -1
            val suffix = if (isDefault) " $defaultSuffix" else ""
            "$baseLabel$suffix"
        }

        SimSelectionMode.ALWAYS_SIM_2 -> {
            val isDefault = sim2SubId == defaultSmsSubId && sim2SubId != -1
            val isAvailable = simCount >= 2
            val suffix = when {
                !isAvailable -> " $notAvailableSuffix"
                isDefault -> " $defaultSuffix"
                else -> ""
            }
            "$baseLabel$suffix"
        }
    }
}
