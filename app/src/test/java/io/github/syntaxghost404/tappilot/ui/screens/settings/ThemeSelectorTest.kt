package io.github.syntaxghost404.tappilot.ui.screens.settings

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.syntaxghost404.tappilot.R
import io.github.syntaxghost404.tappilot.core.data.AppSettings
import io.github.syntaxghost404.tappilot.core.data.ThemeMode
import io.github.syntaxghost404.tappilot.screenshots.Fixtures
import io.github.syntaxghost404.tappilot.ui.theme.TapPilotTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/**
 * Each theme option shows its icon above its label, so labels get the whole width of their button
 * and are not cut short on a narrow phone such as the Redmi 13C.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-port-xhdpi")
class ThemeSelectorTest {
    @get:Rule
    val compose = createComposeRule()

    private fun text(id: Int): String = ApplicationProvider.getApplicationContext<Context>().getString(id)

    private val labels = listOf(R.string.theme_system, R.string.theme_light, R.string.theme_dark)

    private fun show() {
        compose.setContent {
            TapPilotTheme(themeMode = ThemeMode.Dark, dynamicColor = false) {
                SettingsScreen(AppSettings(onboardingDone = true), Fixtures.NoSettings, remember { SnackbarHostState() })
            }
        }
        compose.waitForIdle()
    }

    private fun assertLabelsWhole() {
        for (id in labels) {
            val label = compose.onNodeWithText(text(id), useUnmergedTree = true).fetchSemanticsNode()
            val layouts = mutableListOf<TextLayoutResult>()
            checkNotNull(label.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action).invoke(layouts)
            val layout = layouts.single()
            val whole = layout.multiParagraph.intrinsics.maxIntrinsicWidth
            assertFalse("${text(id)} is cut short", layout.isLineEllipsized(0))
            assertTrue("${text(id)} needs $whole px but has ${layout.size.width}", whole <= layout.size.width + 0.5f)

            // The label sits under the icon, centred in its button.
            val button = compose.onNodeWithText(text(id)).fetchSemanticsNode().boundsInRoot
            val bounds = label.boundsInRoot
            val density = compose.density.density
            assertTrue("${text(id)} has no room above it for the icon", bounds.top - button.top >= 24.dp.value * density)
            assertTrue("${text(id)} is off centre", abs(bounds.center.x - button.center.x) <= density)
        }
    }

    @Test
    fun `theme labels fit under their icons`() {
        show()
        assertLabelsWhole()
    }

    @Test
    @Config(fontScale = 1.3f)
    fun `theme labels fit with larger text`() {
        show()
        assertLabelsWhole()
    }
}
