package info.meuse24.smsforwarderneoA1.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmsCallbackUriTest {

    @Test fun `an identity survives the round trip`() {
        val uri = SmsCallbackUri.build("op-42", attempt = 2, partIndex = 3)

        assertEquals(SmsCallbackUri.Reference("op-42", 2, 3), SmsCallbackUri.parse(uri))
    }

    @Test fun `each part and each attempt gets its own identity`() {
        val first = SmsCallbackUri.build("op-42", 1, 0)
        val samePartLaterAttempt = SmsCallbackUri.build("op-42", 2, 0)
        val otherPart = SmsCallbackUri.build("op-42", 1, 1)

        assertNotEquals(first, samePartLaterAttempt)
        assertNotEquals(first, otherPart)
        assertNotEquals(SmsCallbackUri.requestCode(first), SmsCallbackUri.requestCode(otherPart))
    }

    @Test fun `the request code is derived deterministically and not from a process counter`() {
        val uri = SmsCallbackUri.build("op-42", 1, 0)

        assertEquals(SmsCallbackUri.requestCode(uri), SmsCallbackUri.requestCode(uri))
    }

    @Test fun `foreign or missing uris are rejected instead of guessed`() {
        assertNull(SmsCallbackUri.parse(null))
        assertNull(SmsCallbackUri.parse("smsfwd://op/op-42/part/3"))
        assertNull(SmsCallbackUri.parse("smsfwd://op/op-42/attempt/x/part/3"))
        assertNull(SmsCallbackUri.parse("content://foreign/1"))
    }
}
