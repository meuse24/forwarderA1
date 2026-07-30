package info.meuse24.smsforwarderneoA1.domain.model

/** Zusatz hinter einem SIM-Auswahl-Modus in den Einstellungen. */
enum class SimSelectionSuffix { NONE, DEFAULT_SMS, NOT_AVAILABLE }

/**
 * Entscheidet rein, welcher Zusatz hinter einem Auswahl-Modus steht.
 *
 * Ausgelagert, weil die Beschriftung zuvor über `simCount >= 2` auf Verfügbarkeit schloss.
 * Steckt nur eine Karte, und zwar in Slot 2, ist `simCount == 1` - "Immer SIM 2" wurde dann
 * als nicht verfügbar beschriftet, obwohl der Sendeweg diese Karte korrekt wählt. Umgekehrt
 * hatte "Immer SIM 1" gar keine Prüfung und galt auch bei leerem Slot 1 als wählbar.
 *
 * Maßgeblich ist deshalb allein, ob im jeweiligen Steckplatz eine Karte steckt - also ob
 * [SimInfo.inSlot] eine Subscription liefert. `@Composable` mit `stringResource` ließe sich
 * auf der JVM nicht testen, diese Funktion schon.
 */
object SimSelectionLabels {

    fun suffixFor(
        mode: SimSelectionMode,
        sim1SubscriptionId: Int?,
        sim2SubscriptionId: Int?,
        defaultSmsSubscriptionId: Int
    ): SimSelectionSuffix = when (mode) {
        SimSelectionMode.SAME_AS_INCOMING -> SimSelectionSuffix.NONE
        SimSelectionMode.ALWAYS_SIM_1 -> forSlot(sim1SubscriptionId, defaultSmsSubscriptionId)
        SimSelectionMode.ALWAYS_SIM_2 -> forSlot(sim2SubscriptionId, defaultSmsSubscriptionId)
    }

    private fun forSlot(subscriptionId: Int?, defaultSmsSubscriptionId: Int): SimSelectionSuffix = when {
        subscriptionId == null -> SimSelectionSuffix.NOT_AVAILABLE
        // -1 heisst "keine Standard-SIM ermittelt"; ohne diese Bedingung waere eine leere
        // Ermittlung nicht von einer echten Uebereinstimmung zu unterscheiden.
        subscriptionId == defaultSmsSubscriptionId && subscriptionId != -1 -> SimSelectionSuffix.DEFAULT_SMS
        else -> SimSelectionSuffix.NONE
    }
}
