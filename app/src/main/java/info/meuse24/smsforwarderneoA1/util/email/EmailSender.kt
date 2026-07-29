package info.meuse24.smsforwarderneoA1.util.email

import info.meuse24.smsforwarderneoA1.domain.model.EmailFailure
import info.meuse24.smsforwarderneoA1.domain.model.EmailFailureKind
import info.meuse24.smsforwarderneoA1.domain.model.EmailPortPolicy
import info.meuse24.smsforwarderneoA1.domain.model.EmailTransportSecurity
import info.meuse24.smsforwarderneoA1.domain.model.SmtpFailureClassifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/**
 * Vollstaendige Zugangs- und Transportkonfiguration eines Versands.
 *
 * [fromAddress] ist bereits aufgeloest: Ist keine eigene Absenderadresse gepflegt, steht hier der
 * Benutzername. Login und Absender sind getrennt, weil beides bei vielen Anbietern nicht dasselbe
 * ist - ein Login wie `u1234567` ergibt keine gueltige Absenderadresse.
 */
data class SmtpConfig(
    val host: String,
    val port: Int,
    val security: EmailTransportSecurity,
    val username: String,
    val password: String,
    val fromAddress: String
)

/**
 * Ergebnis eines Versandlaufs ueber **eine** Verbindung.
 *
 * Die Unterscheidung ist wesentlich: Scheitert schon der Verbindungsaufbau, wurde kein Empfaenger
 * beliefert und der Fehler gilt fuer alle. Steht die Verbindung, hat jeder Empfaenger sein eigenes
 * Ergebnis - eine abgelehnte Adresse sagt ueber die anderen nichts aus.
 */
sealed interface EmailSendOutcome {
    /** Verbindung stand; jedes Empfaengerergebnis wurde einzeln gemeldet. */
    data object Completed : EmailSendOutcome

    /** Verbindung kam nicht zustande - kein Empfaenger wurde beliefert. */
    data class ConnectionFailed(val failure: EmailFailure) : EmailSendOutcome

    /**
     * Der Aufrufer konnte ein Empfaengerergebnis nicht haltbar schreiben und hat abgebrochen.
     *
     * Der Auftragszustand bildet den tatsaechlichen Versand ab diesem Punkt **nicht** mehr ab.
     * Er darf deshalb nicht abgeschlossen werden - sonst gilt ein zugestellter Empfaenger als
     * offen und bekaeme die Nachricht beim naechsten Versuch ein zweites Mal.
     */
    data object Aborted : EmailSendOutcome
}

/**
 * SMTP-Versand mit erzwungener Transportverschluesselung.
 *
 * Sicherheitseigenschaften, die nicht zur Disposition stehen:
 * - Verschluesselung ist Pflicht: entweder STARTTLS (erzwungen, kein Fallback auf Klartext) oder
 *   TLS ab dem ersten Byte. Eine unverschluesselte Option gibt es nicht.
 * - TLS 1.2+ ausschliesslich.
 * - Hostname-Pruefung aktiv (verhindert MITM).
 *
 * **Ein Empfaenger je Nachricht.** Frueher standen alle Empfaenger gemeinsam im `To:`; sie sahen
 * damit gegenseitig ihre Adressen, und ein Teilerfolg war nicht auswertbar - eine Wiederholung
 * belieferte bereits zugestellte Empfaenger erneut.
 */
class EmailSender(private val config: SmtpConfig) {

    private val properties = Properties().apply {
        put("mail.smtp.auth", "true")

        when (config.security) {
            EmailTransportSecurity.STARTTLS -> {
                // required verhindert den stillen Rueckfall auf eine Klartextverbindung.
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.starttls.required", "true")
            }

            EmailTransportSecurity.IMPLICIT_TLS -> {
                put("mail.smtp.ssl.enable", "true")
            }
        }

        put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3")
        put("mail.smtp.ssl.checkserveridentity", "true")

        put("mail.smtp.host", config.host)
        put("mail.smtp.port", config.port.toString())

        // Envelope-Absender: An diese Adresse gehen Unzustellbarkeitsmeldungen des Servers.
        put("mail.smtp.from", config.fromAddress)

        put("mail.smtp.connectiontimeout", TIMEOUT_MILLIS)
        put("mail.smtp.timeout", TIMEOUT_MILLIS)
        // Ohne writetimeout blockiert ein waehrend des Schreibens stehengebliebener Server
        // unbegrenzt - die beiden anderen Zeitlimits greifen dort nicht.
        put("mail.smtp.writetimeout", TIMEOUT_MILLIS)
    }

