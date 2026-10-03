package io.github.syntaxghost404.tappilot.core.engine

import io.github.syntaxghost404.tappilot.core.model.CanvasSize
import io.github.syntaxghost404.tappilot.core.model.SwipeStep
import io.github.syntaxghost404.tappilot.core.model.TapStep
import io.github.syntaxghost404.tappilot.core.model.Timing
import io.github.syntaxghost404.tappilot.core.model.Variation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot
import kotlin.random.Random

class GesturePlannerTest {
    private val canvas = CanvasSize(1080, 2400)

    @Test
    fun `keeps taps on screen`() {
        val planner = GesturePlanner(canvas, positionSpreadPx = 0f)
        val plan = planner.plan(TapStep(x = -40f, y = 9_000f), Variation()) as TapPlan
        assertEquals(0f, plan.x)
        assertEquals(2399f, plan.y)
    }

    @Test
    fun `random positions stay inside the target`() {
        val planner = GesturePlanner(canvas, positionSpreadPx = 30f, random = Random(7))
        val step = TapStep(x = 500f, y = 500f)
        repeat(2_000) {
            val plan = planner.plan(step, Variation(position = true)) as TapPlan
            assertTrue(hypot(plan.x - 500f, plan.y - 500f) <= 30f + 0.001f)
        }
    }

    @Test
    fun `timing variation stays within the chosen percentage`() {
        val planner = GesturePlanner(canvas, positionSpreadPx = 0f, random = Random(3))
        val step = TapStep(x = 1f, y = 1f, delayMs = 1_000L)
        val pauses = List(2_000) { planner.pauseAfter(step, Variation(timingPercent = 20)) }
        assertTrue(pauses.all { it in 800L..1_200L })
        assertTrue("pauses should actually vary", pauses.toSet().size > 100)
    }

    @Test
    fun `exact timing without variation`() {
        val planner = GesturePlanner(canvas, positionSpreadPx = 0f)
        val step = TapStep(x = 1f, y = 1f, holdMs = 25L, delayMs = 333L)
        assertEquals(333L, planner.pauseAfter(step, Variation()))
        assertEquals(25L, planner.plan(step, Variation()).durationMs)
    }

    @Test
    fun `durations stay within what the platform accepts`() {
        val planner = GesturePlanner(canvas, positionSpreadPx = 0f)
        val swipe = SwipeStep(startX = 0f, startY = 0f, endX = 10f, endY = 10f, durationMs = 5 * 60_000L)
        assertEquals(Timing.MAX_GESTURE_MS, planner.plan(swipe, Variation()).durationMs)
        val tap = TapStep(x = 1f, y = 1f, holdMs = 0L)
        assertEquals(Timing.MIN_HOLD_MS, planner.plan(tap, Variation()).durationMs)
    }
}
