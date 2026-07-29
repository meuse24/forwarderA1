package info.meuse24.smsforwarderneoA1.domain.model

/**
 * Ursache eines gescheiterten E-Mail-Versands.
 *
 * Die Unterscheidung entscheidet ueber die Wiederholung. Frueher wurde jeder Fehler in eine
 * `IOException` verpackt; damit galt auch ein falsches Passwort als voruebergehend und wurde
 * wiederholt - bei Gmail und Microsoft ein Muster, das zur Kontosperre fuehren kann.
 */
enum class EmailFailureKind {
    /** Netz weg, Server nicht erreichbar, Zeitueberschreitung, SMTP 4xx. Wiederholen. */
    TRANSIENT,

    /** Anmeldung abgelehnt. Nicht wiederholen - weitere Versuche riskieren eine Kontosperre. */
    AUTHENTICATION,

    /** Dieser Empfaenger wurde dauerhaft abgelehnt. Andere Empfaenger bleiben davon unberuehrt. */
    RECIPIENT,

    /**
     * Serverseitige oder transaktionale 5xx-Ablehnung **ohne** Empfaengerbezug (etwa 552, 554).
     * Ohne Empfaenger-Kontext sagt der Code nicht, welche Adresse falsch ist - er darf deshalb
     * nicht als [RECIPIENT] gelten, sonst wuerde ein serverseitiges Problem einer Adresse
     * angelastet.
     */
    PERMANENT,

    /** Konfiguration unbrauchbar: Absenderadresse ungueltig, Host leer, Port unmoeglich. */
    CONFIGURATION,

    /** TLS-Aufbau gescheitert (Zertifikat, Protokollversion, kein STARTTLS). Nicht wiederholen. */
    TRANSPORT_SECURITY;

    /** Heilt ein spaeterer Versuch diesen Fehler moeglicherweise? */
    val isTransient: Boolean get() = this == TRANSIENT
}

/**
 * Ein klassifizierter Fehlschlag.
 *
 * [detail] ist die technische Meldung von Server oder Bibliothek. Sie ist **nicht** fuer die
 * Anzeige gedacht - der angezeigte Text wird aus [kind] abgeleitet und ist damit uebersetzbar.
 */
data class EmailFailure(
    val kind: EmailFailureKind,
    val detail: String? = null,
    val returnCode: Int? = null
)
