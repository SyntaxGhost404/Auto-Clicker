package io.github.syntaxghost404.tappilot.core.engine

import io.github.syntaxghost404.tappilot.core.model.CanvasSize
import io.github.syntaxghost404.tappilot.core.model.Step
import io.github.syntaxghost404.tappilot.core.model.SwipeStep
import io.github.syntaxghost404.tappilot.core.model.TapStep
import io.github.syntaxghost404.tappilot.core.model.Timing
import io.github.syntaxghost404.tappilot.core.model.Variation
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Turns script steps into concrete gestures: applies the script's [Variation], keeps every point
 * on screen and keeps every duration inside what the platform accepts.
 *
 * @param bounds the display the gestures will be performed on.
 * @param positionSpreadPx radius of the circle that randomized taps land in.
 */
class GesturePlanner(
    private val bounds: CanvasSize,
    private val positionSpreadPx: Float,
    private val random: Random = Random.Default,
) {
    fun plan(step: Step, variation: Variation): GesturePlan = when (step) {
        is TapStep -> {
            val offset = if (variation.position) randomOffset() else Offset.Zero
            TapPlan(
                x = clampX(step.x + offset.x),
                y = clampY(step.y + offset.y),
                durationMs = vary(step.holdMs, variation).coerceIn(Timing.MIN_HOLD_MS, Timing.MAX_GESTURE_MS),
            )
        }
        is SwipeStep -> {
            val start = if (variation.position) randomOffset() else Offset.Zero
            val end = if (variation.position) randomOffset() else Offset.Zero
            SwipePlan(
                startX = clampX(step.startX + start.x),
                startY = clampY(step.startY + start.y),
                endX = clampX(step.endX + end.x),
                endY = clampY(step.endY + end.y),
                durationMs = vary(step.durationMs, variation).coerceIn(Timing.MIN_SWIPE_MS, Timing.MAX_GESTURE_MS),
            )
        }
    }

    /** The pause after [step], with timing variation applied. */
    fun pauseAfter(step: Step, variation: Variation): Long =
        vary(step.delayMs.coerceIn(0L, Timing.MAX_DELAY_MS), variation)

    private fun vary(base: Long, variation: Variation): Long {
        val percent = variation.normalized().timingPercent
        if (percent == 0 || base <= 0L) return base.coerceAtLeast(0L)
        val spread = percent / 100.0
        val factor = 1.0 + random.nextDouble(-spread, spread)
        return (base * factor).roundToLong().coerceAtLeast(0L)
    }

    /** A uniformly distributed point inside a disc of radius [positionSpreadPx]. */
    private fun randomOffset(): Offset {
        if (positionSpreadPx <= 0f) return Offset.Zero
        val angle = random.nextDouble(0.0, 2 * PI)
        val distance = positionSpreadPx * sqrt(random.nextDouble())
        return Offset((cos(angle) * distance).toFloat(), (sin(angle) * distance).toFloat())
    }

    private fun clampX(x: Float) = x.coerceIn(0f, (bounds.width - 1).coerceAtLeast(0).toFloat())
    private fun clampY(y: Float) = y.coerceIn(0f, (bounds.height - 1).coerceAtLeast(0).toFloat())

    private data class Offset(val x: Float, val y: Float) {
        companion object {
            val Zero = Offset(0f, 0f)
        }
    }
}
