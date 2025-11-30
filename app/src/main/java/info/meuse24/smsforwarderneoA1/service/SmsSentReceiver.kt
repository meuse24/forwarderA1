package info.meuse24.smsforwarderneoA1.service

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import info.meuse24.smsforwarderneoA1.LoggingManager
import info.meuse24.smsforwarderneoA1.SnackbarManager

/**
 * BroadcastReceiver für SMS-Sendestatus.
 * Empfängt Benachrichtigungen über erfolgreich gesendete oder fehlgeschlagene SMS.
 */
class SmsSentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val partIndex = intent.getIntExtra("part_index", -1)
        val totalParts = intent.getIntExtra("total_parts", 1)
        val recipient = intent.getStringExtra("recipient") ?: "unknown"

        val partInfo = if (totalParts > 1) " (Teil ${partIndex + 1}/$totalParts)" else ""

        when (resultCode) {
            Activity.RESULT_OK -> {
                LoggingManager.logInfo(
                    component = "SmsSentReceiver",
                    action = "SMS_SENT_SUCCESS",
                    message = "SMS erfolgreich gesendet$partInfo",
                    details = mapOf(
                        "recipient" to recipient,
                        "part_index" to partIndex,
                        "total_parts" to totalParts
                    )
                )

                // Nur bei letztem Teil Erfolgs-Snackbar zeigen
                if (partIndex == -1 || partIndex == totalParts - 1) {
                    SnackbarManager.showSuccess("SMS an $recipient erfolgreich gesendet")
                }
            }

            SmsManager.RESULT_ERROR_GENERIC_FAILURE -> {
                LoggingManager.logError(
                    component = "SmsSentReceiver",
                    action = "SMS_SENT_ERROR",
                    message = "SMS-Sendefehler: Generic Failure$partInfo",
                    details = mapOf(
                        "recipient" to recipient,
                        "error_code" to resultCode
                    )
                )
                SnackbarManager.showError("SMS-Sendefehler: Allgemeiner Fehler")
            }

            SmsManager.RESULT_ERROR_NO_SERVICE -> {
                LoggingManager.logError(
                    component = "SmsSentReceiver",
                    action = "SMS_SENT_ERROR",
                    message = "SMS-Sendefehler: Kein Mobilfunkdienst$partInfo",
                    details = mapOf(
                        "recipient" to recipient,
                        "error_code" to resultCode
                    )
                )
                SnackbarManager.showError("SMS-Sendefehler: Kein Netz verfügbar")
            }

            SmsManager.RESULT_ERROR_NULL_PDU -> {
                LoggingManager.logError(
                    component = "SmsSentReceiver",
                    action = "SMS_SENT_ERROR",
                    message = "SMS-Sendefehler: Null PDU$partInfo",
                    details = mapOf(
                        "recipient" to recipient,
                        "error_code" to resultCode
                    )
                )
                SnackbarManager.showError("SMS-Sendefehler: Ungültige Nachricht")
            }

            SmsManager.RESULT_ERROR_RADIO_OFF -> {
                LoggingManager.logError(
                    component = "SmsSentReceiver",
                    action = "SMS_SENT_ERROR",
                    message = "SMS-Sendefehler: Mobilfunk deaktiviert$partInfo",
                    details = mapOf(
                        "recipient" to recipient,
                        "error_code" to resultCode
                    )
                )
                SnackbarManager.showError("SMS-Sendefehler: Flugmodus aktiv?")
            }

            else -> {
                LoggingManager.logError(
                    component = "SmsSentReceiver",
                    action = "SMS_SENT_ERROR",
                    message = "SMS-Sendefehler: Unbekannter Fehler$partInfo",
                    details = mapOf(
                        "recipient" to recipient,
                        "error_code" to resultCode
                    )
                )
                SnackbarManager.showError("SMS-Sendefehler: Code $resultCode")
            }
        }
    }
}
