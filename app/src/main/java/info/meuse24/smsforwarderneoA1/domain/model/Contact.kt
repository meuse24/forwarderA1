package info.meuse24.smsforwarderneoA1.domain.model

/**
 * Domain model representing the contact a forwarding is registered to.
 *
 * There is exactly one selected contact at a time; the app no longer manages a
 * contact list of its own since the switch to the system contact picker.
 */
data class Contact(
    val name: String,
    val phoneNumber: String
)
