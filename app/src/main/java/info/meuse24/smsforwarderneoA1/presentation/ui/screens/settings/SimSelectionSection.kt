package info.meuse24.smsforwarderneoA1.presentation.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
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
import info.meuse24.smsforwarderneoA1.domain.model.SimSelectionMode

/**
 * UI-Komponente für die SIM-Auswahl bei SMS-Weiterleitung.
 * Zeigt Radio-Buttons für die 3 Modi: SAME_AS_INCOMING, ALWAYS_SIM_1, ALWAYS_SIM_2.
 * Markiert die Standard-SMS-SIM mit "(Standard-SMS)".
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
    val sim1ReceiveEnabled by viewModel.sim1ReceiveEnabled.collectAsState()
    val sim2ReceiveEnabled by viewModel.sim2ReceiveEnabled.collectAsState()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.section_sms_sim_selection),
            style = sectionTitleStyle,
            color = MaterialTheme.colorScheme.primary
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
                        availableSims.getOrNull(0)?.subscriptionId,
                        availableSims.getOrNull(1)?.subscriptionId
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

                // Divider vor SMS-Empfangsfilter
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // SMS-Empfangsfilter
                Text(
                    text = stringResource(R.string.section_sms_receive_filter),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = stringResource(R.string.desc_sms_receive_filter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // SIM 1 checkbox
                val sim1 = availableSims.getOrNull(0)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = sim1ReceiveEnabled,
                        onCheckedChange = { viewModel.setSim1ReceiveEnabled(it) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (sim1 != null) {
                            "SIM 1: ${sim1.carrierName}${if (sim1.phoneNumber != null) " (${sim1.phoneNumber})" else ""}"
                        } else {
                            "SIM 1: ${stringResource(R.string.suffix_not_available)}"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // SIM 2 checkbox
                val sim2 = availableSims.getOrNull(1)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = sim2ReceiveEnabled,
                        onCheckedChange = { viewModel.setSim2ReceiveEnabled(it) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (sim2 != null) {
                            "SIM 2: ${sim2.carrierName}${if (sim2.phoneNumber != null) " (${sim2.phoneNumber})" else ""}"
                        } else {
                            "SIM 2: ${stringResource(R.string.suffix_not_available)}"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
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
