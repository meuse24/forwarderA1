package info.meuse24.smsforwarderneoA1.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MmiCodeProfilesTest {
    @Test fun standardProfileIsRecognized() {
        assertEquals(MmiCodeProfile.STANDARD_GSM, MmiCodeProfiles.detect(MmiCodeProfiles.standardGsm))
        assertEquals("**21*", MmiCodeProfiles.standardGsm.activatePrefix)
    }

    @Test fun specialProfileIsRecognized() {
        assertEquals(MmiCodeProfile.A1_SPECIAL, MmiCodeProfiles.detect(MmiCodeProfiles.a1Special))
    }

    @Test fun changedCodeIsCustom() {
        assertEquals(MmiCodeProfile.CUSTOM, MmiCodeProfiles.detect(MmiCodeProfiles.standardGsm.copy(statusCode = "*#61#")))
    }

    @Test fun blankSuffixIsInvalid() {
        assertFalse(MmiCodeProfiles.standardGsm.copy(activateSuffix = "").isValid())
        assertTrue(MmiCodeProfiles.standardGsm.isValid())
    }
}
