package info.meuse24.smsforwarderneoA1.domain.model

/** Pure state transitions for an unfinished carrier command; safe to unit-test on the JVM. */
object MmiOperationReducer {
    fun withUssdResponse(
        operation: PersistedMmiOperation,
        response: String,
        success: Boolean
    ): PersistedMmiOperation = operation.copy(
        state = MmiOperationState.SETTLED,
        verification = if (success) ForwardingVerification.CONFIRMED_SUCCESS else ForwardingVerification.DIAL_FAILED,
        evidence = operation.evidence.copy(ussdResponse = response)
    )

    fun withTimeout(operation: PersistedMmiOperation): PersistedMmiOperation = operation.copy(
        state = MmiOperationState.SETTLED,
        verification = if (operation.mode == MmiExecutionMode.USSD_CALLBACK) {
            ForwardingVerification.UNKNOWN_NO_RESPONSE
        } else {
            ForwardingVerification.DIAL_FAILED
        },
        evidence = operation.evidence.copy(watchdogExpired = true)
    )

    fun withCallState(
        operation: PersistedMmiOperation,
        offHookAtMillis: Long?,
        idleAtMillis: Long
    ): PersistedMmiOperation {
        val observed = offHookAtMillis != null
        return operation.copy(evidence = operation.evidence.copy(
            callObserved = observed,
            callDurationMs = offHookAtMillis?.let { (idleAtMillis - it).coerceAtLeast(0L) }
        ))
    }

    /** Records that no expected voice-call signal was observed in the evidence window. */
    fun withMissingCallObservation(operation: PersistedMmiOperation): PersistedMmiOperation = operation.copy(
        evidence = operation.evidence.copy(watchdogExpired = true)
    )
}
