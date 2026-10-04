package io.github.syntaxghost404.tappilot.ui

import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.syntaxghost404.tappilot.R
import io.github.syntaxghost404.tappilot.core.data.AppSettings
import io.github.syntaxghost404.tappilot.core.data.ThemeMode
import io.github.syntaxghost404.tappilot.screenshots.Fixtures
import io.github.syntaxghost404.tappilot.ui.components.ConnectedChoiceGroup
import io.github.syntaxghost404.tappilot.ui.components.SwitchRow
import io.github.syntaxghost404.tappilot.ui.screens.settings.SettingsActions
import io.github.syntaxghost404.tappilot.ui.screens.settings.SettingsScreen
import io.github.syntaxghost404.tappilot.ui.theme.TapPilotTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val NOTHING = -1

/** Haptics follow the user's setting, and each kind of press has its own feel. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class HapticsTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var view: View
    private var none = 0

    /** The last haptic played on the content's view or any view above it, which Compose may use. */
    private val lastHaptic: Int
        get() = generateSequence(view) { it.parent as? View }
            .map { shadowOf(it).lastHapticFeedbackPerformed() }
            .firstOrNull { it != NOTHING } ?: NOTHING

    private fun show(enabled: Boolean, content: @Composable () -> Unit) {
        compose.setContent {
            view = LocalView.current
            TapPilotTheme(themeMode = ThemeMode.Light, dynamicColor = false) {
                ProvideHaptics(enabled, content)
            }
        }
        compose.waitForIdle()
        none = lastHaptic
    }

    @Test
    fun `switches feel like a switch`() {
        var checked by mutableStateOf(false)
        show(enabled = true) { SwitchRow(title = "Option", checked = checked, onCheckedChange = { checked = it }) }

        compose.onNodeWithText("Option").performClick()
        assertEquals(HapticFeedbackConstants.TOGGLE_ON, lastHaptic)
        compose.onNodeWithText("Option").performClick()
        assertEquals(HapticFeedbackConstants.TOGGLE_OFF, lastHaptic)
    }

    @Test
    fun `a new choice ticks, choosing the current one again does not`() {
        var selected by mutableStateOf("A")
        show(enabled = true) {
            ConnectedChoiceGroup(options = listOf("A", "B"), selected = selected, onSelect = { selected = it }, label = { it })
        }

        compose.onNodeWithText("A").performClick()
        assertEquals(none, lastHaptic)
        compose.onNodeWithText("B").performClick()
        assertEquals(HapticFeedbackConstants.SEGMENT_TICK, lastHaptic)
    }

    @Test
    fun `a plain press clicks`() {
        var pressed = 0
        show(enabled = true) {
            val onClick = withHaptic { pressed++ }
            Box(Modifier.size(48.dp).testTag("button").combinedClickable(onClick = onClick))
        }

        compose.onNodeWithTag("button").performClick()
        assertEquals(1, pressed)
        assertEquals(HapticFeedbackConstants.CONTEXT_CLICK, lastHaptic)
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun `with haptics off nothing vibrates, not even a long press`() {
        var checked by mutableStateOf(false)
        var longPressed = false
        show(enabled = false) {
            Column {
                SwitchRow(title = "Option", checked = checked, onCheckedChange = { checked = it })
                Box(Modifier.size(48.dp).testTag("item").combinedClickable(onLongClick = { longPressed = true }) {})
                Text("end")
            }
        }

        compose.onNodeWithText("Option").performClick()
        compose.onNodeWithTag("item").performTouchInput { longClick() }
        assertEquals(true, checked)
        assertEquals(true, longPressed)
        assertEquals(none, lastHaptic)
    }

    @Test
    @Config(qualifiers = "w360dp-h1280dp-xhdpi")
    fun `turning haptics on is felt, turning them off is not`() {
        var settings by mutableStateOf(AppSettings(onboardingDone = true, hapticFeedback = false))
        val actions = object : SettingsActions by Fixtures.NoSettings {
            override fun onHapticFeedback(enabled: Boolean) {
                settings = settings.copy(hapticFeedback = enabled)
            }
        }
        compose.setContent {
            view = LocalView.current
            TapPilotTheme(themeMode = ThemeMode.Light, dynamicColor = false) {
                ProvideHaptics(settings.hapticFeedback) {
                    SettingsScreen(settings, actions, remember { SnackbarHostState() })
                }
            }
        }
        compose.waitForIdle()
        val toggle = compose.onNodeWithText(ApplicationProvider.getApplicationContext<Context>().getString(R.string.settings_haptic_feedback))

        toggle.performClick()
        assertTrue(settings.hapticFeedback)
        assertEquals(HapticFeedbackConstants.TOGGLE_ON, lastHaptic)

        toggle.performClick()
        assertFalse(settings.hapticFeedback)
        assertEquals(HapticFeedbackConstants.TOGGLE_ON, lastHaptic)
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun `with haptics on a long press is felt`() {
        show(enabled = true) {
            Box(Modifier.size(48.dp).testTag("item").combinedClickable(onLongClick = {}) {})
        }

        compose.onNodeWithTag("item").performTouchInput { longClick() }
        assertEquals(HapticFeedbackConstants.LONG_PRESS, lastHaptic)
    }
}
