package io.github.syntaxghost404.tappilot.core.engine

import io.github.syntaxghost404.tappilot.core.model.Script
import io.github.syntaxghost404.tappilot.core.model.StopMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield

/** Live statistics for a run. [fraction] is null when the run has no end. */
data class RunProgress(
    val cycles: Int = 0,
    val actions: Long = 0L,
    val elapsedMs: Long = 0L,
    val fraction: Float? = null,
)

enum class RunOutcome {
    /** The stop rule was reached. */
    Finished,

    /** The script has no steps. */
    NothingToRun,

    /** The system repeatedly refused gestures, so the run gave up. */
    Rejected,
}

data class RunResult(val outcome: RunOutcome, val progress: RunProgress)

/** Receives callbacks on the runner's coroutine. Keep them cheap. */
interface RunListener {
    /** Called right before a gesture is dispatched. */
    fun onAction(stepIndex: Int, plan: GesturePlan) {}

    fun onProgress(progress: RunProgress) {}
}

/**
 * Plays a [Script] until its stop rule is met or the calling coroutine is cancelled.
 *
 * Scheduling is anchored to when each gesture started rather than when its completion callback
 * arrived, so callback latency is absorbed by the following pause instead of slowly stretching
 * the rhythm.
 */
class ScriptRunner(
    private val dispatcher: GestureDispatcher,
    private val clock: () -> Long,
) {
    suspend fun run(script: Script, planner: GesturePlanner, listener: RunListener = NoOpListener): RunResult {
        val steps = script.steps
        if (steps.isEmpty()) return RunResult(RunOutcome.NothingToRun, RunProgress())

        val rule = script.stopRule.normalized()
        val variation = script.variation.normalized()
        val startedAt = clock()
        val deadline = if (rule.mode == StopMode.Duration) startedAt + rule.durationMs else Long.MAX_VALUE

        var cycles = 0
        var actions = 0L
        var nextStartAt = startedAt
        var consecutiveRejections = 0

        fun progress(stepsDoneInCycle: Int): RunProgress {
            val elapsed = (clock() - startedAt).coerceAtLeast(0L)
            val fraction = when (rule.mode) {
                StopMode.Never -> null
                StopMode.Duration -> (elapsed.toFloat() / rule.durationMs).coerceIn(0f, 1f)
                StopMode.Cycles ->
                    ((cycles + stepsDoneInCycle.toFloat() / steps.size) / rule.cycles).coerceIn(0f, 1f)
            }
            return RunProgress(cycles, actions, elapsed, fraction)
        }

        fun finish(outcome: RunOutcome): RunResult {
            val final = progress(stepsDoneInCycle = 0).let {
                if (outcome == RunOutcome.Finished && rule.mode != StopMode.Never) it.copy(fraction = 1f) else it
            }
            listener.onProgress(final)
            return RunResult(outcome, final)
        }

        listener.onProgress(progress(0))
        while (true) {
            for ((index, step) in steps.withIndex()) {
                val now = clock()
                if (now >= deadline) return finish(RunOutcome.Finished)
                val wait = minOf(nextStartAt, deadline) - now
                if (wait > 0) delay(wait) else yield()
                if (clock() >= deadline) return finish(RunOutcome.Finished)

                val plan = planner.plan(step, variation)
                val actionStartedAt = clock()
                listener.onAction(index, plan)
                when (dispatcher.dispatch(plan)) {
                    DispatchResult.Rejected -> {
                        consecutiveRejections++
                        if (consecutiveRejections >= MAX_CONSECUTIVE_REJECTIONS) return finish(RunOutcome.Rejected)
                    }
                    DispatchResult.Completed, DispatchResult.Cancelled -> consecutiveRejections = 0
                }
                actions++
                nextStartAt = actionStartedAt + plan.durationMs + planner.pauseAfter(step, variation)
                listener.onProgress(progress(stepsDoneInCycle = index + 1))
            }
            cycles++
            if (rule.mode == StopMode.Cycles && cycles >= rule.cycles) return finish(RunOutcome.Finished)
        }
    }

    private object NoOpListener : RunListener

    private companion object {
        const val MAX_CONSECUTIVE_REJECTIONS = 3
    }
}
