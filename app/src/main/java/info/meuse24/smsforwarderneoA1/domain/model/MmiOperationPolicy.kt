package info.meuse24.smsforwarderneoA1.domain.model

/** Pure coordination rules shared by the Android entry points and JVM tests. */
object MmiOperationPolicy {
    const val USSD_RESPONSE_TIMEOUT_MS = 30_000L
    const val VOICE_MMI_OPERATION_TIMEOUT_MS = 60_000L

    fun timeoutFor(mode: MmiExecutionMode): Long = when (mode) {
        MmiExecutionMode.USSD_CALLBACK -> USSD_RESPONSE_TIMEOUT_MS
        MmiExecutionMode.VOICE_MMI_CALL -> VOICE_MMI_OPERATION_TIMEOUT_MS
    }

    fun isExpired(mode: MmiExecutionMode, dialedAtMillis: Long, nowMillis: Long): Boolean =
        nowMillis - dialedAtMillis >= timeoutFor(mode)

    fun mayStartOperation(hasPendingOperation: Boolean, reservationInProgress: Boolean): Boolean =
        !hasPendingOperation && !reservationInProgress

    fun matchesPendingOperation(pendingOperationId: String?, callbackOperationId: String): Boolean =
        pendingOperationId == callbackOperationId

    fun shouldDialAfterWaitingForCall(idleReached: Boolean): Boolean = idleReached

    /** A missing USSD callback is unverifiable, but must not stop local SMS forwarding. */
    fun shouldContinueSmsForwardingAfterTimeout(mode: MmiExecutionMode): Boolean =
        mode == MmiExecutionMode.USSD_CALLBACK
}
