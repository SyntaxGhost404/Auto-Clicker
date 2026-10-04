package io.github.syntaxghost404.tappilot.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonSize
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.syntaxghost404.tappilot.R
import io.github.syntaxghost404.tappilot.core.model.StopMode
import io.github.syntaxghost404.tappilot.core.model.StopRule
import io.github.syntaxghost404.tappilot.core.model.Timing
import io.github.syntaxghost404.tappilot.core.model.Variation
import io.github.syntaxghost404.tappilot.ui.format.TimeUnitChoice
import kotlin.math.roundToInt

/**
 * Single-choice connected button group, the Material 3 Expressive replacement for segmented
 * buttons. The selected item morphs to a fully rounded shape. With an [icon], each option shows its
 * icon above its label, which leaves the label the full width of the button.
 */
@Composable
fun <T> ConnectedChoiceGroup(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    icon: ((T) -> ImageVector)? = null,
    showCheck: Boolean = false,
    size: ToggleButtonSize = ToggleButtonDefaults.size,
    contentPadding: PaddingValues? = null,
    enabled: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, option ->
            val checked = option == selected
            val shapes = when (index) {
                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
            }
            val leading: ImageVector? = when {
                showCheck && checked -> Icons.Rounded.Check
                icon != null -> icon(option)
                else -> null
            }
            val stacked = icon != null
            // Disabled toggle buttons all look alike, so keep the choice visible as disabled
            // segmented buttons do.
            val colors = if (!enabled && checked) {
                ToggleButtonDefaults.colors(
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    disabledContentColor = MaterialTheme.colorScheme.surface,
                )
            } else {
                ToggleButtonDefaults.colors()
            }
            ToggleButton(
                checked = checked,
                onCheckedChange = {
                    if (option != selected) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onSelect(option)
                },
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton },
                enabled = enabled,
                buttonSize = size,
                shapes = shapes,
                colors = colors,
                icon = leading?.takeUnless { stacked }?.let { vector ->
                    { Icon(vector, contentDescription = null, modifier = Modifier.size(ToggleButtonDefaults.IconSize)) }
                },
                contentPadding = contentPadding
                    ?: if (stacked) StackedChoicePadding else ToggleButtonDefaults.contentPaddingFor(size, hasStartIcon = leading != null),
            ) {
                if (stacked && leading != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(leading, contentDescription = null)
                        Spacer(Modifier.height(4.dp))
                        Text(label(option), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                } else {
                    Text(label(option), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

private val StackedChoicePadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)

/** A whole-number field. Emits only valid values within [range]. */
@Composable
fun NumberField(
    value: Long,
    onValueChange: (Long) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    range: LongRange = 0L..Long.MAX_VALUE,
    supportingText: String? = null,
    maxDigits: Int = 7,
    imeAction: ImeAction = ImeAction.Done,
) {
    var text by rememberSaveable { mutableStateOf(value.toString()) }
    LaunchedEffect(value) {
        if (text.toLongOrNull() != value) text = value.toString()
    }
    val parsed = text.toLongOrNull()
    val invalid = parsed == null || parsed !in range
    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            val digits = input.filter(Char::isDigit).take(maxDigits)
            text = digits
            digits.toLongOrNull()?.takeIf { it in range }?.let(onValueChange)
        },
        label = { Text(label, maxLines = 1) },
        supportingText = supportingText?.let { { Text(it) } },
        isError = invalid,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
        modifier = modifier,
    )
}

/**
 * A duration typed as a number plus a unit. Switching the unit keeps the typed number, so "5"
 * then "s" means five seconds, which matches how people say durations out loud.
 */
@Composable
fun DurationField(
    valueMs: Long,
    onValueChange: (Long) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    minMs: Long = 0L,
    maxMs: Long = Timing.MAX_DELAY_MS,
    units: List<TimeUnitChoice> = TimeUnitChoice.entries,
    warnBelowMs: Long? = null,
) {
    var unit by rememberSaveable { mutableStateOf(TimeUnitChoice.bestFor(valueMs, units)) }
    var text by rememberSaveable { mutableStateOf((valueMs / unit.millis).toString()) }

    LaunchedEffect(valueMs) {
        val shown = text.toLongOrNull()?.times(unit.millis)
        if (shown != valueMs) {
            unit = TimeUnitChoice.bestFor(valueMs, units)
            text = (valueMs / unit.millis).toString()
        }
    }

    val typedMs = text.toLongOrNull()?.times(unit.millis)
    val outOfRange = typedMs == null || typedMs < minMs || typedMs > maxMs
    fun emit(newText: String, newUnit: TimeUnitChoice) {
        val ms = newText.toLongOrNull()?.times(newUnit.millis) ?: return
        if (ms in minMs..maxMs) onValueChange(ms)
    }

    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { input ->
                    text = input.filter(Char::isDigit).take(7)
                    emit(text, unit)
                },
                label = { Text(label, maxLines = 1) },
                isError = outOfRange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                modifier = Modifier.weight(1f),
            )
            if (units.size > 1) {
                Spacer(Modifier.width(8.dp))
                ConnectedChoiceGroup(
                    options = units,
                    selected = unit,
                    onSelect = {
                        unit = it
                        emit(text, it)
                    },
                    label = { stringResource(it.label) },
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .width((56 * units.size).dp),
                    size = ToggleButtonSize.Small,
                    contentPadding = PaddingValues(horizontal = 4.dp),
                )
            }
        }
        val warn = warnBelowMs != null && typedMs != null && typedMs < warnBelowMs
        AnimatedVisibility(
            visible = warn,
            enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
            exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()) +
                shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()),
        ) {
            Row(
                modifier = Modifier.padding(start = 16.dp, top = 6.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.warning_fast),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        if (supportingText != null && !warn) {
            Text(
                supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 6.dp, end = 8.dp),
            )
        }
    }
}

