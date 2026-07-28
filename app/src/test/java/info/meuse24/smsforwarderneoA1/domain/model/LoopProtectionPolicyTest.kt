package info.meuse24.smsforwarderneoA1.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LoopProtectionPolicyTest {

    /** Steht fuer den Geraetevergleich; hier genuegt exakte Gleichheit. */
    private val sameNumber: (String, String) -> Boolean = { a, b -> a == b }

    @Test fun `an ordinary message is forwarded`() {
        val decision = LoopProtectionPolicy.decide(
            sender = "MY_BANK",
            targetNumber = "+436609876543",
            ownNumbers = listOf("+436601234567"),
            sameNumber = sameNumber
        )

        assertEquals(LoopProtectionPolicy.Decision.FORWARD, decision)
    }

    @Test fun `a message from the target number would feed itself`() {
        val decision = LoopProtectionPolicy.decide(
            sender = "+436609876543",
            targetNumber = "+436609876543",
            ownNumbers = emptyList(),
            sameNumber = sameNumber
        )

        assertEquals(LoopProtectionPolicy.Decision.BLOCKED_SENDER_IS_TARGET, decision)
    }

    @Test fun `forwarding to an own sim is blocked`() {
        val decision = LoopProtectionPolicy.decide(
            sender = "MY_BANK",
            targetNumber = "+436601234567",
            ownNumbers = listOf("", "+436601234567"),
            sameNumber = sameNumber
        )

        assertEquals(LoopProtectionPolicy.Decision.BLOCKED_TARGET_IS_OWN_SIM, decision)
    }

    @Test fun `an empty own number never blocks`() {
        val decision = LoopProtectionPolicy.decide(
            sender = "MY_BANK",
            targetNumber = "+436609876543",
            ownNumbers = listOf("", ""),
            sameNumber = { a, b -> a == b }
        )

        assertEquals(LoopProtectionPolicy.Decision.FORWARD, decision)
    }
}
