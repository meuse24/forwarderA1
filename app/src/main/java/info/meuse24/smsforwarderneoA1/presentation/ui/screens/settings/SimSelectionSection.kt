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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import info.meuse24.smsforwarderneoA1.ContactsViewModel
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

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "SMS-Weiterleitung - SIM-Auswahl",
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
            }
        }
    }
}

/**
 * Erstellt das Label für einen SIM-Auswahl-Modus.
 * Bei ALWAYS_SIM_1/2 wird "(Standard-SMS)" angehängt, falls zutreffend.
 */
private fun buildLabel(
    mode: SimSelectionMode,
    simCount: Int,
    defaultSmsSubId: Int,
    sim1SubId: Int?,
    sim2SubId: Int?
): String {
    return when (mode) {
        SimSelectionMode.SAME_AS_INCOMING -> mode.displayName

        SimSelectionMode.ALWAYS_SIM_1 -> {
            val isDefault = sim1SubId == defaultSmsSubId && sim1SubId != -1
            val suffix = if (isDefault) " (Standard-SMS)" else ""
            "${mode.displayName}$suffix"
        }

        SimSelectionMode.ALWAYS_SIM_2 -> {
            val isDefault = sim2SubId == defaultSmsSubId && sim2SubId != -1
            val isAvailable = simCount >= 2
            val suffix = when {
                !isAvailable -> " (nicht verfügbar)"
                isDefault -> " (Standard-SMS)"
                else -> ""
            }
            "${mode.displayName}$suffix"
        }
    }
}
