package info.meuse24.smsforwarderneoA1.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailQueueRetentionPolicyTest {

    private val now = 1_700_000_000_000L

    private fun job(
        id: String,
        state: EmailDeliveryState,
        createdAt: Long = now,
        updatedAt: Long = now
    ) = EmailForwardingJob(
        id = id,
        createdAtMillis = createdAt,
        updatedAtMillis = updatedAt,
        sender = "+43664123456",
        receivedAtMillis = createdAt,
        body = "Hallo",
        recipients = listOf(EmailRecipientState("a@example.org")),
        state = state
    )

    @Test fun `an old terminal entry falls out`() {
        val entries = listOf(
            job("old", EmailDeliveryState.SENT, updatedAt = now - EmailQueueRetentionPolicy.MAX_AGE_MILLIS - 1),
            job("fresh", EmailDeliveryState.SENT)
        )

        assertEquals(listOf("fresh"), EmailQueueRetentionPolicy.retain(entries, now).map { it.id })
    }

    /** Ein laufender Auftrag wuerde durch Verdraengung kommentarlos verschwinden. */
    @Test fun `a running entry survives its age`() {
        val entries = listOf(
            job("running", EmailDeliveryState.RETRY, updatedAt = now - EmailQueueRetentionPolicy.MAX_AGE_MILLIS - 1)
        )

        assertEquals(listOf("running"), EmailQueueRetentionPolicy.retain(entries, now).map { it.id })
    }

    @Test fun `beyond the limit the oldest terminal entries give way`() {
        val entries = (1..EmailQueueRetentionPolicy.MAX_ENTRIES + 2).map { index ->
            job("job-$index", EmailDeliveryState.SENT, createdAt = now + index)
        }

        val retained = EmailQueueRetentionPolicy.retain(entries, now)

        assertEquals(EmailQueueRetentionPolicy.MAX_ENTRIES, retained.size)
        assertEquals("job-3", retained.first().id)
    }

    /**
     * Reicht die Zahl terminaler Eintraege nicht aus, bleibt die Queue lieber voruebergehend
     * groesser als die Obergrenze - der Zulauf wird stattdessen an der Quelle begrenzt.
     */
    @Test fun `running entries are never displaced to meet the limit`() {
        val entries = (1..EmailQueueRetentionPolicy.MAX_ENTRIES + 5).map { index ->
            job("job-$index", EmailDeliveryState.RETRY, createdAt = now + index)
        }

        assertEquals(entries.size, EmailQueueRetentionPolicy.retain(entries, now).size)
    }

    @Test fun `the result is ordered oldest first`() {
        val entries = listOf(
            job("second", EmailDeliveryState.SENT, createdAt = now + 10),
            job("first", EmailDeliveryState.SENT, createdAt = now)
        )

        val retained = EmailQueueRetentionPolicy.retain(entries, now)

        assertTrue(retained.map { it.createdAtMillis } == retained.map { it.createdAtMillis }.sorted())
        assertEquals("first", retained.first().id)
    }
}
