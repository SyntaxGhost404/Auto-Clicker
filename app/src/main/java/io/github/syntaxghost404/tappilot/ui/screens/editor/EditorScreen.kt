package io.github.syntaxghost404.tappilot.ui.screens.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.syntaxghost404.tappilot.R
import io.github.syntaxghost404.tappilot.core.model.Script
import io.github.syntaxghost404.tappilot.core.model.Step
import io.github.syntaxghost404.tappilot.core.model.StopRule
import io.github.syntaxghost404.tappilot.core.model.SwipeStep
import io.github.syntaxghost404.tappilot.core.model.TapStep
import io.github.syntaxghost404.tappilot.core.model.Timing
import io.github.syntaxghost404.tappilot.core.model.Variation
import io.github.syntaxghost404.tappilot.ui.components.DetailScaffold
import io.github.syntaxghost404.tappilot.ui.components.DurationField
import io.github.syntaxghost404.tappilot.ui.components.FormCard
import io.github.syntaxghost404.tappilot.ui.components.NumberField
import io.github.syntaxghost404.tappilot.ui.components.SectionTitle
import io.github.syntaxghost404.tappilot.ui.components.ShapeBadge
import io.github.syntaxghost404.tappilot.ui.components.StopRuleEditor
import io.github.syntaxghost404.tappilot.ui.components.VariationEditor
import io.github.syntaxghost404.tappilot.ui.format.Durations
import io.github.syntaxghost404.tappilot.ui.format.Summaries
import io.github.syntaxghost404.tappilot.ui.format.TimeUnitChoice
import kotlin.math.roundToInt

interface EditorActions {
    fun onBack()
    fun onStart()
    fun onRename()
    fun onDuplicate()
    fun onExport()
    fun onDelete()
    fun onStopRule(rule: StopRule)
    fun onVariation(variation: Variation)
    fun onUpdateStep(stepId: String, transform: (Step) -> Step)
    fun onDeleteStep(stepId: String)
    fun onMoveStep(stepId: String, offset: Int)
}

private const val LONG_PRESS_MS = 500L

