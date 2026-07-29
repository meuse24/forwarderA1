package info.meuse24.smsforwarderneoA1.domain.model

/**
 * Aufbau von Betreff und Textkoerper der weitergeleiteten E-Mail.
 *
 * Frei von Android, damit die Regel ohne Geraet pruefbar ist. Die Zeitpunkte werden bereits
 * formatiert uebergeben - die Formatierung selbst haengt an der Locale des Geraets.
 */
object EmailBodyComposer {

    /**
     * Ab dieser Abweichung wird der Versandzeitpunkt zusaetzlich ausgewiesen. Darunter waere die
     * zweite Zeile nur Rauschen.
     */
    const val FORWARD_DELAY_THRESHOLD_MILLIS = 60_000L

    fun subject(sender: String): String = "Neue SMS von $sender"

    /**
     * Ist die Weiterleitung so spaet, dass der Versandzeitpunkt eine eigene Aussage hat?
     *
     * Frueher stand nur der Versandzeitpunkt im Text. Nach einer Verzoegerung - Funkloch, Doze,
     * Serverausfall - behauptete die E-Mail damit einen Empfang, der Stunden zurueckliegen konnte.
     */
    fun needsForwardedAt(receivedAtMillis: Long, nowMillis: Long): Boolean =
        nowMillis - receivedAtMillis >= FORWARD_DELAY_THRESHOLD_MILLIS

    /**
     * @param forwardedAt formatierter Versandzeitpunkt, oder `null`, wenn er nicht abweicht
     */
    fun body(
        sender: String,
        receivedAt: String,
        forwardedAt: String?,
        message: String
    ): String = buildString {
        append("SMS Weiterleitung\n\n")
        append("Absender: $sender\n")
        append("Empfangen: $receivedAt\n")
        if (forwardedAt != null) append("Weitergeleitet: $forwardedAt\n")
        append("\nNachricht:\n")
        append(message)
        append("\n\nDiese E-Mail wurde automatisch durch den SMS Forwarder generiert.")
    }
}
