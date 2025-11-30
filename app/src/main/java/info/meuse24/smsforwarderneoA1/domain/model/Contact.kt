package info.meuse24.smsforwarderneoA1.domain.model

/**
 * Domain model representing a contact with phone number.
 *
 * Equality is based on normalized phone number (digits only),
 * not on the contact name or description.
 */
data class Contact(
    val name: String,
    val phoneNumber: String,
    val description: String
) {
    // Normalisierte Telefonnummer für Vergleiche
    private val normalizedNumber = phoneNumber.filter { it.isDigit() }

    // Der Name sollte bei equals/hashCode NICHT berücksichtigt werden
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Contact) return false
        return normalizedNumber == other.normalizedNumber
    }

    override fun hashCode(): Int {
        return normalizedNumber.hashCode()
    }
}
