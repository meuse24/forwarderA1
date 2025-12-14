package info.meuse24.smsforwarderneoA1.presentation.ui.components.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
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
    AppAlertDialog(
        onDismissRequest = { /* keep dialog for 4s window */ },
        title = stringResource(R.string.mmi_confirmation_title),
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
            DialogConfirmButton(
                text = stringResource(R.string.mmi_confirmation_positive),
                onClick = onConfirm
            )
        },
        dismissButton = {
            DialogDismissButton(
                text = stringResource(R.string.mmi_confirmation_negative),
                onClick = onDecline
            )
        }
    )
}
