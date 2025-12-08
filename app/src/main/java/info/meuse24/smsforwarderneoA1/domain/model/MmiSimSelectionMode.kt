package info.meuse24.smsforwarderneoA1.domain.model

/**
 * Enum für die SIM-Auswahl bei MMI-Code-Ausführung (Rufumleitung).
 * Bestimmt, mit welcher SIM-Karte MMI-Codes (z.B. Anrufweiterleitungen) ausgeführt werden.
 */
enum class MmiSimSelectionMode(val displayName: String) {
    /**
     * Verwendet die Standard-Sprach-SIM des Systems.
     * Dies ist der Standard-Modus und entspricht dem Android-Systemverhalten.
     */
    DEFAULT_VOICE_SIM("Standard-Sprach-SIM (System)"),

    /**
     * Verwendet immer SIM 1 für MMI-Codes.
     */
    ALWAYS_SIM_1("Immer SIM 1"),

    /**
     * Verwendet immer SIM 2 für MMI-Codes.
     */
    ALWAYS_SIM_2("Immer SIM 2");

    companion object {
        /**
         * Konvertiert einen String-Wert zurück zum Enum.
         * @param value Der gespeicherte Enum-Name oder null
         * @return Das entsprechende Enum oder DEFAULT_VOICE_SIM als Standard
         */
        fun fromString(value: String?): MmiSimSelectionMode {
            return values().find { it.name == value } ?: DEFAULT_VOICE_SIM
        }
    }
}
