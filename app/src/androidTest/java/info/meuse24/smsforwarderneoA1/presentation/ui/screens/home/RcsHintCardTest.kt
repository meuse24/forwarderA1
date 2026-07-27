package info.meuse24.smsforwarderneoA1.presentation.ui.screens.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import info.meuse24.smsforwarderneoA1.R
import info.meuse24.smsforwarderneoA1.domain.model.GoogleMessagesState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RcsHintCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val defaultAppText = context.getString(R.string.rcs_hint_default_app)
    private val notDefaultText = context.getString(R.string.rcs_hint_installed_not_default)
    private val dismissLabel = context.getString(R.string.rcs_hint_action_dismiss)
    private val learnMoreLabel = context.getString(R.string.rcs_hint_action_learn_more)

    private fun setCard(
        state: GoogleMessagesState,
        onLearnMore: () -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            MaterialTheme {
                RcsHintCard(state = state, onLearnMore = onLearnMore, onDismiss = onDismiss)
            }
        }
    }

    @Test
    fun defaultSmsAppShowsTheRcsHint() {
        setCard(GoogleMessagesState.DEFAULT_SMS_APP)

        composeTestRule.onNodeWithText(defaultAppText).assertIsDisplayed()
        composeTestRule.onNodeWithText(notDefaultText).assertDoesNotExist()
    }

    @Test
    fun installedButNotDefaultShowsTheRegistrationHint() {
        setCard(GoogleMessagesState.INSTALLED_NOT_DEFAULT)

        composeTestRule.onNodeWithText(notDefaultText).assertIsDisplayed()
        composeTestRule.onNodeWithText(defaultAppText).assertDoesNotExist()
    }

    @Test
    fun withoutGoogleMessagesNothingIsShown() {
        setCard(GoogleMessagesState.NOT_INSTALLED)

        composeTestRule.onNodeWithText(defaultAppText).assertDoesNotExist()
        composeTestRule.onNodeWithText(notDefaultText).assertDoesNotExist()
        composeTestRule.onNodeWithText(dismissLabel).assertDoesNotExist()
    }

    @Test
    fun dismissTriggersTheCallbackExactlyOnce() {
        var dismissCount = 0
        setCard(GoogleMessagesState.DEFAULT_SMS_APP, onDismiss = { dismissCount++ })

        composeTestRule.onNodeWithText(dismissLabel).performClick()

        assertEquals(1, dismissCount)
    }

    @Test
    fun learnMoreNavigatesAndDoesNotDismiss() {
        var learnMoreCalled = false
        var dismissed = false
        setCard(
            GoogleMessagesState.DEFAULT_SMS_APP,
            onLearnMore = { learnMoreCalled = true },
            onDismiss = { dismissed = true }
        )

        composeTestRule.onNodeWithText(learnMoreLabel).performClick()

        assertTrue(learnMoreCalled)
        assertFalse(dismissed)
    }
}
