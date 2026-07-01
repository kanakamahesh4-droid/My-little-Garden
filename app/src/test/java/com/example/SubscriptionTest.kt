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
class SubscriptionTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun test_subscription_ui_and_payment_methods() {
        // Start the application
        composeTestRule.setContent {
            MyApplicationTheme {
                GardenApp()
            }
        }

        // Wait until either the login screen is displayed or the main screen is displayed
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag("google_login_button").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithTag("subscription_menu_button").fetchSemanticsNodes().isNotEmpty()
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

        // Wait for the subscription menu button to appear (handles database/state load lag)
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag("subscription_menu_button").fetchSemanticsNodes().isNotEmpty()
        }

        // 1. Assert that the premium subscription header button exists on the garden screen
        composeTestRule.onNodeWithTag("subscription_menu_button").assertExists()

        // 2. Click to open the subscription screen modal
        composeTestRule.onNodeWithTag("subscription_menu_button").performClick()

        // Wait for the subscription dialog / close button to appear
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("close_subscription_dialog").fetchSemanticsNodes().isNotEmpty()
        }

        // 3. Verify plan duration cards are present
        composeTestRule.onNodeWithTag("plan_card_DAILY").assertExists()
        composeTestRule.onNodeWithTag("plan_card_WEEKLY").assertExists()
        composeTestRule.onNodeWithTag("plan_card_YEARLY").assertExists()

        // 4. Verify payment method tabs are present
        composeTestRule.onNodeWithTag("method_play").assertExists()
        composeTestRule.onNodeWithTag("method_upi").assertExists()

        // 5. Select Weekly plan
        composeTestRule.onNodeWithTag("plan_card_WEEKLY").performClick()

        // 6. Test Google Play payment button
        composeTestRule.onNodeWithTag("pay_play_button").assertExists()

        // 7. Toggle to UPI payment method
        composeTestRule.onNodeWithTag("method_upi").performClick()

        // Wait for UPI specific details to appear
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("upi_qr_code").fetchSemanticsNodes().isNotEmpty()
        }

        // 8. Verify UPI specific details are visible (QR code canvas, copy button, txn input field)
        composeTestRule.onNodeWithTag("upi_qr_code").assertExists()
        composeTestRule.onNodeWithTag("upi_txn_input").assertExists()
        composeTestRule.onNodeWithTag("submit_upi_button").assertExists()

        // 9. Input a test UPI Transaction UTR ID
        composeTestRule.onNodeWithTag("upi_txn_input").performTextInput("123456789012")

        // 10. Click Verify UTR
        composeTestRule.onNodeWithTag("submit_upi_button").performClick()
        composeTestRule.mainClock.advanceTimeBy(3000L)
        composeTestRule.waitForIdle()

        // 11. Capture screenshot of the subscription modal
        composeTestRule.onNodeWithTag("close_subscription_dialog").performScrollTo()
        composeTestRule.onNodeWithTag("close_subscription_dialog").captureRoboImage(filePath = "src/test/screenshots/subscription_modal.png")

        // 12. Dismiss the modal by backing out
        composeTestRule.onNodeWithTag("close_subscription_dialog").performScrollTo().performClick()
        repeat(5) {
            composeTestRule.mainClock.advanceTimeByFrame()
            composeTestRule.waitForIdle()
        }

        // 13. Confirm subscription screen is closed
        composeTestRule.onNodeWithTag("close_subscription_dialog").assertDoesNotExist()
    }
}
