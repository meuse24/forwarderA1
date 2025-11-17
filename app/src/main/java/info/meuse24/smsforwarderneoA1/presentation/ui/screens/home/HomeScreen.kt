package info.meuse24.smsforwarderneoA1.presentation.ui.screens.home

import android.telephony.TelephonyManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import info.meuse24.smsforwarderneoA1.ContactsViewModel
import info.meuse24.smsforwarderneoA1.domain.model.Contact

@Composable
fun HomeScreen(viewModel: ContactsViewModel, callState: androidx.compose.runtime.State<Int>) {
    val contacts by viewModel.contacts.collectAsState()
    val selectedContact by viewModel.selectedContact.collectAsState()
    val forwardingActive by viewModel.forwardingActive.collectAsState()
    val filterText by viewModel.filterText.collectAsState()
    val forwardSmsToEmail by viewModel.forwardSmsToEmail.collectAsState()
    val emailAddresses by viewModel.emailAddresses.collectAsState()
    val currentCallState by callState

    // Check if call is active (for button disabling)
    val isCallActive = currentCallState == TelephonyManager.CALL_STATE_OFFHOOK

    // Initialisierung beim ersten Laden
    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    // Filter neu anwenden beim Betreten des Screens
    LaunchedEffect(Unit) {
        if (filterText.isNotEmpty()) {
            viewModel.applyCurrentFilter()
        }
    }

    BoxWithConstraints {
        @Suppress("UNUSED_EXPRESSION")
        val isLandscape = this.maxWidth > this.maxHeight

        if (isLandscape) {
            LandscapeLayout(
                viewModel = viewModel,
                contacts = contacts,
                selectedContact = selectedContact,
                forwardingActive = forwardingActive,
                filterText = filterText,
                forwardSmsToEmail = forwardSmsToEmail,
                emailAddresses = emailAddresses,
                isCallActive = isCallActive,
                callState = currentCallState
            )
        } else {
            PortraitLayout(
                viewModel = viewModel,
                contacts = contacts,
                selectedContact = selectedContact,
                forwardingActive = forwardingActive,
                filterText = filterText,
                forwardSmsToEmail = forwardSmsToEmail,
                emailAddresses = emailAddresses,
                isCallActive = isCallActive,
                callState = currentCallState
            )
        }
    }
}

@Composable
fun LandscapeLayout(
    viewModel: ContactsViewModel,
    contacts: List<Contact>,
    selectedContact: Contact?,
    forwardingActive: Boolean,
    filterText: String,
    forwardSmsToEmail: Boolean,
    emailAddresses: List<String>,
    isCallActive: Boolean,
    callState: Int
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp)
    ) {
        ContactListBox(
            contacts = contacts,
            selectedContact = selectedContact,
            onSelectContact = viewModel::toggleContactSelection,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            isCallActive = isCallActive
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            FilterAndLogo(
                filterText = filterText,
                onFilterTextChange = {
                    viewModel.updateFilterText(it)
                },
                forwardingActive = forwardingActive,
                onDeactivateForwarding = viewModel::deactivateForwarding
            )

            CallStatusCard(callState = callState)

            ForwardingStatus(
                forwardingActive = forwardingActive,
                selectedContact = selectedContact,
                forwardSmsToEmail = forwardSmsToEmail,
                emailAddresses = emailAddresses,
                onQueryStatus = viewModel::queryForwardingStatus
            )

            ControlButtons(
                onDeactivateForwarding = viewModel::deactivateForwarding,
                onSendTestSms = viewModel::sendTestSms,
                isEnabled = selectedContact != null
            )
        }
    }
}

@Composable
fun PortraitLayout(
    viewModel: ContactsViewModel,
    contacts: List<Contact>,
    selectedContact: Contact?,
    forwardingActive: Boolean,
    filterText: String,
    forwardSmsToEmail: Boolean,
    emailAddresses: List<String>,
    isCallActive: Boolean,
    callState: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        FilterAndLogo(
            filterText = filterText,
            onFilterTextChange = {
                viewModel.updateFilterText(it)
            },
            forwardingActive = forwardingActive,
            onDeactivateForwarding = viewModel::deactivateForwarding
        )

        ContactListBox(
            contacts = contacts,
            selectedContact = selectedContact,
            onSelectContact = viewModel::toggleContactSelection,
            modifier = Modifier.weight(1f),
            isCallActive = isCallActive
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CallStatusCard(callState = callState)
            ForwardingStatus(
                forwardingActive = forwardingActive,
                selectedContact = selectedContact,
                forwardSmsToEmail = forwardSmsToEmail,
                emailAddresses = emailAddresses,
                onQueryStatus = viewModel::queryForwardingStatus
            )
            Spacer(modifier = Modifier.height(4.dp))
            ControlButtons(
                onDeactivateForwarding = viewModel::deactivateForwarding,
                onSendTestSms = viewModel::sendTestSms,
                isEnabled = selectedContact != null
            )
        }
    }
}
