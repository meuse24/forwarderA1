package info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import info.meuse24.smsforwarderneoA1.R
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/**
 * PIN entry dialog for accessing protected features (e.g., logs).
 *
 * Validates the entered PIN against the stored PIN in preferences.
 *
 * @param storedPin The currently stored PIN to validate against
 * @param onPinCorrect Callback when correct PIN is entered
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
fun PinDialog(
    storedPin: String,
    onPinCorrect: () -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_enter_pin)) },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                            pin = it
                            error = false
                        }
                    },
                    label = { Text(stringResource(R.string.label_pin_input)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = error,
                    supportingText = if (error) {
                        { Text(stringResource(R.string.error_wrong_pin)) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (pin == storedPin) {
                    onPinCorrect()
                } else {
                    error = true
                }
            }) {
                Text(stringResource(R.string.btn_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

/**
 * Dialog for changing the log access PIN.
 *
 * Requires current PIN validation before allowing a new PIN to be set.
 * The new PIN must be 4 digits and must be confirmed.
 *
 * @param storedPin The currently stored PIN to validate against
 * @param onPinChanged Callback when PIN is successfully changed (receives the new PIN)
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
fun ChangePinDialog(
    storedPin: String,
    onPinChanged: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_change_pin)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = currentPin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                            currentPin = it
                            error = null
                        }
                    },
                    label = { Text(stringResource(R.string.label_current_pin)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newPin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                            newPin = it
                            error = null
                        }
                    },
                    label = { Text(stringResource(R.string.label_new_pin)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                            confirmPin = it
                            error = null
                        }
                    },
                    label = { Text(stringResource(R.string.label_confirm_new_pin)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                error?.let { errorText ->
                    Text(
                        text = errorText,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    currentPin != storedPin -> {
                        error = context.getString(R.string.error_current_pin_wrong)
                    }
                    newPin.length != 4 -> {
                        error = context.getString(R.string.error_pin_length_invalid)
                    }
                    newPin != confirmPin -> {
                        error = context.getString(R.string.error_pin_mismatch)
                    }
                    else -> {
                        onPinChanged(newPin)
                        onDismiss()
                    }
                }
            }) {
                Text(stringResource(R.string.btn_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}
