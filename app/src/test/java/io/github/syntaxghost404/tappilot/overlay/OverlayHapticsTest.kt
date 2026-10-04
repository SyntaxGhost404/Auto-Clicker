package io.github.syntaxghost404.tappilot.overlay

import android.os.Looper
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.syntaxghost404.tappilot.R
import io.github.syntaxghost404.tappilot.appGraph
import io.github.syntaxghost404.tappilot.core.data.OverlayMode
import io.github.syntaxghost404.tappilot.core.engine.DispatchResult
import io.github.syntaxghost404.tappilot.core.engine.GestureDispatcher
import io.github.syntaxghost404.tappilot.core.model.StopMode
import io.github.syntaxghost404.tappilot.core.model.StopRule
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowChoreographer
import org.robolectric.shadows.ShadowStatsLog
import java.time.Duration

private const val NOTHING = -1

/** The floating controls are felt as well as seen, unless the user turns haptics off. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h800dp-port-xhdpi")
class OverlayHapticsTest {
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
    fun `play and stop feel like a switch, and a finished run is confirmed`() {
        open()
        manager.controlActions.onAddTap()
        idle()

        manager.controlActions.onToggleRun()
        idle()
        assertEquals(HapticFeedbackConstants.TOGGLE_ON, controlsHaptic())
        manager.controlActions.onToggleRun()
        idle()
        assertEquals(HapticFeedbackConstants.TOGGLE_OFF, controlsHaptic())

        manager.dialogActions.updateScript { it.copy(stopRule = StopRule(mode = StopMode.Cycles, cycles = 1)) }
        manager.controlActions.onToggleRun()
        settle { manager.session?.finishedCount == 1 }
        assertEquals(HapticFeedbackConstants.CONFIRM, controlsHaptic())
    }

    @Test
    fun `playing without a target is refused`() {
        open()
        manager.controlActions.onToggleRun()
        idle()
        assertEquals(HapticFeedbackConstants.REJECT, controlsHaptic())
    }

    @Test
    fun `toolbar buttons click`() {
        open()
        press(service.getString(R.string.overlay_add_tap))
        idle()
        assertEquals(1, manager.session!!.script.steps.size)
        assertEquals(HapticFeedbackConstants.CONTEXT_CLICK, controlsHaptic())
    }

    @Test
    fun `picking up and putting down the controls and a target are felt`() {
        open()
        manager.controlActions.onAddTap()
        idle()

        for (view in listOf(checkNotNull(manager.controlsView), manager.markerViews.single())) {
            val downAt = SystemClock.uptimeMillis()
            touch(view, downAt, MotionEvent.ACTION_DOWN, 0f)
            touch(view, downAt, MotionEvent.ACTION_MOVE, 120f)
            touch(view, downAt, MotionEvent.ACTION_MOVE, 160f)
            assertEquals(HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE, lastHaptic(view))
            touch(view, downAt, MotionEvent.ACTION_UP, 160f)
            assertEquals(HapticFeedbackConstants.GESTURE_END, lastHaptic(view))
        }
    }

    @Test
    fun `with haptics off the controls are silent`() {
        runBlocking { service.appGraph.settings.setHapticFeedback(false) }
        open()
        manager.controlActions.onToggleRun()
        idle()
        press(service.getString(R.string.overlay_add_tap))
        idle()
        manager.controlActions.onToggleRun()
        idle(500)
        assertTrue(manager.session!!.running)
        manager.controlActions.onToggleRun()
        idle()

        assertEquals(NOTHING, controlsHaptic())
        assertEquals(NOTHING, lastHaptic(manager.markerViews.single()))
    }

    // region Helpers

    private fun open() {
        manager.open(OverlayMode.Multi, null)
        settle { manager.session != null && (manager.controlsView?.width ?: 0) > 0 }
        idle(1_000)
    }

    private fun idle(millis: Long = 50) {
        var left = millis
        while (left > 0) {
            val step = minOf(left, 100L)
            // Robolectric records a stats event per window frame (see OverlayLayoutTest).
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

    /** The last haptic played in the controls window, by the manager or by Compose. */
    private fun controlsHaptic(): Int = lastHaptic(checkNotNull(manager.controlsView))

    /** The last haptic played on [root] or any view inside it. */
    private fun lastHaptic(root: View): Int {
        fun views(view: View): Sequence<View> = sequence {
            yield(view)
            if (view is ViewGroup) for (i in 0 until view.childCount) yieldAll(views(view.getChildAt(i)))
        }
        return views(root).map { shadowOf(it).lastHapticFeedbackPerformed() }.firstOrNull { it != NOTHING } ?: NOTHING
    }

    /** Presses the control labelled [label] through its click action. */
    private fun press(label: String) {
        val frame = checkNotNull(manager.controlsView) as ViewGroup
        val root = (frame.getChildAt(0) as ViewGroup).getChildAt(0) as ViewRootForTest
        fun find(node: SemanticsNode): SemanticsNode? =
            if (node.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull() == label) {
                node
            } else {
                node.children.firstNotNullOfOrNull { find(it) }
            }
        val node = checkNotNull(find(root.semanticsOwner.rootSemanticsNode)) { "no control labelled $label" }
        checkNotNull(node.config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
    }

    private fun touch(view: View, downAt: Long, action: Int, offset: Float) {
        val event = MotionEvent.obtain(downAt, SystemClock.uptimeMillis(), action, 10f + offset, 10f + offset, 0)
        view.dispatchTouchEvent(event)
        event.recycle()
        idle(16)
    }

    // endregion
}
