package io.github.syntaxghost404.tappilot.ui.screens.setup

import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.AdsClick
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import io.github.syntaxghost404.tappilot.R
import io.github.syntaxghost404.tappilot.ui.ServiceState
import io.github.syntaxghost404.tappilot.ui.components.DetailScaffold
import io.github.syntaxghost404.tappilot.ui.components.FormCard
import io.github.syntaxghost404.tappilot.ui.components.ShapeBadge
import io.github.syntaxghost404.tappilot.ui.components.fitTo
import kotlinx.coroutines.delay

/** True when the user has turned animations off system-wide. */
@Composable
private fun rememberReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}

/** A slowly turning shape that morphs through the Material shape library. */
@Composable
fun MorphingHero(color: Color, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit = {}) {
    val shapes = remember {
        listOf(MaterialShapes.Cookie9Sided, MaterialShapes.Clover8Leaf, MaterialShapes.SoftBurst, MaterialShapes.Cookie12Sided, MaterialShapes.Flower)
    }
    val morphs = remember { shapes.indices.map { Morph(shapes[it], shapes[(it + 1) % shapes.size]) } }
    var index by remember { mutableIntStateOf(0) }
    val progress = remember { Animatable(0f) }
    val reduceMotion = rememberReduceMotion()
    val morphSpec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    val turn = rememberInfiniteTransition(label = "hero-turn")
    val rotation by turn.animateFloat(
        initialValue = 0f,
        targetValue = if (reduceMotion) 0f else 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 30_000, easing = LinearEasing)),
        label = "hero-rotation",
    )
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) return@LaunchedEffect
        while (true) {
            delay(2_200)
            progress.animateTo(1f, morphSpec)
            index = (index + 1) % morphs.size
            progress.snapTo(0f)
        }
    }
    Box(
        modifier = modifier.drawWithCache {
            val path = Path()
            onDrawBehind {
                path.rewind()
                morphs[index].toPath(progress.value.coerceIn(0f, 1f), path)
                path.fitTo(size, rotation)
                drawPath(path, color)
            }
        },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
private fun AppMark(size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
            modifier = Modifier.size(size * 1.35f),
        )
    }
}

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.size(24.dp))
            MorphingHero(MaterialTheme.colorScheme.primaryContainer, Modifier.size(248.dp)) { AppMark(112.dp) }
            Spacer(Modifier.size(32.dp))
            Text(
                stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.displaySmallEmphasized,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.size(12.dp))
            Text(
                stringResource(R.string.onboarding_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 480.dp),
            )
            Spacer(Modifier.size(28.dp))
            Column(
                modifier = Modifier.widthIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                val features = listOf(
                    Triple(Icons.Rounded.AdsClick, R.string.onboarding_feature_single, MaterialShapes.Cookie9Sided),
                    Triple(Icons.Rounded.Route, R.string.onboarding_feature_multi, MaterialShapes.Clover4Leaf),
                    Triple(Icons.Rounded.Lock, R.string.onboarding_feature_private, MaterialShapes.Pentagon),
                )
                features.forEachIndexed { index, (icon, text, shape) ->
                    SegmentedListItem(
                        shapes = ListItemDefaults.segmentedShapes(index = index, count = features.size),
                        leadingContent = {
                            ShapeBadge(shape, MaterialTheme.colorScheme.secondaryContainer, 44.dp) {
                                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        },
                    ) { Text(stringResource(text), style = MaterialTheme.typography.bodyLarge) }
                }
            }
            Spacer(Modifier.size(32.dp))
            Button(
                onClick = onGetStarted,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .heightIn(min = ButtonDefaults.MediumContainerHeight),
                contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
            ) {
                Text(stringResource(R.string.action_get_started), style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight))
            }
            Spacer(Modifier.size(16.dp))
        }
    }
}

interface ServiceSetupActions {
    fun onBack()
    fun onOpenAccessibility()
    fun onOpenAppInfo()
    fun onDone()
    fun onNotNow()
}