    private val session: Session = Session.getInstance(properties, object : Authenticator() {
        override fun getPasswordAuthentication(): PasswordAuthentication =
            PasswordAuthentication(config.username, config.password)
    })

    /**
     * Sendet an jeden Empfaenger einzeln ueber **eine** gemeinsame Verbindung.
     *
     * @param messageId stabile Message-ID des Auftrags, ohne spitze Klammern. Eine Wiederholung
     *   nach einem Prozessverlust ist damit fuer Server und Client als Wiederholung erkennbar.
     *   `null` ueberlaesst die Vergabe JavaMail.
     * @param onRecipientResult wird je Empfaenger genau einmal aufgerufen - `null` bedeutet
     *   zugestellt. Der Aufrufer persistiert das Ergebnis sofort, damit ein Prozessende die
     *   bereits zugestellten Empfaenger nicht vergisst. Gibt er `false` zurueck, konnte er das
     *   Ergebnis **nicht** haltbar schreiben; dann wird kein weiterer Empfaenger angeschrieben
     *   (siehe [EmailSendOutcome.Aborted]).
     */
    suspend fun send(
        recipients: List<String>,
        subject: String,
        body: String,
        messageId: String? = null,
        onRecipientResult: (address: String, failure: EmailFailure?) -> Boolean
    ): EmailSendOutcome = withContext(Dispatchers.IO) {
        if (recipients.isEmpty()) {
            return@withContext EmailSendOutcome.ConnectionFailed(
                EmailFailure(EmailFailureKind.CONFIGURATION, "keine Empfaenger")
            )
        }

        configurationFailure()?.let { return@withContext EmailSendOutcome.ConnectionFailed(it) }

        val from = runCatching { InternetAddress(config.fromAddress).apply { validate() } }
            .getOrElse { error ->
                return@withContext EmailSendOutcome.ConnectionFailed(
                    EmailFailure(EmailFailureKind.CONFIGURATION, detailOf(error))
                )
            }

        val transport = try {
            session.getTransport("smtp").apply {
                connect(config.host, config.port, config.username, config.password)
            }
        } catch (e: CancellationException) {
            // Ein Abbruch ist kein Versandfehler. Wuerde er hier klassifiziert, ginge er als
            // TRANSIENT durch, verbrauchte einen Versuch und verhinderte, dass der Auftrag beim
            // Wiederanlauf sauber aus ATTEMPTING zurueckgeholt wird.
            throw e
        } catch (e: Exception) {
            return@withContext EmailSendOutcome.ConnectionFailed(toFailure(e))
        }

        var aborted = false
        try {
            for (address in recipients) {
                val failure = sendOne(transport, from, address, subject, body, messageId)
                if (!onRecipientResult(address, failure)) {
                    aborted = true
                    break
                }
            }
        } finally {
            runCatching { transport.close() }
        }

        if (aborted) EmailSendOutcome.Aborted else EmailSendOutcome.Completed
    }

    /** Einzelversand fuer Diagnosezwecke (Test-E-Mail). `null` bedeutet zugestellt. */
    suspend fun sendSingle(recipient: String, subject: String, body: String): EmailFailure? {
        var failure: EmailFailure? = null
        val outcome = send(listOf(recipient), subject, body) { _, result ->
            failure = result
            true
        }
        return when (outcome) {
            is EmailSendOutcome.ConnectionFailed -> outcome.failure
            EmailSendOutcome.Completed, EmailSendOutcome.Aborted -> failure
        }
    }

