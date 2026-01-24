package info.meuse24.smsforwarderneoA1.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import info.meuse24.smsforwarderneoA1.LoggingManager

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    /**
     * Diese Methode wird aufgerufen, wenn eine Broadcast-Nachricht empfangen wird.
     * Sie verarbeitet eingehende SMS und gesendete SMS-Bestätigungen.
     */
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: ${intent.action}")
        when (intent.action) {
            Telephony.Sms.Intents.SMS_RECEIVED_ACTION -> {
                if (isSmsIntentValid(intent)) {
                    handleSmsReceived(context, intent)
                } else {
                    LoggingManager.logWarning(
                        component = "SmsReceiver",
                        action = "INVALID_SMS",
                        message = "Ungültige SMS empfangen"
                    )
                }
            }

            "SMS_SENT" -> handleSmsSent()
            else -> Log.d(TAG, "Unbekannte Aktion empfangen: ${intent.action}")
        }
    }

    private fun isSmsIntentValid(intent: Intent): Boolean {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            Log.w(TAG, "Received SMS intent with no messages")
            return false
        }

        for (smsMessage in messages) {
            val sender = smsMessage.originatingAddress
            val messageBody = smsMessage.messageBody

            if (sender.isNullOrEmpty() || messageBody.isNullOrEmpty()) {
                Log.w(TAG, "Received SMS with empty sender or body")
                return false
            }
        }
        return true
    }

    /**
     * Extrahiert die Subscription ID aus dem SMS-Intent.
     * Verschiedene Android-Geräte und OEMs verwenden unterschiedliche Extra-Keys.
     * @param intent Der eingehende SMS-Intent
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
     * @param context Android Context
     * @param subscriptionId Die Subscription ID der empfangenden SIM-Karte
     * @return true wenn Weiterleitung erlaubt, false wenn gefiltert
     */
    private fun shouldForwardFromSubscription(context: Context, subscriptionId: Int): Boolean {
        val prefsManager = info.meuse24.smsforwarderneoA1.AppContainer.requirePrefsManager()

        // Ermittle alle SIM-Karten und finde die Slot-Nummer für diese Subscription
        val allSims = info.meuse24.smsforwarderneoA1.PhoneSmsUtils.getAllSimInfo(context)
        val sim = allSims.find { it.subscriptionId == subscriptionId }

        // Prüfe ob diese SIM-Karte für Empfang aktiviert ist
        val sim1Enabled = prefsManager.isSim1ReceiveEnabled()
        val sim2Enabled = prefsManager.isSim2ReceiveEnabled()

        val shouldForward = when (sim?.slotIndex) {
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

        return shouldForward
    }

    /**
     * Verarbeitet eingehende SMS-Nachrichten.
     * Wenn die Weiterleitung aktiviert ist, werden die Nachrichten zusammengeführt und weitergeleitet.
     */
    private fun handleSmsReceived(context: Context, intent: Intent) {
        // Extrahiere Subscription ID (Multi-SIM-Support)
        // Nutzt mehrere mögliche Keys, da verschiedene Geräte/OEMs unterschiedliche verwenden
        val subscriptionId = getSubscriptionIdFromIntent(intent)

        // Prüfe SIM-Filter: Soll SMS von dieser SIM-Karte weitergeleitet werden?
        if (!shouldForwardFromSubscription(context, subscriptionId)) {
            val prefsManager = info.meuse24.smsforwarderneoA1.AppContainer.requirePrefsManager()
            LoggingManager.logWarning(
                component = "SmsReceiver",
                action = "SIM_FILTER_BLOCKED",
                message = "SMS von gefilterter SIM-Karte nicht weitergeleitet",
                details = mapOf(
                    "subscription_id" to subscriptionId,
                    "sim1_enabled" to prefsManager.isSim1ReceiveEnabled(),
                    "sim2_enabled" to prefsManager.isSim2ReceiveEnabled()
                )
            )
            return  // SMS wird nicht weitergeleitet - Service wird nicht gestartet
        }

        val serviceIntent = Intent(context, SmsForegroundService::class.java).apply {
            action = "PROCESS_SMS"
            // Kopiere alle SMS-relevanten Extras
            intent.extras?.let { extras ->
                putExtras(extras)
            }
            // Füge die Original-Action hinzu
            putExtra("original_action", intent.action)
            // WICHTIG: Subscription ID explizit weitergeben
            putExtra("subscription", subscriptionId)
            flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            
            LoggingManager.logInfo(
                component = "SmsReceiver",
                action = "FORWARD_TO_SERVICE",
                message = "SMS-Daten an Service übergeben",
                details = mapOf(
                    "has_extras" to (intent.extras != null),
                    "extras_count" to (intent.extras?.size() ?: 0),
                    "subscription_id" to subscriptionId
                )
            )
        } catch (e: Exception) {
            // Abfangen von ForegroundServiceStartNotAllowedException (Android 14+)
            // und IllegalStateException (Android 8+ Background Limits)
            LoggingManager.logError(
                component = "SmsReceiver",
                action = "START_SERVICE_ERROR",
                message = "Konnte Service nicht starten - Hintergrundbeschränkungen?",
                error = e
            )
        }
    }

    /**
     * Verarbeitet Bestätigungen für gesendete SMS.
     */
    private fun handleSmsSent() {
        // Note: resultCode is only available in ordered broadcasts
        // This method is kept for backward compatibility but may not function as expected
        Log.d(TAG, "SMS_SENT action received")
    }
}
