package io.github.syntaxghost404.tappilot.screenshots

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.syntaxghost404.tappilot.appGraph
import io.github.syntaxghost404.tappilot.core.data.AppSettings
import io.github.syntaxghost404.tappilot.core.data.ControlSize
import io.github.syntaxghost404.tappilot.core.data.OverlayMode
import io.github.syntaxghost404.tappilot.core.data.ThemeMode
import io.github.syntaxghost404.tappilot.core.engine.RunProgress
import io.github.syntaxghost404.tappilot.core.model.Script
import io.github.syntaxghost404.tappilot.core.model.SwipeStep
import io.github.syntaxghost404.tappilot.core.model.TapStep
import io.github.syntaxghost404.tappilot.overlay.ControlsLayout
import io.github.syntaxghost404.tappilot.overlay.Handle
import io.github.syntaxghost404.tappilot.overlay.HandlePart
import io.github.syntaxghost404.tappilot.overlay.OverlayDialog
import io.github.syntaxghost404.tappilot.overlay.OverlaySession
import io.github.syntaxghost404.tappilot.overlay.ui.ControlActions
import io.github.syntaxghost404.tappilot.overlay.ui.ControlBar
import io.github.syntaxghost404.tappilot.overlay.ui.DialogActions
import io.github.syntaxghost404.tappilot.overlay.ui.MarkerState
import io.github.syntaxghost404.tappilot.overlay.ui.OverlayDialogHost
import io.github.syntaxghost404.tappilot.overlay.ui.PathLayer
import io.github.syntaxghost404.tappilot.overlay.ui.TargetMarker
import io.github.syntaxghost404.tappilot.service.OverlayStatus
import io.github.syntaxghost404.tappilot.ui.ServiceState
import io.github.syntaxghost404.tappilot.ui.screens.editor.EditorScreen
import io.github.syntaxghost404.tappilot.ui.screens.editor.EditorState
import io.github.syntaxghost404.tappilot.ui.screens.help.AboutScreen
import io.github.syntaxghost404.tappilot.ui.screens.help.HowToScreen
import io.github.syntaxghost404.tappilot.ui.screens.help.TroubleshootingScreen
import io.github.syntaxghost404.tappilot.ui.screens.help.TroubleshootingState
import io.github.syntaxghost404.tappilot.ui.screens.home.HomeScreen
import io.github.syntaxghost404.tappilot.ui.screens.home.HomeState
import io.github.syntaxghost404.tappilot.ui.screens.sequences.SequencesScreen
import io.github.syntaxghost404.tappilot.ui.screens.settings.SettingsScreen
import io.github.syntaxghost404.tappilot.ui.screens.setup.ServiceSetupScreen
import io.github.syntaxghost404.tappilot.ui.screens.setup.WelcomeScreen
import io.github.syntaxghost404.tappilot.ui.screens.single.SinglePointScreen
import io.github.syntaxghost404.tappilot.ui.theme.TapPilotTheme
import androidx.compose.material3.SnackbarHostState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.roundToInt

/** The Redmi 13C held sideways. */
private const val LANDSCAPE = "w800dp-h360dp-land-xhdpi"

