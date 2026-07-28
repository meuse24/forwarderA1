package info.meuse24.smsforwarderneoA1.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsForwardingComposerTest {

    private fun part(
        body: String,
        sender: String = "+436601234567",
        intentId: String = "intent-1",
        position: Int = 0,
        timestamp: Long = 1_000L
    ) = SmsMessagePart(
        body = body,
        timestamp = timestamp,
        referenceNumber = intentId.hashCode(),
        sequencePosition = position,
        totalParts = 1,
        sender = sender,
        subscriptionId = 1,
        intentId = intentId
    )

    @Test fun `parts of one intent form a single group in sequence order`() {
        val parts = listOf(
            part("zweiter", position = 1),
            part("erster", position = 0)
        )

        val groups = SmsForwardingComposer.group(parts)

        assertEquals(1, groups.size)
        assertEquals("ersterzweiter", groups.first().text)
    }

    @Test fun `messages from different intents are not merged`() {
        val groups = SmsForwardingComposer.group(
            listOf(part("A", intentId = "intent-1"), part("B", intentId = "intent-2"))
        )

        assertEquals(2, groups.size)
    }

    @Test fun `a sender containing an underscore survives grouping in full`() {
        // GSM-7 laesst Unterstriche zu; frueher wurde der Absender aus dem Gruppenschluessel
        // zurueckgeparst und dabei bei "MY_BANK" nach "MY" abgeschnitten.
        val groups = SmsForwardingComposer.group(listOf(part("Kontostand", sender = "MY_BANK")))

        assertEquals("MY_BANK", groups.single().sender)
    }

    @Test fun `the header names sender time and sim slot`() {
        val header = SmsForwardingComposer.header("MY_BANK", "28.07. 14:05", slotIndex = 1)

        // slotIndex 1 ist SIM 2 - nicht die Subscription-ID.
        assertEquals("MY_BANK 28.07. 14:05 SIM2", header)
    }

    @Test fun `an unknown sim slot is simply omitted`() {
        assertEquals("MY_BANK 28.07. 14:05", SmsForwardingComposer.header("MY_BANK", "28.07. 14:05", null))
    }

    @Test fun `the header stays well below the length of the previous multiline form`() {
        val header = SmsForwardingComposer.header("+436601234567", "28.07. 14:05", 0)

        val previousForm = "Von: +436601234567\nZeit: 28.07.2026 14:05:33\nSIM: Slot 1\nNachricht:\n"
        assertTrue(header.length + 1 < previousForm.length - 30)
    }

    @Test fun `a short message is forwarded unchanged`() {
        val composed = SmsForwardingComposer.compose("MY_BANK", "28.07. 14:05", 0, "Kontostand")

        assertEquals("MY_BANK 28.07. 14:05 SIM1\nKontostand", composed)
    }

    @Test fun `an overlong message is truncated and marked instead of being dropped`() {
        val body = "x".repeat(2_000)

        val composed = SmsForwardingComposer.compose("MY_BANK", "28.07. 14:05", 0, body)

        assertTrue(composed.length <= SmsForwardingComposer.MAX_SMS_LENGTH)
        assertTrue(composed.contains("gekürzt"))
        assertTrue(composed.startsWith("MY_BANK 28.07. 14:05 SIM1\n"))
    }

    @Test fun `truncation still fits when the missing count needs many digits`() {
        val body = "y".repeat(100_000)

        val composed = SmsForwardingComposer.compose("A", "28.07. 14:05", null, body)

        assertTrue(composed.length <= SmsForwardingComposer.MAX_SMS_LENGTH)
    }
}
