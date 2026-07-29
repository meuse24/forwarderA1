package info.meuse24.smsforwarderneoA1.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailBodyComposerTest {

    private val received = 1_700_000_000_000L

    @Test fun `a prompt forwarding names only the time of receipt`() {
        assertFalse(EmailBodyComposer.needsForwardedAt(received, received + 5_000))

        val body = EmailBodyComposer.body(
            sender = "+43664123456",
            receivedAt = "29.07.2026 14:03:11",
            forwardedAt = null,
            message = "Hallo"
        )

        assertTrue(body.contains("Empfangen: 29.07.2026 14:03:11"))
        assertFalse(body.contains("Weitergeleitet:"))
    }

    /**
     * Nach einer Verzoegerung behauptete die fruehere Fassung mit dem Versandzeitpunkt einen
     * Empfang, der Stunden zurueckliegen konnte.
     */
    @Test fun `a delayed forwarding names both times`() {
        assertTrue(EmailBodyComposer.needsForwardedAt(received, received + 30 * 60_000))

        val body = EmailBodyComposer.body(
            sender = "+43664123456",
            receivedAt = "29.07.2026 14:03:11",
            forwardedAt = "29.07.2026 14:33:02",
            message = "Hallo"
        )

        assertTrue(body.contains("Empfangen: 29.07.2026 14:03:11"))
        assertTrue(body.contains("Weitergeleitet: 29.07.2026 14:33:02"))
    }

    @Test fun `the threshold is one minute`() {
        assertFalse(
            EmailBodyComposer.needsForwardedAt(
                received,
                received + EmailBodyComposer.FORWARD_DELAY_THRESHOLD_MILLIS - 1
            )
        )
        assertTrue(
            EmailBodyComposer.needsForwardedAt(
                received,
                received + EmailBodyComposer.FORWARD_DELAY_THRESHOLD_MILLIS
            )
        )
    }

    @Test fun `the message text is carried unchanged`() {
        val message = "Zeile 1\nZeile 2 mit Umlauten: äöüß"

        val body = EmailBodyComposer.body("+43664123456", "jetzt", null, message)

        assertTrue(body.contains(message))
    }

    @Test fun `the subject names the sender`() {
        assertTrue(EmailBodyComposer.subject("+43664123456").contains("+43664123456"))
    }
}
