package io.github.syntaxghost404.tappilot.core.engine

import io.github.syntaxghost404.tappilot.core.model.CanvasSize
import io.github.syntaxghost404.tappilot.core.model.Script
import io.github.syntaxghost404.tappilot.core.model.StopMode
import io.github.syntaxghost404.tappilot.core.model.StopRule
import io.github.syntaxghost404.tappilot.core.model.SwipeStep
import io.github.syntaxghost404.tappilot.core.model.TapStep
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class ScriptRunnerTest {
    private val canvas = CanvasSize(1080, 2400)
    private val planner = GesturePlanner(canvas, positionSpreadPx = 0f, random = Random(1))

    private class RecordingDispatcher(
        private val latencyMs: Long = 0L,
        private val result: DispatchResult = DispatchResult.Completed,
    ) : GestureDispatcher {
        val plans = mutableListOf<GesturePlan>()
        val startTimes = mutableListOf<Long>()
        var clock: () -> Long = { 0L }

        override suspend fun dispatch(plan: GesturePlan): DispatchResult {
            startTimes += clock()
            plans += plan
            delay(plan.durationMs + latencyMs)
            return result
        }
    }

    private fun TestScope.runner(dispatcher: RecordingDispatcher): ScriptRunner {
        dispatcher.clock = { testScheduler.currentTime }
        return ScriptRunner(dispatcher) { testScheduler.currentTime }
    }

    private fun tap(x: Float = 100f, y: Float = 200f, hold: Long = 10L, delay: Long = 100L) =
        TapStep(x = x, y = y, holdMs = hold, delayMs = delay)

    @Test
    fun `stops after the requested number of cycles`() = runTest {
        val dispatcher = RecordingDispatcher()
        val script = Script(
            name = "two steps",
            steps = listOf(tap(), SwipeStep(startX = 10f, startY = 10f, endX = 500f, endY = 10f, durationMs = 300L, delayMs = 50L)),
            stopRule = StopRule(mode = StopMode.Cycles, cycles = 3),
        )

        val result = runner(dispatcher).run(script, planner)

        assertEquals(RunOutcome.Finished, result.outcome)
        assertEquals(6, dispatcher.plans.size)
        assertEquals(3, result.progress.cycles)
        assertEquals(1f, result.progress.fraction)
        assertTrue(dispatcher.plans[1] is SwipePlan)
    }

    @Test
    fun `stops when the time limit is reached`() = runTest {
        val dispatcher = RecordingDispatcher()
        val script = Script(
            name = "timed",
            steps = listOf(tap(hold = 10L, delay = 90L)),
            stopRule = StopRule(mode = StopMode.Duration, durationMs = 1_000L),
        )

        val result = runner(dispatcher).run(script, planner)

        assertEquals(RunOutcome.Finished, result.outcome)
        // One tap every 100 ms for one second.
        assertEquals(10, dispatcher.plans.size)
        assertEquals(1_000L, result.progress.elapsedMs)
    }

    @Test
    fun `keeps a steady rhythm despite callback latency`() = runTest {
        val dispatcher = RecordingDispatcher(latencyMs = 30L)
        val script = Script(
            name = "rhythm",
            steps = listOf(tap(hold = 10L, delay = 90L)),
            stopRule = StopRule(mode = StopMode.Cycles, cycles = 5),
        )

        runner(dispatcher).run(script, planner)

        val gaps = dispatcher.startTimes.zipWithNext { a, b -> b - a }
        assertEquals(listOf(100L, 100L, 100L, 100L), gaps)
    }

    @Test
    fun `runs until cancelled when there is no limit`() = runTest {
        val dispatcher = RecordingDispatcher()
        val script = Script(name = "forever", steps = listOf(tap(hold = 10L, delay = 40L)))
        val job = launch { runner(dispatcher).run(script, planner) }

        advanceTimeBy(1_001L)
        job.cancel()

        assertTrue(job.isCancelled)
        assertEquals(21, dispatcher.plans.size)
    }

    @Test
    fun `gives up when the system keeps rejecting gestures`() = runTest {
        val dispatcher = RecordingDispatcher(result = DispatchResult.Rejected)
        val script = Script(name = "rejected", steps = listOf(tap()))

        val result = runner(dispatcher).run(script, planner)

        assertEquals(RunOutcome.Rejected, result.outcome)
        assertEquals(3, dispatcher.plans.size)
    }

    @Test
    fun `reports nothing to run for an empty script`() = runTest {
        val result = runner(RecordingDispatcher()).run(Script(name = "empty"), planner)
        assertEquals(RunOutcome.NothingToRun, result.outcome)
    }

    @Test
    fun `reports progress toward a cycle limit`() = runTest {
        val dispatcher = RecordingDispatcher()
        val script = Script(
            name = "progress",
            steps = listOf(tap(), tap()),
            stopRule = StopRule(mode = StopMode.Cycles, cycles = 2),
        )
        val fractions = mutableListOf<Float?>()
        runner(dispatcher).run(
            script,
            planner,
            object : RunListener {
                override fun onProgress(progress: RunProgress) {
                    fractions += progress.fraction
                }
            },
        )
        assertEquals(listOf(0f, 0.25f, 0.5f, 0.75f, 1f, 1f), fractions)
    }
}
