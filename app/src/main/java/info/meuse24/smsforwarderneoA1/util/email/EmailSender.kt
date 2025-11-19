package info.meuse24.smsforwarderneoA1.util.email

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
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
 * SMTP email sender with authentication.
 *
 * Sends emails using JavaMail API with STARTTLS encryption.
 * Supports multiple recipients and UTF-8 encoding.
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
        put("mail.smtp.auth", "true")
        put("mail.smtp.starttls.enable", "true")
        put("mail.smtp.host", host)
        put("mail.smtp.port", port)
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

        } catch (e: Exception) {
            EmailResult.Error("Fehler beim E-Mail-Versand: ${e.message}")
        }
    }
}
