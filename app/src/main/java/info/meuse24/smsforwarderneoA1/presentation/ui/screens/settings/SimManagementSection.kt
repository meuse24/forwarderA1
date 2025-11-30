package info.meuse24.smsforwarderneoA1.presentation.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import info.meuse24.smsforwarderneoA1.AppContainer
import info.meuse24.smsforwarderneoA1.ContactsViewModel
import info.meuse24.smsforwarderneoA1.LoggingManager
import info.meuse24.smsforwarderneoA1.PhoneSmsUtils
import info.meuse24.smsforwarderneoA1.domain.model.SimInfo

@Composable
fun SimManagementSection(
    viewModel: ContactsViewModel,
    onFocusChanged: (Boolean) -> Unit,
    sectionTitleStyle: TextStyle
) {
    val context = LocalContext.current
    var simInfoList by remember { mutableStateOf<List<SimInfo>>(emptyList()) }
    var storedNumbers by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var defaultSimIds by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Load SIM info on first composition
    LaunchedEffect(Unit) {
        try {
            val sims = PhoneSmsUtils.getAllSimInfo(context)
            val stored = AppContainer.getPrefsManagerSafe()?.getSimPhoneNumbers() ?: emptyMap()
            val defaults = PhoneSmsUtils.getDefaultSimIds(context)
            simInfoList = sims
            storedNumbers = stored
            defaultSimIds = defaults
            isLoading = false
        } catch (e: Exception) {
            LoggingManager.logError(
                component = "SimManagementSection",
                action = "LOAD_SIM_INFO",
                message = "Fehler beim Laden der SIM-Informationen",
                error = e
            )
            isLoading = false
        }
    }

    // No focus tracking needed for read-only display
    LaunchedEffect(Unit) {
        onFocusChanged(false)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "SIM-Karten Übersicht",
            style = sectionTitleStyle,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        } else if (simInfoList.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Keine SIM-Karten gefunden",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "Stellen Sie sicher, dass Berechtigungen erteilt wurden",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        } else {
            // Show SIM cards (read-only)
            simInfoList.forEach { sim ->
                val currentNumber = storedNumbers[sim.subscriptionId] ?: sim.phoneNumber ?: ""

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // SIM Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "SIM ${sim.slotIndex + 1}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (!sim.carrierName.isNullOrEmpty()) {
                                    Text(
                                        text = sim.carrierName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (!sim.displayName.isNullOrEmpty() && sim.displayName != sim.carrierName) {
                                    Text(
                                        text = sim.displayName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Check if this SIM is default for SMS or Voice
                                val isDefaultSms = defaultSimIds?.first == sim.subscriptionId
                                val isDefaultVoice = defaultSimIds?.second == sim.subscriptionId

                                // Default SIM Badge
                                if (isDefaultSms || isDefaultVoice) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.tertiaryContainer
                                    ) {
                                        Text(
                                            text = when {
                                                isDefaultSms && isDefaultVoice -> "Standard"
                                                isDefaultSms -> "Standard SMS"
                                                else -> "Standard Voice"
                                            },
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }

                                if (sim.isAutoDetected) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "Auto",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                // Status Badge
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = if (currentNumber.isNotEmpty()) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.errorContainer
                                    }
                                ) {
                                    Text(
                                        text = if (currentNumber.isNotEmpty()) "Konfiguriert" else "Fehlend",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (currentNumber.isNotEmpty()) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onErrorContainer
                                        }
                                    )
                                }
                            }
                        }

                        // Phone Number Display
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentNumber.isNotEmpty()) {
                                    currentNumber
                                } else {
                                    "Nicht konfiguriert"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (currentNumber.isNotEmpty()) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontFamily = if (currentNumber.isNotEmpty()) {
                                    FontFamily.Monospace
                                } else {
                                    FontFamily.Default
                                }
                            )
                        }

                        // Source Info
                        if (currentNumber.isNotEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (sim.isAutoDetected) Icons.Default.Visibility else Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (sim.isAutoDetected) {
                                            "Automatisch erkannt"
                                        } else {
                                            "Manuell eingegeben"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Summary info
            if (simInfoList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Zusammenfassung:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "• ${simInfoList.size} SIM-Karte(n) erkannt",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val autoDetected = simInfoList.count { !it.phoneNumber.isNullOrEmpty() }
                        Text(
                            text = "• $autoDetected automatisch erkannt",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val withStoredNumbers = storedNumbers.values.count { it.isNotEmpty() }
                        Text(
                            text = "• $withStoredNumbers Nummer(n) konfiguriert",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Hinweis für fehlende Nummern
                        if (withStoredNumbers < simInfoList.size) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "ℹ️ Fehlende Nummern werden beim App-Start abgefragt",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
