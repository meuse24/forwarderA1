package info.meuse24.smsforwarderneoA1.domain.model

/**
 * Aufbewahrung der E-Mail-Queue: 100 Eintraege oder 7 Tage, was zuerst greift.
 *
 * Gleiche Begruendung wie bei [ForwardingQueueRetentionPolicy]: Die Queue enthaelt SMS-Volltexte
 * und wird deshalb aktiv begrenzt. **Verdraengt werden ausschliesslich terminale Eintraege** - ein
 * laufender Auftrag wuerde durch Verdraengung kommentarlos verschwinden, genau das, was die Queue
 * verhindern soll.
 *
 * Die Obergrenze liegt hoeher als beim SMS-Kanal, weil die Frist eines Auftrags mit 24 Stunden
 * deutlich laenger ist als die des SMS-Kanals und sich entsprechend mehr laufende Auftraege
 * ansammeln koennen.
 */
object EmailQueueRetentionPolicy {

    const val MAX_ENTRIES = 100
    const val MAX_AGE_MILLIS = 7L * 24 * 60 * 60 * 1000

    fun retain(entries: List<EmailForwardingJob>, now: Long): List<EmailForwardingJob> {
        val fresh = entries
            .filterNot { it.state.isTerminal && now - it.updatedAtMillis > MAX_AGE_MILLIS }
            .sortedBy { it.createdAtMillis }
        if (fresh.size <= MAX_ENTRIES) return fresh

        val excess = fresh.size - MAX_ENTRIES
        val dropped = fresh.filter { it.state.isTerminal }.take(excess).map { it.id }.toSet()
        return fresh.filterNot { it.id in dropped }
    }
}
