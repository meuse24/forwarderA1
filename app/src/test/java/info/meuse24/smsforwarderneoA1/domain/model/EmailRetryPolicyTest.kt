package info.meuse24.smsforwarderneoA1.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class EmailRetryPolicyTest {

    /** Steht stellvertretend fuer javax.mail.MessagingException. */
    private class MessagingException(message: String) : Exception(message)

    @Test fun `transport errors are retried`() {
        assertTrue(EmailRetryPolicy.isRetryable(IOException("connection refused")))
        assertTrue(EmailRetryPolicy.isRetryable(MessagingException("auth failed")))
    }

    @Test fun `a programming error is not retried`() {
        assertFalse(EmailRetryPolicy.isRetryable(IllegalArgumentException("bad address")))
    }

    @Test fun `three repetitions with linear backoff`() {
        assertEquals(5_000L, EmailRetryPolicy.delayMillis(1))
        assertEquals(10_000L, EmailRetryPolicy.delayMillis(2))
        assertEquals(15_000L, EmailRetryPolicy.delayMillis(3))
    }

    @Test fun `after the third repetition the email channel gives up`() {
        val error = IOException("still down")

        assertTrue(EmailRetryPolicy.shouldRetry(error, attempt = 3))
        assertFalse(EmailRetryPolicy.shouldRetry(error, attempt = 4))
    }
}
