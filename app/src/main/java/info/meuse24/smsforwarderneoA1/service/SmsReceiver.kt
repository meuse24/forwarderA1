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
        context.startForegroundService(serviceIntent)
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
