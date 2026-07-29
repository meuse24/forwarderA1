package info.meuse24.smsforwarderneoA1.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MmiOperationReducerTest {
    private fun operation(mode: MmiExecutionMode = MmiExecutionMode.USSD_CALLBACK) =
        PersistedMmiOperation("id", "ACTIVATE", "*21*123#", mode, 0, null, null)

    @Test fun `USSD response confirms operation and stores response`() {
        val result = MmiOperationReducer.withUssdResponse(operation(), "OK", true)
        assertEquals(MmiOperationState.SETTLED, result.state)
        assertEquals(ForwardingVerification.CONFIRMED_SUCCESS, result.verification)
        assertEquals("OK", result.evidence.ussdResponse)
    }

    @Test fun `USSD timeout is unknown rather than failed`() {
        val result = MmiOperationReducer.withTimeout(operation())
        assertEquals(ForwardingVerification.UNKNOWN_NO_RESPONSE, result.verification)
        assertTrue(result.evidence.watchdogExpired)
    }

    @Test fun `voice timeout is a dialing failure`() {
        val result = MmiOperationReducer.withTimeout(operation(MmiExecutionMode.VOICE_MMI_CALL))
        assertEquals(ForwardingVerification.DIAL_FAILED, result.verification)
    }

    @Test fun `call evidence records observed duration`() {
        val result = MmiOperationReducer.withCallState(operation(), 1_000, 3_500)
        assertTrue(result.evidence.callObserved)
        assertEquals(2_500L, result.evidence.callDurationMs)
    }

    @Test fun `idle without offhook is not observed`() {
        val result = MmiOperationReducer.withCallState(operation(), null, 3_500)
        assertFalse(result.evidence.callObserved)
    }

    @Test fun `missing voice call observation only marks evidence`() {
        val initial = operation(MmiExecutionMode.VOICE_MMI_CALL).copy(
            state = MmiOperationState.SETTLED,
            verification = ForwardingVerification.ASSUMED_SUCCESS
        )

        val result = MmiOperationReducer.withMissingCallObservation(initial)

        assertEquals(MmiOperationState.SETTLED, result.state)
        assertEquals(ForwardingVerification.ASSUMED_SUCCESS, result.verification)
        assertTrue(result.evidence.watchdogExpired)
    }
}
