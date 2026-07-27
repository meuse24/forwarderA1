package info.meuse24.smsforwarderneoA1.domain.model

/** Independent, non-authoritative observations collected while a carrier command runs. */
data class MmiEvidence(
    val callObserved: Boolean = false,
    val callDurationMs: Long? = null,
    val watchdogExpired: Boolean = false,
    val ussdResponse: String? = null
)
