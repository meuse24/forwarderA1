package info.meuse24.smsforwarderneoA1.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SmtpFailureClassifierTest {

    @Test fun `an authentication rejection is recognised by its code`() {
        assertEquals(
            EmailFailureKind.AUTHENTICATION,
            SmtpFailureClassifier.classify("SMTPSendFailedException", 535, "auth failed")
        )
        assertEquals(
            EmailFailureKind.AUTHENTICATION,
            SmtpFailureClassifier.classify("SMTPSendFailedException", 530, null)
        )
    }

    @Test fun `an authentication rejection is recognised by its type`() {
        assertEquals(
            EmailFailureKind.AUTHENTICATION,
            SmtpFailureClassifier.classify("AuthenticationFailedException", null, null)
        )
    }

    /** Nur hier ist der Code einer Adresse zuzuordnen. */
    @Test fun `a rejected recipient is attributed to that recipient`() {
        assertEquals(
            EmailFailureKind.RECIPIENT,
            SmtpFailureClassifier.classify("SMTPAddressFailedException", 550, "no such user")
        )
    }

    @Test fun `a temporarily rejected recipient is retried`() {
        assertEquals(
            EmailFailureKind.TRANSIENT,
            SmtpFailureClassifier.classify("SMTPAddressFailedException", 450, "mailbox busy")
        )
    }

    /**
     * Der entscheidende Unterschied zu [a rejected recipient is attributed to that recipient]:
     * Ohne Empfaengerbezug sagt ein 5xx nicht, welche Adresse falsch ist.
     */
    @Test fun `a transactional 5xx is permanent but not a recipient problem`() {
        assertEquals(
            EmailFailureKind.PERMANENT,
            SmtpFailureClassifier.classify("SMTPSendFailedException", 554, "transaction failed")
        )
        assertEquals(
            EmailFailureKind.PERMANENT,
            SmtpFailureClassifier.classify("SMTPSendFailedException", 552, "message too large")
        )
    }

    @Test fun `a 4xx without recipient context is transient`() {
        assertEquals(
            EmailFailureKind.TRANSIENT,
            SmtpFailureClassifier.classify("SMTPSendFailedException", 421, "service not available")
        )
    }

    @Test fun `an unusable address is a configuration problem`() {
        assertEquals(
            EmailFailureKind.CONFIGURATION,
            SmtpFailureClassifier.classify("AddressException", null, "missing @")
        )
    }

    @Test fun `a failed tls handshake is not retried`() {
        assertEquals(
            EmailFailureKind.TRANSPORT_SECURITY,
            SmtpFailureClassifier.classify("SSLHandshakeException", null, "trust anchor")
        )
        assertEquals(
            EmailFailureKind.TRANSPORT_SECURITY,
            SmtpFailureClassifier.classify("CertificateExpiredException", null, null)
        )
    }

    /** JavaMail lehnt das erzwungene STARTTLS mit einer gewoehnlichen MessagingException ab. */
    @Test fun `a refused starttls is recognised by its message`() {
        assertEquals(
            EmailFailureKind.TRANSPORT_SECURITY,
            SmtpFailureClassifier.classify(
                "MessagingException",
                null,
                "STARTTLS is required but host does not support it"
            )
        )
    }

    @Test fun `network errors and unknown causes are retried`() {
        assertEquals(
            EmailFailureKind.TRANSIENT,
            SmtpFailureClassifier.classify("UnknownHostException", null, "smtp.example.org")
        )
        assertEquals(
            EmailFailureKind.TRANSIENT,
            SmtpFailureClassifier.classify("SocketTimeoutException", null, null)
        )
        assertEquals(
            EmailFailureKind.TRANSIENT,
            SmtpFailureClassifier.classify("SomethingCompletelyNew", null, null)
        )
    }
}
