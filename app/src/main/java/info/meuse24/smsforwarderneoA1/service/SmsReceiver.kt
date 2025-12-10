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
        val shouldForward = when (sim?.slotIndex) {
            0 -> prefsManager.isSim1ReceiveEnabled()  // SIM 1 (slot 0)
            1 -> prefsManager.isSim2ReceiveEnabled()  // SIM 2 (slot 1)
            else -> {
                // Unbekannte SIM oder subscription_id = -1
                LoggingManager.logWarning(
                    component = "SmsReceiver",
                    action = "UNKNOWN_SIM_DETECTED",
                    message = "SMS von unbekannter SIM empfangen",
                    details = mapOf(
                        "subscription_id" to subscriptionId,
                        "sim_found" to (sim != null),
                        "slot_index" to (sim?.slotIndex ?: "null"),
                        "available_sims_count" to allSims.size,
                        "note" to "SMS wird geblockt (Fail-Closed-Verhalten)"
                    )
                )
                false  // Unbekannte SIM, nicht weiterleiten (Fail-Closed)
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
        val subscriptionId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            intent.extras?.getInt("subscription", -1) ?: -1
        } else {
            -1
        }

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
