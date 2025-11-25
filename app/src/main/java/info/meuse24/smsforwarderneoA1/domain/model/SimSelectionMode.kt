package info.meuse24.smsforwarderneoA1.domain.model

/**
 * Enum für die SIM-Auswahl bei der SMS-Weiterleitung.
 * Bestimmt, mit welcher SIM-Karte weitergeleitete SMS versendet werden.
 */
enum class SimSelectionMode(val displayName: String) {
    /**
     * Verwendet dieselbe SIM wie die eingehende SMS.
     * Dies ist der Standard-Modus und die sicherste Option.
     */
    SAME_AS_INCOMING("Gleiche SIM wie SMS-Eingang"),

    /**
     * Verwendet immer SIM 1 für ausgehende SMS.
     */
    ALWAYS_SIM_1("Immer SIM 1"),

    /**
     * Verwendet immer SIM 2 für ausgehende SMS.
     */
    ALWAYS_SIM_2("Immer SIM 2");

    companion object {
        /**
         * Konvertiert einen String-Wert zurück zum Enum.
         * @param value Der gespeicherte Enum-Name oder null
         * @return Das entsprechende Enum oder SAME_AS_INCOMING als Standard
         */
        fun fromString(value: String?): SimSelectionMode {
            return values().find { it.name == value } ?: SAME_AS_INCOMING
        }
    }
}
