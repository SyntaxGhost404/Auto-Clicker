package io.github.syntaxghost404.tappilot.overlay

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.syntaxghost404.tappilot.appGraph
import io.github.syntaxghost404.tappilot.core.data.OverlayMode
import io.github.syntaxghost404.tappilot.core.engine.DispatchResult
import io.github.syntaxghost404.tappilot.core.engine.GestureDispatcher
import io.github.syntaxghost404.tappilot.core.engine.GesturePlan
import io.github.syntaxghost404.tappilot.core.engine.SwipePlan
import io.github.syntaxghost404.tappilot.core.engine.TapPlan
import io.github.syntaxghost404.tappilot.core.model.Script
import io.github.syntaxghost404.tappilot.core.model.StopMode
import io.github.syntaxghost404.tappilot.core.model.StopRule
import io.github.syntaxghost404.tappilot.core.model.SwipeStep
import io.github.syntaxghost404.tappilot.core.model.TapStep
import io.github.syntaxghost404.tappilot.service.OverlayStatus
import io.github.syntaxghost404.tappilot.service.TapPilotAccessibilityService
import io.github.syntaxghost404.tappilot.service.TapPilotRuntime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

/** Drives the real overlay manager inside a Robolectric accessibility service. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class OverlayManagerTest {
    private lateinit var service: TapPilotAccessibilityService
    private lateinit var manager: OverlayManager
    private val dispatched = mutableListOf<GesturePlan>()

    private val dispatcher = GestureDispatcher { plan ->
        dispatched += plan
        delay(plan.durationMs)
        DispatchResult.Completed
    }

    @Before
    fun setUp() {
        // Pace frames like a 60 Hz display, delivered only as the test advances the clock. Unpaused,
        // Robolectric runs each requested frame at once and moves the clock itself, so continuous
        // animations such as a running play button race the clock ahead.
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

    /** Lets main-thread work and DataStore's background writes settle until [done] holds. */
    private fun settle(timeoutMs: Long = 30_000, done: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!done() && System.currentTimeMillis() < deadline) {
            idle(100)
            Thread.sleep(10)
        }
        assertTrue("condition not reached in time", done())
    }

    @Test
    fun `opens a new sequence with floating controls`() {
        manager.open(OverlayMode.Multi, null)
        settle { manager.session != null }

        val session = manager.session!!
        assertTrue(manager.controlsShown)
        assertTrue(session.isMulti)
        assertTrue(session.script.steps.isEmpty())
        assertTrue(TapPilotRuntime.status.value is OverlayStatus.Visible)
    }

    @Test
    fun `adding steps creates one window per handle and autosaves the sequence`() {
        manager.open(OverlayMode.Multi, null)
        settle { manager.session != null }

        manager.controlActions.onAddTap()
        manager.controlActions.onAddSwipe()
        idle()

        val session = manager.session!!
        assertEquals(2, session.script.steps.size)
        assertTrue(session.script.steps[1] is SwipeStep)
        assertEquals(3, manager.markerWindowCount)

        settle { runBlocking { service.appGraph.scripts.all() }.isNotEmpty() }
        val saved = runBlocking { service.appGraph.scripts.all() }.single()
        assertEquals("Sequence 1", saved.name)
        assertEquals(2, saved.steps.size)

        manager.controlActions.onRemoveLast()
        idle()
        assertEquals(1, manager.markerWindowCount)
    }

    @Test
    fun `plays the sequence until its limit and then stops`() {
        manager.open(OverlayMode.Multi, null)
        settle { manager.session != null }
        manager.controlActions.onAddTap()
        manager.controlActions.onAddSwipe()
        manager.dialogActions.updateScript { it.copy(stopRule = StopRule(mode = StopMode.Cycles, cycles = 2)) }
        idle()

        manager.controlActions.onToggleRun()
        idle(10)
        assertTrue(manager.session!!.running)

        settle { manager.session?.running == false }
        assertEquals(4, dispatched.size)
        assertTrue(dispatched[0] is TapPlan)
        assertTrue(dispatched[1] is SwipePlan)
        assertEquals(1, manager.session!!.finishedCount)
    }

    @Test
    fun `stop interrupts a run without closing the controls`() {
        manager.open(OverlayMode.Single, null)
        settle { manager.session != null }

        manager.controlActions.onToggleRun()
        idle(500)
        assertTrue(manager.session!!.running)
        assertTrue(dispatched.isNotEmpty())

        manager.stopRun()
        idle()
        assertFalse(manager.session!!.running)
        assertTrue(manager.controlsShown)
    }

    @Test
    fun `single point starts centred on screen`() {
        manager.open(OverlayMode.Single, null)
        settle { manager.session != null }

        val script = manager.session!!.script
        val canvas = checkNotNull(script.canvas)
        val tap = script.steps.single() as TapStep
        assertEquals(canvas.width / 2f, tap.x)
        assertEquals(canvas.height / 2f, tap.y)
        assertEquals(1, manager.markerWindowCount)
        assertEquals(Script.QUICK_ID, script.id)
    }

    @Test
    fun `closing removes every window and reports hidden`() {
        manager.open(OverlayMode.Multi, null)
        settle { manager.session != null }
        manager.controlActions.onAddTap()
        idle()

        manager.controlActions.onClose()
        idle()

        assertNull(manager.session)
        assertFalse(manager.controlsShown)
        assertEquals(0, manager.markerWindowCount)
        assertEquals(OverlayStatus.Hidden, runBlocking { TapPilotRuntime.status.first() })
    }
}