/** Hours, minutes and seconds for the "run for" limit. */
@Composable
fun HmsField(valueMs: Long, onValueChange: (Long) -> Unit, modifier: Modifier = Modifier) {
    val totalSeconds = valueMs / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    fun emit(h: Long, m: Long, s: Long) {
        val ms = ((h * 3_600L) + (m * 60L) + s) * 1_000L
        onValueChange(ms.coerceIn(Timing.MIN_RUN_DURATION_MS, Timing.MAX_RUN_DURATION_MS))
    }
    Column(modifier) {
        Text(
            stringResource(R.string.stop_time_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(
                value = hours, onValueChange = { emit(it, minutes, seconds) },
                label = stringResource(R.string.hours_short), range = 0L..99L, maxDigits = 2,
                imeAction = ImeAction.Next, modifier = Modifier.weight(1f),
            )
            NumberField(
                value = minutes, onValueChange = { emit(hours, it, seconds) },
                label = stringResource(R.string.minutes_short), range = 0L..59L, maxDigits = 2,
                imeAction = ImeAction.Next, modifier = Modifier.weight(1f),
            )
            NumberField(
                value = seconds, onValueChange = { emit(hours, minutes, it) },
                label = stringResource(R.string.seconds_short), range = 0L..59L, maxDigits = 2,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun StopRuleEditor(
    rule: StopRule,
    onChange: (StopRule) -> Unit,
    cyclesSupport: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        ConnectedChoiceGroup(
            options = StopMode.entries,
            selected = rule.mode,
            onSelect = { onChange(rule.copy(mode = it)) },
            label = {
                when (it) {
                    StopMode.Never -> stringResource(R.string.stop_never)
                    StopMode.Duration -> stringResource(R.string.stop_time)
                    StopMode.Cycles -> stringResource(R.string.stop_cycles)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 8.dp),
        )
        Spacer(Modifier.size(12.dp))
        val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.IntSize>()
        val fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
        val fadeOutSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
        AnimatedContent(
            targetState = rule.mode,
            transitionSpec = {
                (fadeIn(fadeInSpec) togetherWith fadeOut(fadeOutSpec)).using(SizeTransform(clip = false) { _, _ -> spatial })
            },
            label = "stop-rule",
        ) { mode ->
            when (mode) {
                StopMode.Never -> Text(
                    stringResource(R.string.stop_never_support),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                StopMode.Duration -> HmsField(
                    valueMs = rule.durationMs,
                    onValueChange = { onChange(rule.copy(durationMs = it)) },
                )
                StopMode.Cycles -> NumberField(
                    value = rule.cycles.toLong(),
                    onValueChange = { onChange(rule.copy(cycles = it.toInt())) },
                    label = stringResource(R.string.stop_cycles_label),
                    range = 1L..Timing.MAX_CYCLES.toLong(),
                    supportingText = cyclesSupport,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun VariationEditor(variation: Variation, onChange: (Variation) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SwitchRow(
            title = stringResource(R.string.variation_position),
            supporting = stringResource(R.string.variation_position_support),
            checked = variation.position,
            onCheckedChange = { onChange(variation.copy(position = it)) },
        )
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.variation_timing),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (variation.timingPercent == 0) {
                        stringResource(R.string.variation_timing_off)
                    } else {
                        stringResource(R.string.variation_timing_value, variation.timingPercent)
                    },
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            val sliderState = rememberSliderState(
                value = variation.timingPercent.toFloat(),
                steps = Timing.MAX_TIMING_VARIATION / 5 - 1,
                trackRange = 0f..Timing.MAX_TIMING_VARIATION.toFloat(),
            )
            LaunchedEffect(variation.timingPercent) {
                if (sliderState.value.roundToInt() != variation.timingPercent) {
                    sliderState.value = variation.timingPercent.toFloat()
                }
            }
            val description = stringResource(R.string.variation_timing_value, variation.timingPercent)
            val haptics = LocalHapticFeedback.current
            Slider(
                state = sliderState,
                onValueChange = {
                    sliderState.value = it
                    val percent = it.roundToInt()
                    if (percent != variation.timingPercent) {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                        onChange(variation.copy(timingPercent = percent))
                    }
                },
                modifier = Modifier.semantics { stateDescription = description },
            )
        }
    }
}

/** A labelled switch that toggles when the whole row is tapped. */
@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    enabled: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch) {
                haptics.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                onCheckedChange(it)
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (supporting != null) {
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            thumbContent = if (checked) {
                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
            } else {
                null
            },
        )
    }
}
