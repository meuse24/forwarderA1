package info.meuse24.smsforwarderneoA1.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MmiExecutionModeResolverTest {
    @Test fun capturedStandardDeactivationRemainsUssdAfterSettingsChange() {
        assertEquals(
            MmiExecutionMode.USSD_CALLBACK,
            MmiExecutionModeResolver.resolve(MmiExecutionMode.USSD_CALLBACK, MmiExecutionMode.VOICE_MMI_CALL),
        )
    }
}
