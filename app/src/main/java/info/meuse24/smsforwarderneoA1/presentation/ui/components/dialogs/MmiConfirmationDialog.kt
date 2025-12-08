package info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import info.meuse24.smsforwarderneoA1.R

@Composable
fun MmiConfirmationDialog(
    contactName: String?,
    contactNumber: String?,
    onConfirm: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* keep dialog for 4s window */ },
        title = {
            Text(
                text = stringResource(R.string.mmi_confirmation_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(text = stringResource(R.string.mmi_confirmation_body))
                if (!contactName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = contactName,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (!contactNumber.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = contactNumber)
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(text = stringResource(R.string.mmi_confirmation_positive))
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text(text = stringResource(R.string.mmi_confirmation_negative))
            }
        }
    )
}
