package io.github.syntaxghost404.tappilot.overlay.ui

import android.os.SystemClock
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AdsClick
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarHorizontalFabPosition
import androidx.compose.material3.FloatingToolbarVerticalFabPosition
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalFloatingToolbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import io.github.syntaxghost404.tappilot.R
import io.github.syntaxghost404.tappilot.core.engine.RunProgress
import io.github.syntaxghost404.tappilot.core.model.StopMode
import io.github.syntaxghost404.tappilot.overlay.ControlsLayout
import io.github.syntaxghost404.tappilot.overlay.OverlaySession
import io.github.syntaxghost404.tappilot.ui.components.MorphShape
import io.github.syntaxghost404.tappilot.ui.format.Durations
import kotlinx.coroutines.delay

internal interface ControlActions {
    fun onToggleRun()
    fun onAddTap()
    fun onAddSwipe()
    fun onRemoveLast()
    fun onOpenSettings()
    fun onOpenSequences()
    fun onClose()
}

/**
 * The floating controls: a floating toolbar with the play button as its FAB. It runs vertically,
 * or horizontally where a vertical one would not fit the screen (see [ControlsLayout]). While a
 * run is going the toolbar collapses into the FAB, which grows and shows live progress.
 */
@Composable
internal fun ControlBar(session: OverlaySession, actions: ControlActions) {
    val layout = session.controlsLayout
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density * layout.scale, density.fontScale)) {
        val expanded = !session.running
        val collapsed = rememberCollapsedIntoFab(expanded)
        val modifier = Modifier
            .shrinkToFab(collapsed)
            .padding(ControlsLayout.EDGE_DP.dp)
        val colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors()
        val fab: @Composable () -> Unit = { PlayButton(session, actions::onToggleRun) }
        if (layout.vertical) {
            VerticalFloatingToolbar(
                expanded = expanded,
                modifier = modifier,
                colors = colors,
                floatingActionButtonPosition = FloatingToolbarVerticalFabPosition.Top,
                expandedShadowElevation = 3.dp,
                collapsedShadowElevation = 3.dp,
                floatingActionButton = fab,
            ) { ToolbarButtons(session, actions) }
        } else {
            // The FAB leads, so it stays at the window's corner when the toolbar collapses.
            HorizontalFloatingToolbar(
                expanded = expanded,
                floatingActionButton = fab,
                modifier = modifier,
                colors = colors,
                floatingActionButtonPosition = FloatingToolbarHorizontalFabPosition.Start,
                expandedShadowElevation = 3.dp,
                collapsedShadowElevation = 3.dp,
            ) { ToolbarButtons(session, actions) }
        }
    }
}

/** The toolbar's buttons. [ControlsLayout.buttonCount] must match. */
@Composable
private fun ToolbarButtons(session: OverlaySession, actions: ControlActions) {
    val hasSteps = session.script.steps.isNotEmpty()
    if (session.isMulti) {
        ToolbarButton(Icons.Rounded.AdsClick, stringResource(R.string.overlay_add_tap), actions::onAddTap)
        ToolbarButton(Icons.Rounded.Swipe, stringResource(R.string.overlay_add_swipe), actions::onAddSwipe)
        ToolbarButton(
            Icons.AutoMirrored.Rounded.Undo,
            stringResource(R.string.overlay_remove_last),
            actions::onRemoveLast,
            enabled = hasSteps,
        )
    }
    ToolbarButton(Icons.Rounded.Tune, stringResource(R.string.overlay_settings), actions::onOpenSettings)
    if (session.isMulti) {
        ToolbarButton(Icons.Rounded.FolderOpen, stringResource(R.string.overlay_sequences), actions::onOpenSequences)
    }
    ToolbarButton(Icons.Rounded.Close, stringResource(R.string.overlay_close), actions::onClose)
}

/**
 * Whether the toolbar has finished collapsing into its FAB, read during layout. It turns false the
 * moment the toolbar starts to expand again.
 */
@Composable
private fun rememberCollapsedIntoFab(expanded: Boolean): () -> Boolean {
    // Touch exploration keeps the Material toolbar expanded, so it never collapses then.
    val touchExploration = rememberTouchExplorationEnabled()
    val showToolbar = expanded || touchExploration
    // Follows the toolbar's own expand animation, which uses the same spec.
    val progress = remember { Animatable(if (showToolbar) 1f else 0f) }
    val spec = FloatingToolbarDefaults.animationSpec<Float>()
    LaunchedEffect(showToolbar) {
        progress.animateTo(if (showToolbar) 1f else 0f, spec)
    }
    return { !showToolbar && !progress.isRunning && progress.value == 0f }
}

/**
 * The Material toolbar keeps its expanded size after collapsing into its FAB, which would leave an
 * invisible but touchable area beside the play button, swallowing taps meant for the app below.
 * Once [collapsed], this reports only the play button's square so the window shrinks to it.
 */
