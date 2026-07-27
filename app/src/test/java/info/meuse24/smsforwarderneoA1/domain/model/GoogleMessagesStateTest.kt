package info.meuse24.smsforwarderneoA1.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GoogleMessagesStateTest {

    @Test fun `google messages as default sms app is detected`() {
        val state = resolveGoogleMessagesState(
            isInstalled = true,
            defaultSmsPackage = GOOGLE_MESSAGES_PACKAGE
        )

        assertEquals(GoogleMessagesState.DEFAULT_SMS_APP, state)
    }

    @Test fun `installed but another default app keeps the registration warning`() {
        val state = resolveGoogleMessagesState(
            isInstalled = true,
            defaultSmsPackage = "com.example.other.messenger"
        )

        assertEquals(GoogleMessagesState.INSTALLED_NOT_DEFAULT, state)
    }

    @Test fun `not installed produces no hint`() {
        val state = resolveGoogleMessagesState(
            isInstalled = false,
            defaultSmsPackage = "com.example.other.messenger"
        )

        assertEquals(GoogleMessagesState.NOT_INSTALLED, state)
    }

    @Test fun `missing default sms package is treated as another app`() {
        // Telephony.Sms.getDefaultSmsPackage() darf null liefern; das darf nie zu einem
        // faelschlich behaupteten RCS-Risiko fuehren.
        assertEquals(
            GoogleMessagesState.INSTALLED_NOT_DEFAULT,
            resolveGoogleMessagesState(isInstalled = true, defaultSmsPackage = null)
        )
        assertEquals(
            GoogleMessagesState.NOT_INSTALLED,
            resolveGoogleMessagesState(isInstalled = false, defaultSmsPackage = null)
        )
    }

    @Test fun `default sms app wins even if the package check failed`() {
        // Schutz gegen einen fehlgeschlagenen PackageManager-Aufruf: Ist Google Messages
        // die Standard-App, ist es zwangslaeufig installiert.
        val state = resolveGoogleMessagesState(
            isInstalled = false,
            defaultSmsPackage = GOOGLE_MESSAGES_PACKAGE
        )

        assertEquals(GoogleMessagesState.DEFAULT_SMS_APP, state)
    }
}
