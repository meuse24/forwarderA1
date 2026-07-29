package info.meuse24.smsforwarderneoA1.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailDeliveryReducerTest {

    private val now = 1_700_000_000_000L
    private val transient = EmailFailure(EmailFailureKind.TRANSIENT, "connection refused")
    private val permanent = EmailFailure(EmailFailureKind.RECIPIENT, "no such user", 550)

    private fun job(vararg recipients: String) = EmailDeliveryReducer.queue(
        id = "job-1",
        now = now,
        sender = "+43664123456",
        receivedAtMillis = now - 5_000,
        body = "Hallo",
        recipients = recipients.toList()
    )

    @Test fun `a queued job is due immediately`() {
        val queued = job("a@example.org")

        assertEquals(EmailDeliveryState.QUEUED, queued.state)
        assertEquals(0, queued.attempt)
        assertTrue(EmailDeliveryReducer.isDispatchDue(queued, now))
    }

    @Test fun `duplicate recipients are collapsed`() {
        assertEquals(1, job("a@example.org", "a@example.org").recipients.size)
    }

    /** Der Zaehler wird vor dem Verbindungsaufbau geschrieben - ein Seiteneffekt ist damit belegt. */
    @Test fun `taking ownership counts the attempt`() {
        val attempting = EmailDeliveryReducer.onAttemptStart(job("a@example.org"), now)

        assertEquals(EmailDeliveryState.ATTEMPTING, attempting.state)
        assertEquals(1, attempting.attempt)
    }

    /**
     * Ohne diese Bedingung koennten der Anstoss nach dem Einreihen und ein gleichzeitiger Scan
     * denselben Auftrag zweimal versenden.
     */
    @Test fun `a second run cannot take an owned job`() {
        val attempting = EmailDeliveryReducer.onAttemptStart(job("a@example.org"), now)
        val second = EmailDeliveryReducer.onAttemptStart(attempting, now)

        assertSame(attempting, second)
    }

    @Test fun `all recipients delivered ends as sent`() {
        var current = EmailDeliveryReducer.onAttemptStart(job("a@example.org", "b@example.org"), now)
        current = EmailDeliveryReducer.onRecipientDelivered(current, now, "a@example.org")
        current = EmailDeliveryReducer.onRecipientDelivered(current, now, "b@example.org")
        current = EmailDeliveryReducer.onAttemptFinished(current, now)

        assertEquals(EmailDeliveryState.SENT, current.state)
        assertNull(current.nextAttemptAtMillis)
    }

    @Test fun `one delivered and one permanently rejected ends as partial`() {
        var current = EmailDeliveryReducer.onAttemptStart(job("a@example.org", "b@example.org"), now)
        current = EmailDeliveryReducer.onRecipientDelivered(current, now, "a@example.org")
        current = EmailDeliveryReducer.onRecipientFailed(current, now, "b@example.org", permanent)
        current = EmailDeliveryReducer.onAttemptFinished(current, now)

        assertEquals(EmailDeliveryState.PARTIAL, current.state)
        assertEquals(1, current.deliveredCount)
    }

    /** Kern der Teilzustellung: Ein Neuversuch darf den bereits belieferten nicht erneut anschreiben. */
    @Test fun `a retry only addresses recipients that are still open`() {
        var current = EmailDeliveryReducer.onAttemptStart(job("a@example.org", "b@example.org"), now)
        current = EmailDeliveryReducer.onRecipientDelivered(current, now, "a@example.org")
        current = EmailDeliveryReducer.onRecipientFailed(current, now, "b@example.org", transient)
        current = EmailDeliveryReducer.onAttemptFinished(current, now)

        assertEquals(EmailDeliveryState.RETRY, current.state)
        assertEquals(listOf("b@example.org"), current.pendingRecipients)
    }

    @Test fun `a permanently rejected job is not retried`() {
        var current = EmailDeliveryReducer.onAttemptStart(job("a@example.org"), now)
        current = EmailDeliveryReducer.onRecipientFailed(current, now, "a@example.org", permanent)
        current = EmailDeliveryReducer.onAttemptFinished(current, now)

        assertEquals(EmailDeliveryState.FAILED, current.state)
        assertNull(current.nextAttemptAtMillis)
    }

    @Test fun `the retry follows the backoff schedule`() {
        var current = EmailDeliveryReducer.onAttemptStart(job("a@example.org"), now)
        current = EmailDeliveryReducer.onRecipientFailed(current, now, "a@example.org", transient)
        current = EmailDeliveryReducer.onAttemptFinished(current, now)

        assertEquals(now + EmailRetryPolicy.delayMillis(1), current.nextAttemptAtMillis)
        assertTrue(EmailDeliveryReducer.isDispatchDue(current, now + EmailRetryPolicy.delayMillis(1)))
        assertTrue(!EmailDeliveryReducer.isDispatchDue(current, now + 1))
    }

    /** Ein Anmeldefehler betrifft nicht eine Adresse, sondern den ganzen Auftrag. */
    @Test fun `a connection failure hits every open recipient`() {
        var current = EmailDeliveryReducer.onAttemptStart(job("a@example.org", "b@example.org"), now)
        current = EmailDeliveryReducer.onConnectionFailed(
            current,
            now,
            EmailFailure(EmailFailureKind.AUTHENTICATION, "535")
        )
        current = EmailDeliveryReducer.onAttemptFinished(current, now)

        assertEquals(EmailDeliveryState.FAILED, current.state)
        assertEquals(0, current.deliveredCount)
    }

    @Test fun `a connection failure spares recipients already delivered`() {
        var current = EmailDeliveryReducer.onAttemptStart(job("a@example.org", "b@example.org"), now)
        current = EmailDeliveryReducer.onRecipientDelivered(current, now, "a@example.org")
        current = EmailDeliveryReducer.onConnectionFailed(current, now, transient)

        assertNull(current.recipients.first { it.address == "a@example.org" }.failure)
        assertEquals(transient, current.recipients.first { it.address == "b@example.org" }.failure)
    }

    /**
     * Die bewusste Umkehrung der SMS-Regel: Eine E-Mail kostet nichts, der Verlust einer
     * Weiterleitung ist der Schadensfall.
     */
    @Test fun `a job found attempting after a process restart is sent again`() {
        val attempting = EmailDeliveryReducer.onAttemptStart(job("a@example.org"), now)
        val restarted = EmailDeliveryReducer.onProcessRestart(attempting, now + 1000)

        assertEquals(EmailDeliveryState.QUEUED, restarted.state)
        assertTrue(EmailDeliveryReducer.isDispatchDue(restarted, now + 1000))
        // Der Zaehler bleibt stehen: Der Versuch hat stattgefunden.
        assertEquals(1, restarted.attempt)
    }

    @Test fun `a delivered recipient is not sent again after a restart`() {
        var current = EmailDeliveryReducer.onAttemptStart(job("a@example.org", "b@example.org"), now)
        current = EmailDeliveryReducer.onRecipientDelivered(current, now, "a@example.org")
        current = EmailDeliveryReducer.onProcessRestart(current, now + 1000)

        assertEquals(listOf("b@example.org"), current.pendingRecipients)
    }

    /**
     * Ohne diesen Uebergang bliebe ein Auftrag, dessen Empfaengerergebnis nicht schreibbar war,
     * bis zum naechsten Prozessstart liegen: unsichtbar, weil nicht terminal, und unbearbeitet,
     * weil nicht sendefaellig.
     */
    @Test fun `a stuck attempt is taken back within the running process`() {
        val attempting = EmailDeliveryReducer.onAttemptStart(job("a@example.org"), now)

        val recovered = EmailDeliveryReducer.onStaleAttempt(
            attempting,
            now + EmailDeliveryReducer.STALE_ATTEMPT_MILLIS
        )

        assertEquals(EmailDeliveryState.QUEUED, recovered.state)
        assertTrue(EmailDeliveryReducer.isDispatchDue(recovered, now))
    }

    /** Ein langsamer, aber laufender Versand darf nicht abgeraeumt werden. */
    @Test fun `a running attempt is left alone`() {
        val attempting = EmailDeliveryReducer.onAttemptStart(job("a@example.org"), now)

        assertSame(
            attempting,
            EmailDeliveryReducer.onStaleAttempt(
                attempting,
                now + EmailDeliveryReducer.STALE_ATTEMPT_MILLIS - 1
            )
        )
    }

    @Test fun `a job that is not attempting is never taken back as stale`() {
        val queued = job("a@example.org")

        assertSame(
            queued,
            EmailDeliveryReducer.onStaleAttempt(queued, now + EmailDeliveryReducer.STALE_ATTEMPT_MILLIS)
        )
    }

    @Test fun `a stuck attempt keeps the recipients already delivered`() {
        var current = EmailDeliveryReducer.onAttemptStart(job("a@example.org", "b@example.org"), now)
        current = EmailDeliveryReducer.onRecipientDelivered(current, now, "a@example.org")
        current = EmailDeliveryReducer.onStaleAttempt(
            current,
            now + EmailDeliveryReducer.STALE_ATTEMPT_MILLIS
        )

        assertEquals(listOf("b@example.org"), current.pendingRecipients)
    }

    @Test fun `a terminal job is untouched by a restart`() {
        var current = EmailDeliveryReducer.onAttemptStart(job("a@example.org"), now)
        current = EmailDeliveryReducer.onRecipientDelivered(current, now, "a@example.org")
        current = EmailDeliveryReducer.onAttemptFinished(current, now)

        assertSame(current, EmailDeliveryReducer.onProcessRestart(current, now + 1000))
    }

    @Test fun `the deadline ends the job`() {
        var current = EmailDeliveryReducer.onAttemptStart(job("a@example.org"), now)
        current = EmailDeliveryReducer.onRecipientFailed(current, now, "a@example.org", transient)
        current = EmailDeliveryReducer.onAttemptFinished(current, now)

        val expired = EmailDeliveryReducer.onExpiryScan(
            current,
            now + EmailRetryPolicy.MAX_LIFETIME_MILLIS
        )

        assertEquals(EmailDeliveryState.FAILED, expired.state)
        assertNull(expired.nextAttemptAtMillis)
    }

    @Test fun `a job that reaches its deadline mid attempt is not retried`() {
        var current = EmailDeliveryReducer.onAttemptStart(job("a@example.org"), now)
        val late = now + EmailRetryPolicy.MAX_LIFETIME_MILLIS
        current = EmailDeliveryReducer.onRecipientFailed(current, late, "a@example.org", transient)
        current = EmailDeliveryReducer.onAttemptFinished(current, late)

        assertEquals(EmailDeliveryState.FAILED, current.state)
    }

    @Test fun `an expiry scan spares a job within its deadline`() {
        val queued = job("a@example.org")

        assertSame(queued, EmailDeliveryReducer.onExpiryScan(queued, now + 1000))
    }

    @Test fun `finishing an attempt that was never started changes nothing`() {
        val queued = job("a@example.org")

        assertSame(queued, EmailDeliveryReducer.onAttemptFinished(queued, now))
    }

    @Test fun `a result for an unknown recipient changes nothing`() {
        val attempting = EmailDeliveryReducer.onAttemptStart(job("a@example.org"), now)

        assertSame(
            attempting,
            EmailDeliveryReducer.onRecipientDelivered(attempting, now, "unknown@example.org")
        )
    }
}
