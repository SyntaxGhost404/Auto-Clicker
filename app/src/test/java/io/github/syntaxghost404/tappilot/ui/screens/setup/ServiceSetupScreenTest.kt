package io.github.syntaxghost404.tappilot.ui.screens.setup

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.syntaxghost404.tappilot.R
import io.github.syntaxghost404.tappilot.core.data.ThemeMode
import io.github.syntaxghost404.tappilot.ui.ServiceState
import io.github.syntaxghost404.tappilot.ui.theme.TapPilotTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The disclosure is shown until the user agrees to it, once. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h1600dp-xhdpi")
class ServiceSetupScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val calls = mutableListOf<String>()

    private val actions = object : ServiceSetupActions {
        override fun onBack() = record("back")
        override fun onAgree() = record("agree")
        override fun onOpenAccessibility() = record("open-accessibility")
        override fun onOpenAppInfo() = record("app-info")
        override fun onDone() = record("done")
        override fun onNotNow() = record("not-now")
    }

    private fun record(call: String) {
        calls += call
    }

    private fun show(service: ServiceState, consented: Boolean) {
        compose.setContent {
            TapPilotTheme(themeMode = ThemeMode.Light, dynamicColor = false) {
                ServiceSetupScreen(service, actions, consented)
            }
        }
        compose.waitForIdle()
    }

    private fun text(id: Int): String = ApplicationProvider.getApplicationContext<Context>().getString(id)

    @Test
    fun `without consent the disclosure comes first, and agreeing is recorded`() {
        show(ServiceState.Off, consented = false)
        compose.onNodeWithText(text(R.string.disclosure_title)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.action_open_settings)).assertDoesNotExist()

        compose.onNodeWithText(text(R.string.action_agree_continue)).performClick()
        compose.waitForIdle()
        assertEquals(listOf("agree"), calls)
        compose.onNodeWithText(text(R.string.setup_title)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.action_open_settings)).assertIsDisplayed().performClick()
        assertEquals(listOf("agree", "open-accessibility"), calls)
    }

    @Test
    fun `with consent given before, the steps come straight away`() {
        show(ServiceState.Off, consented = true)
        compose.onNodeWithText(text(R.string.action_agree_continue)).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.disclosure_title)).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.setup_title)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.setup_waiting)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.setup_step_enable)).assertIsDisplayed()
        assertEquals(emptyList<String>(), calls)
    }

    @Test
    fun `a service that is on but not responding is turned off and on again`() {
        show(ServiceState.Stuck, consented = true)
        compose.onNodeWithText(text(R.string.setup_title_restart)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.setup_stuck)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.setup_step_restart)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.setup_step_enable)).assertDoesNotExist()
    }
}
