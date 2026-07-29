package info.meuse24.smsforwarderneoA1.domain.model

/**
 * Art der Transportverschluesselung des SMTP-Versands.
 *
 * Eine unverschluesselte Option gibt es bewusst nicht: Die Zugangsdaten und der SMS-Volltext
 * gingen dabei im Klartext ueber das Netz.
 */
enum class EmailTransportSecurity {
    /** Klartextverbindung, die per STARTTLS zwingend auf TLS gehoben wird. Typisch Port 587. */
    STARTTLS,

    /** TLS ab dem ersten Byte (SMTPS). Typisch Port 465. */
    IMPLICIT_TLS;

    companion object {
        const val DEFAULT_STARTTLS_PORT = 587
        const val DEFAULT_IMPLICIT_TLS_PORT = 465

        /**
         * Ableitung fuer Bestandsinstallationen, die den Modus noch nicht gespeichert haben.
         *
         * Wer Port 465 eingetragen hatte, meinte implizites TLS - mit der frueheren
         * STARTTLS-Konfiguration hat dieser Versand nie funktioniert. Alle anderen behalten das
         * bisherige Verhalten.
         */
        fun forPort(port: Int): EmailTransportSecurity =
            if (port == DEFAULT_IMPLICIT_TLS_PORT) IMPLICIT_TLS else STARTTLS

        fun fromNameOrNull(name: String?): EmailTransportSecurity? =
            entries.firstOrNull { it.name == name }
    }

    /** Vorschlag beim Umschalten des Modus. Ueberschreibt einen abweichenden Wert nicht von selbst. */
    val defaultPort: Int
        get() = if (this == IMPLICIT_TLS) DEFAULT_IMPLICIT_TLS_PORT else DEFAULT_STARTTLS_PORT
}
