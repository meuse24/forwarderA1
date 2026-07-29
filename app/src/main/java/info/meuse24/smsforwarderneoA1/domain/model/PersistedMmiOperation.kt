package info.meuse24.smsforwarderneoA1.domain.model

/** Data-layer-safe representation of an unfinished carrier command. */
data class PersistedMmiOperation(
    val id: String, val action: String, val code: String, val mode: MmiExecutionMode,
    val dialedAtMillis: Long, val contactName: String?, val contactNumber: String?,
    val state: MmiOperationState = MmiOperationState.DIALING,
    val verification: ForwardingVerification = ForwardingVerification.NOT_CHECKED,
    val evidence: MmiEvidence = MmiEvidence(),
    val targetSubscriptionId: Int? = null,
    val dialPath: DialPath = DialPath.NOT_DISPATCHED,
    val userMessage: String? = null
)
