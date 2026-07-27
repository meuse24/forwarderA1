package info.meuse24.smsforwarderneoA1.util

import androidx.test.platform.app.InstrumentationRegistry
import info.meuse24.smsforwarderneoA1.domain.model.GOOGLE_MESSAGES_PACKAGE
import info.meuse24.smsforwarderneoA1.domain.model.GoogleMessagesState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft den Fehlerpfad des Paketzugriffs. Der Detektor darf unter keinen Umstaenden
 * werfen - ein Absturz auf der Startseite waere ein deutlich schlimmerer Fehler als
 * ein fehlender Hinweis.
 */
class GoogleMessagesDetectorTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun missingPackageYieldsFalseInsteadOfThrowing() {
        val result = GoogleMessagesDetector.isPackageInstalledAndEnabled(
            context.packageManager,
            "com.example.definitely.not.installed.$PACKAGE_SUFFIX"
        )

        assertFalse(result)
    }

    @Test
    fun ownPackageIsRecognisedAsInstalled() {
        // Gegenprobe: Die Pruefung liefert nicht pauschal false.
        assertTrue(
            GoogleMessagesDetector.isPackageInstalledAndEnabled(
                context.packageManager,
                context.packageName
            )
        )
    }

    @Test
    fun detectAlwaysReturnsAValidStateAndNeverThrows() {
        val state = GoogleMessagesDetector.detect(context)

        assertTrue(state in GoogleMessagesState.entries)
    }

    @Test
    fun detectIsConsistentWithTheQueriesManifestEntry() {
        // Ist Google Messages sichtbar, muss der Zustand einer der beiden
        // "vorhanden"-Faelle sein - sonst fehlt der queries-Eintrag im Manifest.
        val visible = GoogleMessagesDetector.isPackageInstalledAndEnabled(
            context.packageManager,
            GOOGLE_MESSAGES_PACKAGE
        )
        val state = GoogleMessagesDetector.detect(context)

        if (visible) {
            assertTrue(
                "Google Messages ist sichtbar, aber detect() meldet NOT_INSTALLED",
                state != GoogleMessagesState.NOT_INSTALLED
            )
        } else {
            assertEquals(GoogleMessagesState.NOT_INSTALLED, state)
        }
    }

    private companion object {
        const val PACKAGE_SUFFIX = "rcs_hint_test"
    }
}
