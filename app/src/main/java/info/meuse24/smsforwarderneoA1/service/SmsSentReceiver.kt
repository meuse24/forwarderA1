package info.meuse24.smsforwarderneoA1.service

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import info.meuse24.smsforwarderneoA1.LoggingManager
import info.meuse24.smsforwarderneoA1.R
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
                    SnackbarManager.showSuccess(context.getString(R.string.snackbar_sms_sent_success, recipient))
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
                SnackbarManager.showError(context.getString(R.string.snackbar_sms_error_generic))
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
                SnackbarManager.showError(context.getString(R.string.snackbar_sms_error_no_service))
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
                SnackbarManager.showError(context.getString(R.string.snackbar_sms_error_null_pdu))
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
                SnackbarManager.showError(context.getString(R.string.snackbar_sms_error_radio_off))
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
                SnackbarManager.showError(context.getString(R.string.snackbar_sms_error_code, resultCode))
            }
        }
    }
}
