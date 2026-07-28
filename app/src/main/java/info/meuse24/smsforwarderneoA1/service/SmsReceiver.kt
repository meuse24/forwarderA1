package info.meuse24.smsforwarderneoA1.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import info.meuse24.smsforwarderneoA1.AppContainer
import info.meuse24.smsforwarderneoA1.LoggingManager
import info.meuse24.smsforwarderneoA1.PhoneSmsUtils
import info.meuse24.smsforwarderneoA1.domain.model.SimInfo

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    /**
     * Einstieg fuer eingehende SMS.
     *
     * Vollstaendig in try/catch: Eine Exception hier bedeutete bisher stillen Verlust der SMS,
     * und im 10-Sekunden-Fenster eines Broadcasts zusaetzlich ANR-Risiko.
     */
    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
                Log.d(TAG, "Unbekannte Aktion empfangen: ${intent.action}")
                return
            }

            if (!hasUsableMessage(intent)) {
                LoggingManager.logWarning(
                    component = "SmsReceiver",
                    action = "INVALID_SMS",
                    message = "SMS ohne verwertbaren Inhalt empfangen"
                )
                return
            }

            handleSmsReceived(context, intent)
        } catch (e: Exception) {
            LoggingManager.logError(
                component = "SmsReceiver",
                action = "RECEIVE_ERROR",
                message = "Fehler bei der Broadcast-Verarbeitung",
                error = e
            )
        }
    }

    /**
     * Genuegt ein einziger verwertbarer Teil, wird der Intent angenommen.
     *
     * Frueher verwarf ein einzelner leerer Teil den gesamten Intent - bei einer mehrteiligen
     * Nachricht ging damit alles verloren. Die leeren Teile filtert die Verarbeitung selbst.
     */
    private fun hasUsableMessage(intent: Intent): Boolean {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            Log.w(TAG, "Received SMS intent with no messages")
            return false
        }
        return messages.any { message ->
            !message?.originatingAddress.isNullOrEmpty() && !message?.messageBody.isNullOrEmpty()
        }
    }

    /**
     * Extrahiert die Subscription ID aus dem SMS-Intent.
     * Verschiedene Android-Geräte und OEMs verwenden unterschiedliche Extra-Keys.
     * @return Die Subscription ID oder -1 wenn nicht ermittelbar
     */
    private fun getSubscriptionIdFromIntent(intent: Intent): Int {
        val extras = intent.extras ?: return -1

        // Verschiedene Keys die von unterschiedlichen Geräten/OEMs verwendet werden
        val subscriptionKeys = listOf(
            "subscription",                                    // Ältere Geräte, manche OEMs
            "android.telephony.extra.SUBSCRIPTION_INDEX",      // SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX (API 22+)
            "phone",                                           // Manche Samsung-Geräte
            "slot",                                            // Manche Geräte liefern nur Slot
            "simId",                                           // Ältere Dual-SIM Implementierungen
            "sim_id"                                           // Alternative Schreibweise
        )

        for (key in subscriptionKeys) {
            if (extras.containsKey(key)) {
                val value = extras.getInt(key, -1)
                if (value != -1) {
                    Log.d(TAG, "Subscription ID $value gefunden über Key: $key")
                    return value
                }
            }
        }

        // Debug: Alle verfügbaren Keys loggen bei Fehlschlag
        LoggingManager.logWarning(
            component = "SmsReceiver",
            action = "SUBSCRIPTION_ID_NOT_FOUND",
            message = "Konnte Subscription ID nicht aus Intent extrahieren",
            details = mapOf(
                "available_keys" to extras.keySet().joinToString(", "),
                "extras_count" to extras.size()
            )
        )
        return -1
    }

    /**
     * Prüft, ob SMS von der angegebenen SIM-Karte weitergeleitet werden sollen.
     * @param allSims bereits ermittelte SIM-Liste - im Broadcast zaehlt jeder Binder-Call
     */
    private fun shouldForwardFromSubscription(
        subscriptionId: Int,
        allSims: List<SimInfo>
    ): Boolean {
        val prefsManager = AppContainer.requirePrefsManager()
        val sim = allSims.find { it.subscriptionId == subscriptionId }

        val sim1Enabled = prefsManager.isSim1ReceiveEnabled()
        val sim2Enabled = prefsManager.isSim2ReceiveEnabled()

        return when (sim?.slotIndex) {
            0 -> sim1Enabled  // SIM 1 (slot 0)
            1 -> sim2Enabled  // SIM 2 (slot 1)
            else -> {
                // Smart Fail-Open: Wenn Subscription nicht ermittelbar,
                // weiterleiten falls BEIDE SIMs aktiv oder nur 1 SIM vorhanden
                val smartFailOpen = (sim1Enabled && sim2Enabled) || allSims.size <= 1

                LoggingManager.logWarning(
                    component = "SmsReceiver",
                    action = "UNKNOWN_SIM_DETECTED",
                    message = if (smartFailOpen)
                        "SMS von unbekannter SIM - wird weitergeleitet (Smart Fail-Open)"
                    else
                        "SMS von unbekannter SIM - wird gefiltert (nur eine SIM aktiv)",
                    details = mapOf(
                        "subscription_id" to subscriptionId,
                        "sim_found" to (sim != null),
                        "slot_index" to (sim?.slotIndex ?: "null"),
                        "available_sims_count" to allSims.size,
                        "sim1_enabled" to sim1Enabled,
                        "sim2_enabled" to sim2Enabled,
                        "smart_fail_open" to smartFailOpen
                    )
                )
                smartFailOpen
            }
        }
    }

    /**
     * Uebergibt die Nachricht an den Dienst.
     *
     * Der Startversuch kann aus dem Hintergrund scheitern; das wird protokolliert, aber nicht
     * als Selbstheilung ausgegeben - eine Zusicherung gibt die Plattform hier nicht.
     */
    private fun handleSmsReceived(context: Context, intent: Intent) {
        val subscriptionId = getSubscriptionIdFromIntent(intent)
        // Einmal je Broadcast: getAllSimInfo() ist ein synchroner Binder-Call.
        val allSims = PhoneSmsUtils.getAllSimInfo(context)

        if (!shouldForwardFromSubscription(subscriptionId, allSims)) {
            LoggingManager.logWarning(
                component = "SmsReceiver",
                action = "SIM_FILTER_BLOCKED",
                message = "SMS von gefilterter SIM-Karte nicht weitergeleitet",
                details = mapOf("subscription_id" to subscriptionId)
            )
            return
        }

        val serviceIntent = Intent(context, SmsForegroundService::class.java).apply {
            action = SmsForegroundService.ACTION_PROCESS_SMS
            intent.extras?.let { extras -> putExtras(extras) }
            putExtra("original_action", intent.action)
            // Explizit weitergeben: Der Dienst kann den Intent nicht erneut auswerten.
            putExtra("subscription", subscriptionId)
            flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
        }

        try {
            context.startForegroundService(serviceIntent)

            LoggingManager.logInfo(
                component = "SmsReceiver",
                action = "FORWARD_TO_SERVICE",
                message = "SMS-Daten an Service übergeben",
                details = mapOf(
                    "extras_count" to (intent.extras?.size() ?: 0),
                    "subscription_id" to subscriptionId
                )
            )
        } catch (e: Exception) {
            // ForegroundServiceStartNotAllowedException (Android 12+) oder
            // IllegalStateException aus den Hintergrundbeschraenkungen.
            LoggingManager.logError(
                component = "SmsReceiver",
                action = "START_SERVICE_ERROR",
                message = "Konnte Service nicht starten - Hintergrundbeschränkungen?",
                error = e
            )
        }
    }
}
