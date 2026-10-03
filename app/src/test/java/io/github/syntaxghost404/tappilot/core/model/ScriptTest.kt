package io.github.syntaxghost404.tappilot.core.model

import io.github.syntaxghost404.tappilot.ui.format.Durations
import io.github.syntaxghost404.tappilot.ui.format.TimeUnitChoice
import org.junit.Assert.assertEquals
import org.junit.Test

class ScriptTest {
    @Test
    fun `fits coordinates to a different screen size`() {
        val script = Script(
            name = "s",
            canvas = CanvasSize(1000, 2000),
            steps = listOf(
                TapStep(x = 500f, y = 1000f),
                SwipeStep(startX = 100f, startY = 200f, endX = 900f, endY = 1800f),
            ),
        )

        val fitted = script.fittedTo(CanvasSize(2000, 1000))

        assertEquals(CanvasSize(2000, 1000), fitted.canvas)
        assertEquals(TapStep(id = script.steps[0].id, x = 1000f, y = 500f), fitted.steps[0])
        val swipe = fitted.steps[1] as SwipeStep
        assertEquals(200f, swipe.startX)
        assertEquals(100f, swipe.startY)
        assertEquals(1800f, swipe.endX)
        assertEquals(900f, swipe.endY)
    }

    @Test
    fun `keeps coordinates when the screen is unchanged`() {
        val script = Script(name = "s", canvas = CanvasSize(10, 10), steps = listOf(TapStep(x = 3f, y = 4f)))
        assertEquals(script, script.fittedTo(CanvasSize(10, 10)))
    }

    @Test
    fun `cycle duration adds gestures and pauses`() {
        val script = Script(
            name = "s",
            steps = listOf(TapStep(x = 0f, y = 0f, holdMs = 10L, delayMs = 90L), SwipeStep(startX = 0f, startY = 0f, endX = 1f, endY = 1f, durationMs = 400L, delayMs = 100L)),
        )
        assertEquals(600L, script.cycleDurationMs)
    }

    @Test
    fun `moves and updates steps by id`() {
        val a = TapStep(id = "a", x = 0f, y = 0f)
        val b = TapStep(id = "b", x = 1f, y = 1f)
        val script = Script(name = "s", steps = listOf(a, b)).updateStep("b") { it.withDelay(5L) }
        assertEquals(5L, script.steps[1].delayMs)
        assertEquals(a, script.steps[0])
    }

    @Test
    fun `picks the largest exact unit`() {
        assertEquals(TimeUnitChoice.Milliseconds, TimeUnitChoice.bestFor(1_500L))
        assertEquals(TimeUnitChoice.Seconds, TimeUnitChoice.bestFor(2_000L))
        assertEquals(TimeUnitChoice.Minutes, TimeUnitChoice.bestFor(120_000L))
        assertEquals(TimeUnitChoice.Milliseconds, TimeUnitChoice.bestFor(0L))
        assertEquals(TimeUnitChoice.Seconds, TimeUnitChoice.bestFor(120_000L, listOf(TimeUnitChoice.Milliseconds, TimeUnitChoice.Seconds)))
    }

    @Test
    fun `formats a stopwatch`() {
        assertEquals("0:00", Durations.clock(0L))
        assertEquals("4:05", Durations.clock(245_000L))
        assertEquals("1:02:03", Durations.clock(3_723_000L))
    }
}
