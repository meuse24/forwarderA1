package info.meuse24.smsforwarderneoA1.util

import androidx.annotation.StringRes
import info.meuse24.smsforwarderneoA1.R
import info.meuse24.smsforwarderneoA1.domain.model.EmailFailureKind

/**
 * Anzeigetext einer Fehlerursache.
 *
 * Die Zuordnung liegt hier und nicht im Modell: `EmailFailure.detail` ist die technische Meldung
 * des Servers und gehoert ins Protokoll, nicht in die Oberflaeche. Der angezeigte Text haengt
 * ausschliesslich an der Ursache und ist damit uebersetzbar.
 *
 * Bei [EmailFailureKind.AUTHENTICATION] und [EmailFailureKind.CONFIGURATION] benennt der Text die
 * betroffene Einstellung - hier ist eine Nutzeraktion tatsaechlich noetig.
 */
@StringRes
fun EmailFailureKind.messageRes(): Int = when (this) {
    EmailFailureKind.AUTHENTICATION -> R.string.warning_email_cause_authentication
    EmailFailureKind.CONFIGURATION -> R.string.warning_email_cause_configuration
    EmailFailureKind.RECIPIENT -> R.string.warning_email_cause_recipient
    EmailFailureKind.PERMANENT -> R.string.warning_email_cause_permanent
    EmailFailureKind.TRANSPORT_SECURITY -> R.string.warning_email_cause_transport_security
    EmailFailureKind.TRANSIENT -> R.string.warning_email_cause_transient
}
