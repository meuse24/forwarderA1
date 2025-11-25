package info.meuse24.smsforwarderneoA1.service

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import info.meuse24.smsforwarderneoA1.LoggingManager

/**
 * BroadcastReceiver für SMS-Zustellungsstatus.
 * Empfängt Benachrichtigungen über erfolgreich zugestellte SMS.
 */
class SmsDeliveredReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val partIndex = intent.getIntExtra("part_index", -1)
        val totalParts = intent.getIntExtra("total_parts", 1)
        val recipient = intent.getStringExtra("recipient") ?: "unknown"

        val partInfo = if (totalParts > 1) " (Teil ${partIndex + 1}/$totalParts)" else ""

        when (resultCode) {
            Activity.RESULT_OK -> {
                LoggingManager.logInfo(
                    component = "SmsDeliveredReceiver",
                    action = "SMS_DELIVERED_SUCCESS",
                    message = "SMS erfolgreich zugestellt$partInfo",
                    details = mapOf(
                        "recipient" to recipient,
                        "part_index" to partIndex,
                        "total_parts" to totalParts
                    )
                )
            }

            Activity.RESULT_CANCELED -> {
                LoggingManager.logWarning(
                    component = "SmsDeliveredReceiver",
                    action = "SMS_DELIVERED_FAILED",
                    message = "SMS-Zustellung fehlgeschlagen$partInfo",
                    details = mapOf(
                        "recipient" to recipient,
                        "part_index" to partIndex,
                        "total_parts" to totalParts
                    )
                )
            }

            else -> {
                LoggingManager.logWarning(
                    component = "SmsDeliveredReceiver",
                    action = "SMS_DELIVERED_UNKNOWN",
                    message = "SMS-Zustellungsstatus unbekannt$partInfo",
                    details = mapOf(
                        "recipient" to recipient,
                        "result_code" to resultCode
                    )
                )
            }
        }
    }
}
