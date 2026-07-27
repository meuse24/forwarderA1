package info.meuse24.smsforwarderneoA1.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MmiOperationPolicyTest {
    @Test fun `USSD expires after thirty seconds and continues local SMS forwarding`() {
        assertTrue(MmiOperationPolicy.isExpired(MmiExecutionMode.USSD_CALLBACK, 1_000, 31_000))
        assertTrue(MmiOperationPolicy.shouldContinueSmsForwardingAfterTimeout(MmiExecutionMode.USSD_CALLBACK))
    }

    @Test fun `voice MMI remains pending before sixty seconds and expires at hard limit`() {
        assertFalse(MmiOperationPolicy.isExpired(MmiExecutionMode.VOICE_MMI_CALL, 1_000, 60_999))
        assertTrue(MmiOperationPolicy.isExpired(MmiExecutionMode.VOICE_MMI_CALL, 1_000, 61_000))
        assertFalse(MmiOperationPolicy.shouldContinueSmsForwardingAfterTimeout(MmiExecutionMode.VOICE_MMI_CALL))
    }

    @Test fun `late callback with a foreign operation id is rejected`() {
        assertFalse(MmiOperationPolicy.matchesPendingOperation("new-operation", "old-operation"))
        assertTrue(MmiOperationPolicy.matchesPendingOperation("current-operation", "current-operation"))
    }

    @Test fun `dialing lock rejects a second request while pending or reserved`() {
        assertFalse(MmiOperationPolicy.mayStartOperation(hasPendingOperation = true, reservationInProgress = false))
        assertFalse(MmiOperationPolicy.mayStartOperation(hasPendingOperation = false, reservationInProgress = true))
        assertTrue(MmiOperationPolicy.mayStartOperation(hasPendingOperation = false, reservationInProgress = false))
    }

    @Test fun `pre dial timeout never continues to dialing`() {
        assertFalse(MmiOperationPolicy.shouldDialAfterWaitingForCall(idleReached = false))
        assertTrue(MmiOperationPolicy.shouldDialAfterWaitingForCall(idleReached = true))
    }

    @Test fun `timeout transitions preserve activate and deactivate actions`() {
        val activate = PersistedMmiOperation("activate", "ACTIVATE", "*21*1#", MmiExecutionMode.USSD_CALLBACK, 0, null, null, null)
        val deactivate = PersistedMmiOperation("deactivate", "DEACTIVATE", "##21#", MmiExecutionMode.USSD_CALLBACK, 0, null, null, null)

        assertTrue(MmiOperationReducer.withTimeout(activate).action == "ACTIVATE")
        assertTrue(MmiOperationReducer.withTimeout(deactivate).action == "DEACTIVATE")
    }
}
