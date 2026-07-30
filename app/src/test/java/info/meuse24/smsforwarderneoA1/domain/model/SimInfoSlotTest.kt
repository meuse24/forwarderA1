package info.meuse24.smsforwarderneoA1.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SimInfoSlotTest {
    private fun sim(subscriptionId: Int, slotIndex: Int) =
        SimInfo(subscriptionId, slotIndex, displayName = null, carrierName = null)

    @Test fun `slot lookup ignores list order`() {
        val sims = listOf(sim(subscriptionId = 7, slotIndex = 1), sim(subscriptionId = 3, slotIndex = 0))

        assertEquals(3, sims.inSlot(0)?.subscriptionId)
        assertEquals(7, sims.inSlot(1)?.subscriptionId)
    }

    /**
     * Der Grund für diesen Helfer: Steckt nur die zweite Karte, ist deren Listenindex 0.
     * `getOrNull(0)` hätte sie als "SIM 1" geliefert und "Immer SIM 1" hätte über SIM 2 gesendet.
     */
    @Test fun `an empty slot has no sim even when another card is the first entry`() {
        val onlySecondSlot = listOf(sim(subscriptionId = 7, slotIndex = 1))

        assertNull(onlySecondSlot.inSlot(0))
        assertEquals(7, onlySecondSlot.inSlot(1)?.subscriptionId)
    }

    @Test fun `no sims means no slot is occupied`() {
        assertNull(emptyList<SimInfo>().inSlot(0))
        assertNull(emptyList<SimInfo>().inSlot(1))
    }
}
