package info.meuse24.smsforwarderneoA1.domain.model

/**
 * Zustand einer Weiterleitung per E-Mail.
 *
 * Die Semantik weicht bewusst von [ForwardingState] ab: Dort wird im mehrdeutigen Fenster
 * **nicht** neu gesendet, weil jede SMS Geld kostet. Eine E-Mail kostet nichts, und der Verlust
 * einer Weiterleitung ist der eigentliche Schadensfall dieser App - hier wird im Zweifel also
 * erneut gesendet.
 */
enum class EmailDeliveryState {
    /** Auftrag haltbar geschrieben, Versand noch nicht begonnen. */
    QUEUED,

    /** Unmittelbar vor dem Verbindungsaufbau geschrieben. Nach Prozessverlust wieder QUEUED. */
    ATTEMPTING,

    /** Alle Empfaenger zugestellt. */
    SENT,

    /** Mindestens ein Empfaenger steht noch aus, ein Neuversuch ist geplant. */
    RETRY,

    /** Kein Empfaenger erreicht und kein Neuversuch mehr moeglich. */
    FAILED,

    /** Ein Teil der Empfaenger wurde beliefert, der Rest dauerhaft abgelehnt. */
    PARTIAL;

    /** Kein Automatismus greift mehr; der Eintrag existiert nur noch zur Anzeige. */
    val isTerminal: Boolean
        get() = this == SENT || this == FAILED || this == PARTIAL
}

/**
 * Zustellstand eines einzelnen Empfaengers.
 *
 * Je Empfaenger gefuehrt, nicht als Gesamtergebnis: Ein Server kann eine Adresse ablehnen und die
 * andere annehmen. Ohne diese Trennung wuerde eine Wiederholung die bereits belieferten
 * Empfaenger erneut anschreiben.
 */
data class EmailRecipientState(
    val address: String,
    val delivered: Boolean = false,
    val failure: EmailFailure? = null
) {
    /** Steht dieser Empfaenger noch aus? Ein voruebergehender Fehler zaehlt als „noch offen". */
    val isPending: Boolean
        get() = !delivered && (failure == null || failure.kind.isTransient)
}

/**
 * Ein persistierter Weiterleitungsauftrag des E-Mail-Kanals.
 *
 * [receivedAtMillis] ist der Empfangszeitpunkt der SMS, nicht der Verarbeitungszeitpunkt. Bei
 * einer verzoegerten Zustellung - Funkloch, Doze, Serverausfall - waere der Versandzeitpunkt eine
 * irrefuehrende Angabe.
 *
 * [attempt] wird **vor** dem Verbindungsaufbau erhoeht und haltbar geschrieben. Nach einem
 * Prozessverlust ist damit belegt, dass ein Seiteneffekt moeglich war.
 */
data class EmailForwardingJob(
    val id: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val sender: String,
    val receivedAtMillis: Long,
    val body: String,
    val recipients: List<EmailRecipientState>,
    val state: EmailDeliveryState = EmailDeliveryState.QUEUED,
    val attempt: Int = 0,
    val nextAttemptAtMillis: Long? = null,
    val lastFailure: EmailFailure? = null,
    /** Vom Nutzer zur Kenntnis genommen; zaehlt in der Warnanzeige nicht mehr mit. */
    val acknowledged: Boolean = false
) {
    /** Empfaenger, die ein Versuch noch anschreiben muss. */
    val pendingRecipients: List<String>
        get() = recipients.filter { it.isPending }.map { it.address }

    val deliveredCount: Int get() = recipients.count { it.delivered }
}
