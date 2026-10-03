package io.github.syntaxghost404.tappilot.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * A runnable automation: an ordered list of [Step]s plus the rules that decide when the run ends
 * and how much natural variation is applied while it runs.
 *
 * Coordinates are absolute screen pixels measured on a display of [canvas] size. When a script is
 * opened on a display with different dimensions, [fittedTo] rescales every point proportionally.
 */
@Serializable
data class Script(
    val id: String = newId(),
    val name: String,
    val steps: List<Step> = emptyList(),
    val stopRule: StopRule = StopRule(),
    val variation: Variation = Variation(),
    val canvas: CanvasSize? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val lastRunAt: Long = 0L,
) {
    val tapCount: Int get() = steps.count { it is TapStep }
    val swipeCount: Int get() = steps.count { it is SwipeStep }

    /** Duration of one pass through every step, ignoring random variation. */
    val cycleDurationMs: Long get() = steps.sumOf { it.gestureDurationMs + it.delayMs }

    fun updateStep(id: String, transform: (Step) -> Step): Script =
        copy(steps = steps.map { if (it.id == id) transform(it) else it })

    /** Returns a copy whose coordinates are rescaled from [canvas] to [target]. */
    fun fittedTo(target: CanvasSize): Script {
        val source = canvas
        if (source == null || source == target || source.width <= 0 || source.height <= 0) {
            return copy(canvas = target)
        }
        val sx = target.width.toFloat() / source.width
        val sy = target.height.toFloat() / source.height
        return copy(steps = steps.map { it.scaled(sx, sy) }, canvas = target)
    }

    companion object {
        const val QUICK_ID = "quick"
        fun newId(): String = UUID.randomUUID().toString()
    }
}

@Serializable
data class CanvasSize(val width: Int, val height: Int)

/** One action in a script. [delayMs] is the pause after the action finishes. */
@Serializable
sealed interface Step {
    val id: String
    val delayMs: Long
    val gestureDurationMs: Long

    fun withDelay(delayMs: Long): Step
    fun withId(id: String): Step
    fun scaled(sx: Float, sy: Float): Step
}

@Serializable
@SerialName("tap")
data class TapStep(
    override val id: String = Script.newId(),
    val x: Float,
    val y: Float,
    /** How long the finger stays down. Longer values turn the tap into a long press. */
    val holdMs: Long = Timing.DEFAULT_HOLD_MS,
    override val delayMs: Long = Timing.DEFAULT_DELAY_MS,
) : Step {
    override val gestureDurationMs: Long get() = holdMs
    override fun withDelay(delayMs: Long): Step = copy(delayMs = delayMs)
    override fun withId(id: String): Step = copy(id = id)
    override fun scaled(sx: Float, sy: Float): Step = copy(x = x * sx, y = y * sy)
}

@Serializable
@SerialName("swipe")
data class SwipeStep(
    override val id: String = Script.newId(),
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val durationMs: Long = Timing.DEFAULT_SWIPE_MS,
    override val delayMs: Long = Timing.DEFAULT_DELAY_MS,
) : Step {
    override val gestureDurationMs: Long get() = durationMs
    override fun withDelay(delayMs: Long): Step = copy(delayMs = delayMs)
    override fun withId(id: String): Step = copy(id = id)
    override fun scaled(sx: Float, sy: Float): Step =
        copy(startX = startX * sx, startY = startY * sy, endX = endX * sx, endY = endY * sy)
}

@Serializable
enum class StopMode { Never, Duration, Cycles }

/**
 * When a run ends. Both limits are kept even while unused so switching modes in the UI never
 * throws away a value the user typed.
 */
@Serializable
data class StopRule(
    val mode: StopMode = StopMode.Never,
    val durationMs: Long = 5 * 60_000L,
    val cycles: Int = 100,
) {
    fun normalized(): StopRule = copy(
        durationMs = durationMs.coerceIn(Timing.MIN_RUN_DURATION_MS, Timing.MAX_RUN_DURATION_MS),
        cycles = cycles.coerceIn(1, Timing.MAX_CYCLES),
    )
}

/** Natural variation applied while running, which makes the input look less mechanical. */
@Serializable
data class Variation(
    /** Spread each tap randomly within the inner area of its target marker. */
    val position: Boolean = false,
    /** Randomly lengthen or shorten every pause by up to this percentage. */
    val timingPercent: Int = 0,
) {
    val isActive: Boolean get() = position || timingPercent > 0

    fun normalized(): Variation = copy(timingPercent = timingPercent.coerceIn(0, Timing.MAX_TIMING_VARIATION))
}

object Timing {
    const val DEFAULT_DELAY_MS = 300L
    const val DEFAULT_INTERVAL_MS = 100L
    const val DEFAULT_HOLD_MS = 10L
    const val DEFAULT_SWIPE_MS = 500L

    const val MIN_HOLD_MS = 1L
    const val MIN_SWIPE_MS = 50L
    /** The platform rejects gestures longer than a minute. */
    const val MAX_GESTURE_MS = 60_000L
    const val MAX_DELAY_MS = 24 * 60 * 60_000L

    const val MIN_RUN_DURATION_MS = 1_000L
    const val MAX_RUN_DURATION_MS = 99 * 60 * 60_000L + 59 * 60_000L + 59_000L
    const val MAX_CYCLES = 9_999_999
    const val MAX_TIMING_VARIATION = 50

    /** Below this pause some devices struggle to keep up and the UI may stutter. */
    const val FAST_DELAY_WARNING_MS = 40L
}
