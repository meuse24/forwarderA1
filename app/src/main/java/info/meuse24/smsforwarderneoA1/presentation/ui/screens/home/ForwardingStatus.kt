package info.meuse24.smsforwarderneoA1.presentation.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import info.meuse24.smsforwarderneoA1.domain.model.Contact

@Composable
fun ForwardingStatus(
    forwardingActive: Boolean,
    selectedContact: Contact?,
    forwardSmsToEmail: Boolean,
    emailAddresses: List<String>,
    onQueryStatus: () -> Unit
) {
    val hasEmailForwarding = forwardSmsToEmail && emailAddresses.isNotEmpty()
    val hasAnyForwarding = forwardingActive || hasEmailForwarding

    Surface(
        color = if (hasAnyForwarding) Color.Green else Color.Red,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Text(s)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (forwardingActive) {
                    Text(
                        text = "SMS-Weiterleitung aktiv zu ${selectedContact?.phoneNumber}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                if (hasEmailForwarding) {
                    Text(
                        text = "Email-Weiterleitung aktiv an ${emailAddresses.size} Adresse(n)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                if (!hasAnyForwarding) {
                    Text(
                        text = "Weiterleitung inaktiv",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Status Query Icon Button
            IconButton(
                onClick = onQueryStatus,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Status abfragen",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
