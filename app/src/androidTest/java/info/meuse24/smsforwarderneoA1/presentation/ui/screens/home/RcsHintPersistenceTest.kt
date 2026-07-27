package info.meuse24.smsforwarderneoA1.presentation.ui.screens.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import info.meuse24.smsforwarderneoA1.AppContainer
import info.meuse24.smsforwarderneoA1.R
import info.meuse24.smsforwarderneoA1.data.local.SharedPreferencesManager
import info.meuse24.smsforwarderneoA1.domain.model.GoogleMessagesState
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Prueft die Anbindung von [RcsHintCardHost] an die verschluesselten Einstellungen:
 * Ausblenden wird gespeichert und ueberlebt eine vollstaendig neue Composition.
 *
 * Der urspruengliche Wert wird nach jedem Test wiederhergestellt, damit die
 * App-Einstellungen auf dem Geraet nicht dauerhaft veraendert werden.
 */
class RcsHintPersistenceTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val defaultAppText = context.getString(R.string.rcs_hint_default_app)
    private val dismissLabel = context.getString(R.string.rcs_hint_action_dismiss)

    private lateinit var prefs: SharedPreferencesManager
    private var originalValue = false

    @Before
    fun setUp() {
        prefs = AppContainer.requirePrefsManager()
        originalValue = prefs.isRcsHintDismissed()
        // Auslieferungszustand herstellen: Hinweis ist nicht ausgeblendet.
        // Ueber RcsHintVisibility, damit auch der beobachtbare Compose-Zustand mitzieht.
        RcsHintVisibility.setDismissed(prefs, false)
    }

    @After
    fun tearDown() {
        RcsHintVisibility.setDismissed(prefs, originalValue)
    }

    @Test
    fun freshInstallDoesNotHideTheHint() {
        assertFalse(
            "Der Auslieferungszustand muss 'nicht ausgeblendet' sein",
            prefs.isRcsHintDismissed()
        )
    }

    @Test
    fun dismissedFlagRoundTripsThroughEncryptedPreferences() {
        RcsHintVisibility.setDismissed(prefs, true)
        assertTrue(prefs.isRcsHintDismissed())
        assertTrue("Compose-Zustand muss der Persistenz folgen", RcsHintVisibility.isDismissed)

        RcsHintVisibility.setDismissed(prefs, false)
        assertFalse(prefs.isRcsHintDismissed())
        assertFalse(RcsHintVisibility.isDismissed)
    }

    @Test
    fun dismissingIsPersistedAndTheHintStaysGoneAfterRestart() {
        var restartKey by mutableIntStateOf(0)

        composeTestRule.setContent {
            MaterialTheme {
                // key() baut den Host vollstaendig neu auf und laesst ihn seine
                // remember-Zustaende neu aus den Preferences lesen - das entspricht
                // einem App-Neustart, ohne die App auf dem Geraet neu zu installieren.
                key(restartKey) {
                    RcsHintCardHost(
                        onNavigateToHelp = {},
                        detectState = { GoogleMessagesState.DEFAULT_SMS_APP }
                    )
                }
            }
        }

        // Erster Start: Hinweis sichtbar, dann ausblenden.
        composeTestRule.onNodeWithText(defaultAppText).assertIsDisplayed()
        composeTestRule.onNodeWithText(dismissLabel).performClick()
        composeTestRule.onNodeWithText(defaultAppText).assertDoesNotExist()
        assertTrue("Ausblenden muss persistiert werden", prefs.isRcsHintDismissed())

        // Neustart: Der Hinweis darf nicht wiederkommen.
        restartKey++
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(defaultAppText).assertDoesNotExist()
    }

    @Test
    fun settingsSwitchBringsTheHintBackWithoutRestart() {
        // Genau der Fall, fuer den der Schalter in den Einstellungen existiert: Die
        // Startseite liegt in einem Pager und wird nicht neu aufgebaut - der Hinweis muss
        // trotzdem sofort wieder erscheinen.
        composeTestRule.setContent {
            MaterialTheme {
                RcsHintCardHost(
                    onNavigateToHelp = {},
                    detectState = { GoogleMessagesState.DEFAULT_SMS_APP }
                )
            }
        }

        composeTestRule.onNodeWithText(defaultAppText).assertIsDisplayed()

        // Ausblenden (entspricht "Verstanden")
        RcsHintVisibility.setDismissed(prefs, true)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(defaultAppText).assertDoesNotExist()

        // Wieder einschalten (entspricht dem Schalter in den Einstellungen)
        RcsHintVisibility.setDismissed(prefs, false)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(defaultAppText).assertIsDisplayed()
    }

    @Test
    fun withoutGoogleMessagesTheHostShowsNothingEvenWhenNotDismissed() {
        composeTestRule.setContent {
            MaterialTheme {
                RcsHintCardHost(
                    onNavigateToHelp = {},
                    detectState = { GoogleMessagesState.NOT_INSTALLED }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(defaultAppText).assertDoesNotExist()
        composeTestRule.onNodeWithText(dismissLabel).assertDoesNotExist()
    }

    @Test
    fun learnMoreFromTheHostOpensTheHelpWithoutDismissing() {
        var learnMoreCalled = false

        composeTestRule.setContent {
            MaterialTheme {
                RcsHintCardHost(
                    onNavigateToHelp = { learnMoreCalled = true },
                    detectState = { GoogleMessagesState.DEFAULT_SMS_APP }
                )
            }
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.rcs_hint_action_learn_more))
            .performClick()

        assertTrue(learnMoreCalled)
        assertFalse("Mehr erfahren darf den Hinweis nicht ausblenden", prefs.isRcsHintDismissed())
    }
}
