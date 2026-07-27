package info.meuse24.smsforwarderneoA1.domain.model

/**
 * Pure decision of the local consequences of a completed forwarding operation.
 * Android persistence, notifications and UI apply this decision in the ViewModel.
 */
object ForwardingResolutionReducer {
    enum class ContactHandling { KEEP, SAVE_PENDING_CONTACT, CLEAR }

    data class Resolution(
        val forwardingActive: Boolean?,
        val contactHandling: ContactHandling
    )

    fun resolve(action: String, success: Boolean): Resolution = when {
        action == "ACTIVATE" && success -> Resolution(true, ContactHandling.SAVE_PENDING_CONTACT)
        action == "DEACTIVATE" && success -> Resolution(false, ContactHandling.CLEAR)
        else -> Resolution(null, ContactHandling.KEEP)
    }
}
