package io.github.syntaxghost404.tappilot.ui.screens.settings

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.syntaxghost404.tappilot.R
import io.github.syntaxghost404.tappilot.core.data.AppSettings
import io.github.syntaxghost404.tappilot.core.data.ControlSize
import io.github.syntaxghost404.tappilot.core.data.ThemeMode
import io.github.syntaxghost404.tappilot.ui.navigation.TopLevelRoute
import io.github.syntaxghost404.tappilot.ui.theme.TapPilotTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The floating controls settings lock only while a run is tapping. The screen is tall so that every
 * row clicked here sits clear of the navigation bar.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h1280dp-xhdpi")
class SettingsLockTest {
    @get:Rule
    val compose = createComposeRule()

    private val calls = mutableListOf<String>()
    private var runActive by mutableStateOf(false)

    private val actions = object : SettingsActions {
        override fun onSelectTab(tab: TopLevelRoute) = record("tab")
        override fun onTheme(mode: ThemeMode) = record("theme")
        override fun onDynamicColor(enabled: Boolean) = record("dynamic-color")
        override fun onControlSize(size: ControlSize) = record("control-size:$size")
        override fun onMarkerSize(dp: Int) = record("target-size")
        override fun onTapFeedback(enabled: Boolean) = record("tap-ripples")
        override fun onKeepScreenOn(enabled: Boolean) = record("keep-screen-on")
        override fun onStopOnScreenOff(enabled: Boolean) = record("stop-on-screen-off")
        override fun onDefaultDelay(ms: Long) = record("delay")
        override fun onDefaultHold(ms: Long) = record("hold")
        override fun onDefaultSwipe(ms: Long) = record("swipe")
        override fun onStopRun() = record("stop-run")
        override fun onExportAll() = record("export")
        override fun onImport() = record("import")
        override fun onHowTo() = record("how-to")
        override fun onTroubleshooting() = record("troubleshooting")
        override fun onAbout() = record("about")
    }

    private fun record(call: String) {
        calls += call
    }

    private fun show(running: Boolean) {
        runActive = running
        compose.setContent {
            TapPilotTheme(themeMode = ThemeMode.Light, dynamicColor = false) {
                SettingsScreen(AppSettings(onboardingDone = true), actions, remember { SnackbarHostState() }, runActive = runActive)
            }
        }
    }

    private fun text(id: Int): String = ApplicationProvider.getApplicationContext<Context>().getString(id)

    /** Scrolls the node matching [matcher] into view. */
    private fun find(matcher: SemanticsMatcher): SemanticsNodeInteraction {
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(matcher)
        return compose.onNode(matcher)
    }

    private fun node(id: Int): SemanticsNodeInteraction = find(hasText(text(id)))

    /** Asserts the enabled state of every control in the floating controls section. */
    private fun assertFloatingControls(enabled: Boolean) {
        val controls = listOf(
            hasText(text(R.string.size_compact)),
            hasText(text(R.string.size_regular)),
            hasText(text(R.string.size_large)),
            SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress),
            hasText(text(R.string.settings_tap_feedback)),
            hasText(text(R.string.settings_keep_screen_on)),
        )
        controls.forEach { matcher ->
            val control = find(matcher)
            if (enabled) control.assertIsEnabled() else control.assertIsNotEnabled()
        }
    }

    @Test
    fun `a running tap locks every floating controls setting`() {
        show(running = true)

        node(R.string.settings_controls_locked_title).assertIsDisplayed()
        assertFloatingControls(enabled = false)
        node(R.string.size_large).performClick()
        node(R.string.settings_tap_feedback).performClick()
        node(R.string.settings_keep_screen_on).performClick()
        assertTrue(calls.toString(), calls.isEmpty())
    }

    @Test
    fun `settings outside the floating controls stay available during a run`() {
        show(running = true)

        node(R.string.theme_dark).assertIsEnabled().performClick()
        node(R.string.settings_stop_screen_off).assertIsEnabled().performClick()
        assertEquals(listOf("theme", "stop-on-screen-off"), calls)
    }

    @Test
    fun `stopping the run unlocks the settings`() {
        show(running = true)

        node(R.string.action_stop).performClick()
        assertEquals(listOf("stop-run"), calls)

        runActive = false
        compose.waitForIdle()
        compose.onNodeWithText(text(R.string.settings_controls_locked_title)).assertDoesNotExist()
        assertFloatingControls(enabled = true)
        node(R.string.size_large).performClick()
        assertEquals(listOf("stop-run", "control-size:Large"), calls)
    }

    @Test
    fun `idle floating controls leave every setting enabled`() {
        show(running = false)

        compose.onNodeWithText(text(R.string.settings_controls_locked_title)).assertDoesNotExist()
        assertFloatingControls(enabled = true)
        node(R.string.size_large).performClick()
        node(R.string.settings_tap_feedback).performClick()
        node(R.string.settings_keep_screen_on).performClick()
        assertEquals(listOf("control-size:Large", "tap-ripples", "keep-screen-on"), calls)
    }
}
