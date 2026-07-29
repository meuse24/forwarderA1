package info.meuse24.smsforwarderneoA1.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class EmailPortPolicyTest {

    @Test fun `the usual ports are accepted`() {
        assertEquals(EmailPortPolicy.Result.Valid(587), EmailPortPolicy.validate("587"))
        assertEquals(EmailPortPolicy.Result.Valid(465), EmailPortPolicy.validate("465"))
        assertEquals(EmailPortPolicy.Result.Valid(2525), EmailPortPolicy.validate(" 2525 "))
    }

    @Test fun `an empty entry is reported rather than swallowed`() {
        assertEquals(EmailPortPolicy.Result.Empty, EmailPortPolicy.validate(""))
        assertEquals(EmailPortPolicy.Result.Empty, EmailPortPolicy.validate("   "))
    }

    @Test fun `text is not a port`() {
        assertEquals(EmailPortPolicy.Result.NotANumber, EmailPortPolicy.validate("587a"))
        assertEquals(EmailPortPolicy.Result.NotANumber, EmailPortPolicy.validate("-"))
    }

    @Test fun `values outside the port range are rejected`() {
        assertEquals(EmailPortPolicy.Result.OutOfRange, EmailPortPolicy.validate("0"))
        assertEquals(EmailPortPolicy.Result.OutOfRange, EmailPortPolicy.validate("65536"))
        assertEquals(EmailPortPolicy.Result.OutOfRange, EmailPortPolicy.validate("-1"))
    }
}
