package info.meuse24.smsforwarderneoA1.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForwardingQueueRetentionPolicyTest {

    private val now = 10_000_000_000L

    private fun operation(
        id: String,
        state: ForwardingState,
        createdAt: Long = now,
        updatedAt: Long = createdAt
    ) = ForwardingOperation(
        id = id,
        createdAtMillis = createdAt,
        updatedAtMillis = updatedAt,
        sender = "MY_BANK",
        targetNumber = "+436609876543",
        text = "Text",
        subscriptionId = 1,
        expectedParts = 1,
        state = state
    )

    @Test fun `terminal entries older than seven days are dropped`() {
        val old = operation("old", ForwardingState.SENT, updatedAt = now - ForwardingQueueRetentionPolicy.MAX_AGE_MILLIS - 1)
        val fresh = operation("fresh", ForwardingState.FAILED)

        val retained = ForwardingQueueRetentionPolicy.retain(listOf(old, fresh), now)

        assertEquals(listOf("fresh"), retained.map { it.id })
    }

    @Test fun `a running operation is never dropped by age alone`() {
        val stale = operation("stale", ForwardingState.RETRY, updatedAt = now - ForwardingQueueRetentionPolicy.MAX_AGE_MILLIS - 1)

        val retained = ForwardingQueueRetentionPolicy.retain(listOf(stale), now)

        assertEquals(listOf("stale"), retained.map { it.id })
    }

    @Test fun `beyond fifty entries the oldest terminal ones give way first`() {
        val terminal = (1..10).map { operation("terminal-$it", ForwardingState.SENT, createdAt = now - 10_000 + it) }
        val running = (1..45).map { operation("running-$it", ForwardingState.HANDED_OVER, createdAt = now + it) }

        val retained = ForwardingQueueRetentionPolicy.retain(terminal + running, now)

        assertEquals(ForwardingQueueRetentionPolicy.MAX_ENTRIES, retained.size)
        assertTrue(running.all { candidate -> retained.any { it.id == candidate.id } })
        assertTrue(retained.none { it.id == "terminal-1" })
    }

    @Test fun `running entries are never displaced even beyond the entry limit`() {
        // Ein laufender Vorgang wuerde durch Verdraengung kommentarlos verschwinden - genau das,
        // was die Queue verhindern soll. Die Obergrenze weicht deshalb zurueck.
        val running = (1..55).map { operation("running-$it", ForwardingState.QUEUED, createdAt = now + it) }

        val retained = ForwardingQueueRetentionPolicy.retain(running, now)

        assertEquals(55, retained.size)
        assertTrue(retained.any { it.id == "running-1" })
    }

    @Test fun `a single terminal entry among full active entries is displaced`() {
        // Deshalb darf ein Verlust wegen voller Warteschlange nicht als Queue-Eintrag vermerkt
        // werden: Er waere genau der Eintrag, den diese Regel als naechstes verdraengt.
        val active = (1..ForwardingQueueRetentionPolicy.MAX_ENTRIES)
            .map { operation("active-$it", ForwardingState.HANDED_OVER, createdAt = now - 1_000 + it) }
        val note = operation("note", ForwardingState.FAILED, createdAt = now)

        val retained = ForwardingQueueRetentionPolicy.retain(active + note, now)

        assertTrue(retained.none { it.id == "note" })
    }

    @Test fun `terminal entries give way as far as they reach`() {
        val terminal = (1..3).map { operation("terminal-$it", ForwardingState.SENT, createdAt = now - 100 + it) }
        val running = (1..55).map { operation("running-$it", ForwardingState.RETRY, createdAt = now + it) }

        val retained = ForwardingQueueRetentionPolicy.retain(terminal + running, now)

        // Alle drei terminalen weichen, die 55 laufenden bleiben vollstaendig.
        assertEquals(55, retained.size)
        assertTrue(retained.none { it.id.startsWith("terminal-") })
    }
}
