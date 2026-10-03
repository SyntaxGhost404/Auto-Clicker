package io.github.syntaxghost404.tappilot.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import kotlin.math.absoluteValue

/** Fits a unit-sized shape path into [size], centred, optionally rotated about its centre. */
internal fun Path.fitTo(size: Size, rotationDegrees: Float = 0f): Path = fitInto(size, size.center, rotationDegrees)

/** Scales a unit-sized shape path to [box] and centres it on [center]. */
internal fun Path.fitInto(box: Size, center: Offset, rotationDegrees: Float = 0f): Path {
    transform(Matrix().apply { scale(box.width, box.height) })
    translate(center - getBounds().center)
    if (rotationDegrees != 0f) {
        val c = center
        transform(
            Matrix().apply {
                translate(c.x, c.y)
                rotateZ(rotationDegrees)
                translate(-c.x, -c.y)
            },
        )
    }
    return this
}

/** A [Shape] for one frame of a [Morph]. Create a new instance whenever [progress] changes. */
class MorphShape(
    private val morph: Morph,
    private val progress: Float,
    private val rotationDegrees: Float = 0f,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = morph.toPath(progress, Path()).fitTo(size, rotationDegrees)
        return Outline.Generic(path)
    }
}

/**
 * Draws [polygon] filled with [color] behind [content]. The shape can spin via [rotation] without
 * rotating the content, which keeps icons and numbers upright.
 */
@Composable
fun ShapeBadge(
    polygon: RoundedPolygon,
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    rotation: () -> Float = { 0f },
    content: @Composable BoxScope.() -> Unit = {},
) {
    val unitPath = polygon.toPath()
    Box(
        modifier = modifier
            .size(size)
            .drawWithCache {
                val path = Path()
                onDrawBehind {
                    path.reset()
                    path.addPath(unitPath)
                    path.fitTo(this.size, rotation())
                    drawPath(path, color)
                }
            },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** A stable, varied shape for an item (for example a saved sequence), picked from its key. */
@Composable
fun rememberShapeFor(key: String): RoundedPolygon = remember(key) {
    AvatarShapes[(key.hashCode().absoluteValue) % AvatarShapes.size]
}

private val AvatarShapes: List<RoundedPolygon> by lazy {
    listOf(
        MaterialShapes.Cookie9Sided,
        MaterialShapes.Clover4Leaf,
        MaterialShapes.Sunny,
        MaterialShapes.Cookie6Sided,
        MaterialShapes.Pentagon,
        MaterialShapes.SoftBurst,
        MaterialShapes.Gem,
        MaterialShapes.Cookie4Sided,
        MaterialShapes.Puffy,
        MaterialShapes.Flower,
    )
}
