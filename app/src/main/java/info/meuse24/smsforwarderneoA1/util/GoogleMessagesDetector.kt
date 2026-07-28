package info.meuse24.smsforwarderneoA1.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import info.meuse24.smsforwarderneoA1.domain.model.GOOGLE_MESSAGES_PACKAGE
import info.meuse24.smsforwarderneoA1.domain.model.GoogleMessagesState
import info.meuse24.smsforwarderneoA1.domain.model.resolveGoogleMessagesState

/**
 * Ermittelt, ob Google Messages installiert bzw. Standard-SMS-App ist.
 *
 * Das ist die einzige Information, die ohne zusaetzliche Berechtigung verfuegbar ist.
 * Der eigentliche RCS-Status ist nicht auslesbar - alle Texte der App muessen deshalb
 * als Moeglichkeit formuliert sein ("falls RCS aktiv ist"), nie als Feststellung.
 *
 * Benoetigt den queries-Eintrag im Manifest (Paketsichtbarkeit ab Android 11).
 */
object GoogleMessagesDetector {

    /**
     * Liest den aktuellen Zustand.
     * Faellt bei jedem Fehler auf [GoogleMessagesState.NOT_INSTALLED] zurueck, damit ein
     * unerwartetes Systemverhalten nie einen irrefuehrenden Hinweis erzeugt.
     */
    fun detect(context: Context): GoogleMessagesState = try {
        detect(
            isInstalled = { isGoogleMessagesInstalled(context) },
            defaultSmsPackage = { Telephony.Sms.getDefaultSmsPackage(context) }
        )
    } catch (e: Exception) {
        GoogleMessagesState.NOT_INSTALLED
    }

    /**
     * Schmale, Android-freie Kapselung der beiden Systemabfragen.
     *
     * Sie erlaubt JVM-Tests fuer Fehler aus dem PackageManager bzw. der Telephony-API,
     * ohne einen Emulator oder ein reales Fremdpaket zu benoetigen.
     */
    internal fun detect(
        isInstalled: () -> Boolean,
        defaultSmsPackage: () -> String?
    ): GoogleMessagesState = try {
        resolveGoogleMessagesState(
            isInstalled = isInstalled(),
            defaultSmsPackage = defaultSmsPackage()
        )
    } catch (e: Exception) {
        GoogleMessagesState.NOT_INSTALLED
    }

    private fun isGoogleMessagesInstalled(context: Context): Boolean =
        isPackageInstalledAndEnabled(context.packageManager, GOOGLE_MESSAGES_PACKAGE)

    /**
     * Prueft, ob das Paket installiert UND aktiviert ist. Eine deaktivierte App empfaengt
     * keine Nachrichten und wird deshalb wie "nicht installiert" behandelt.
     *
     * Ein fehlendes Paket ist der Normalfall und darf nie als Fehler nach aussen dringen:
     * Ohne den queries-Eintrag im Manifest oder bei nicht installierter App wirft
     * getPackageInfo() eine NameNotFoundException, die hier zu `false` wird.
     *
     * `internal` statt `private`, damit dieser Fehlerpfad direkt getestet werden kann.
     */
    internal fun isPackageInstalledAndEnabled(pm: PackageManager, packageName: String): Boolean = try {
        getPackageInfoCompat(pm, packageName).applicationInfo?.enabled ?: false
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    @Suppress("DEPRECATION")
    private fun getPackageInfoCompat(pm: PackageManager, packageName: String) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L))
        } else {
            pm.getPackageInfo(packageName, 0)
        }
}
