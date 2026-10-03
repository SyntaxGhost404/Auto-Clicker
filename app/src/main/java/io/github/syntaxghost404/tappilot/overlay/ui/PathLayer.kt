package io.github.syntaxghost404.tappilot.overlay.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.syntaxghost404.tappilot.core.model.Step
import io.github.syntaxghost404.tappilot.core.model.SwipeStep
import io.github.syntaxghost404.tappilot.core.model.TapStep
import io.github.syntaxghost404.tappilot.overlay.OverlaySession
import io.github.syntaxghost404.tappilot.overlay.Pulse
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private const val PULSE_DURATION_NANOS = 420_000_000L

/**
 * Full-screen, touch-transparent layer under the targets. It draws the order of a sequence as
 * dashed connectors, every swipe as an arrow, and a short ripple wherever a gesture lands.
 */
@Composable
internal fun PathLayer(session: OverlaySession) {
    val colors = MaterialTheme.colorScheme
    val markerRadius = with(LocalDensity.current) { session.settings.markerSizeDp.dp.toPx() / 2f }
    val pulses = session.pulses
    var frameNanos by remember { mutableLongStateOf(0L) }

    val animating = pulses.isNotEmpty()
    LaunchedEffect(animating) {
        while (pulses.isNotEmpty()) {
            withFrameNanos { now ->
                frameNanos = now
                for (i in pulses.indices) {
                    if (pulses[i].startedAtNanos == Pulse.UNSTARTED) pulses[i] = pulses[i].copy(startedAtNanos = now)
                }
                pulses.removeAll { now - it.startedAtNanos > PULSE_DURATION_NANOS }
            }
        }
    }

    Canvas(Modifier.fillMaxSize()) {
        val steps = session.script.steps
        val dim = if (session.running) 0.45f else 1f
        if (session.isMulti) {
            for (i in 0 until steps.lastIndex) {
                drawConnector(steps[i].exitPoint(), steps[i + 1].entryPoint(), markerRadius, colors.outline.copy(alpha = 0.8f * dim))
            }
            steps.forEach { step ->
                if (step is SwipeStep) {
                    drawSwipe(step, markerRadius, colors.tertiary.copy(alpha = dim), colors.tertiaryContainer.copy(alpha = dim))
                }
            }
        }
        val now = frameNanos
        pulses.forEach { pulse ->
            if (pulse.startedAtNanos == Pulse.UNSTARTED) return@forEach
            val t = ((now - pulse.startedAtNanos).toFloat() / PULSE_DURATION_NANOS).coerceIn(0f, 1f)
            val eased = 1f - (1f - t) * (1f - t) * (1f - t)
            val color = if (pulse.swipe) colors.tertiary else colors.primary
            drawCircle(
                color = color.copy(alpha = 0.55f * (1f - t)),
                radius = markerRadius * (0.35f + 0.85f * eased),
                center = Offset(pulse.x, pulse.y),
                style = Stroke(width = 2.dp.toPx() + 3.dp.toPx() * (1f - t)),
            )
        }
    }
}

private fun Step.entryPoint(): Offset = when (this) {
    is TapStep -> Offset(x, y)
    is SwipeStep -> Offset(startX, startY)
}

private fun Step.exitPoint(): Offset = when (this) {
    is TapStep -> Offset(x, y)
    is SwipeStep -> Offset(endX, endY)
}

/** Returns the segment between two marker centres, trimmed so it starts and ends at their rims. */
private fun trimmed(from: Offset, to: Offset, inset: Float): Pair<Offset, Offset>? {
    val length = hypot(to.x - from.x, to.y - from.y)
    if (length <= inset * 2f + 1f) return null
    val ux = (to.x - from.x) / length
    val uy = (to.y - from.y) / length
    return Offset(from.x + ux * inset, from.y + uy * inset) to Offset(to.x - ux * inset, to.y - uy * inset)
}

private fun DrawScope.drawConnector(from: Offset, to: Offset, radius: Float, color: Color) {
    val (start, end) = trimmed(from, to, radius + 4.dp.toPx()) ?: return
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 7.dp.toPx())),
    )
    val mid = Offset((start.x + end.x) / 2f, (start.y + end.y) / 2f)
    drawChevron(mid, atan2(end.y - start.y, end.x - start.x), 5.dp.toPx(), color)
}

private fun DrawScope.drawSwipe(step: SwipeStep, radius: Float, color: Color, halo: Color) {
    val (start, end) = trimmed(Offset(step.startX, step.startY), Offset(step.endX, step.endY), radius * 0.7f) ?: return
    drawLine(halo, start, end, strokeWidth = 10.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color, start, end, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
    val angle = atan2(end.y - start.y, end.x - start.x)
    val head = 12.dp.toPx()
    val path = Path().apply {
        moveTo(end.x, end.y)
        lineTo(end.x - head * cos(angle - 0.45f), end.y - head * sin(angle - 0.45f))
        lineTo(end.x - head * cos(angle + 0.45f), end.y - head * sin(angle + 0.45f))
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawChevron(at: Offset, angle: Float, size: Float, color: Color) {
    val back = 0.6f
    val a = Offset(at.x - size * cos(angle - back), at.y - size * sin(angle - back))
    val b = Offset(at.x - size * cos(angle + back), at.y - size * sin(angle + back))
    drawLine(color, a, at, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color, b, at, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
}
