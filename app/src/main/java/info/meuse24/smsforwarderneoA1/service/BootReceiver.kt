package info.meuse24.smsforwarderneoA1.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import info.meuse24.smsforwarderneoA1.LoggingManager

/**
 * BroadcastReceiver zum automatischen Starten des SMS Foreground Service nach Geräte-Neustart.
 *
 * Dieser Receiver startet den SmsForegroundService automatisch, wenn das Gerät neu gestartet wird,
 * um eine lückenlose SMS-Weiterleitung zu gewährleisten.
 *
 * Hinweis: Ab Android 12 (API 31) gelten Einschränkungen für das Starten von Foreground Services
 * aus dem Hintergrund. BOOT_COMPLETED ist jedoch eine erlaubte Ausnahme für den Service-Start.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            try {
                LoggingManager.logInfo(
                    component = "BootReceiver",
                    action = "BOOT_COMPLETED",
                    message = "Gerät wurde neu gestartet - starte SMS Foreground Service"
                )

                // Starte den Service nach Neustart
                SmsForegroundService.startService(context)

                LoggingManager.logInfo(
                    component = "BootReceiver",
                    action = "SERVICE_STARTED",
                    message = "SMS Foreground Service erfolgreich nach Neustart gestartet"
                )

            } catch (e: Exception) {
                LoggingManager.logError(
                    component = "BootReceiver",
                    action = "BOOT_START_ERROR",
                    message = "Fehler beim Starten des Service nach Neustart",
                    error = e
                )
                Log.e("BootReceiver", "Error starting service after boot", e)
            }
        }
    }
}
