package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.GardenApp
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class FeedbackTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun test_feedback_flow_and_modal() {
        // Start the application
        composeTestRule.setContent {
            MyApplicationTheme {
                GardenApp()
            }
        }

        // Wait until either the login screen is displayed or the main screen is displayed
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag("google_login_button").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithTag("feedback_fab").fetchSemanticsNodes().isNotEmpty()
        }

        // Bypass login screen if displayed
        val isLoginScreen = composeTestRule.onAllNodesWithTag("google_login_button").fetchSemanticsNodes().isNotEmpty()
        if (isLoginScreen) {
            composeTestRule.onNodeWithTag("google_login_button").performClick()
            
            // Wait for account chooser to appear
            composeTestRule.waitUntil(timeoutMillis = 5000) {
                composeTestRule.onAllNodesWithTag("google_account_default").fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithTag("google_account_default").performClick()
        }

        // Wait for the feedback FAB to appear (handles database/state load lag)
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag("feedback_fab").fetchSemanticsNodes().isNotEmpty()
        }

        // 1. Assert that the feedback floating action button exists
        composeTestRule.onNodeWithTag("feedback_fab").assertExists()

        // Turn off autoAdvance to freeze the clock/animations and prevent idling timeouts
        composeTestRule.mainClock.autoAdvance = false

        // 2. Click the FAB to open the feedback panel modal
        composeTestRule.onNodeWithTag("feedback_fab").performClick()

        // Wait for modal dialog to appear by manually advancing the clock
        composeTestRule.mainClock.advanceTimeBy(1000L)

        // 3. Verify dialog and rating stars are displayed
        composeTestRule.onNodeWithTag("feedback_modal_dialog").assertExists()
        composeTestRule.onNodeWithTag("rating_star_1").assertExists()
        composeTestRule.onNodeWithTag("rating_star_3").assertExists()
        composeTestRule.onNodeWithTag("rating_star_5").assertExists()

        // 4. Verify text input and social links/email exist in modal
        composeTestRule.onNodeWithTag("feedback_text_input").assertExists()
        composeTestRule.onNodeWithTag("email_developer_button").assertExists()
        composeTestRule.onNodeWithTag("social_button_github").assertExists()
        composeTestRule.onNodeWithTag("social_button_linkedin").assertExists()
        composeTestRule.onNodeWithTag("social_button_twitter").assertExists()

        // 5. Select 4-star rating and enter feedback message
        composeTestRule.onNodeWithTag("rating_star_4").performClick()
        composeTestRule.mainClock.advanceTimeBy(100L)
        composeTestRule.onNodeWithTag("feedback_text_input").performTextInput("Love the app! The growth tracker charts look amazing.")
        composeTestRule.mainClock.advanceTimeBy(100L)

        // 6. Submit feedback
        composeTestRule.onNodeWithTag("submit_feedback_button").performClick()
        composeTestRule.mainClock.advanceTimeBy(1000L)

        // 7. Capture a screenshot of the feedback dialog
        composeTestRule.onNodeWithTag("feedback_modal_dialog").captureRoboImage(filePath = "src/test/screenshots/feedback_modal_dialog.png")

        // 8. Close the modal dialog
        composeTestRule.onNodeWithTag("close_feedback_modal_button").performClick()
        composeTestRule.mainClock.advanceTimeBy(1000L)

        // 9. Confirm modal is closed
        composeTestRule.onNodeWithTag("feedback_modal_dialog").assertDoesNotExist()
    }
}
