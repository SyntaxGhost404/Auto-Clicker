package io.github.syntaxghost404.tappilot.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.syntaxghost404.tappilot.AppGraph
import io.github.syntaxghost404.tappilot.BuildConfig
import io.github.syntaxghost404.tappilot.R
import io.github.syntaxghost404.tappilot.core.data.AppSettings
import io.github.syntaxghost404.tappilot.core.data.ControlSize
import io.github.syntaxghost404.tappilot.core.data.ThemeMode
import io.github.syntaxghost404.tappilot.core.model.Timing
import io.github.syntaxghost404.tappilot.ui.components.ConnectedChoiceGroup
import io.github.syntaxghost404.tappilot.ui.components.DurationField
import io.github.syntaxghost404.tappilot.ui.components.SectionTitle
import io.github.syntaxghost404.tappilot.ui.components.SegmentedPanel
import io.github.syntaxghost404.tappilot.ui.components.ShapeBadge
import io.github.syntaxghost404.tappilot.ui.components.TopLevelScaffold
import io.github.syntaxghost404.tappilot.ui.format.Durations
import io.github.syntaxghost404.tappilot.ui.format.TimeUnitChoice
import io.github.syntaxghost404.tappilot.ui.navigation.Settings
import io.github.syntaxghost404.tappilot.ui.navigation.TopLevelRoute
import io.github.syntaxghost404.tappilot.ui.theme.supportsDynamicColor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class SettingsViewModel(private val graph: AppGraph) : ViewModel() {
    val settings: StateFlow<AppSettings?> = graph.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun update(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    fun setTheme(mode: ThemeMode) = update { graph.settings.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = update { graph.settings.setDynamicColor(enabled) }
    fun setControlSize(size: ControlSize) = update { graph.settings.setControlSize(size) }
    fun setMarkerSize(dp: Int) = update { graph.settings.setMarkerSize(dp) }
    fun setTapFeedback(enabled: Boolean) = update { graph.settings.setTapFeedback(enabled) }
    fun setKeepScreenOn(enabled: Boolean) = update { graph.settings.setKeepScreenOn(enabled) }
    fun setStopOnScreenOff(enabled: Boolean) = update { graph.settings.setStopOnScreenOff(enabled) }
    fun setDefaultDelay(ms: Long) = update { graph.settings.setDefaultDelay(ms) }
    fun setDefaultHold(ms: Long) = update { graph.settings.setDefaultHold(ms) }
    fun setDefaultSwipe(ms: Long) = update { graph.settings.setDefaultSwipe(ms) }
}

interface SettingsActions {
    fun onSelectTab(tab: TopLevelRoute)
    fun onTheme(mode: ThemeMode)
    fun onDynamicColor(enabled: Boolean)
    fun onControlSize(size: ControlSize)
    fun onMarkerSize(dp: Int)
    fun onTapFeedback(enabled: Boolean)
    fun onKeepScreenOn(enabled: Boolean)
    fun onStopOnScreenOff(enabled: Boolean)
    fun onDefaultDelay(ms: Long)
    fun onDefaultHold(ms: Long)
    fun onDefaultSwipe(ms: Long)
    fun onExportAll()
    fun onImport()
    fun onHowTo()
    fun onTroubleshooting()
    fun onAbout()
}

private enum class DefaultField { Delay, Hold, Swipe }

@Composable
fun SettingsScreen(settings: AppSettings?, actions: SettingsActions, snackbarHostState: SnackbarHostState) {
    var editing by rememberSaveable { mutableStateOf<DefaultField?>(null) }
    TopLevelScaffold(
        current = Settings,
        onSelectTab = actions::onSelectTab,
        title = stringResource(R.string.settings_title),
        subtitle = stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
        snackbarHostState = snackbarHostState,
    ) { padding ->
        if (settings == null) {
            Box(Modifier.fillMaxSize())
            return@TopLevelScaffold
        }
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
            appearance(settings, actions)
            controls(settings, actions)
            running(settings, actions)
            defaults(settings, onEdit = { editing = it })
            backup(actions)
            help(actions)
        }
    }

    editing?.let { field ->
        if (settings != null) {
            DefaultDurationDialog(
                field = field,
                settings = settings,
                onConfirm = { ms ->
                    when (field) {
                        DefaultField.Delay -> actions.onDefaultDelay(ms)
                        DefaultField.Hold -> actions.onDefaultHold(ms)
                        DefaultField.Swipe -> actions.onDefaultSwipe(ms)
                    }
                    editing = null
                },
                onDismiss = { editing = null },
            )
        }
    }
}

private fun LazyListScope.group(key: String, title: Int) {
    item(key = key) {
        SectionTitle(stringResource(title), Modifier.padding(top = if (key == "appearance") 0.dp else 20.dp))
    }
}

private fun LazyListScope.appearance(settings: AppSettings, actions: SettingsActions) {
    group("appearance", R.string.settings_appearance)
    val count = if (supportsDynamicColor) 2 else 1
    item(key = "theme") {
        SegmentedPanel(index = 0, count = count) {
            Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(12.dp))
            ConnectedChoiceGroup(
                options = ThemeMode.entries,
                selected = settings.themeMode,
                onSelect = actions::onTheme,
                label = {
                    stringResource(
                        when (it) {
                            ThemeMode.System -> R.string.theme_system
                            ThemeMode.Light -> R.string.theme_light
                            ThemeMode.Dark -> R.string.theme_dark
                        },
                    )
                },
                icon = {
                    when (it) {
                        ThemeMode.System -> Icons.Rounded.BrightnessAuto
                        ThemeMode.Light -> Icons.Rounded.LightMode
                        ThemeMode.Dark -> Icons.Rounded.DarkMode
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    if (supportsDynamicColor) {
        item(key = "dynamic") {
            SwitchItem(
                index = 1,
                count = 2,
                icon = Icons.Rounded.Palette,
                title = stringResource(R.string.settings_dynamic_color),
                supporting = stringResource(R.string.settings_dynamic_color_support),
                checked = settings.dynamicColor,
                onCheckedChange = actions::onDynamicColor,
            )
        }
    }
}

private fun LazyListScope.controls(settings: AppSettings, actions: SettingsActions) {
    group("controls", R.string.settings_controls)
    item(key = "control-size") {
        SegmentedPanel(index = 0, count = 4) {
            Text(stringResource(R.string.settings_control_size), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(12.dp))
            ConnectedChoiceGroup(
                options = ControlSize.entries,
                selected = settings.controlSize,
                onSelect = actions::onControlSize,
                label = {
                    stringResource(
                        when (it) {
                            ControlSize.Compact -> R.string.size_compact
                            ControlSize.Regular -> R.string.size_regular
                            ControlSize.Large -> R.string.size_large
                        },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    item(key = "marker-size") { MarkerSizePanel(settings.markerSizeDp, actions::onMarkerSize) }
    item(key = "feedback") {
        SwitchItem(
            index = 2, count = 4,
            title = stringResource(R.string.settings_tap_feedback),
            supporting = stringResource(R.string.settings_tap_feedback_support),
            checked = settings.tapFeedback,
            onCheckedChange = actions::onTapFeedback,
        )
    }
    item(key = "keep-on") {
        SwitchItem(
            index = 3, count = 4,
            title = stringResource(R.string.settings_keep_screen_on),
            supporting = stringResource(R.string.settings_keep_screen_on_support),
            checked = settings.keepScreenOn,
            onCheckedChange = actions::onKeepScreenOn,
        )
    }
}

@Composable
private fun MarkerSizePanel(sizeDp: Int, onChange: (Int) -> Unit) {
    val steps = (AppSettings.MAX_MARKER_DP - AppSettings.MIN_MARKER_DP) / 4 - 1
    val state = rememberSliderState(
        value = sizeDp.toFloat(),
        steps = steps,
        trackRange = AppSettings.MIN_MARKER_DP.toFloat()..AppSettings.MAX_MARKER_DP.toFloat(),
    )
    LaunchedEffect(sizeDp) {
        if (state.value.roundToInt() != sizeDp) state.value = sizeDp.toFloat()
    }
    val previewDp = state.value.roundToInt()
    SegmentedPanel(index = 1, count = 4) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_target_size), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.settings_target_size_value, previewDp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(Modifier.size(AppSettings.MAX_MARKER_DP.dp), contentAlignment = Alignment.Center) {
                MarkerPreview(previewDp)
            }
        }
        Slider(
            state = state,
            onValueChange = { state.value = it },
            onValueChangeFinished = { onChange(state.value.roundToInt()) },
        )
    }
}

/** A static rendering of a target marker at the chosen size. */
@Composable
private fun MarkerPreview(sizeDp: Int) {
    val primary = MaterialTheme.colorScheme.primary
    val container = MaterialTheme.colorScheme.primaryContainer
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .drawBehind {
                val ring = 2.dp.toPx()
                val radius = size.minDimension / 2f - ring
                drawCircle(primary.copy(alpha = 0.16f), radius)
                drawCircle(primary, radius, style = Stroke(ring))
            },
        contentAlignment = Alignment.Center,
    ) {
        ShapeBadge(MaterialShapes.Cookie9Sided, container, (sizeDp * 0.56f).dp) {
            Text("1", style = MaterialTheme.typography.labelLargeEmphasized, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

private fun LazyListScope.running(settings: AppSettings, actions: SettingsActions) {
    group("running", R.string.settings_running)
    item(key = "screen-off") {
        SwitchItem(
            index = 0, count = 1,
            title = stringResource(R.string.settings_stop_screen_off),
            supporting = stringResource(R.string.settings_stop_screen_off_support),
            checked = settings.stopOnScreenOff,
            onCheckedChange = actions::onStopOnScreenOff,
        )
    }
}

private fun LazyListScope.defaults(settings: AppSettings, onEdit: (DefaultField) -> Unit) {
    group("defaults", R.string.settings_defaults)
    val rows = listOf(
        Triple(DefaultField.Delay, R.string.settings_default_delay, settings.defaultDelayMs),
        Triple(DefaultField.Hold, R.string.settings_default_hold, settings.defaultHoldMs),
        Triple(DefaultField.Swipe, R.string.settings_default_swipe, settings.defaultSwipeMs),
    )
    rows.forEachIndexed { index, (field, label, value) ->
        item(key = "default-$field") {
            val resources = LocalResources.current
            SegmentedListItem(
                onClick = { onEdit(field) },
                shapes = ListItemDefaults.segmentedShapes(index = index, count = rows.size),
                trailingContent = {
                    Text(
                        Durations.format(resources, value),
                        style = MaterialTheme.typography.labelLargeEmphasized,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
            ) { Text(stringResource(label)) }
        }
    }
}

private fun LazyListScope.backup(actions: SettingsActions) {
    group("backup", R.string.settings_backup)
    item(key = "export") {
        NavItem(0, 2, Icons.Rounded.Upload, stringResource(R.string.action_export_all), stringResource(R.string.settings_export_support), actions::onExportAll)
    }
    item(key = "import") {
        NavItem(1, 2, Icons.Rounded.Download, stringResource(R.string.action_import), stringResource(R.string.settings_import_support), actions::onImport)
    }
}

private fun LazyListScope.help(actions: SettingsActions) {
    group("help", R.string.settings_help)
    item(key = "howto") { NavItem(0, 3, Icons.AutoMirrored.Rounded.HelpOutline, stringResource(R.string.settings_how_to), null, actions::onHowTo) }
    item(key = "troubleshooting") { NavItem(1, 3, Icons.Rounded.Build, stringResource(R.string.settings_troubleshooting), null, actions::onTroubleshooting) }
    item(key = "about") {
        NavItem(
            2, 3, Icons.Rounded.PrivacyTip,
            stringResource(R.string.settings_privacy) + " · " + stringResource(R.string.settings_about),
            null,
            actions::onAbout,
        )
    }
}

@Composable
private fun NavItem(index: Int, count: Int, icon: ImageVector, title: String, supporting: String?, onClick: () -> Unit) {
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        leadingContent = { Icon(icon, contentDescription = null) },
        supportingContent = supporting?.let { { Text(it) } },
        trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null) },
    ) { Text(title) }
}

@Composable
private fun SwitchItem(
    index: Int,
    count: Int,
    title: String,
    supporting: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
) {
    // Neutral container in both states: the switch already shows the state, so the row stays calm.
    val neutral = ListItemDefaults.segmentedColors()
    SegmentedListItem(
        checked = checked,
        onCheckedChange = onCheckedChange,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = ListItemDefaults.segmentedColors(
            selectedContainerColor = neutral.containerColor,
            selectedContentColor = neutral.contentColor,
            selectedLeadingContentColor = neutral.leadingContentColor,
            selectedTrailingContentColor = neutral.trailingContentColor,
            selectedSupportingContentColor = neutral.supportingContentColor,
        ),
        leadingContent = icon?.let { { Icon(it, contentDescription = null) } },
        supportingContent = supporting?.let { { Text(it) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
    ) { Text(title) }
}

@Composable
private fun DefaultDurationDialog(field: DefaultField, settings: AppSettings, onConfirm: (Long) -> Unit, onDismiss: () -> Unit) {
    val initial = when (field) {
        DefaultField.Delay -> settings.defaultDelayMs
        DefaultField.Hold -> settings.defaultHoldMs
        DefaultField.Swipe -> settings.defaultSwipeMs
    }
    var value by rememberSaveable { mutableLongStateOf(initial) }
    val title = stringResource(
        when (field) {
            DefaultField.Delay -> R.string.settings_default_delay
            DefaultField.Hold -> R.string.settings_default_hold
            DefaultField.Swipe -> R.string.settings_default_swipe
        },
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Info, contentDescription = null) },
        title = { Text(title) },
        text = {
            DurationField(
                valueMs = value,
                onValueChange = { value = it },
                label = title,
                minMs = when (field) {
                    DefaultField.Delay -> 0L
                    DefaultField.Hold -> Timing.MIN_HOLD_MS
                    DefaultField.Swipe -> Timing.MIN_SWIPE_MS
                },
                maxMs = if (field == DefaultField.Delay) Timing.MAX_DELAY_MS else Timing.MAX_GESTURE_MS,
                units = if (field == DefaultField.Delay) TimeUnitChoice.entries else listOf(TimeUnitChoice.Milliseconds, TimeUnitChoice.Seconds),
                warnBelowMs = if (field == DefaultField.Delay) Timing.FAST_DELAY_WARNING_MS else null,
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(value) }, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
