package info.meuse24.smsforwarderneoA1.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsDeliveryReducerTest {

    private val start = 1_000_000L

    private fun queued(expectedParts: Int = 1) = SmsDeliveryReducer.queue(
        id = "op-1",
        now = start,
        sender = "+436601234567",
        targetNumber = "+436609876543",
        text = "Text",
        subscriptionId = 1,
        expectedParts = expectedParts
    )

    private fun attempting(expectedParts: Int = 1) =
        SmsDeliveryReducer.onAttemptStart(queued(expectedParts), start, expectedParts)

    private fun handedOver(expectedParts: Int = 1) =
        SmsDeliveryReducer.onHandedOver(attempting(expectedParts), start)

    /** Bringt einen Vorgang ueber echte transiente Fehlschlaege auf den n-ten Sendeversuch. */
    private fun handedOverAtAttempt(attempt: Int, expectedParts: Int = 1): ForwardingOperation {
        var operation = handedOver(expectedParts)
        while (operation.attempt < attempt) {
            operation = SmsDeliveryReducer.onPartResult(
                operation, operation.attempt, 0, SmsPartResult.TRANSIENT_FAILURE,
                RESULT_ERROR_NO_SERVICE, start
            )
            val due = operation.nextAttemptAtMillis!!
            operation = SmsDeliveryReducer.onAttemptStart(operation, due, expectedParts)
            operation = SmsDeliveryReducer.onHandedOver(operation, due)
        }
        return operation
    }

    @Test fun `a fresh operation is queued and therefore provably unsent`() {
        val operation = queued()

        assertEquals(ForwardingState.QUEUED, operation.state)
        assertEquals(0, operation.attempt)
        assertTrue(SmsDeliveryReducer.isDispatchDue(operation, start))
    }

    @Test fun `starting an attempt counts up and clears the results of the previous attempt`() {
        val retrying = SmsDeliveryReducer.onPartResult(
            handedOver(3), 1, 0, SmsPartResult.TRANSIENT_FAILURE, RESULT_ERROR_NO_SERVICE, start
        )

        val second = SmsDeliveryReducer.onAttemptStart(retrying, retrying.nextAttemptAtMillis!!, 3)

        assertEquals(ForwardingState.ATTEMPTING, second.state)
        assertEquals(2, second.attempt)
        assertEquals(0, second.confirmedParts)
        assertEquals(0, second.failedParts)
    }

    @Test fun `an operation already being sent cannot be claimed a second time`() {
        // Der Versand direkt nach dem Einreihen und ein gleichzeitiger Queue-Scan greifen sonst
        // nach demselben Vorgang - und schicken ihn zweimal hinaus.
        val claimed = attempting()

        val secondClaim = SmsDeliveryReducer.onAttemptStart(claimed, start, 1)

        assertEquals(claimed, secondClaim)
        assertEquals(1, secondClaim.attempt)
    }

    @Test fun `a claim is refused from every state that is not due for dispatch`() {
        listOf(handedOver(), attempting(), SmsDeliveryReducer.onPreconditionRejected(queued(), start, "x"))
            .forEach { operation ->
                assertEquals(operation, SmsDeliveryReducer.onAttemptStart(operation, start, 1))
            }
    }

    @Test fun `a retry may not be claimed before its backoff has elapsed`() {
        val retrying = SmsDeliveryReducer.onPartResult(
            handedOver(), 1, 0, SmsPartResult.TRANSIENT_FAILURE, RESULT_ERROR_NO_SERVICE, start
        )
        val due = retrying.nextAttemptAtMillis!!

        assertEquals(retrying, SmsDeliveryReducer.onAttemptStart(retrying, due - 1, 1))
        assertEquals(ForwardingState.ATTEMPTING, SmsDeliveryReducer.onAttemptStart(retrying, due, 1).state)
    }

    @Test fun `a violated precondition fails without ever writing attempting`() {
        val failed = SmsDeliveryReducer.onPreconditionRejected(queued(), start, "empty_target_number")

        assertEquals(ForwardingState.FAILED, failed.state)
        assertEquals(0, failed.attempt)
        assertEquals("empty_target_number", failed.reason)
    }

    @Test fun `a stale precondition rejection cannot fail an operation that is already being sent`() {
        // Sonst erklaerte das Pruefergebnis eines veralteten Durchlaufs den laufenden Versand fuer
        // gescheitert - und alle folgenden Callbacks waeren wirkungslos.
        val running = attempting()

        val unchanged = SmsDeliveryReducer.onPreconditionRejected(running, start, "missing_permission_send_sms")

        assertEquals(running, unchanged)
        assertEquals(ForwardingState.ATTEMPTING, unchanged.state)
    }

    @Test fun `a precondition rejection applies to an operation that is still due for dispatch`() {
        val rejected = SmsDeliveryReducer.onPreconditionRejected(queued(), start, "empty_text")

        assertEquals(ForwardingState.FAILED, rejected.state)
    }

    @Test fun `only a named precondition type may turn a dispatch exception into failed`() {
        val precondition = SmsDeliveryReducer.onDispatchException(attempting(), start, SecurityException("denied"))

        assertEquals(ForwardingState.FAILED, precondition.state)
    }

    @Test fun `any other dispatch exception is unknown because handover is not disproven`() {
        // Stellvertretend fuer DeadObjectException / RemoteException aus dem Binder-Aufruf.
        val binderFailure = SmsDeliveryReducer.onDispatchException(attempting(), start, IllegalStateException("dead"))

        assertEquals(ForwardingState.UNKNOWN, binderFailure.state)
        assertNull(binderFailure.nextAttemptAtMillis)
    }

    @Test fun `attempting found after a process restart becomes unknown without resend`() {
        val recovered = SmsDeliveryReducer.onProcessRestart(attempting(), start + 5_000)

        assertEquals(ForwardingState.UNKNOWN, recovered.state)
        assertFalse(SmsDeliveryReducer.isDispatchDue(recovered, start + 5_000))
    }

    @Test fun `queued found after a process restart stays queued and is resent`() {
        val recovered = SmsDeliveryReducer.onProcessRestart(queued(), start + 5_000)

        assertEquals(ForwardingState.QUEUED, recovered.state)
        assertTrue(SmsDeliveryReducer.isDispatchDue(recovered, start + 5_000))
    }

    @Test fun `a multipart message reaches sent only after every part is confirmed`() {
        var operation = handedOver(expectedParts = 3)

        operation = SmsDeliveryReducer.onPartResult(operation, 1, 0, SmsPartResult.OK, RESULT_OK, start)
        assertEquals(ForwardingState.HANDED_OVER, operation.state)

        operation = SmsDeliveryReducer.onPartResult(operation, 1, 1, SmsPartResult.OK, RESULT_OK, start)
        assertEquals(ForwardingState.HANDED_OVER, operation.state)

        operation = SmsDeliveryReducer.onPartResult(operation, 1, 2, SmsPartResult.OK, RESULT_OK, start)
        assertEquals(ForwardingState.SENT, operation.state)
        assertEquals(3, operation.confirmedParts)
    }

    @Test fun `a repeated callback for the same part cannot complete a message on its own`() {
        // Zaehler statt Teilindizes wuerden hier faelschlich SENT ergeben, obwohl Teil 1 fehlt.
        var operation = handedOver(expectedParts = 2)
        operation = SmsDeliveryReducer.onPartResult(operation, 1, 0, SmsPartResult.OK, RESULT_OK, start)

        val duplicate = SmsDeliveryReducer.onPartResult(operation, 1, 0, SmsPartResult.OK, RESULT_OK, start + 1)

        assertEquals(operation, duplicate)
        assertEquals(ForwardingState.HANDED_OVER, duplicate.state)
        assertEquals(1, duplicate.confirmedParts)
    }

    @Test fun `a contradicting later callback for an already reported part is ignored`() {
        var operation = handedOver(expectedParts = 2)
        operation = SmsDeliveryReducer.onPartResult(operation, 1, 0, SmsPartResult.OK, RESULT_OK, start)

        val contradiction = SmsDeliveryReducer.onPartResult(
            operation, 1, 0, SmsPartResult.TRANSIENT_FAILURE, RESULT_ERROR_NO_SERVICE, start + 1
        )

        assertEquals(operation, contradiction)
    }

    @Test fun `a part index outside the expected range is ignored`() {
        val operation = handedOver(expectedParts = 2)

        assertEquals(operation, SmsDeliveryReducer.onPartResult(operation, 1, 2, SmsPartResult.OK, RESULT_OK, start))
        assertEquals(operation, SmsDeliveryReducer.onPartResult(operation, 1, -1, SmsPartResult.OK, RESULT_OK, start))
    }

    @Test fun `a transient failure schedules a retry with the documented backoff`() {
        val operation = SmsDeliveryReducer.onPartResult(
            handedOver(), 1, 0, SmsPartResult.TRANSIENT_FAILURE, RESULT_ERROR_NO_SERVICE, start
        )

        assertEquals(ForwardingState.RETRY, operation.state)
        assertEquals(start + 30_000, operation.nextAttemptAtMillis)
        assertFalse(SmsDeliveryReducer.isDispatchDue(operation, start))
        assertTrue(SmsDeliveryReducer.isDispatchDue(operation, start + 30_000))
    }

    @Test fun `a partly successful multipart message is resent as a whole`() {
        var operation = handedOver(expectedParts = 2)
        operation = SmsDeliveryReducer.onPartResult(operation, 1, 0, SmsPartResult.OK, RESULT_OK, start)
        operation = SmsDeliveryReducer.onPartResult(
            operation, 1, 1, SmsPartResult.TRANSIENT_FAILURE, RESULT_ERROR_NO_SERVICE, start
        )

        // Bewusst in Kauf genommen: Teil 1 kann beim Empfaenger doppelt ankommen. Ein Fragment,
        // das nie zusammengesetzt wird, waere der groessere Schaden.
        assertEquals(ForwardingState.RETRY, operation.state)
        assertEquals(1, operation.confirmedParts)
    }

    @Test fun `generic failure is carried as a retry reason despite its residual duplicate risk`() {
        val operation = SmsDeliveryReducer.onPartResult(
            handedOver(), 1, 0, SmsPartResult.TRANSIENT_FAILURE, RESULT_ERROR_GENERIC_FAILURE, start
        )

        assertEquals(ForwardingState.RETRY, operation.state)
    }

    @Test fun `a terminal result code fails immediately without a retry`() {
        val operation = SmsDeliveryReducer.onPartResult(
            handedOver(), 1, 0, SmsPartResult.TERMINAL_FAILURE, RESULT_ERROR_NULL_PDU, start
        )

        assertEquals(ForwardingState.FAILED, operation.state)
        assertNull(operation.nextAttemptAtMillis)
    }

    @Test fun `retries are exhausted after the initial attempt plus three repetitions`() {
        val lastAttempt = handedOverAtAttempt(SmsDeliveryReducer.MAX_ATTEMPTS)

        val operation = SmsDeliveryReducer.onPartResult(
            lastAttempt, SmsDeliveryReducer.MAX_ATTEMPTS, 0, SmsPartResult.TRANSIENT_FAILURE,
            RESULT_ERROR_RADIO_OFF, start
        )

        assertEquals(ForwardingState.FAILED, operation.state)
        assertTrue(operation.reason!!.contains("retries_exhausted"))
    }

    @Test fun `a callback of an earlier attempt is ignored`() {
        val secondAttempt = handedOverAtAttempt(2)

        val unchanged = SmsDeliveryReducer.onPartResult(secondAttempt, 1, 0, SmsPartResult.OK, RESULT_OK, start)

        assertEquals(secondAttempt, unchanged)
    }

    @Test fun `a missing callback becomes unknown only after the fifteen minute window`() {
        val operation = handedOver()

        val early = SmsDeliveryReducer.onExpiryScan(operation, start + SmsDeliveryReducer.HANDOVER_TIMEOUT_MILLIS - 1)
        assertEquals(ForwardingState.HANDED_OVER, early.state)

        val late = SmsDeliveryReducer.onExpiryScan(operation, start + SmsDeliveryReducer.HANDOVER_TIMEOUT_MILLIS)
        assertEquals(ForwardingState.UNKNOWN, late.state)
        assertFalse(SmsDeliveryReducer.isDispatchDue(late, start + SmsDeliveryReducer.HANDOVER_TIMEOUT_MILLIS))
    }

    @Test fun `handover does not overwrite a state a fast callback already advanced`() {
        val operation = SmsDeliveryReducer.onPartResult(attempting(), 1, 0, SmsPartResult.OK, RESULT_OK, start)
        assertEquals(ForwardingState.SENT, operation.state)

        assertEquals(ForwardingState.SENT, SmsDeliveryReducer.onHandedOver(operation, start + 1).state)
    }

    private companion object {
        // Werte der Plattform, hier als Zahl, weil der Reducer bewusst nichts von Android weiss.
        const val RESULT_OK = -1
        const val RESULT_ERROR_GENERIC_FAILURE = 1
        const val RESULT_ERROR_RADIO_OFF = 2
        const val RESULT_ERROR_NULL_PDU = 3
        const val RESULT_ERROR_NO_SERVICE = 4
    }
}
