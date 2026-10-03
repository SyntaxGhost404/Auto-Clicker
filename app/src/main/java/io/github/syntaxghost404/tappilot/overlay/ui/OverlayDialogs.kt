package io.github.syntaxghost404.tappilot.overlay.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.syntaxghost404.tappilot.AppGraph
import io.github.syntaxghost404.tappilot.R
import io.github.syntaxghost404.tappilot.core.data.BackupCodec
import io.github.syntaxghost404.tappilot.core.model.Script
import io.github.syntaxghost404.tappilot.core.model.SwipeStep
import io.github.syntaxghost404.tappilot.core.model.TapStep
import io.github.syntaxghost404.tappilot.core.model.Timing
import io.github.syntaxghost404.tappilot.overlay.OverlayDialog
import io.github.syntaxghost404.tappilot.overlay.OverlaySession
import io.github.syntaxghost404.tappilot.ui.components.DurationField
import io.github.syntaxghost404.tappilot.ui.components.ShapeBadge
import io.github.syntaxghost404.tappilot.ui.components.StopRuleEditor
import io.github.syntaxghost404.tappilot.ui.components.VariationEditor
import io.github.syntaxghost404.tappilot.ui.components.SequenceAvatar
import io.github.syntaxghost404.tappilot.ui.format.Summaries
import io.github.syntaxghost404.tappilot.ui.format.TimeUnitChoice
import kotlin.math.roundToInt

internal interface DialogActions {
    fun dismiss()
    fun updateScript(transform: (Script) -> Script)
    fun deleteStep(stepId: String)
    fun openSequence(id: String?)
}

