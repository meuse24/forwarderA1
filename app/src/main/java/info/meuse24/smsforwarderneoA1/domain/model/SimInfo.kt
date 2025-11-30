package info.meuse24.smsforwarderneoA1.domain.model

/**
 * Domain model for SIM card information.
 *
 * Contains subscription ID, slot index, display names, and phone number.
 */
data class SimInfo(
    val subscriptionId: Int,
    val slotIndex: Int,
    val displayName: String?,
    val carrierName: String?,
    val phoneNumber: String? = null, // Auto-erkannt oder aus Preferences
    val isAutoDetected: Boolean = false
)
