package info.meuse24.smsforwarderneoA1.domain.model

/**
 * Wiederholungsregel des E-Mail-Kanals.
 *
 * Der Kanal ist vom SMS-Kanal getrennt: Ein SMTP-Ausfall darf keine zusaetzliche,
 * kostenpflichtige SMS ausloesen. Wiederholt wird deshalb ausschliesslich der E-Mail-Versand.
 *
 * Wiederholt wird **nach der Ursache**, nicht nach dem Exception-Typ. Frueher wurde jeder Fehler
 * als `IOException` weitergereicht und damit ausnahmslos wiederholt - die Regel war faktisch
 * wirkungslos.
 */
object EmailRetryPolicy {

    /**
     * Backoff vor dem 1., 2., 3., 4. und jedem weiteren Neuversuch.
     *
     * Der erste Wert ist bewusst laenger als die frueheren 5 s: Der haeufigste voruebergehende
     * Fehler ist ein Netzwechsel oder ein Funkloch, und dafuer sind Sekunden nutzlos.
     */
    val BACKOFF_MILLIS = longArrayOf(
        60_000L,          // 1 min
        5 * 60_000L,      // 5 min
        15 * 60_000L,     // 15 min
        60 * 60_000L,     // 60 min
        3 * 60 * 60_000L  // 3 h
    )

    /**
     * Frist ab dem Einreihen. Danach wird nicht mehr versucht - eine SMS, deren Weiterleitung
     * einen Tag lang nicht gelingt, ist als Benachrichtigung ohnehin wertlos, und der Auftrag
     * belegt bis dahin Speicher mit einem SMS-Volltext.
     */
    const val MAX_LIFETIME_MILLIS = 24L * 60 * 60 * 1000

    /** Heilt ein spaeterer Versuch diesen Fehler moeglicherweise? */
    fun isRetryable(kind: EmailFailureKind): Boolean = kind.isTransient

    /** @param attempt Anzahl bereits unternommener Versuche, beginnt bei 1. */
    fun delayMillis(attempt: Int): Long =
        BACKOFF_MILLIS[(attempt - 1).coerceIn(0, BACKOFF_MILLIS.lastIndex)]

    /** Ist die Frist seit dem Einreihen abgelaufen? */
    fun isExpired(createdAtMillis: Long, now: Long): Boolean =
        now - createdAtMillis >= MAX_LIFETIME_MILLIS
}
