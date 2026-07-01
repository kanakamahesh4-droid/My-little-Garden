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
class HouseholdFertilizersTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun test_fertilizers_search_and_display() {
        // Start the application
        composeTestRule.setContent {
            MyApplicationTheme {
                GardenApp()
            }
        }

        // Wait until either the login screen is displayed or the main screen is displayed
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag("google_login_button").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithTag("easy_fertilizers_dashboard_card").fetchSemanticsNodes().isNotEmpty()
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

        // Wait for the fertilizers card to appear (handles database/state load lag)
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag("easy_fertilizers_dashboard_card").fetchSemanticsNodes().isNotEmpty()
        }

        // 1. Assert that the "Easy Household Fertilizers" dashboard card exists
        composeTestRule.onNodeWithTag("easy_fertilizers_dashboard_card").assertExists()

        // Turn off autoAdvance to freeze the clock/animations and prevent idling timeouts
        composeTestRule.mainClock.autoAdvance = false

        // 2. Click the card to open the dialog
        composeTestRule.onNodeWithTag("easy_fertilizers_dashboard_card").performClick()

        // Wait for dialog input to appear by manually advancing the clock
        composeTestRule.mainClock.advanceTimeBy(1000L)

        // 3. Assert that the dialog and search input exist
        composeTestRule.onNodeWithTag("fertilizers_search_input").assertExists()

        // 4. Search for "Banana"
        composeTestRule.onNodeWithTag("fertilizers_search_input").performTextInput("Banana")
        composeTestRule.mainClock.advanceTimeBy(1000L)

        // 5. Assert that the Banana remedy card is visible
        composeTestRule.onNodeWithText("Bananas").assertExists()

        // 6. Expand the Bananas remedy card
        composeTestRule.onNodeWithText("Bananas").performClick()
        composeTestRule.mainClock.advanceTimeBy(1000L)

        // 7. Assert that details (like "BEST FOR") are displayed
        composeTestRule.onNodeWithText("BEST FOR").assertExists()
        
        // 8. Capture a beautiful screenshot of our dialog for record-keeping
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/easy_fertilizers_dialog.png")
    }
}