private fun Modifier.shrinkToFab(collapsed: () -> Boolean): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val side = ControlsLayout.COLLAPSED_DP.dp.roundToPx()
    val shrink = collapsed()
    val width = if (shrink) minOf(side, placeable.width) else placeable.width
    val height = if (shrink) minOf(side, placeable.height) else placeable.height
    layout(width, height) { placeable.placeRelative(0, 0) }
}

/** Whether a screen reader is exploring by touch. */
@Composable
private fun rememberTouchExplorationEnabled(): Boolean {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(AccessibilityManager::class.java) }
    var enabled by remember(manager) { mutableStateOf(manager?.isTouchExplorationEnabled == true) }
    DisposableEffect(manager) {
        val listener = AccessibilityManager.TouchExplorationStateChangeListener { enabled = it }
        manager?.addTouchExplorationStateChangeListener(listener)
        onDispose { manager?.removeTouchExplorationStateChangeListener(listener) }
    }
    return enabled
}

@Composable
private fun ToolbarButton(icon: ImageVector, label: String, onClick: () -> Unit, enabled: Boolean = true) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        shapes = IconButtonDefaults.shapes(),
    ) {
        Icon(icon, contentDescription = label)
    }
}

/** Play/stop. Morphs from a scalloped "go" shape to a calm square while running. */
@Composable
private fun PlayButton(session: OverlaySession, onClick: () -> Unit) {
    val running = session.running
    val morph = remember { Morph(MaterialShapes.Cookie9Sided, MaterialShapes.Square) }
    val morphProgress by animateFloatAsState(
        targetValue = if (running) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "fab-morph",
    )
    // Cookie9Sided repeats every 40 degrees, so a 40 degree turn lands on an identical outline.
    val spin = remember { Animatable(0f) }
    val spinSpec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(running) {
        if (!running) spin.animateTo(spin.value + 40f, spinSpec)
    }

    var celebrating by remember { mutableStateOf(false) }
    LaunchedEffect(session.finishedCount) {
        if (session.finishedCount > 0) {
            celebrating = true
            delay(1_600)
            celebrating = false
        }
    }
    val pop by animateFloatAsState(
        targetValue = if (celebrating) 1.08f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "fab-pop",
    )

    val label = stringResource(if (running) R.string.overlay_stop else R.string.overlay_play)
    FloatingToolbarDefaults.VibrantFloatingActionButton(
        onClick = onClick,
        modifier = Modifier
            .scale(pop)
            .semantics { contentDescription = label },
        shape = MorphShape(morph, morphProgress, rotationDegrees = spin.value * (1f - morphProgress)),
    ) {
        val scaleSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
        val fadeSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
        AnimatedContent(
            targetState = when {
                celebrating && !running -> FabFace.Done
                running -> FabFace.Running
                else -> FabFace.Idle
            },
            transitionSpec = {
                (scaleIn(scaleSpec, initialScale = 0.6f) + fadeIn(fadeSpec)) togetherWith
                    (scaleOut(scaleSpec, targetScale = 0.6f) + fadeOut(fadeSpec))
            },
            label = "fab-face",
        ) { face ->
            when (face) {
                FabFace.Idle -> Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(28.dp))
                FabFace.Done -> Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(28.dp))
                FabFace.Running -> RunningFace(session)
            }
        }
    }
}

private enum class FabFace { Idle, Running, Done }

@Composable
private fun RunningFace(session: OverlaySession) {
    val progress = session.progress
    val rule = session.script.stopRule
    // Time-based limits tick on their own so the countdown stays live between slow actions.
    var elapsed by remember { mutableLongStateOf(progress.elapsedMs) }
    LaunchedEffect(session.runStartedAt) {
        while (true) {
            elapsed = SystemClock.elapsedRealtime() - session.runStartedAt
            delay(250)
        }
    }
    val fraction = when (rule.mode) {
        StopMode.Duration -> (elapsed.toFloat() / rule.durationMs).coerceIn(0f, 1f)
        else -> progress.fraction
    }
    Box(Modifier.fillMaxSize().padding(6.dp), contentAlignment = Alignment.Center) {
        val color = LocalContentColor.current
        if (fraction != null) {
            CircularWavyProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxSize(),
                color = color,
                trackColor = color.copy(alpha = 0.22f),
            )
        } else {
            CircularWavyProgressIndicator(
                modifier = Modifier.fillMaxSize(),
                color = color,
                trackColor = color.copy(alpha = 0.22f),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(22.dp))
            Text(
                runningLabel(session, progress, elapsed),
                style = MaterialTheme.typography.labelSmallEmphasized,
                maxLines = 1,
            )
        }
    }
}

private fun runningLabel(session: OverlaySession, progress: RunProgress, elapsedMs: Long): String {
    val rule = session.script.stopRule
    return when (rule.mode) {
        StopMode.Duration -> Durations.clock((rule.durationMs - elapsedMs).coerceAtLeast(0L))
        StopMode.Cycles -> "${Durations.count(progress.cycles.toLong())}/${Durations.count(rule.cycles.toLong())}"
        StopMode.Never -> Durations.count(progress.actions)
    }
}
