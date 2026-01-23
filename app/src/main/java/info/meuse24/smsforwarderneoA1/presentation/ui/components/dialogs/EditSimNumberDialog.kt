package info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import info.meuse24.smsforwarderneoA1.R
import info.meuse24.smsforwarderneoA1.domain.model.SimInfo
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.DialogDefaults
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.DialogButtonRow
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.DialogButtonSpacer
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.DialogConfirmButton
import info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs.DialogDismissButton

@Composable
fun EditSimNumberDialog(
    simInfo: SimInfo,
    currentNumber: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var number by remember { mutableStateOf(currentNumber) }

    // Validation helper
    fun isValidPhoneNumber(input: String): Boolean {
        // Allow optional + at start, followed by 3 to 15 digits
        return input.trim().matches(Regex("^\\+?[0-9]{3,15}$"))
    }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.dialog_title_edit_sim_number),
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.msg_edit_sim_number_explanation),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = DialogDefaults.StandardSpacing)
                )

                Text(
                    text = stringResource(R.string.label_sim_slot, simInfo.slotIndex + 1),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (!simInfo.carrierName.isNullOrEmpty()) {
                    Text(
                        text = simInfo.carrierName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = DialogDefaults.StandardSpacing)
                    )
                }

                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text(stringResource(R.string.label_phone_number)) },
                    placeholder = { Text(stringResource(R.string.placeholder_phone_number_example)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            DialogButtonRow {
                DialogDismissButton(
                    text = stringResource(R.string.btn_cancel),
                    onClick = onDismiss
                )
                DialogButtonSpacer()
                DialogConfirmButton(
                    text = stringResource(R.string.btn_save),
                    onClick = { onSave(number.trim()) },
                    enabled = number.isNotBlank() && isValidPhoneNumber(number)
                )
            }
        }
    )
}