/**
 * Renders every screen and the floating controls so the design can be reviewed without a device.
 * Run with `./gradlew recordRoborazziDebug`; images land in app/build/screenshots.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class ScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private fun snap(name: String, dark: Boolean = false, settleMs: Long = 2_000L, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            TapPilotTheme(themeMode = if (dark) ThemeMode.Dark else ThemeMode.Light, dynamicColor = false, content = content)
        }
        compose.mainClock.advanceTimeBy(settleMs)
        compose.onRoot().captureRoboImage("build/screenshots/$name.png")
    }

    private val snackbar = SnackbarHostState()

    @Test fun welcome() = snap("01_welcome") { WelcomeScreen(onGetStarted = {}) }

    @Test fun disclosure() = snap("02_setup_disclosure") { ServiceSetupScreen(ServiceState.Off, Fixtures.NoSetup) }

    @Test fun setupDone() = snap("03_setup_connected_dark", dark = true) { ServiceSetupScreen(ServiceState.Connected, Fixtures.NoSetup) }

    @Test fun homeServiceOff() = snap("04_home_service_off") {
        HomeScreen(HomeState(ServiceState.Off, OverlayStatus.Hidden, Fixtures.quick, emptyList()), Fixtures.NoHome)
    }

    @Test fun home() = snap("05_home") {
        HomeScreen(HomeState(ServiceState.Connected, OverlayStatus.Hidden, Fixtures.quick, Fixtures.library.take(3)), Fixtures.NoHome)
    }

    @Test fun homeDark() = snap("06_home_dark", dark = true) {
        HomeScreen(HomeState(ServiceState.Connected, OverlayStatus.Hidden, Fixtures.quick, Fixtures.library.take(3)), Fixtures.NoHome)
    }

    @Test fun homeRunning() = snap("07_home_running_dark", dark = true) {
        val status = OverlayStatus.Visible(
            mode = OverlayMode.Multi,
            scriptId = "daily",
            scriptName = "Daily reward",
            running = true,
            progress = RunProgress(cycles = 12, actions = 48, elapsedMs = 95_000L, fraction = 0.24f),
        )
        HomeScreen(HomeState(ServiceState.Connected, status, Fixtures.quick, Fixtures.library.take(2)), Fixtures.NoHome)
    }

    @Test fun sequences() = snap("08_sequences") { SequencesScreen(Fixtures.library, Fixtures.NoSequences, snackbar) }

    @Test fun sequencesEmpty() = snap("09_sequences_empty_dark", dark = true) {
        SequencesScreen(emptyList(), Fixtures.NoSequences, snackbar)
    }

    @Test fun editor() = snap("10_editor") { EditorScreen(EditorState.Loaded(Fixtures.sequence), Fixtures.NoEditor, snackbar) }

    @Test fun singlePoint() = snap("11_single_point_dark", dark = true) { SinglePointScreen(Fixtures.quick, Fixtures.NoSingle, snackbar) }

    @Test fun settings() = snap("12_settings") { SettingsScreen(AppSettings(onboardingDone = true), Fixtures.NoSettings, snackbar) }

    @Test fun settingsDark() = snap("13_settings_dark", dark = true) {
        SettingsScreen(AppSettings(onboardingDone = true, themeMode = ThemeMode.Dark), Fixtures.NoSettings, snackbar)
    }

    @Test fun settingsLocked() = snap("23_settings_locked_while_running") {
        SettingsScreen(AppSettings(onboardingDone = true), Fixtures.NoSettings, snackbar, runActive = true)
    }

    @Test fun settingsLockedDark() = snap("24_settings_locked_while_running_dark", dark = true) {
        SettingsScreen(AppSettings(onboardingDone = true, themeMode = ThemeMode.Dark), Fixtures.NoSettings, snackbar, runActive = true)
    }

    @Test fun howTo() = snap("14_how_to") { HowToScreen(onBack = {}) }

    @Test fun troubleshooting() = snap("15_troubleshooting") {
        TroubleshootingScreen(TroubleshootingState(ServiceState.Connected, batteryOptimized = true), Fixtures.NoTroubleshooting)
    }

    @Test fun about() = snap("16_about_dark", dark = true) { AboutScreen(onBack = {}) }

    @Test fun overlayEditing() = snap("17_overlay_sequence") {
        OverlayScene(session(OverlayMode.Multi, Fixtures.sequence))
    }

    @Test fun overlayEditingDark() = snap("18_overlay_sequence_dark", dark = true) {
        OverlayScene(session(OverlayMode.Multi, Fixtures.sequence), darkApp = true)
    }

    @Test fun overlayRunning() = snap("19_overlay_running") {
        val s = session(OverlayMode.Multi, Fixtures.sequence).apply {
            running = true
            activeStepId = "s2"
            progress = RunProgress(cycles = 12, actions = 48, elapsedMs = 95_000L, fraction = 0.24f)
        }
        OverlayScene(s)
    }

    @Test fun overlaySingle() = snap("20_overlay_single_dark", dark = true) {
        OverlayScene(session(OverlayMode.Single, Fixtures.quick), darkApp = true)
    }

    @Test fun overlayStepDialog() = snap("21_overlay_step_dialog") {
        val s = session(OverlayMode.Multi, Fixtures.sequence).apply { dialog = OverlayDialog.EditStep("s2") }
        OverlayScene(s, withDialog = true)
    }

    @Test fun overlaySettingsDialog() = snap("22_overlay_settings_dialog_dark", dark = true) {
        val s = session(OverlayMode.Single, Fixtures.quick).apply { dialog = OverlayDialog.Settings }
        OverlayScene(s, darkApp = true, withDialog = true)
    }

    @Test
    @Config(qualifiers = LANDSCAPE)
    fun overlayLandscape() = snap("25_overlay_landscape_regular") {
        OverlayScene(landscapeSession(ControlSize.Regular), controlsAt = DpOffset(8.dp, 16.dp))
    }

    @Test
    @Config(qualifiers = LANDSCAPE)
    fun overlayLandscapeLarge() = snap("26_overlay_landscape_large_dark", dark = true) {
        OverlayScene(landscapeSession(ControlSize.Large), darkApp = true, controlsAt = DpOffset(8.dp, 16.dp))
    }

    @Test
    @Config(qualifiers = LANDSCAPE)
    fun overlayLandscapeRunning() = snap("27_overlay_landscape_running") {
        val s = landscapeSession(ControlSize.Large).apply {
            running = true
            activeStepId = "l2"
            progress = RunProgress(cycles = 12, actions = 36, elapsedMs = 95_000L, fraction = 0.24f)
        }
        OverlayScene(s, controlsAt = DpOffset(8.dp, 16.dp))
    }

    @Test fun overlayMinimized() = snap("28_overlay_minimized") {
        OverlayScene(session(OverlayMode.Multi, Fixtures.sequence).apply { toolbarMinimized = true })
    }

    @Test
    @Config(qualifiers = LANDSCAPE)
    fun overlayLandscapeMinimized() = snap("29_overlay_landscape_minimized_dark", dark = true) {
        OverlayScene(landscapeSession(ControlSize.Large).apply { toolbarMinimized = true }, darkApp = true, controlsAt = DpOffset(8.dp, 16.dp))
    }

    private fun session(mode: OverlayMode, script: Script) =
        OverlaySession(mode, script, AppSettings(onboardingDone = true), persisted = true)

    /** A sequence on the Redmi 13C held sideways, laid out as the overlay would lay it out there. */
    private fun landscapeSession(size: ControlSize) = OverlaySession(
        OverlayMode.Multi,
        Fixtures.landscapeSequence,
        AppSettings(onboardingDone = true, controlSize = size),
        persisted = true,
    ).apply { controlsLayout = ControlsLayout.choose(800f, 360f, OverlayMode.Multi, size) }

    /** The overlay windows composed in one scene, over a stand-in for another app. */
    @Composable
    private fun OverlayScene(
        session: OverlaySession,
        darkApp: Boolean = false,
        withDialog: Boolean = false,
        controlsAt: DpOffset = DpOffset(4.dp, 150.dp),
    ) {
        val density = LocalDensity.current
        val markerPx = with(density) { session.settings.markerSizeDp.dp.roundToPx() }
        Box(Modifier.fillMaxSize()) {
            FakeApp(darkApp)
            PathLayer(session)
            session.script.steps.forEachIndexed { index, step ->
                val handles = when (step) {
                    is TapStep -> listOf(Triple(HandlePart.Tap, step.x, step.y))
                    is SwipeStep -> listOf(
                        Triple(HandlePart.SwipeStart, step.startX, step.startY),
                        Triple(HandlePart.SwipeEnd, step.endX, step.endY),
                    )
                }
                handles.forEach { (part, x, y) ->
                    val state = MarkerState(Handle(step.id, part)).apply { number = index + 1 }
                    Box(
                        Modifier
                            .offset { IntOffset((x - markerPx / 2f).roundToInt(), (y - markerPx / 2f).roundToInt()) }
                            .size(session.settings.markerSizeDp.dp),
                    ) { TargetMarker(state, session) }
                }
            }
            Box(Modifier.offset(x = controlsAt.x, y = controlsAt.y)) { ControlBar(session, NoControls) }
            if (withDialog) {
                val graph = ApplicationProvider.getApplicationContext<Context>().appGraph
                OverlayDialogHost(session, graph, NoDialog)
            }
        }
    }

    /** A neutral stand-in for whatever app the user automates. */
    @Composable
    private fun FakeApp(dark: Boolean) {
        val background = if (dark) Color(0xFF15161A) else Color(0xFFF4F4F6)
        val block = if (dark) Color(0xFF2A2C33) else Color(0xFFE2E3E8)
        val accent = if (dark) Color(0xFF3B3F4A) else Color(0xFFD3D5DD)
        Column(
            Modifier
                .fillMaxSize()
                .background(background)
                .statusBarsPadding()
                .padding(16.dp),
        ) {
            Box(Modifier.fillMaxWidth().height(48.dp).background(block, RoundedCornerShape(12.dp)))
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().height(180.dp).background(accent, RoundedCornerShape(20.dp)))
            Spacer(Modifier.height(16.dp))
            repeat(3) {
                Row {
                    Box(Modifier.weight(1f).height(120.dp).background(block, RoundedCornerShape(16.dp)))
                    Spacer(Modifier.size(12.dp))
                    Box(Modifier.weight(1f).height(120.dp).background(block, RoundedCornerShape(16.dp)))
                }
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.fillMaxWidth().height(64.dp).background(accent, RoundedCornerShape(32.dp)))
        }
    }

    private object NoControls : ControlActions {
        override fun onToggleRun() = Unit
        override fun onAddTap() = Unit
        override fun onAddSwipe() = Unit
        override fun onRemoveLast() = Unit
        override fun onOpenSettings() = Unit
        override fun onOpenSequences() = Unit
        override fun onToggleMinimized() = Unit
        override fun onClose() = Unit
    }

    private object NoDialog : DialogActions {
        override fun dismiss() = Unit
        override fun updateScript(transform: (Script) -> Script) = Unit
        override fun deleteStep(stepId: String) = Unit
        override fun openSequence(id: String?) = Unit
    }
}
