package info.meuse24.smsforwarderneoA1.presentation.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import info.meuse24.smsforwarderneoA1.ContactsViewModel
import info.meuse24.smsforwarderneoA1.R
import info.meuse24.smsforwarderneoA1.domain.model.MmiSimSelectionMode

/**
 * UI-Komponente für die SIM-Auswahl bei MMI-Code-Ausführung (Rufumleitung).
 * Zeigt Radio-Buttons für die 3 Modi: DEFAULT_VOICE_SIM, ALWAYS_SIM_1, ALWAYS_SIM_2.
 * Markiert die Standard-Sprach-SIM mit "(Standard-Sprach-SIM)".
 */
@Composable
fun MmiSimSelectionSection(
    viewModel: ContactsViewModel,
    sectionTitleStyle: TextStyle,
    modifier: Modifier = Modifier
) {
    val selectedMode by viewModel.mmiSimSelectionMode.collectAsState()
    val availableSims by viewModel.availableSimCards.collectAsState()
    val defaultVoiceSubId by viewModel.defaultVoiceSubscriptionId.collectAsState()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.section_mmi_sim_selection),
            style = sectionTitleStyle,
            color = MaterialTheme.colorScheme.primary
        )

        // Beschreibung
        Text(
            text = stringResource(R.string.desc_mmi_sim_selection),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                MmiSimSelectionMode.values().forEach { mode ->
                    val label = buildMmiLabel(
                        mode,
                        availableSims.size,
                        defaultVoiceSubId,
                        availableSims.find { it.slotIndex == 0 },
                        availableSims.find { it.slotIndex == 1 }
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
                            onClick = { viewModel.setMmiSimSelectionMode(mode) },
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
 * Erstellt das Label für einen MMI-SIM-Auswahl-Modus.
 * Zeigt Carrier-Namen und markiert die Standard-Sprach-SIM bzw. nicht verfügbare SIMs.
 */
@Composable
private fun buildMmiLabel(
    mode: MmiSimSelectionMode,
    simCount: Int,
    defaultVoiceSubId: Int,
    sim1: info.meuse24.smsforwarderneoA1.domain.model.SimInfo?,
    sim2: info.meuse24.smsforwarderneoA1.domain.model.SimInfo?
): String {
    val baseLabel = mode.displayName
    val defaultVoiceSuffix = stringResource(R.string.suffix_default_voice)
    val notAvailableSuffix = stringResource(R.string.suffix_not_available)

    return when (mode) {
        MmiSimSelectionMode.DEFAULT_VOICE_SIM -> {
            // Zeige welche SIM die Standard-Sprach-SIM ist
            val defaultSim = listOfNotNull(sim1, sim2).find { it.subscriptionId == defaultVoiceSubId }
            val simInfo = defaultSim?.carrierName?.let { " ($it)" } ?: ""
            "$baseLabel$simInfo"
        }

        MmiSimSelectionMode.ALWAYS_SIM_1 -> {
            val isDefault = sim1?.subscriptionId == defaultVoiceSubId && sim1.subscriptionId != -1
            val suffix = when {
                sim1 == null -> " $notAvailableSuffix"
                isDefault -> " $defaultVoiceSuffix - ${sim1.carrierName ?: "SIM 1"}"
                else -> " (${sim1.carrierName ?: "SIM 1"})"
            }
            "$baseLabel$suffix"
        }

        MmiSimSelectionMode.ALWAYS_SIM_2 -> {
            val isDefault = sim2?.subscriptionId == defaultVoiceSubId && sim2.subscriptionId != -1
            val suffix = when {
                sim2 == null -> " $notAvailableSuffix"
                isDefault -> " $defaultVoiceSuffix - ${sim2.carrierName ?: "SIM 2"}"
                else -> " (${sim2.carrierName ?: "SIM 2"})"
            }
            "$baseLabel$suffix"
        }
    }
}
