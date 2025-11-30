package info.meuse24.smsforwarderneoA1.presentation.state

import info.meuse24.smsforwarderneoA1.domain.model.Contact

/**
 * UI state for contacts screen.
 *
 * Contains loading state, contacts list, selected contact, forwarding status,
 * email settings, and error messages.
 */
data class ContactsState(
    val isLoading: Boolean = false,
    val contacts: List<Contact> = emptyList(),
    val selectedContact: Contact? = null,
    val forwardingActive: Boolean = false,
    val selectedPhoneNumber: String = "",
    val emailForwardingEnabled: Boolean = false,
    val emailAddresses: List<String> = emptyList(),
    val error: String? = null
)
