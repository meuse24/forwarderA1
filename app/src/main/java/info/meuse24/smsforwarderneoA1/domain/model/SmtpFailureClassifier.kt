package info.meuse24.smsforwarderneoA1.domain.model

/**
 * Ordnet einem SMTP-Fehlschlag seine Ursache zu.
 *
 * Bewusst frei von JavaMail: Die Regel bekommt nur Klassennamen, Antwortcode und Meldungstext und
 * ist damit ohne Mail-Abhaengigkeit und ohne Geraet pruefbar. Die Umsetzung von Exception auf
 * diese drei Angaben leistet `EmailSender`.
 */
object SmtpFailureClassifier {

    /** Antwortcodes, mit denen ein Server die Anmeldung ablehnt. */
    private val AUTHENTICATION_CODES = setOf(530, 534, 535)

    /**
     * Die einzige Exception, die einen Antwortcode **einem bestimmten Empfaenger** zuordnet.
     * Alles andere ist transaktions- oder verbindungsbezogen.
     */
    private const val RECIPIENT_SCOPED_EXCEPTION = "SMTPAddressFailedException"

    /**
     * @param exceptionClassName einfacher Klassenname der aussagekraeftigsten Exception der Kette
     * @param returnCode SMTP-Antwortcode, sofern der Server einen geliefert hat
     * @param message Meldungstext, wird nur auf STARTTLS-Hinweise geprueft
     */
    fun classify(
        exceptionClassName: String,
        returnCode: Int?,
        message: String?
    ): EmailFailureKind {
        // 1. Anmeldung: gilt vor allem anderen. Ein 535 ist auch dann eine Anmeldeablehnung,
        //    wenn er in einer empfaengerbezogenen Exception steckt.
        if (returnCode in AUTHENTICATION_CODES) return EmailFailureKind.AUTHENTICATION
        if (exceptionClassName == "AuthenticationFailedException") return EmailFailureKind.AUTHENTICATION

        // 2. Empfaengerbezogene Ablehnung - nur hier ist der Code einer Adresse zuzuordnen.
        if (exceptionClassName == RECIPIENT_SCOPED_EXCEPTION && returnCode != null) {
            return when (returnCode) {
                in 400..499 -> EmailFailureKind.TRANSIENT
                in 500..599 -> EmailFailureKind.RECIPIENT
                else -> EmailFailureKind.TRANSIENT
            }
        }

        // 3./4. Antwortcode ohne Empfaengerbezug: 5xx dauerhaft, 4xx voruebergehend.
        when (returnCode) {
            in 500..599 -> return EmailFailureKind.PERMANENT
            in 400..499 -> return EmailFailureKind.TRANSIENT
        }

        // 5. Eine unbrauchbare Adresse ist ein Konfigurationsfehler, kein Transportproblem.
        if (exceptionClassName == "AddressException") return EmailFailureKind.CONFIGURATION

        // 6. TLS. Der Meldungstext wird mitgeprueft, weil JavaMail das erzwungene STARTTLS mit
        //    einer gewoehnlichen MessagingException ablehnt.
        if (exceptionClassName.contains("SSL") || exceptionClassName.contains("Certificate")) {
            return EmailFailureKind.TRANSPORT_SECURITY
        }
        if (message?.contains("STARTTLS", ignoreCase = true) == true) {
            return EmailFailureKind.TRANSPORT_SECURITY
        }

        // 7./8. Alles Uebrige - insbesondere UnknownHostException, SocketTimeoutException und
        //    ConnectException - gilt als voruebergehend. Ein unaufloesbarer Host ist bei fehlendem
        //    Netz nicht von einem Tippfehler zu unterscheiden; die Wiederholung ist billig, und
        //    das endgueltige Scheitern wird ohnehin sichtbar gemacht. Ein unbekannter Fehler wird
        //    lieber einmal zu oft wiederholt als eine Nachricht verloren.
        return EmailFailureKind.TRANSIENT
    }
}
