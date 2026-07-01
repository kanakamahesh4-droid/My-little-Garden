package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.PlantImageLightbox
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class LightboxTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun test_lightbox_renders_and_dismisses() {
        var isDismissed = false
        val testImageUrl = "https://images.unsplash.com/photo-1614594975525-e45190c55d0b?q=80&w=600"

        // Set content with the Lightbox Composable
        composeTestRule.setContent {
            MyApplicationTheme {
                PlantImageLightbox(
                    imageUrl = testImageUrl,
                    onDismiss = { isDismissed = true }
                )
            }
        }

        composeTestRule.waitForIdle()

        // 1. Assert that the lightbox overlay and controls exist
        composeTestRule.onNodeWithTag("plant_lightbox_overlay").assertExists()
        composeTestRule.onNodeWithTag("lightbox_image").assertExists()
        composeTestRule.onNodeWithTag("close_lightbox_button").assertExists()

        // 2. Capture a high-resolution screenshot of the premium lightbox design
        composeTestRule.onNodeWithTag("plant_lightbox_overlay").captureRoboImage(filePath = "src/test/screenshots/plant_image_lightbox.png")

        // 3. Click the close button
        composeTestRule.onNodeWithTag("close_lightbox_button").performClick()
        composeTestRule.waitForIdle()

        // 4. Assert that the dismiss callback was successfully triggered
        assertTrue(isDismissed)
    }
}