@Composable
fun ServiceSetupScreen(service: ServiceState, actions: ServiceSetupActions) {
    var agreed by rememberSaveable { mutableStateOf(false) }
    val showSteps = agreed || service == ServiceState.Connected
    DetailScaffold(
        title = stringResource(if (showSteps) R.string.setup_title else R.string.disclosure_title),
        onBack = actions::onBack,
    ) { padding ->
        val enter = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
        val exit = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
        AnimatedContent(
            targetState = showSteps,
            transitionSpec = { fadeIn(enter) togetherWith fadeOut(exit) },
            label = "setup-phase",
        ) { steps ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                if (steps) SetupSteps(service, actions) else Disclosure(onAgree = { agreed = true }, onNotNow = actions::onNotNow)
            }
        }
    }
}

@Composable
private fun Disclosure(onAgree: () -> Unit, onNotNow: () -> Unit) {
    FormCard(color = MaterialTheme.colorScheme.secondaryContainer) {
        ShapeBadge(MaterialShapes.SoftBurst, MaterialTheme.colorScheme.onSecondaryContainer, 64.dp) {
            Icon(Icons.Rounded.AccessibilityNew, contentDescription = null, tint = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.size(16.dp))
        Text(stringResource(R.string.disclosure_body), style = MaterialTheme.typography.bodyLarge)
    }
    Spacer(Modifier.size(12.dp))
    val points = listOf(
        Icons.Rounded.TouchApp to R.string.disclosure_point_gestures,
        Icons.Rounded.VisibilityOff to R.string.disclosure_point_no_reading,
        Icons.Rounded.CloudOff to R.string.disclosure_point_no_data,
    )
    points.forEachIndexed { index, (icon, text) ->
        SegmentedListItem(
            shapes = ListItemDefaults.segmentedShapes(index = index, count = points.size),
            leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        ) { Text(stringResource(text), style = MaterialTheme.typography.bodyLarge) }
    }
    Spacer(Modifier.size(20.dp))
    Button(
        onClick = onAgree,
        shapes = ButtonDefaults.shapes(),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ButtonDefaults.MediumContainerHeight),
        contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
    ) {
        Text(stringResource(R.string.action_agree_continue), style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight))
    }
    TextButton(onClick = onNotNow, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_not_now)) }
}

@Composable
private fun SetupSteps(service: ServiceState, actions: ServiceSetupActions) {
    val connected = service == ServiceState.Connected
    FormCard(color = if (connected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (connected) {
                ShapeBadge(MaterialShapes.Cookie9Sided, MaterialTheme.colorScheme.onPrimaryContainer, 56.dp) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primaryContainer)
                }
            } else {
                LoadingIndicator(modifier = Modifier.size(56.dp))
            }
            Spacer(Modifier.size(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (connected) R.string.setup_connected else R.string.setup_waiting),
                    style = MaterialTheme.typography.titleLargeEmphasized,
                )
                if (!connected) {
                    Text(
                        stringResource(R.string.setup_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (connected) {
            Spacer(Modifier.size(16.dp))
            Button(
                onClick = actions::onDone,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    contentColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = ButtonDefaults.MediumContainerHeight),
                contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
            ) {
                Text(stringResource(R.string.action_done), style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight))
            }
        }
    }
    if (!connected) {
        Spacer(Modifier.size(12.dp))
        val steps = listOf(R.string.setup_step_open, R.string.setup_step_find, R.string.setup_step_enable)
        steps.forEachIndexed { index, text ->
            SegmentedListItem(
                shapes = ListItemDefaults.segmentedShapes(index = index, count = steps.size),
                leadingContent = {
                    ShapeBadge(MaterialShapes.Cookie9Sided, MaterialTheme.colorScheme.primaryContainer, 40.dp) {
                        Text(
                            (index + 1).toString(),
                            style = MaterialTheme.typography.titleSmallEmphasized,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                },
            ) { Text(stringResource(text), style = MaterialTheme.typography.bodyLarge) }
        }
        Spacer(Modifier.size(20.dp))
        Button(
            onClick = actions::onOpenAccessibility,
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ButtonDefaults.MediumContainerHeight),
            contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
        ) {
            Text(stringResource(R.string.action_open_settings), style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight))
        }
        Spacer(Modifier.size(20.dp))
        FormCard {
            Text(stringResource(R.string.setup_restricted_title), style = MaterialTheme.typography.titleMediumEmphasized)
            Spacer(Modifier.size(6.dp))
            Text(
                stringResource(R.string.setup_restricted_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(12.dp))
            FilledTonalButton(onClick = actions::onOpenAppInfo, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.action_app_info))
            }
        }
    }
}
