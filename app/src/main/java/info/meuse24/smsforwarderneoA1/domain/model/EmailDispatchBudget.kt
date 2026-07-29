package info.meuse24.smsforwarderneoA1.domain.model

/**
 * Zeitbudgets des E-Mail-Versands.
 *
 * Grund: Die Empfaengerliste ist nicht begrenzt, und ein einzelner Versand kann an jedem der drei
 * SMTP-Zeitlimits (Verbindung, Lesen, Schreiben) je 10 s haengen. Ohne Budget kann ein Durchlauf
 * seinen WakeLock ueberdauern und ohne ihn weiterlaufen - dann entscheidet der Doze-Modus, ob die
 * Weiterleitung noch hinausgeht.
 *
 * Ein ueberschrittenes Budget kostet **nichts**: Wer nicht mehr drankam, bleibt eingereiht und ist
 * beim naechsten Durchlauf an der Reihe. Deshalb wird begrenzt statt die Empfaengerzahl zu kappen -
 * eine stillschweigend gekuerzte Empfaengerliste waere echter Verlust.
 */
object EmailDispatchBudget {

    /** Die drei SMTP-Zeitlimits von je 10 s plus Reserve fuer Anmeldung und Nachrichtenaufbau. */
    const val PER_RECIPIENT_MILLIS = 40_000L

    /** Obergrenze eines Auftrags. Darueber hinaus offene Empfaenger kommen im naechsten Durchlauf dran. */
    const val MAX_JOB_MILLIS = 5 * 60 * 1000L

    /**
     * **Startgrenze** eines Durchlaufs, keine harte Gesamtlaufzeit: Nach Ablauf wird kein
     * *weiterer* Auftrag mehr begonnen, ein bereits laufender aber zu Ende gefuehrt.
     *
     * Bewusst so. Ein laufender Auftrag haelt seinen eigenen, auf seine Empfaengerzahl bemessenen
     * WakeLock - er ist also gedeckt. Ihn mittendrin zu kappen wuerde nichts gewinnen und einen
     * zusaetzlichen Neuversuch erzeugen. Die Grenze soll verhindern, dass ein Rueckstau den
     * Versandlauf unbegrenzt belegt, nicht einen gedeckten Versand abschneiden.
     *
     * Die daraus folgende schlechteste Gesamtlaufzeit ist benannt und begrenzt:
     * [worstCaseRunMillis].
     */
    const val MAX_RUN_START_MILLIS = 10 * 60 * 1000L

    /**
     * Laengstmoegliche Dauer eines Durchlaufs: Ein Auftrag, der eine Millisekunde vor
     * [MAX_RUN_START_MILLIS] beginnt, laeuft noch sein volles Auftragsbudget plus den einen
     * Einzelversand, der es ueberschreiten darf.
     */
    fun worstCaseRunMillis(): Long = MAX_RUN_START_MILLIS + MAX_JOB_MILLIS + PER_RECIPIENT_MILLIS

    /** Zeitbudget fuer die Empfaengerschleife eines Auftrags. */
    fun forJob(pendingRecipients: Int): Long =
        (pendingRecipients.coerceAtLeast(1) * PER_RECIPIENT_MILLIS).coerceAtMost(MAX_JOB_MILLIS)

    /**
     * Laufzeit des WakeLocks fuer diesen Auftrag.
     *
     * Um einen Empfaenger groesser als das Budget: Geprueft wird erst **nach** der Rueckkehr eines
     * Einzelversands, der letzte darf das Budget also ueberschreiten. Ein WakeLock, der genau auf
     * dem Budget endet, waere fuer eben diesen Versand schon abgelaufen.
     */
    fun wakeLockMillisForJob(pendingRecipients: Int): Long =
        forJob(pendingRecipients) + PER_RECIPIENT_MILLIS
}
