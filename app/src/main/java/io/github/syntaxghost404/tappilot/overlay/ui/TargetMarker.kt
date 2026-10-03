package io.github.syntaxghost404.tappilot.overlay.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import io.github.syntaxghost404.tappilot.R
import io.github.syntaxghost404.tappilot.overlay.Handle
import io.github.syntaxghost404.tappilot.overlay.HandlePart
import io.github.syntaxghost404.tappilot.overlay.OverlaySession
import io.github.syntaxghost404.tappilot.ui.components.fitInto
import kotlin.math.cos
import kotlin.math.sin

@Stable
internal class MarkerState(val handle: Handle) {
    var number by mutableIntStateOf(1)
    var pressed by mutableStateOf(false)
}

/**
 * One draggable target. The translucent disc is the area natural variation can land in; ticks on
 * the ring point at the exact centre; the chip in the middle carries the step number and morphs to
 * a circle while held.
 */
@Composable
internal fun TargetMarker(state: MarkerState, session: OverlaySession) {
    val colors = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val part = state.handle.part
    val swipe = part != HandlePart.Tap
    val accent = if (swipe) colors.tertiary else colors.primary
    val container = if (swipe) colors.tertiaryContainer else colors.primaryContainer
    val onContainer = if (swipe) colors.onTertiaryContainer else colors.onPrimaryContainer
    val running = session.running
    val active = running && session.activeStepId == state.handle.stepId

    val press by animateFloatAsState(if (state.pressed) 1f else 0f, motion.fastSpatialSpec(), label = "press")
    val emphasis by animateFloatAsState(if (active) 1f else 0f, motion.fastSpatialSpec(), label = "active")
    val fade by animateFloatAsState(if (running && !active) 0.5f else 1f, motion.defaultEffectsSpec(), label = "fade")

    val morph = remember(part) {
        val rest = when (part) {
            HandlePart.Tap -> MaterialShapes.Cookie9Sided
            HandlePart.SwipeStart -> MaterialShapes.Cookie6Sided
            HandlePart.SwipeEnd -> MaterialShapes.Circle
        }
        Morph(rest, MaterialShapes.Circle)
    }
    val chipFraction = if (part == HandlePart.SwipeEnd) 0.36f else 0.56f
    val description = when {
        part == HandlePart.SwipeEnd -> stringResource(R.string.swipe_end_label, state.number)
        session.isMulti -> stringResource(R.string.target_label, state.number)
        else -> stringResource(R.string.target_single)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = description }
            .graphicsLayer { alpha = fade }
            .drawWithCache {
                val ring = 2.dp.toPx()
                val tick = 5.dp.toPx()
                val radius = size.minDimension / 2f - ring
                val centre = Offset(size.width / 2f, size.height / 2f)
                val chip = Path()
                onDrawBehind {
                    drawCircle(accent.copy(alpha = 0.16f + 0.12f * press), radius, centre)
                    drawCircle(accent, radius, centre, style = Stroke(width = ring * (1f + 0.6f * press)))
                    for (i in 0 until 4) {
                        val angle = Math.toRadians(i * 90.0 + 45.0)
                        val outer = Offset(centre.x + cos(angle).toFloat() * radius, centre.y + sin(angle).toFloat() * radius)
                        val inner = Offset(
                            centre.x + cos(angle).toFloat() * (radius - tick),
                            centre.y + sin(angle).toFloat() * (radius - tick),
                        )
                        drawLine(accent, outer, inner, strokeWidth = ring, cap = StrokeCap.Round)
                    }
                    val chipSide = size.minDimension * chipFraction * (1f + 0.12f * press + 0.14f * emphasis)
                    chip.rewind()
                    morph.toPath(press, chip)
                    chip.fitInto(Size(chipSide, chipSide), centre, rotationDegrees = 40f * emphasis)
                    drawPath(chip, container)
                    if (!session.isMulti || part == HandlePart.SwipeEnd) {
                        drawCircle(onContainer, 2.5.dp.toPx(), centre)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (session.isMulti && part != HandlePart.SwipeEnd) {
            Text(
                text = state.number.toString(),
                style = if (state.number < 100) {
                    MaterialTheme.typography.titleMediumEmphasized
                } else {
                    MaterialTheme.typography.labelMediumEmphasized
                },
                color = onContainer,
                modifier = Modifier.graphicsLayer {
                    val s = 1f + 0.12f * press + 0.14f * emphasis
                    scaleX = s
                    scaleY = s
                },
            )
        }
    }
}
