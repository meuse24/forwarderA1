package info.meuse24.smsforwarderneoA1.presentation.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import info.meuse24.smsforwarderneoA1.domain.model.Contact

@Composable
fun ContactListBox(
    contacts: List<Contact>,
    selectedContact: Contact?,
    onSelectContact: (Contact) -> Unit,
    modifier: Modifier = Modifier,
    isCallActive: Boolean = false
) {
    Box(modifier = modifier) {
        if (contacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Keine Kontakte gefunden",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                items(contacts) { contact ->
                    ContactItem(
                        contact = contact,
                        isSelected = contact == selectedContact,
                        onSelect = { onSelectContact(contact) },
                        isCallActive = isCallActive
                    )
                }
            }
        }
    }
}

@Composable
fun ContactItem(
    contact: Contact,
    isSelected: Boolean,
    onSelect: () -> Unit,
    isCallActive: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isCallActive) {
                // Eigentelefonnummer-Prüfung entfernt - wird über SIM-Verwaltung abgewickelt
                onSelect()
            }
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .padding(vertical = 4.dp, horizontal = 16.dp)
    ) {
        Text(
            text = contact.name,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = contact.description,
            style = MaterialTheme.typography.bodySmall
        )
    }
    HorizontalDivider()
}
