package info.meuse24.smsforwarderneoA1.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SimSelectionLabelsTest {

    private fun suffix(
        mode: SimSelectionMode,
        sim1: Int? = null,
        sim2: Int? = null,
        defaultSms: Int = -1
    ) = SimSelectionLabels.suffixFor(mode, sim1, sim2, defaultSms)

    /**
     * Der gemeldete Fall: eine einzige Karte, und zwar in Slot 2. Die Beschriftung schloss
     * über `simCount >= 2` auf Verfügbarkeit und behauptete "nicht verfügbar", während der
     * Sendeweg diese Karte korrekt wählt.
     */
    @Test fun `only a card in slot 2 makes SIM 2 available`() {
        assertEquals(
            SimSelectionSuffix.NONE,
            suffix(SimSelectionMode.ALWAYS_SIM_2, sim1 = null, sim2 = 7)
        )
    }

    /** Die Gegenrichtung, die vorher gar nicht geprüft wurde. */
    @Test fun `an empty slot 1 makes SIM 1 unavailable`() {
        assertEquals(
            SimSelectionSuffix.NOT_AVAILABLE,
            suffix(SimSelectionMode.ALWAYS_SIM_1, sim1 = null, sim2 = 7)
        )
    }

    @Test fun `an empty slot 2 makes SIM 2 unavailable`() {
        assertEquals(
            SimSelectionSuffix.NOT_AVAILABLE,
            suffix(SimSelectionMode.ALWAYS_SIM_2, sim1 = 3, sim2 = null)
        )
    }

    @Test fun `the default sms sim is marked as such`() {
        assertEquals(
            SimSelectionSuffix.DEFAULT_SMS,
            suffix(SimSelectionMode.ALWAYS_SIM_1, sim1 = 3, sim2 = 7, defaultSms = 3)
        )
        assertEquals(
            SimSelectionSuffix.DEFAULT_SMS,
            suffix(SimSelectionMode.ALWAYS_SIM_2, sim1 = 3, sim2 = 7, defaultSms = 7)
        )
    }

    @Test fun `an occupied slot that is not the default sms sim gets no suffix`() {
        assertEquals(
            SimSelectionSuffix.NONE,
            suffix(SimSelectionMode.ALWAYS_SIM_2, sim1 = 3, sim2 = 7, defaultSms = 3)
        )
    }

    /** Ohne ermittelte Standard-SIM ist -1 kein Treffer, auch wenn beide Werte -1 sind. */
    @Test fun `an undetermined default sms sim never marks a slot`() {
        assertEquals(
            SimSelectionSuffix.NONE,
            suffix(SimSelectionMode.ALWAYS_SIM_1, sim1 = -1, sim2 = 7, defaultSms = -1)
        )
    }

    @Test fun `same as incoming never carries a suffix`() {
        assertEquals(
            SimSelectionSuffix.NONE,
            suffix(SimSelectionMode.SAME_AS_INCOMING, sim1 = null, sim2 = null)
        )
    }
}
