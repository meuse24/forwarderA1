package info.meuse24.smsforwarderneoA1.util.email

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/**
 * Result type for email sending operations.
 */
sealed class EmailResult {
    data object Success : EmailResult()
    data class Error(val message: String) : EmailResult()
}

/**
 * SMTP email sender with authentication and enhanced TLS security.
 *
 * Sends emails using JavaMail API with enforced STARTTLS encryption.
 * Supports multiple recipients and UTF-8 encoding.
 *
 * Security features:
 * - STARTTLS required (prevents downgrade attacks)
 * - TLS 1.2+ only (blocks deprecated protocols)
 * - Hostname verification enabled (prevents MITM attacks)
 *
 * @param host SMTP server hostname
 * @param port SMTP server port (typically 587 for TLS)
 * @param username SMTP authentication username
 * @param password SMTP authentication password
 */
class EmailSender(
    private val host: String,
    private val port: Int,
    private val username: String,
    private val password: String
) {
    private val properties = Properties().apply {
        // Authentication
        put("mail.smtp.auth", "true")

        // STARTTLS - Required (prevents downgrade attacks)
        put("mail.smtp.starttls.enable", "true")
        put("mail.smtp.starttls.required", "true")

        // SSL/TLS Protocol versions - TLS 1.2+ only
        put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3")

        // Hostname verification - Prevents MITM attacks
        put("mail.smtp.ssl.checkserveridentity", "true")

        // Server configuration
        put("mail.smtp.host", host)
        put("mail.smtp.port", port)

        // Timeouts
        put("mail.smtp.timeout", "10000")
        put("mail.smtp.connectiontimeout", "10000")
    }

    private val session = Session.getInstance(properties, object : Authenticator() {
        override fun getPasswordAuthentication(): PasswordAuthentication {
            return PasswordAuthentication(username, password)
        }
    })

    /**
     * Sends an email to multiple recipients.
     *
     * @param to List of recipient email addresses
     * @param subject Email subject line
     * @param body Email body text (UTF-8)
     * @return EmailResult.Success or EmailResult.Error with message
     */
    suspend fun sendEmail(
        to: List<String>,
        subject: String,
        body: String
    ): EmailResult = withContext(Dispatchers.IO) {
        try {
            // Validierung
            if (to.isEmpty()) {
                return@withContext EmailResult.Error("Keine Empfänger angegeben")
            }

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(username))
                setRecipients(
                    Message.RecipientType.TO,
                    to.map { InternetAddress(it) }.toTypedArray()
                )
                setSubject(subject, "UTF-8")
                setText(body, "UTF-8")
                sentDate = java.util.Date()
            }

            Transport.send(message)
            EmailResult.Success

        } catch (e: MessagingException) {
            // Enhanced error handling for SSL/TLS specific errors
            val errorMessage = when {
                e.message?.contains("STARTTLS", ignoreCase = true) == true ->
                    "Server unterstützt keine sichere TLS-Verbindung"

                e.message?.contains("certificate", ignoreCase = true) == true ||
                e.message?.contains("SSL", ignoreCase = true) == true ->
                    "Zertifikatsfehler: Server-Identität konnte nicht verifiziert werden"

                e.message?.contains("authentication failed", ignoreCase = true) == true ->
                    "Authentifizierung fehlgeschlagen: Benutzername oder Passwort falsch"

                e.message?.contains("connection", ignoreCase = true) == true ->
                    "Verbindung zum Server fehlgeschlagen"

                else ->
                    "Fehler beim E-Mail-Versand: ${e.message}"
            }
            EmailResult.Error(errorMessage)

        } catch (e: Exception) {
            EmailResult.Error("Unerwarteter Fehler beim E-Mail-Versand: ${e.message}")
        }
    }
}
