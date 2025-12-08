package info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import info.meuse24.smsforwarderneoA1.R
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import info.meuse24.smsforwarderneoA1.domain.model.SimInfo

/**
 * Dialog for manual SIM card phone number input.
 *
 * Shown when the app cannot automatically detect phone numbers for SIM cards.
 * Users must manually enter phone numbers for each SIM card before proceeding.
 *
 * The dialog is non-dismissible until either all numbers are entered or the user
 * explicitly chooses to skip the process.
 *
 * @param missingSims List of SIM cards that need manual phone number entry
 * @param onDismiss Callback when user dismisses/skips the dialog
 * @param onSaveNumber Callback when a phone number is saved (receives subscriptionId and number)
 */
@Composable
fun SimNumbersDialog(
    missingSims: List<SimInfo>,
    onDismiss: () -> Unit,
    onSaveNumber: (Int, String) -> Unit
) {
    val numberInputs = remember {
        mutableStateMapOf<Int, String>().apply {
            missingSims.forEach { sim ->
                this[sim.subscriptionId] = ""
            }
        }
    }

    AlertDialog(
        onDismissRequest = { /* Dialog kann nicht ohne Eingabe geschlossen werden */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        title = {
            Text(
                text = stringResource(R.string.dialog_title_sim_numbers_required),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.msg_sim_numbers_explanation),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(missingSims) { sim ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.label_sim_slot, sim.slotIndex + 1),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (!sim.carrierName.isNullOrEmpty()) {
                                Text(
                                    text = stringResource(R.string.label_carrier, sim.carrierName),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (!sim.displayName.isNullOrEmpty() && sim.displayName != sim.carrierName) {
                                Text(
                                    text = stringResource(R.string.label_display_name, sim.displayName),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            OutlinedTextField(
                                value = numberInputs[sim.subscriptionId] ?: "",
                                onValueChange = { value ->
                                    numberInputs[sim.subscriptionId] = value
                                },
                                label = { Text(stringResource(R.string.label_phone_number)) },
                                placeholder = { Text(stringResource(R.string.placeholder_phone_number_example)) },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Next
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            val allFilled = missingSims.all { sim ->
                val input = numberInputs[sim.subscriptionId]
                !input.isNullOrBlank() && input.trim().length >= 5
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.btn_skip))
                }

                Button(
                    onClick = {
                        // Speichere alle eingegebenen Nummern
                        missingSims.forEach { sim ->
                            val number = numberInputs[sim.subscriptionId]?.trim()
                            if (!number.isNullOrBlank()) {
                                onSaveNumber(sim.subscriptionId, number)
                            }
                        }
                        onDismiss()
                    },
                    enabled = allFilled
                ) {
                    Text(stringResource(R.string.btn_save))
                }
            }
        }
    )
}
