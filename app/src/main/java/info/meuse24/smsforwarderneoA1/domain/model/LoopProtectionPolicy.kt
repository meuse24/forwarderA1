package info.meuse24.smsforwarderneoA1.domain.model

/**
 * Entscheidet, ob eine empfangene SMS weitergeleitet werden darf.
 *
 * Der Nummernvergleich wird hereingereicht, weil er Geraetewissen braucht (Laendervorwahl,
 * Normalisierung). Die Entscheidungsregel selbst bleibt dadurch ohne Android pruefbar.
 */
object LoopProtectionPolicy {

    enum class Decision {
        /** Weiterleiten. */
        FORWARD,

        /** Absender ist die Zielrufnummer - die Weiterleitung wuerde sich selbst fuettern. */
        BLOCKED_SENDER_IS_TARGET,

        /** Ziel ist eine eigene SIM - jede weitergeleitete SMS erzeugt die naechste. */
        BLOCKED_TARGET_IS_OWN_SIM
    }

    /**
     * @param ownNumbers automatisch erkannte und manuell hinterlegte eigene Rufnummern
     * @param sameNumber Vergleich zweier Rufnummern in normalisierter Form
     */
    fun decide(
        sender: String,
        targetNumber: String,
        ownNumbers: List<String>,
        sameNumber: (String, String) -> Boolean
    ): Decision = when {
        targetNumber.isBlank() -> Decision.FORWARD // Vorbedingung, nicht Schleifenschutz
        sameNumber(sender, targetNumber) -> Decision.BLOCKED_SENDER_IS_TARGET
        ownNumbers.any { it.isNotBlank() && sameNumber(it, targetNumber) } -> Decision.BLOCKED_TARGET_IS_OWN_SIM
        else -> Decision.FORWARD
    }
}
