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
class WeeklyRemindersTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun test_reminders_card_existence_and_toggle() {
        // Start the application
        composeTestRule.setContent {
            MyApplicationTheme {
                GardenApp()
            }
        }

        // Wait until either the login screen is displayed or the main screen is displayed
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag("google_login_button").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithTag("weekly_reminder_card").fetchSemanticsNodes().isNotEmpty()
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

        // Wait for the weekly reminder card to appear (handles database seeding/state load lag)
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag("weekly_reminder_card").fetchSemanticsNodes().isNotEmpty()
        }

        // 1. Assert that the Weekly Reminder card exists in MyGardenScreen
        composeTestRule.onNodeWithTag("weekly_reminder_card").assertExists()

        // Turn off autoAdvance to freeze the clock/animations and prevent idling timeouts
        composeTestRule.mainClock.autoAdvance = false

        // 2. Assert that toggle switch exists
        composeTestRule.onNodeWithTag("reminder_toggle_switch").assertExists()

        // 3. Click the toggle switch to expand reminder settings
        composeTestRule.onNodeWithTag("reminder_toggle_switch").performClick()
        composeTestRule.mainClock.advanceTimeBy(1000L)

        // 4. Assert that reminder day options and schedule saving button exist
        composeTestRule.onNodeWithTag("reminder_day_chip_sun").assertExists()
        composeTestRule.onNodeWithTag("reminder_time_chip_9").assertExists()
        composeTestRule.onNodeWithTag("save_reminder_button").assertExists()
        composeTestRule.onNodeWithTag("send_test_reminder_button").assertExists()

        // 5. Select a day and save the reminder schedule
        composeTestRule.onNodeWithTag("reminder_day_chip_sat").performClick()
        composeTestRule.mainClock.advanceTimeBy(500L)
        composeTestRule.onNodeWithTag("save_reminder_button").performClick()
        composeTestRule.mainClock.advanceTimeBy(1000L)

        // 6. Capture a screenshot of the weekly reminder card
        composeTestRule.onNodeWithTag("weekly_reminder_card").captureRoboImage(filePath = "src/test/screenshots/weekly_reminder_card.png")
    }
}
