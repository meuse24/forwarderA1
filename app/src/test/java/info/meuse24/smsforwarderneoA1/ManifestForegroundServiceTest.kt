package info.meuse24.smsforwarderneoA1

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Haelt die Typwahl des Foreground Service fest.
 *
 * `dataSync` hat zwei belegte Folgen, die den Dauerbetrieb brechen: ein Zeitlimit von sechs
 * Stunden je 24 Stunden und - ab targetSdk 35 - ein Startverbot aus `BOOT_COMPLETED`. Beides
 * gilt fuer `specialUse` nicht. Eine versehentliche Rueckstellung faellt hier auf.
 */
class ManifestForegroundServiceTest {

    private val manifest: String
        get() = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml")
        ).firstOrNull(File::isFile)
            ?.readText()
            ?: error("AndroidManifest.xml was not found from the test working directory")

    @Test fun serviceRunsAsSpecialUseAndNotAsDataSync() {
        val content = manifest

        assertTrue(content.contains("android:foregroundServiceType=\"specialUse\""))
        assertFalse(content.contains("android:foregroundServiceType=\"dataSync\""))
        assertFalse(content.contains("FOREGROUND_SERVICE_DATA_SYNC"))
    }

    @Test fun specialUseRequiresItsPermissionAndSubtypeProperty() {
        val content = manifest

        assertTrue(content.contains("android.permission.FOREGROUND_SERVICE_SPECIAL_USE"))
        assertTrue(content.contains("android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"))
    }

    /**
     * Ohne ACCESS_NETWORK_STATE laesst sich vor einem SMTP-Versuch nicht feststellen, ob Netz
     * vorhanden ist; jeder Versuch im Funkloch wuerde das Wiederholungsbudget aufbrauchen.
     */
    @Test fun theEmailChannelCanCheckForNetwork() {
        assertTrue(manifest.contains("android.permission.ACCESS_NETWORK_STATE"))
    }
}