    private fun sendOne(
        transport: javax.mail.Transport,
        from: InternetAddress,
        address: String,
        subject: String,
        body: String,
        messageId: String?
    ): EmailFailure? = try {
        val message = buildMessage(from, address, subject, body, messageId)
        transport.sendMessage(message, message.allRecipients)
        null
    } catch (e: CancellationException) {
        // Siehe oben: Ein Abbruch darf nicht als Zustellfehler dieses Empfaengers gelten.
        throw e
    } catch (e: Exception) {
        toFailure(e)
    }

    private fun buildMessage(
        from: InternetAddress,
        address: String,
        subject: String,
        body: String,
        messageId: String?
    ): MimeMessage {
        val message = if (messageId == null) {
            MimeMessage(session)
        } else {
            // JavaMail vergibt die Message-ID in saveChanges() selbst; ein vorher gesetzter Header
            // wird dabei ueberschrieben. Der vorgesehene Weg, eine eigene ID zu behalten, ist das
            // Ueberschreiben genau dieser Methode.
            object : MimeMessage(session) {
                override fun updateMessageID() {
                    setHeader("Message-ID", "<$messageId>")
                }
            }
        }
        return message.apply {
            setFrom(from)
            setRecipient(Message.RecipientType.TO, InternetAddress(address))
            setSubject(subject, "UTF-8")
            setText(body, "UTF-8")
            sentDate = java.util.Date()
        }
    }

    /** Prueft, was ohne Netzzugriff feststellbar ist. Spart einen sinnlosen Verbindungsaufbau. */
    private fun configurationFailure(): EmailFailure? {
        val missing = when {
            config.host.isBlank() -> "smtp_host"
            config.username.isBlank() -> "smtp_username"
            config.password.isBlank() -> "smtp_password"
            config.fromAddress.isBlank() -> "smtp_from"
            EmailPortPolicy.validate(config.port.toString()) !is EmailPortPolicy.Result.Valid -> "smtp_port"
            else -> return null
        }
        return EmailFailure(EmailFailureKind.CONFIGURATION, missing)
    }

    /**
     * Uebersetzt eine Exception in eine klassifizierte Ursache.
     *
     * Die aussagekraeftigste Angabe steckt selten in der aeussersten Exception: JavaMail verpackt
     * die eigentliche Serverantwort in `nextException`. Gesucht wird deshalb zuerst ein
     * empfaengerbezogenes Glied, dann das erste Glied mit Antwortcode.
     */
    private fun toFailure(error: Throwable): EmailFailure {
        val chain = chainOf(error)
        val candidate = chain.firstOrNull { it.javaClass.simpleName == "SMTPAddressFailedException" }
            ?: chain.firstOrNull { returnCodeOf(it) != null }
            ?: error
        val returnCode = returnCodeOf(candidate)
        val kind = SmtpFailureClassifier.classify(
            exceptionClassName = candidate.javaClass.simpleName,
            returnCode = returnCode,
            message = candidate.message ?: error.message
        )
        return EmailFailure(kind = kind, detail = detailOf(candidate), returnCode = returnCode)
    }

    private fun chainOf(error: Throwable): List<Throwable> = buildList {
        var current: Throwable? = error
        while (current != null && size < MAX_CHAIN_DEPTH && none { it === current }) {
            add(current)
            current = (current as? MessagingException)?.nextException ?: current.cause
        }
    }

    /**
     * Antwortcode der Serverantwort. Ueber Reflection, weil die tragenden Klassen im
     * herstellerspezifischen Paket `com.sun.mail.smtp` liegen; ein direkter Import wuerde diese
     * Datei an eine Implementierung binden, die JavaMail selbst nicht als API fuehrt.
     */
    private fun returnCodeOf(error: Throwable): Int? = runCatching {
        error.javaClass.getMethod("getReturnCode").invoke(error) as? Int
    }.getOrNull()?.takeIf { it > 0 }

    private fun detailOf(error: Throwable): String =
        "${error.javaClass.simpleName}: ${error.message ?: ""}".trim()

    private companion object {
        const val TIMEOUT_MILLIS = "10000"
        const val MAX_CHAIN_DEPTH = 10
    }
}
