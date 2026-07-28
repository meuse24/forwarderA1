package info.meuse24.smsforwarderneoA1.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class A1DetectorTest {
    @Test fun detectsPrimaryA1SimNetwork() {
        assertEquals(A1Detection.A1_CONFIRMED, A1Detector.detect(14, "23201", null, null))
    }

    @Test fun foreignA1NameIsNotEnough() {
        assertEquals(A1Detection.UNKNOWN, A1Detector.detect(null, "21910", "A1 Croatia", null))
        assertEquals(A1Detection.UNKNOWN, A1Detector.detect(null, null, null, null))
    }

    @Test fun otherKnownNameIsNotA1() {
        assertEquals(A1Detection.NOT_A1, A1Detector.detect(null, "23203", "Drei", null))
    }
}
