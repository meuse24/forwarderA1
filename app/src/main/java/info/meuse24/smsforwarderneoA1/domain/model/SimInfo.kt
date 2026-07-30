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
    val isAutoDetected: Boolean = false,
    val carrierId: Int? = null,
    val mccMnc: String? = null,
)

/**
 * Die SIM in einem Steckplatz - `slotIndex` 0 ist "SIM 1", 1 ist "SIM 2".
 *
 * Immer hierüber zugreifen, **nie** über den Listenindex. `activeSubscriptionInfoList` sichert
 * keine Reihenfolge zu, und bei nur einer bestückten Karte ist deren Listenindex 0, egal in
 * welchem Steckplatz sie sitzt: `getOrNull(0)` liefert dann die SIM aus Slot 2 als "SIM 1".
 */
fun List<SimInfo>.inSlot(slotIndex: Int): SimInfo? = firstOrNull { it.slotIndex == slotIndex }
