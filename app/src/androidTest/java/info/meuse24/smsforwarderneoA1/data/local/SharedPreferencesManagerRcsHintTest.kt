package info.meuse24.smsforwarderneoA1.data.local

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SharedPreferencesManagerRcsHintTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun clearPreferences() {
        context.deleteSharedPreferences("sms_forwarder_secure_prefs")
    }

    @After
    fun resetPreference() {
        context.deleteSharedPreferences("sms_forwarder_secure_prefs")
    }

    @Test
    fun rcsHintIsVisibleByDefaultAndDismissalSurvivesManagerRecreation() {
        val initialManager = SharedPreferencesManager(context)

        assertFalse(initialManager.isRcsHintDismissed())

        initialManager.setRcsHintDismissed(true)

        val recreatedManager = SharedPreferencesManager(context)
        assertTrue(recreatedManager.isRcsHintDismissed())
    }
}
