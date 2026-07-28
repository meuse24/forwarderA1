package info.meuse24.smsforwarderneoA1.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import info.meuse24.smsforwarderneoA1.LoggingManager

/**
 * Startet den SMS Foreground Service nach einem Geräteneustart.
 *
 * Wichtig fuer den Bootpfad: Ab `targetSdk` 35 darf ein `BOOT_COMPLETED`-Receiver bestimmte
 * Foreground-Service-Typen nicht mehr starten - darunter `dataSync`. Der Dienst laeuft deshalb
 * als `specialUse`; dieser Typ steht nicht auf der Sperrliste.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            try {
                LoggingManager.logInfo(
                    component = "BootReceiver",
                    action = "BOOT_COMPLETED",
                    message = "Gerät wurde neu gestartet"
                )

                // Der Dienst wird auch ohne POST_NOTIFICATIONS gestartet: Die Plattform verlangt
                // die Berechtigung fuer einen Foreground Service nicht. Fehlt sie, ist lediglich
                // die Statusanzeige unterdrueckt - die Weiterleitung laeuft.
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