@Composable
fun EditorScreen(state: EditorState, actions: EditorActions, snackbarHostState: SnackbarHostState) {
    val resources = LocalResources.current
    val script = (state as? EditorState.Loaded)?.script
    var menu by remember { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf<String?>(null) }

    DetailScaffold(
        title = script?.name.orEmpty(),
        subtitle = script?.let { Summaries.composition(resources, it) },
        onBack = actions::onBack,
        snackbarHostState = snackbarHostState,
        actions = {
            if (script != null) {
                IconButton(onClick = actions::onRename, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.Rounded.DriveFileRenameOutline, contentDescription = stringResource(R.string.action_rename))
                }
                Box {
                    IconButton(onClick = { menu = true }, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.more_options))
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_duplicate)) },
                            leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                            onClick = {
                                menu = false
                                actions.onDuplicate()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_export)) },
                            leadingIcon = { Icon(Icons.Rounded.FileUpload, contentDescription = null) },
                            onClick = {
                                menu = false
                                actions.onExport()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_delete)) },
                            leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                            onClick = {
                                menu = false
                                actions.onDelete()
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        when (state) {
            EditorState.Loading -> Box(Modifier.fillMaxSize())
            EditorState.Missing -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.editor_not_found), style = MaterialTheme.typography.bodyLarge)
            }
            is EditorState.Loaded -> EditorContent(state.script, padding, actions, onEditStep = { editing = it })
        }
    }

    val editingStep = script?.steps?.firstOrNull { it.id == editing }
    if (script != null && editingStep != null) {
        StepSheet(
            step = editingStep,
            number = script.steps.indexOf(editingStep) + 1,
            onUpdate = { transform -> actions.onUpdateStep(editingStep.id, transform) },
            onDelete = {
                actions.onDeleteStep(editingStep.id)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun EditorContent(script: Script, padding: PaddingValues, actions: EditorActions, onEditStep: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        item(key = "start") {
            Button(
                onClick = actions::onStart,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = ButtonDefaults.MediumContainerHeight)
                    .padding(bottom = 16.dp),
                contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(ButtonDefaults.iconSizeFor(ButtonDefaults.MediumContainerHeight)))
                Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(ButtonDefaults.MediumContainerHeight)))
                Text(stringResource(R.string.action_start), style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight))
            }
        }
        item(key = "steps-title") { SectionTitle(stringResource(R.string.editor_steps)) }
        if (script.steps.isEmpty()) {
            item(key = "steps-empty") {
                FormCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ShapeBadge(MaterialShapes.Cookie9Sided, MaterialTheme.colorScheme.secondaryContainer, 48.dp) {
                            Icon(Icons.Rounded.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Spacer(Modifier.size(16.dp))
                        Column {
                            Text(stringResource(R.string.editor_no_steps_title), style = MaterialTheme.typography.titleMediumEmphasized)
                            Text(
                                stringResource(R.string.editor_no_steps_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        itemsIndexed(script.steps, key = { _, step -> step.id }) { index, step ->
            StepRow(
                step = step,
                index = index,
                count = script.steps.size,
                actions = actions,
                onClick = { onEditStep(step.id) },
                modifier = Modifier.animateItem(),
            )
        }
        item(key = "stop-title") { SectionTitle(stringResource(R.string.editor_when_to_stop), Modifier.padding(top = 16.dp)) }
        item(key = "stop") {
            FormCard {
                StopRuleEditor(
                    rule = script.stopRule,
                    onChange = actions::onStopRule,
                    cyclesSupport = stringResource(R.string.stop_cycles_support_multi),
                )
            }
        }
        item(key = "variation-title") { SectionTitle(stringResource(R.string.variation_title), Modifier.padding(top = 16.dp)) }
        item(key = "variation") {
            FormCard { VariationEditor(variation = script.variation, onChange = actions::onVariation) }
        }
    }
}

@Composable
private fun StepRow(step: Step, index: Int, count: Int, actions: EditorActions, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val resources = LocalResources.current
    var menu by remember { mutableStateOf(false) }
    val isSwipe = step is SwipeStep
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        modifier = modifier,
        leadingContent = {
            ShapeBadge(
                polygon = if (isSwipe) MaterialShapes.Cookie6Sided else MaterialShapes.Cookie9Sided,
                color = if (isSwipe) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
                size = 44.dp,
            ) {
                Text(
                    (index + 1).toString(),
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    color = if (isSwipe) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        },
        supportingContent = {
            Column {
                Text(
                    when (step) {
                        is TapStep -> stringResource(R.string.step_position, step.x.roundToInt(), step.y.roundToInt())
                        is SwipeStep -> stringResource(
                            R.string.step_swipe_path,
                            step.startX.roundToInt(), step.startY.roundToInt(), step.endX.roundToInt(), step.endY.roundToInt(),
                        )
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    when (step) {
                        is TapStep -> stringResource(
                            R.string.step_timing_tap,
                            Durations.format(resources, step.holdMs),
                            Durations.format(resources, step.delayMs),
                        )
                        is SwipeStep -> stringResource(
                            R.string.step_timing_swipe,
                            Durations.format(resources, step.durationMs),
                            Durations.format(resources, step.delayMs),
                        )
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        trailingContent = {
            Box {
                IconButton(onClick = { menu = true }, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.more_options))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.move_up)) },
                        leadingIcon = { Icon(Icons.Rounded.ArrowUpward, contentDescription = null) },
                        enabled = index > 0,
                        onClick = {
                            menu = false
                            actions.onMoveStep(step.id, -1)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.move_down)) },
                        leadingIcon = { Icon(Icons.Rounded.ArrowDownward, contentDescription = null) },
                        enabled = index < count - 1,
                        onClick = {
                            menu = false
                            actions.onMoveStep(step.id, 1)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete_step)) },
                        leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                        onClick = {
                            menu = false
                            actions.onDeleteStep(step.id)
                        },
                    )
                }
            }
        },
    ) {
        Text(
            stringResource(
                when {
                    step is SwipeStep -> R.string.step_swipe
                    step is TapStep && step.holdMs >= LONG_PRESS_MS -> R.string.step_long_press
                    else -> R.string.step_tap
                },
            ),
        )
    }
}

@Composable
private fun StepSheet(step: Step, number: Int, onUpdate: ((Step) -> Step) -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(if (step is SwipeStep) R.string.step_editor_swipe_title else R.string.step_editor_tap_title, number),
                style = MaterialTheme.typography.headlineSmallEmphasized,
            )
            DurationField(
                valueMs = step.delayMs,
                onValueChange = { ms -> onUpdate { it.withDelay(ms) } },
                label = stringResource(R.string.field_wait_after),
                supportingText = stringResource(R.string.field_wait_after_support),
                warnBelowMs = Timing.FAST_DELAY_WARNING_MS,
            )
            when (step) {
                is TapStep -> {
                    DurationField(
                        valueMs = step.holdMs,
                        onValueChange = { ms -> onUpdate { (it as TapStep).copy(holdMs = ms) } },
                        label = stringResource(R.string.field_hold),
                        supportingText = stringResource(R.string.field_hold_support),
                        minMs = Timing.MIN_HOLD_MS,
                        maxMs = Timing.MAX_GESTURE_MS,
                        units = listOf(TimeUnitChoice.Milliseconds, TimeUnitChoice.Seconds),
                    )
                    PositionFields(step.x, step.y) { x, y -> onUpdate { (it as TapStep).copy(x = x, y = y) } }
                }
                is SwipeStep -> {
                    DurationField(
                        valueMs = step.durationMs,
                        onValueChange = { ms -> onUpdate { (it as SwipeStep).copy(durationMs = ms) } },
                        label = stringResource(R.string.field_swipe_duration),
                        supportingText = stringResource(R.string.field_swipe_duration_support),
                        minMs = Timing.MIN_SWIPE_MS,
                        maxMs = Timing.MAX_GESTURE_MS,
                        units = listOf(TimeUnitChoice.Milliseconds, TimeUnitChoice.Seconds),
                    )
                    PositionFields(step.startX, step.startY) { x, y -> onUpdate { (it as SwipeStep).copy(startX = x, startY = y) } }
                    PositionFields(step.endX, step.endY) { x, y -> onUpdate { (it as SwipeStep).copy(endX = x, endY = y) } }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.action_delete_step))
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = onDismiss, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.action_done)) }
            }
        }
    }
}

@Composable
private fun PositionFields(x: Float, y: Float, onChange: (Float, Float) -> Unit) {
    Column {
        Text(
            stringResource(R.string.field_position),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(
                value = x.roundToInt().toLong(),
                onValueChange = { onChange(it.toFloat(), y) },
                label = "X",
                range = 0L..MAX_COORDINATE,
                maxDigits = 5,
                modifier = Modifier.weight(1f),
            )
            NumberField(
                value = y.roundToInt().toLong(),
                onValueChange = { onChange(x, it.toFloat()) },
                label = "Y",
                range = 0L..MAX_COORDINATE,
                maxDigits = 5,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private const val MAX_COORDINATE = 20_000L
