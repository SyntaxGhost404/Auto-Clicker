package io.github.syntaxghost404.tappilot.ui.screens.home

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.syntaxghost404.tappilot.R
import io.github.syntaxghost404.tappilot.core.data.ThemeMode
import io.github.syntaxghost404.tappilot.screenshots.Fixtures
import io.github.syntaxghost404.tappilot.service.OverlayStatus
import io.github.syntaxghost404.tappilot.ui.ServiceState
import io.github.syntaxghost404.tappilot.ui.navigation.TopLevelRoute
import io.github.syntaxghost404.tappilot.ui.theme.TapPilotTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The Home card says what is wrong with the service, and offers the way to fix that. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-xhdpi")
class HomeServiceCardTest {
    @get:Rule
    val compose = createComposeRule()

    private val calls = mutableListOf<String>()
    private var state by mutableStateOf(home(ServiceState.Connected))

    private val actions = object : HomeActions {
        override fun onSelectTab(tab: TopLevelRoute) = record("tab")
        override fun onTurnOn() = record("turn-on")
        override fun onOpenAccessibility() = record("open-accessibility")
        override fun onTroubleshoot() = record("troubleshoot")
        override fun onStartSingle() = record("start-single")
        override fun onSingleSettings() = record("single-settings")
        override fun onNewSequence() = record("new-sequence")
        override fun onOpenLibrary() = record("library")
        override fun onOpenSequence(id: String) = record("open-sequence")
        override fun onStartSequence(id: String) = record("start-sequence")
        override fun onStop() = record("stop")
        override fun onHideControls() = record("hide-controls")
    }

    private fun record(call: String) {
        calls += call
    }

    private fun home(service: ServiceState, setUp: Boolean = false, consented: Boolean = false) =
        HomeState(service, OverlayStatus.Hidden, Fixtures.quick, emptyList(), serviceSetUp = setUp, consented = consented)

    private fun show(initial: HomeState) {
        state = initial
        compose.setContent {
            TapPilotTheme(themeMode = ThemeMode.Light, dynamicColor = false) {
                HomeScreen(state, actions)
            }
        }
        compose.waitForIdle()
    }

    private fun text(id: Int): String = ApplicationProvider.getApplicationContext<Context>().getString(id)

    private val cardTitles = listOf(R.string.home_service_off_title, R.string.home_service_turned_off_title, R.string.home_service_stuck_title)

    /** Asserts that the only service card on screen is the one titled [title], or none at all. */
    private fun assertCard(title: Int?) {
        for (id in cardTitles) {
            val node = compose.onNodeWithText(text(id))
            if (id == title) node.assertIsDisplayed() else node.assertDoesNotExist()
        }
    }

    @Test
    fun `a working service needs no card`() {
        show(home(ServiceState.Connected, setUp = true, consented = true))
        assertCard(null)
        compose.onNodeWithText(text(R.string.home_status_ready)).assertIsDisplayed()
    }

    @Test
    fun `a service that is still starting gets no card, only a status line`() {
        show(home(ServiceState.Starting, setUp = true, consented = true))
        assertCard(null)
        compose.onNodeWithText(text(R.string.home_status_starting)).assertIsDisplayed()
    }

    @Test
    fun `the first time, the card leads to setup`() {
        show(home(ServiceState.Off))
        assertCard(R.string.home_service_off_title)
        compose.onNodeWithText(text(R.string.home_service_off_body)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.home_status_off)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.action_troubleshoot)).assertDoesNotExist()

        compose.onNodeWithText(text(R.string.action_turn_on)).performClick()
        assertEquals(listOf("turn-on"), calls)
    }

    @Test
    fun `a service that was turned off goes straight back to settings once the user has agreed`() {
        show(home(ServiceState.Off, setUp = true, consented = true))
        assertCard(R.string.home_service_turned_off_title)
        compose.onNodeWithText(text(R.string.home_service_turned_off_body)).assertIsDisplayed()

        compose.onNodeWithText(text(R.string.action_turn_on_again)).performClick()
        compose.onNodeWithText(text(R.string.action_troubleshoot)).performClick()
        assertEquals(listOf("open-accessibility", "troubleshoot"), calls)
    }

    @Test
    fun `a service that was turned off shows the disclosure first if the user never agreed`() {
        show(home(ServiceState.Off, setUp = true, consented = false))
        assertCard(R.string.home_service_turned_off_title)

        compose.onNodeWithText(text(R.string.action_turn_on_again)).performClick()
        assertEquals(listOf("turn-on"), calls)
    }

    @Test
    fun `a service that is not responding is restarted in settings`() {
        show(home(ServiceState.Stuck, setUp = true, consented = false))
        assertCard(R.string.home_service_stuck_title)
        compose.onNodeWithText(text(R.string.home_service_stuck_body)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.home_status_stuck)).assertIsDisplayed()

        // It is on already, so there is nothing to agree to before restarting it.
        compose.onNodeWithText(text(R.string.action_open_settings)).performClick()
        compose.onNodeWithText(text(R.string.action_troubleshoot)).performClick()
        assertEquals(listOf("open-accessibility", "troubleshoot"), calls)
    }

    @Test
    fun `the card follows the service as it changes`() {
        show(home(ServiceState.Starting, setUp = true))
        assertCard(null)

        state = home(ServiceState.Stuck, setUp = true)
        compose.waitForIdle()
        assertCard(R.string.home_service_stuck_title)

        state = home(ServiceState.Off, setUp = true)
        compose.waitForIdle()
        assertCard(R.string.home_service_turned_off_title)

        state = home(ServiceState.Connected, setUp = true)
        compose.waitForIdle()
        assertCard(null)
    }
}
