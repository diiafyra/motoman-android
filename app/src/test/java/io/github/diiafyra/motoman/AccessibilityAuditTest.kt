package io.github.diiafyra.motoman

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.math.roundToInt

/**
 * Two things TalkBack and motor accessibility depend on, checked on every screen of the main flow:
 *
 * 1. Everything that can be clicked has a name TalkBack can read (text or a content description).
 * 2. Everything that can be clicked is at least 48 dp in both directions.
 *
 * Android's own accessibility test framework did not catch either violation when I tried it on
 * Robolectric (I removed a label on purpose and it still passed), so these checks are written directly
 * against the semantics tree. A failure lists the offending elements and the screen.
 */
@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class, qualifiers = "w411dp-h891dp-xxhdpi")
class AccessibilityAuditTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun everyClickableThingOnEveryScreenHasANameAndASufficientTouchTarget() {
        compose.waitUntilExactlyOneExists(hasText("Add your first vehicle"), TIMEOUT_MS)
        audit("no vehicle yet")
        compose.onNodeWithText("Add vehicle").performClick()

        compose.waitUntilExactlyOneExists(hasText("New vehicle"), TIMEOUT_MS)
        audit("vehicle form")
        compose.onNodeWithText("Save").performScrollTo().performClick()
        audit("vehicle form with errors")
        compose.onNodeWithText("Name").performTextInput("Wave Alpha")
        compose.onNodeWithText("Odometer now").performTextInput("12000")
        compose.onNodeWithText("Save").performScrollTo().performClick()

        compose.waitUntilExactlyOneExists(hasText("No fill-ups yet"), TIMEOUT_MS)
        audit("overview")
        compose.onNode(hasText("Maintenance") and hasClickAction()).performClick()
        compose.waitUntilExactlyOneExists(hasText("Oil change"), TIMEOUT_MS)
        audit("maintenance")
        compose.onNode(hasText("Fuel") and hasClickAction()).performClick()
        compose.waitUntilExactlyOneExists(hasText("Log fill-up"), TIMEOUT_MS)
        audit("fuel, empty")
        compose.onNodeWithText("Log fill-up").performClick()
        compose.waitUntilExactlyOneExists(hasText("New fill-up"), TIMEOUT_MS)
        audit("fill-up form")
        compose.onNode(hasContentDescription("Back")).performClick()

        compose.onNode(hasContentDescription("Settings")).performClick()
        compose.waitUntilExactlyOneExists(hasText("Language"), TIMEOUT_MS)
        audit("settings")
    }

    private fun audit(screen: String) {
        val minPx = (MIN_TOUCH_TARGET_DP * compose.density.density).roundToInt()
        val problems = mutableListOf<String>()
        compose.onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach { node ->
            val config = node.config
            val hasName =
                config.getOrNull(SemanticsProperties.Text)?.any { it.text.isNotBlank() } == true ||
                    !config.getOrNull(SemanticsProperties.ContentDescription).isNullOrEmpty()
            if (!hasName) problems += "no name at ${node.boundsInRoot}"
            // Material buttons draw 40 dp tall but accept touches in 48 dp: touchBounds is what counts.
            val touch = node.touchBoundsInRoot
            if (touch.width < minPx || touch.height < minPx) {
                problems +=
                    "touch target ${touch.width.roundToInt()}x${touch.height.roundToInt()}px is under 48 dp at $touch"
            }
        }
        assertTrue("$screen: ${problems.joinToString("; ")}", problems.isEmpty())
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
        const val MIN_TOUCH_TARGET_DP = 48
    }
}
