package info.meuse24.smsforwarderneoA1.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailRetryPolicyTest {

    @Test fun `only transient causes are retried`() {
        assertTrue(EmailRetryPolicy.isRetryable(EmailFailureKind.TRANSIENT))

        // Alle uebrigen sind dauerhaft. Eine Wiederholung nach abgelehnter Anmeldung riskiert
        // zusaetzlich eine Kontosperre.
        assertFalse(EmailRetryPolicy.isRetryable(EmailFailureKind.AUTHENTICATION))
        assertFalse(EmailRetryPolicy.isRetryable(EmailFailureKind.RECIPIENT))
        assertFalse(EmailRetryPolicy.isRetryable(EmailFailureKind.PERMANENT))
        assertFalse(EmailRetryPolicy.isRetryable(EmailFailureKind.CONFIGURATION))
        assertFalse(EmailRetryPolicy.isRetryable(EmailFailureKind.TRANSPORT_SECURITY))
    }

    @Test fun `the backoff grows and then stays capped`() {
        assertEquals(60_000L, EmailRetryPolicy.delayMillis(1))
        assertEquals(5 * 60_000L, EmailRetryPolicy.delayMillis(2))
        assertEquals(15 * 60_000L, EmailRetryPolicy.delayMillis(3))
        assertEquals(60 * 60_000L, EmailRetryPolicy.delayMillis(4))
        assertEquals(3 * 60 * 60_000L, EmailRetryPolicy.delayMillis(5))
        assertEquals(3 * 60 * 60_000L, EmailRetryPolicy.delayMillis(42))
    }

    @Test fun `an out of range attempt does not throw`() {
        assertEquals(60_000L, EmailRetryPolicy.delayMillis(0))
        assertEquals(60_000L, EmailRetryPolicy.delayMillis(-1))
    }

    @Test fun `the deadline is one day after queueing`() {
        val created = 1_000_000L
        assertFalse(EmailRetryPolicy.isExpired(created, created + 23 * 60 * 60 * 1000))
        assertTrue(EmailRetryPolicy.isExpired(created, created + 24 * 60 * 60 * 1000))
    }
}
