package info.meuse24.smsforwarderneoA1.domain.model

/**
 * Zustand eines Weiterleitungsvorgangs per SMS.
 *
 * Die Semantik ist bewusst asymmetrisch: Neu gesendet wird nur, wo ein Fehlschlag
 * belegt ist. Wo jede Aussage fehlt, wird nicht gesendet - siehe [UNKNOWN].
 */
enum class ForwardingState {
    /** Vorgang samt Zieldaten persistiert, Sendeaufruf nachweislich noch nicht erfolgt. */
    QUEUED,

    /** Unmittelbar vor dem Sendeaufruf geschrieben. Nach einem Prozessneustart mehrdeutig. */
    ATTEMPTING,

    /** Sendeaufruf ohne Exception zurueckgekehrt, Callbacks stehen aus. */
    HANDED_OVER,

    /** Alle Teile per Callback bestaetigt. */
    SENT,

    /** Mindestens ein Teil meldet einen transienten Fehler; Neuversand geplant. */
    RETRY,

    /** Terminaler Fehler, verletzte Vorbedingung oder erschoepfte Versuche. */
    FAILED,

    /** Keine oder mehrdeutige Aussage. Kein Neuversand, nur Anzeige. */
    UNKNOWN;

    /** Kein Automatismus greift mehr; der Eintrag existiert nur noch zur Anzeige. */
    val isTerminal: Boolean
        get() = this == SENT || this == FAILED || this == UNKNOWN
}

/** Ergebnis eines einzelnen SMS-Teils, wie es der Plattform-Callback meldet. */
enum class SmsPartResult {
    /** RESULT_OK - das Netz hat den Teil angenommen. */
    OK,

    /** Belegter, voraussichtlich voruebergehender Fehlschlag dieses Teils. */
    TRANSIENT_FAILURE,

    /** Belegter Fehlschlag, den ein Neuversand nicht heilt. */
    TERMINAL_FAILURE
}

/**
 * Ein persistierter Weiterleitungsvorgang.
 *
 * [expectedParts] steht bereits beim Anlegen fest, damit [ATTEMPTING][ForwardingState.ATTEMPTING]
 * vollstaendig ist und die Vollzaehligkeit der Callbacks entscheidbar bleibt.
 *
 * [attempt] zaehlt Sendeversuche ab 1. Der Wert wandert in die Callback-URI, damit
 * verspaetete Rueckmeldungen eines frueheren Versuchs erkennbar sind.
 *
 * Rueckmeldungen werden als **Mengen von Teilindizes** gefuehrt, nicht als Zaehler. Nur so ist
 * eine doppelt zugestellte Rueckmeldung desselben Teils folgenlos: Ein Zaehler wuerde zweimal
 * hochlaufen und einen Vorgang als vollstaendig ausweisen, dessen anderer Teil nie bestaetigt
 * wurde.
 */
data class ForwardingOperation(
    val id: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val sender: String,
    val targetNumber: String,
    val text: String,
    val subscriptionId: Int,
    val expectedParts: Int,
    val state: ForwardingState = ForwardingState.QUEUED,
    val attempt: Int = 0,
    val confirmedPartIndices: Set<Int> = emptySet(),
    val failedPartIndices: Set<Int> = emptySet(),
    val nextAttemptAtMillis: Long? = null,
    val handedOverAtMillis: Long? = null,
    val lastResultCode: Int? = null,
    val reason: String? = null,
    /** Vom Nutzer zur Kenntnis genommen; zaehlt in der Warnanzeige nicht mehr mit. */
    val acknowledged: Boolean = false
) {
    val confirmedParts: Int get() = confirmedPartIndices.size
    val failedParts: Int get() = failedPartIndices.size
    val reportedParts: Int get() = confirmedParts + failedParts

    /** Wurde dieser Teil im laufenden Versuch bereits gemeldet? */
    fun hasReported(partIndex: Int): Boolean =
        partIndex in confirmedPartIndices || partIndex in failedPartIndices
}
