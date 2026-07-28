package info.meuse24.smsforwarderneoA1.domain.model

/**
 * Aufbewahrung der Weiterleitungs-Queue: 50 Eintraege oder 7 Tage, was zuerst greift.
 *
 * Die Queue enthaelt SMS-Volltexte. Sie wird deshalb aktiv begrenzt, statt unbegrenzt zu
 * wachsen - auch wenn das heisst, dass ein alter Fehlschlag irgendwann aus der Anzeige faellt.
 *
 * **Verdraengt werden ausschliesslich terminale Eintraege.** Bei ihnen laeuft kein Automatismus
 * mehr; ihr Verschwinden ist eine Frage der Anzeigedauer. Ein laufender Vorgang dagegen wuerde
 * durch Verdraengung kommentarlos verschwinden - genau das, was die Queue verhindern soll. Reicht
 * die Zahl terminaler Eintraege nicht aus, bleibt die Queue lieber voruebergehend groesser als
 * die Obergrenze. Der Zulauf wird stattdessen an der Quelle begrenzt: Ist die Zahl laufender
 * Vorgaenge erreicht, wird ein neuer Vorgang sichtbar als fehlgeschlagen vermerkt, statt still
 * einen alten zu verdraengen.
 */
object ForwardingQueueRetentionPolicy {

    const val MAX_ENTRIES = 50
    const val MAX_AGE_MILLIS = 7L * 24 * 60 * 60 * 1000

    fun retain(entries: List<ForwardingOperation>, now: Long): List<ForwardingOperation> {
        val fresh = entries
            .filterNot { it.state.isTerminal && now - it.updatedAtMillis > MAX_AGE_MILLIS }
            .sortedBy { it.createdAtMillis }
        if (fresh.size <= MAX_ENTRIES) return fresh

        val excess = fresh.size - MAX_ENTRIES
        val dropped = fresh.filter { it.state.isTerminal }.take(excess).map { it.id }.toSet()
        return fresh.filterNot { it.id in dropped }
    }
}
