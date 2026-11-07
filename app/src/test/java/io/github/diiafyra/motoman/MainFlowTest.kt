package io.github.diiafyra.motoman

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The flow every new user goes through: add a vehicle, then log a fill-up. Runs the real app (Room,
 * DataStore, navigation, Hilt) on the JVM with Robolectric, so CI needs no emulator.
 */
@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
// A phone-sized screen, so forms fit the way they do on a device.
@Config(application = HiltTestApplication::class, qualifiers = "w411dp-h891dp-xxhdpi")
class MainFlowTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun addAVehicleThenLogAFillUp() {
        // No vehicle yet: every tab offers to add one.
        compose.waitUntilExactlyOneExists(hasText("Add your first vehicle"), TIMEOUT_MS)
        compose.onNodeWithText("Add vehicle").performClick()

        // Saving the empty form shows errors instead of saving.
        compose.waitUntilExactlyOneExists(hasText("New vehicle"), TIMEOUT_MS)
        compose.onNodeWithText("Save").performScrollTo().performClick()
        compose.onNodeWithText("Enter a name").assertExists()

        compose.onNodeWithText("Name").performTextInput("Wave Alpha")
        compose.onNodeWithText("Odometer now").performTextInput("12000")
        compose.onNodeWithText("Save").performScrollTo().performClick()

        // Back on Overview, with the new vehicle selected.
        compose.waitUntilExactlyOneExists(hasContentDescription("Switch vehicle, Wave Alpha selected"), TIMEOUT_MS)
        compose.waitUntilExactlyOneExists(hasText("No fill-ups yet"), TIMEOUT_MS)

        compose.onNodeWithText("Fuel").performClick()
        compose.waitUntilExactlyOneExists(hasText("Log fill-up"), TIMEOUT_MS)
        compose.onNodeWithText("Log fill-up").performClick()

        compose.waitUntilExactlyOneExists(hasText("Previous reading: 12,000 km"), TIMEOUT_MS)
        compose.onNodeWithText("Odometer").performTextInput("12150")
        compose.onNodeWithText("Volume").performTextInput("3,5")
        compose.onNodeWithText("Total paid").performTextInput("87500")
        compose.onNodeWithText("25,000 ₫ per litre").assertExists()
        compose.onNodeWithText("Save").performScrollTo().performClick()

        // The fill-up is listed under its month.
        compose.waitUntilExactlyOneExists(hasText("3.5 L · 87,500 ₫"), TIMEOUT_MS)
        compose.onNodeWithContentDescription("Switch vehicle, Wave Alpha selected").assertExists()
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}