/** Full-window host for the dialog currently requested by the session. */
@Composable
internal fun OverlayDialogHost(session: OverlaySession, graph: AppGraph, actions: DialogActions) {
    val dialog = session.dialog ?: return
    val appear = remember { MutableTransitionState(false) }.apply { targetState = true }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { actions.dismiss() }
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visibleState = appear,
            enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                scaleIn(MaterialTheme.motionScheme.defaultSpatialSpec(), initialScale = 0.88f),
        ) {
            BoxWithConstraints {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .padding(20.dp)
                        .widthIn(min = 280.dp, max = 460.dp)
                        .heightIn(max = maxHeight * 0.9f)
                        // Swallow taps so they do not reach the scrim behind the card.
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                ) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                    ) {
                        when (dialog) {
                            is OverlayDialog.EditStep -> StepEditor(session, dialog.stepId, actions)
                            OverlayDialog.Settings -> SessionSettings(session, actions)
                            OverlayDialog.OpenSequence -> OpenSequence(session, graph, actions)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogHeader(title: String, subtitle: String?, badge: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 20.dp)) {
        badge()
        Spacer(Modifier.size(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmallEmphasized)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun IconBadge(icon: ImageVector) {
    ShapeBadge(
        polygon = MaterialShapes.Cookie9Sided,
        color = MaterialTheme.colorScheme.secondaryContainer,
        size = 48.dp,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmallEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
    )
}

@Composable
private fun StepEditor(session: OverlaySession, stepId: String, actions: DialogActions) {
    val index = session.script.steps.indexOfFirst { it.id == stepId }
    val step = session.script.steps.getOrNull(index)
    if (step == null) {
        LaunchedEffect(Unit) { actions.dismiss() }
        return
    }
    val number = index + 1
    val isSwipe = step is SwipeStep
    val title = stringResource(if (isSwipe) R.string.step_editor_swipe_title else R.string.step_editor_tap_title, number)
    val subtitle = when (step) {
        is TapStep -> stringResource(R.string.step_position, step.x.roundToInt(), step.y.roundToInt())
        is SwipeStep -> stringResource(
            R.string.step_swipe_path,
            step.startX.roundToInt(), step.startY.roundToInt(), step.endX.roundToInt(), step.endY.roundToInt(),
        )
    }
    DialogHeader(title, subtitle) {
        ShapeBadge(
            polygon = if (isSwipe) MaterialShapes.Cookie6Sided else MaterialShapes.Cookie9Sided,
            color = if (isSwipe) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
            size = 48.dp,
        ) {
            Text(
                number.toString(),
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = if (isSwipe) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }

    DurationField(
        valueMs = step.delayMs,
        onValueChange = { ms -> actions.updateScript { it.updateStep(stepId) { s -> s.withDelay(ms) } } },
        label = stringResource(R.string.field_wait_after),
        supportingText = stringResource(R.string.field_wait_after_support),
        maxMs = Timing.MAX_DELAY_MS,
        warnBelowMs = Timing.FAST_DELAY_WARNING_MS,
    )
    Spacer(Modifier.size(16.dp))
    when (step) {
        is TapStep -> DurationField(
            valueMs = step.holdMs,
            onValueChange = { ms -> actions.updateScript { it.updateStep(stepId) { s -> (s as TapStep).copy(holdMs = ms) } } },
            label = stringResource(R.string.field_hold),
            supportingText = stringResource(R.string.field_hold_support),
            minMs = Timing.MIN_HOLD_MS,
            maxMs = Timing.MAX_GESTURE_MS,
            units = listOf(TimeUnitChoice.Milliseconds, TimeUnitChoice.Seconds),
        )
        is SwipeStep -> DurationField(
            valueMs = step.durationMs,
            onValueChange = { ms -> actions.updateScript { it.updateStep(stepId) { s -> (s as SwipeStep).copy(durationMs = ms) } } },
            label = stringResource(R.string.field_swipe_duration),
            supportingText = stringResource(R.string.field_swipe_duration_support),
            minMs = Timing.MIN_SWIPE_MS,
            maxMs = Timing.MAX_GESTURE_MS,
            units = listOf(TimeUnitChoice.Milliseconds, TimeUnitChoice.Seconds),
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = { actions.deleteStep(stepId) },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.action_delete_step))
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = actions::dismiss, shapes = ButtonDefaults.shapes()) {
            Text(stringResource(R.string.action_done))
        }
    }
}

@Composable
private fun SessionSettings(session: OverlaySession, actions: DialogActions) {
    val script = session.script
    DialogHeader(
        title = stringResource(if (session.isMulti) R.string.overlay_settings_multi else R.string.overlay_settings_single),
        subtitle = if (session.isMulti) script.name.ifBlank { null } else null,
    ) { IconBadge(Icons.Rounded.Tune) }

    if (session.isMulti) {
        OutlinedTextField(
            value = script.name,
            onValueChange = { name -> actions.updateScript { it.copy(name = name.take(BackupCodec.MAX_NAME_LENGTH)) } },
            label = { Text(stringResource(R.string.name_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        val target = script.steps.firstOrNull()
        if (target != null) {
            DurationField(
                valueMs = target.delayMs,
                onValueChange = { ms -> actions.updateScript { it.updateStep(target.id) { s -> s.withDelay(ms) } } },
                label = stringResource(R.string.field_interval),
                supportingText = stringResource(R.string.field_interval_support),
                warnBelowMs = Timing.FAST_DELAY_WARNING_MS,
            )
        }
    }

    SectionLabel(stringResource(R.string.editor_when_to_stop))
    StopRuleEditor(
        rule = script.stopRule,
        onChange = { rule -> actions.updateScript { it.copy(stopRule = rule) } },
        cyclesSupport = stringResource(
            if (session.isMulti) R.string.stop_cycles_support_multi else R.string.stop_cycles_support_single,
        ),
    )

    SectionLabel(stringResource(R.string.variation_title))
    VariationEditor(
        variation = script.variation,
        onChange = { variation -> actions.updateScript { it.copy(variation = variation) } },
    )

    Row(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.End) {
        Button(onClick = actions::dismiss, shapes = ButtonDefaults.shapes()) {
            Text(stringResource(R.string.action_done))
        }
    }
}

@Composable
private fun OpenSequence(session: OverlaySession, graph: AppGraph, actions: DialogActions) {
    val scripts by graph.scripts.scripts.collectAsState(initial = emptyList())
    val resources = LocalResources.current
    DialogHeader(title = stringResource(R.string.overlay_open_title), subtitle = null) { IconBadge(Icons.Rounded.FolderOpen) }

    FilledTonalButton(
        onClick = { actions.openSequence(null) },
        shapes = ButtonDefaults.shapes(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(stringResource(R.string.action_new_sequence))
    }
    Spacer(Modifier.size(16.dp))

    if (scripts.isEmpty()) {
        Text(
            stringResource(R.string.overlay_no_saved),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
            scripts.forEachIndexed { index, script ->
                val current = session.persisted && script.id == session.script.id
                SegmentedListItem(
                    onClick = { actions.openSequence(script.id) },
                    shapes = ListItemDefaults.segmentedShapes(index = index, count = scripts.size),
                    leadingContent = { SequenceAvatar(script) },
                    supportingContent = {
                        Text(
                            Summaries.composition(resources, script),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    trailingContent = if (current) {
                        {
                            Text(
                                stringResource(R.string.overlay_current),
                                style = MaterialTheme.typography.labelMediumEmphasized,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        null
                    },
                ) {
                    Text(script.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }

    Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = actions::dismiss) { Text(stringResource(R.string.action_cancel)) }
    }
}
