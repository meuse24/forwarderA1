package info.meuse24.smsforwarderneoA1.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ForwardingResolutionReducerTest {
    @Test fun `successful activation enables forwarding and saves selected contact`() {
        val result = ForwardingResolutionReducer.resolve("ACTIVATE", success = true)

        assertEquals(true, result.forwardingActive)
        assertEquals(ForwardingResolutionReducer.ContactHandling.SAVE_PENDING_CONTACT, result.contactHandling)
    }

    @Test fun `successful deactivation disables forwarding and clears selected contact`() {
        val result = ForwardingResolutionReducer.resolve("DEACTIVATE", success = true)

        assertEquals(false, result.forwardingActive)
        assertEquals(ForwardingResolutionReducer.ContactHandling.CLEAR, result.contactHandling)
    }

    @Test fun `failed activation and deactivation preserve the existing local forwarding state`() {
        val activation = ForwardingResolutionReducer.resolve("ACTIVATE", success = false)
        val deactivation = ForwardingResolutionReducer.resolve("DEACTIVATE", success = false)

        assertNull(activation.forwardingActive)
        assertNull(deactivation.forwardingActive)
        assertEquals(ForwardingResolutionReducer.ContactHandling.KEEP, activation.contactHandling)
        assertEquals(ForwardingResolutionReducer.ContactHandling.KEEP, deactivation.contactHandling)
    }
}
