package io.github.syntaxghost404.tappilot.overlay

import android.graphics.Rect
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.syntaxghost404.tappilot.R
import io.github.syntaxghost404.tappilot.appGraph
import io.github.syntaxghost404.tappilot.core.data.ControlSize
import io.github.syntaxghost404.tappilot.core.data.OverlayMode
import io.github.syntaxghost404.tappilot.core.engine.DispatchResult
import io.github.syntaxghost404.tappilot.core.engine.GestureDispatcher
import io.github.syntaxghost404.tappilot.service.TapPilotAccessibilityService
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowChoreographer
import org.robolectric.shadows.ShadowStatsLog
import java.time.Duration
import kotlin.math.abs
import kotlin.math.roundToInt

/** The Redmi 13C held sideways: 1600 x 720 px at 320 dpi. */
private const val LANDSCAPE = "w800dp-h360dp-land-xhdpi"
private const val PORTRAIT = "w360dp-h800dp-port-xhdpi"

/**
 * Geometry of the floating controls on a phone-sized screen. Every control must be on screen and
 * inside its window at every control size, in both orientations.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = LANDSCAPE)
class OverlayLayoutTest {
    private lateinit var service: TapPilotAccessibilityService
    private lateinit var manager: OverlayManager

    private val dispatcher = GestureDispatcher { plan ->
        delay(plan.durationMs)
        DispatchResult.Completed
    }

    @Before
    fun setUp() {
        // Frames at 60 Hz, delivered only as the test advances the clock (see OverlayManagerTest).
        ShadowChoreographer.setPaused(true)
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(16))
        service = Robolectric.setupService(TapPilotAccessibilityService::class.java)
        manager = OverlayManager(service, service.appGraph, dispatcher)
    }

    @After
    fun tearDown() {
        manager.dispose()
        idle(100)
    }

    @Test
    fun `compact sequence controls fit a landscape screen`() {
        open(OverlayMode.Multi, ControlSize.Compact)
        assertAllControlsVisible(OverlayMode.Multi)
        // Compact already fitted sideways, so it keeps its vertical toolbar.
        assertVertical(true)
    }

    @Test
    fun `regular sequence controls fit a landscape screen`() {
        open(OverlayMode.Multi, ControlSize.Regular)
        assertAllControlsVisible(OverlayMode.Multi)
        assertVertical(false)
    }

    @Test
    fun `the controls keep their full size when their window offers less`() {
        open(OverlayMode.Multi, ControlSize.Regular)
        // A device first measures a wrap-content window at the platform's preferred dialog width,
        // 320 dp on phones, and never taller than the screen. Robolectric's windows always span
        // the display, so offer those limits directly: the horizontal controls are 380 x 92 dp.
        val frame = checkNotNull(manager.controlsView)
        frame.measure(
            View.MeasureSpec.makeMeasureSpec(dp(320f), View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(dp(80f), View.MeasureSpec.AT_MOST),
        )
        assertEquals(dp(380f).toFloat(), frame.measuredWidth.toFloat(), 3f)
        assertEquals(dp(92f).toFloat(), frame.measuredHeight.toFloat(), 3f)
        frame.requestLayout()
        idle()
    }

    @Test
    fun `large sequence controls fit a landscape screen`() {
        open(OverlayMode.Multi, ControlSize.Large)
        assertAllControlsVisible(OverlayMode.Multi)
        assertVertical(false)
    }

    @Test
    fun `single point controls fit a landscape screen at every size`() {
        for (size in ControlSize.entries) {
            open(OverlayMode.Single, size)
            assertAllControlsVisible(OverlayMode.Single)
            assertVertical(true)
        }
    }

    @Test
    @Config(qualifiers = PORTRAIT)
    fun `portrait controls keep their vertical toolbar and size`() {
        for (size in ControlSize.entries) {
            open(OverlayMode.Multi, size)
            assertAllControlsVisible(OverlayMode.Multi)
            assertVertical(true)
            val scale = when (size) {
                ControlSize.Compact -> 0.86f
                ControlSize.Regular -> 1f
                ControlSize.Large -> 1.16f
            }
            val view = checkNotNull(manager.controlsView)
            // Toolbar of six 48 dp buttons with 8 dp padding, an 8 dp gap, a 56 dp play button and
            // 6 dp around it all: 380 x 92 dp before scaling.
            assertEquals(dp(92f * scale).toFloat(), view.width.toFloat(), 3f)
            assertEquals(dp(380f * scale).toFloat(), view.height.toFloat(), 3f)
        }
    }

    @Test
    fun `while running the controls shrink to the play button and expand again on stop`() {
        open(OverlayMode.Multi, ControlSize.Regular)
        manager.controlActions.onAddTap()
        idle()

        manager.controlActions.onToggleRun()
        idle(1_500)
        assertTrue(manager.session!!.running)
        val view = checkNotNull(manager.controlsView)
        // Only the 80 dp play button and the 6 dp around it remain.
        assertEquals(dp(92f).toFloat(), view.width.toFloat(), 2f)
        assertEquals(dp(92f).toFloat(), view.height.toFloat(), 2f)
        assertOnScreen(controls().getValue(label(R.string.overlay_stop)))

        manager.stopRun()
        idle(1_500)
        assertAllControlsVisible(OverlayMode.Multi)
    }

    @Test
    @Config(qualifiers = PORTRAIT)
    fun `rotating to landscape keeps every control on screen`() {
        open(OverlayMode.Multi, ControlSize.Large)
        assertAllControlsVisible(OverlayMode.Multi)

        RuntimeEnvironment.setQualifiers(LANDSCAPE)
        manager.onDisplayChanged()
        idle(1_000)
        assertEquals(1600, screen().width())
        assertAllControlsVisible(OverlayMode.Multi)

        RuntimeEnvironment.setQualifiers(PORTRAIT)
        manager.onDisplayChanged()
        idle(1_000)
        assertAllControlsVisible(OverlayMode.Multi)
        assertVertical(true)
    }

    @Test
    @Config(qualifiers = PORTRAIT)
    fun `minimizing leaves run and maximize, and maximizing brings every control back`() {
        open(OverlayMode.Multi, ControlSize.Regular)
        press(label(R.string.overlay_minimize))
        idle(1_000)
        assertMinimizedControlsVisible()
        // The play button beside one 48 dp button in a toolbar padded by 8 dp: 140 x 92 dp.
        val view = checkNotNull(manager.controlsView)
        assertEquals(dp(92f).toFloat(), view.width.toFloat(), 3f)
        assertEquals(dp(140f).toFloat(), view.height.toFloat(), 3f)

        press(label(R.string.overlay_maximize))
        idle(1_000)
        assertAllControlsVisible(OverlayMode.Multi)
        assertEquals(dp(380f).toFloat(), view.height.toFloat(), 3f)
    }

    @Test
    @Config(qualifiers = PORTRAIT)
    fun `maximizing near the bottom edge keeps every control on screen`() {
        open(OverlayMode.Multi, ControlSize.Large)
        press(label(R.string.overlay_minimize))
        idle(1_000)
        dragControlsBy(0f, screen().height().toFloat())
        idle(500)
        assertMinimizedControlsVisible()
        assertTrue(windowBounds().bottom >= screen().bottom - dp(2f))

        press(label(R.string.overlay_maximize))
        idle(1_000)
        assertAllControlsVisible(OverlayMode.Multi)
    }

    @Test
    fun `minimized controls stay minimized through a run in landscape`() {
        open(OverlayMode.Multi, ControlSize.Large)
        manager.controlActions.onAddTap()
        idle()
        press(label(R.string.overlay_minimize))
        idle(1_000)
        assertMinimizedControlsVisible()
        val found = controls()
        assertTrue(found.getValue(label(R.string.overlay_maximize)).left >= found.getValue(label(R.string.overlay_play)).right)

        manager.controlActions.onToggleRun()
        idle(1_500)
        assertTrue(manager.session!!.running)
        assertEquals(dp(92f * 1.16f).toFloat(), checkNotNull(manager.controlsView).width.toFloat(), 3f)

        manager.stopRun()
        idle(1_500)
        assertMinimizedControlsVisible()
    }

    // region Helpers

    private fun open(mode: OverlayMode, size: ControlSize) {
        manager.close()
        runBlocking { service.appGraph.settings.setControlSize(size) }
        manager.open(mode, null)
        settle { manager.session?.settings?.controlSize == size && (manager.controlsView?.width ?: 0) > 0 }
        // Let composition, window layout and any repositioning finish.
        idle(1_000)
    }

    private fun idle(millis: Long = 50) {
        var left = millis
        while (left > 0) {
            val step = minOf(left, 100L)
            // Robolectric records a stats event per window frame in a copy-on-write list, which
            // makes long animated runs quadratic; nothing here reads it.
            ShadowStatsLog.reset()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(step))
            left -= step
        }
    }

    private fun settle(timeoutMs: Long = 30_000, done: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!done() && System.currentTimeMillis() < deadline) {
            idle(100)
            Thread.sleep(10)
        }
        assertTrue("condition not reached in time", done())
    }

    private fun dp(value: Float): Int = (value * service.resources.displayMetrics.density).roundToInt()

    private fun label(id: Int): String = service.getString(id)

    private fun screen(): Rect {
        val bounds = service.getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
        return Rect(0, 0, bounds.width(), bounds.height())
    }

    private val composeView: ViewGroup
        get() = (checkNotNull(manager.controlsView) as ViewGroup).getChildAt(0) as ViewGroup

    private fun semanticsRoot(): SemanticsNode = (composeView.getChildAt(0) as ViewRootForTest).semanticsOwner.rootSemanticsNode

    private fun SemanticsNode.label(): String? = config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()

    /** Screen bounds of every labelled control in the floating controls. */
    private fun controls(): Map<String, Rect> {
        val origin = checkNotNull(manager.controlsOrigin)
        val found = LinkedHashMap<String, Rect>()
        fun visit(node: SemanticsNode) {
            val label = node.label()
            if (label != null && label !in found) {
                val left = origin.x + composeView.left + node.positionInRoot.x.roundToInt()
                val top = origin.y + composeView.top + node.positionInRoot.y.roundToInt()
                found[label] = Rect(left, top, left + node.size.width, top + node.size.height)
            }
            node.children.forEach(::visit)
        }
        visit(semanticsRoot())
        return found
    }

    /** Presses the control labelled [label] through its click action, as a screen reader would. */
    private fun press(label: String) {
        fun find(node: SemanticsNode): SemanticsNode? =
            if (node.label() == label) node else node.children.firstNotNullOfOrNull { find(it) }
        val node = checkNotNull(find(semanticsRoot())) { "no control labelled $label" }
        checkNotNull(node.config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
    }

    /** Drags the controls by ([dx], [dy]) pixels from an empty spot, like a finger would. */
    private fun dragControlsBy(dx: Float, dy: Float) {
        val frame = checkNotNull(manager.controlsView)
        val start = dp(3f).toFloat()
        val downAt = SystemClock.uptimeMillis()
        fun send(action: Int, x: Float, y: Float) {
            val event = MotionEvent.obtain(downAt, SystemClock.uptimeMillis(), action, x, y, 0)
            frame.dispatchTouchEvent(event)
            event.recycle()
            idle(16)
        }
        send(MotionEvent.ACTION_DOWN, start, start)
        send(MotionEvent.ACTION_MOVE, start + dx / 2, start + dy / 2)
        send(MotionEvent.ACTION_MOVE, start + dx, start + dy)
        send(MotionEvent.ACTION_UP, start + dx, start + dy)
    }

    private fun windowBounds(): Rect {
        val view = checkNotNull(manager.controlsView)
        val origin = checkNotNull(manager.controlsOrigin)
        return Rect(origin.x, origin.y, origin.x + view.width, origin.y + view.height)
    }

    private fun assertOnScreen(bounds: Rect) {
        assertTrue("$bounds is not fully on screen ${screen()}", screen().contains(bounds))
    }

    /** Every control of [mode] is fully on screen and inside the controls window. */
    private fun assertAllControlsVisible(mode: OverlayMode) {
        val expected = buildList {
            add(label(R.string.overlay_play))
            if (mode == OverlayMode.Multi) {
                add(label(R.string.overlay_add_tap))
                add(label(R.string.overlay_add_swipe))
                add(label(R.string.overlay_remove_last))
            }
            add(label(R.string.overlay_settings))
            if (mode == OverlayMode.Multi) add(label(R.string.overlay_minimize))
            add(label(R.string.overlay_close))
        }
        assertOnlyVisible(expected)
    }

    /** Minimized sequence controls show only the play button and maximize. */
    private fun assertMinimizedControlsVisible() {
        assertOnlyVisible(listOf(label(R.string.overlay_play), label(R.string.overlay_maximize)))
    }

    private fun assertOnlyVisible(expected: List<String>) {
        val found = controls()
        assertEquals(expected.toSet(), found.keys)
        val window = windowBounds()
        found.forEach { (label, bounds) ->
            assertTrue("$label at $bounds is not fully on screen ${screen()}", screen().contains(bounds))
            assertTrue("$label at $bounds is clipped by its window $window", window.contains(bounds))
        }
    }

    private fun assertVertical(vertical: Boolean) {
        val found = controls()
        val play = found.getValue(label(R.string.overlay_play))
        val close = found.getValue(label(R.string.overlay_close))
        if (vertical) {
            assertTrue("close $close is not below play $play", close.top >= play.bottom)
            assertTrue(abs(close.centerX() - play.centerX()) <= dp(2f))
        } else {
            assertTrue("close $close is not beside play $play", close.left >= play.right)
            assertTrue(abs(close.centerY() - play.centerY()) <= dp(2f))
        }
    }

    // endregion
}
