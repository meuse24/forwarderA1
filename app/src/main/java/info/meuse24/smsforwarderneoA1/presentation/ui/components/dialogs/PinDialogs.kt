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
        title = { Text("PIN eingeben") },
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
                    label = { Text("PIN") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = error,
                    supportingText = if (error) {
                        { Text("Falsche PIN") }
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
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
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
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PIN ändern") },
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
                    label = { Text("Aktuelle PIN") },
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
                    label = { Text("Neue PIN") },
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
                    label = { Text("Neue PIN bestätigen") },
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
                        error = "Aktuelle PIN ist falsch"
                    }
                    newPin.length != 4 -> {
                        error = "Neue PIN muss 4 Stellen haben"
                    }
                    newPin != confirmPin -> {
                        error = "PINs stimmen nicht überein"
                    }
                    else -> {
                        onPinChanged(newPin)
                        onDismiss()
                    }
                }
            }) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
