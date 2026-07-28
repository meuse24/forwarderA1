package info.meuse24.smsforwarderneoA1

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ManifestRcsVisibilityTest {

    @Test
    fun manifestDeclaresGoogleMessagesPackageVisibility() {
        val manifest = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml")
        ).firstOrNull(File::isFile)
            ?: error("AndroidManifest.xml was not found from the test working directory")

        val content = manifest.readText()

        assertTrue(content.contains("<queries>"))
        assertTrue(content.contains("<package android:name=\"com.google.android.apps.messaging\" />"))
    }
}
