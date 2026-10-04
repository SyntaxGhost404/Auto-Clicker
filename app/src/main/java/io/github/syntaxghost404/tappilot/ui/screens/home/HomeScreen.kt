package io.github.syntaxghost404.tappilot.ui.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.AdsClick
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.SyncProblem
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.syntaxghost404.tappilot.R
import io.github.syntaxghost404.tappilot.core.data.OverlayMode
import io.github.syntaxghost404.tappilot.core.model.Script
import io.github.syntaxghost404.tappilot.core.model.StopMode
import io.github.syntaxghost404.tappilot.service.OverlayStatus
import io.github.syntaxghost404.tappilot.ui.ServiceState
import io.github.syntaxghost404.tappilot.ui.components.SequenceAvatar
import io.github.syntaxghost404.tappilot.ui.components.ShapeBadge
import io.github.syntaxghost404.tappilot.ui.components.OnCardColors
import io.github.syntaxghost404.tappilot.ui.components.TopLevelScaffold
import io.github.syntaxghost404.tappilot.ui.components.onCardColors
import io.github.syntaxghost404.tappilot.ui.format.Durations
import io.github.syntaxghost404.tappilot.ui.format.Summaries
import io.github.syntaxghost404.tappilot.ui.navigation.Home
import io.github.syntaxghost404.tappilot.ui.navigation.TopLevelRoute
import io.github.syntaxghost404.tappilot.ui.withHaptic

data class HomeState(
    val service: ServiceState,
    val status: OverlayStatus,
    val quick: Script?,
    val recent: List<Script>,
    /** The service has worked on this device before, so if it is off now, it was turned off. */
    val serviceSetUp: Boolean = false,
    /** The user has agreed to the accessibility disclosure. */
    val consented: Boolean = false,
)

interface HomeActions {
    fun onSelectTab(tab: TopLevelRoute)

    /** Walks the user through turning the service on, starting with the disclosure if needed. */
    fun onTurnOn()

    /** Opens the system accessibility settings, where the service is switched on and off. */
    fun onOpenAccessibility()
    fun onTroubleshoot()
    fun onStartSingle()
    fun onSingleSettings()
    fun onNewSequence()
    fun onOpenLibrary()
    fun onOpenSequence(id: String)
    fun onStartSequence(id: String)
    fun onStop()
    fun onHideControls()
}

@Composable
fun HomeScreen(state: HomeState, actions: HomeActions) {
    val resources = LocalResources.current
    val status = state.status
    val subtitle = when {
        state.service == ServiceState.Starting -> stringResource(R.string.home_status_starting)
        state.service == ServiceState.Stuck -> stringResource(R.string.home_status_stuck)
        state.service != ServiceState.Connected -> stringResource(R.string.home_status_off)
        status is OverlayStatus.Visible && status.running -> stringResource(
            R.string.home_status_running,
            resources.getQuantityString(R.plurals.action_count, status.progress.actions.toInt().coerceAtMost(Int.MAX_VALUE), Durations.count(status.progress.actions)),
        )
        status is OverlayStatus.Visible -> stringResource(R.string.home_status_controls)
        else -> stringResource(R.string.home_status_ready)
    }
    TopLevelScaffold(
        current = Home,
        onSelectTab = actions::onSelectTab,
        title = stringResource(R.string.app_name),
        subtitle = subtitle,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "service") {
                val resize = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()
                val enter = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
                val exit = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
                // Full width throughout, so that showing, hiding or changing the card only animates its height.
                AnimatedContent(
                    targetState = serviceIssue(state),
                    transitionSpec = { fadeIn(enter) togetherWith fadeOut(exit) using SizeTransform { _, _ -> resize } },
                    contentAlignment = Alignment.TopCenter,
                    label = "service-card",
                    modifier = Modifier.fillMaxWidth(),
                ) { issue ->
                    if (issue != null) ServiceCard(issue, state.consented, actions)
                }
            }
            item(key = "live") {
                AnimatedVisibility(
                    visible = status is OverlayStatus.Visible,
                    enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                        expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
                    exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()) +
                        shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
                ) {
                    if (status is OverlayStatus.Visible) LiveCard(status, actions)
                }
            }
            item(key = "single") { SingleCard(state.quick, actions) }
            item(key = "multi") { SequenceCard(actions) }
            if (state.recent.isNotEmpty()) {
                item(key = "recent-header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.home_recent),
                            style = MaterialTheme.typography.titleMediumEmphasized,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                        )
                        TextButton(onClick = actions::onOpenLibrary) { Text(stringResource(R.string.action_see_all)) }
                    }
                }
                item(key = "recent") {
                    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                        state.recent.forEachIndexed { index, script ->
                            SegmentedListItem(
                                onClick = { actions.onOpenSequence(script.id) },
                                shapes = ListItemDefaults.segmentedShapes(index = index, count = state.recent.size),
                                leadingContent = { SequenceAvatar(script) },
                                supportingContent = {
                                    Text(
                                        Summaries.composition(resources, script) + " · " + Summaries.stopRule(resources, script.stopRule),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                trailingContent = {
                                    FilledTonalIconButton(
                                        onClick = withHaptic { actions.onStartSequence(script.id) },
                                        shapes = IconButtonDefaults.shapes(),
                                    ) {
                                        Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.action_start))
                                    }
                                },
                            ) {
                                Text(script.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Why the service card is showing. Each case asks for something different. */
private enum class ServiceIssue {
    /** Never turned on: the first-run setup. */
    NotSetUp,

    /** Worked before, and has since been switched off, by the user or by the phone. */
    TurnedOff,

    /** On in settings, but not running. Turning it off and on again restarts it. */
    NotResponding,
}

/** The card the service calls for, or null while it works or is still starting. */
private fun serviceIssue(state: HomeState): ServiceIssue? = when (state.service) {
    ServiceState.Connected, ServiceState.Starting -> null
    ServiceState.Stuck -> ServiceIssue.NotResponding
    ServiceState.Off -> if (state.serviceSetUp) ServiceIssue.TurnedOff else ServiceIssue.NotSetUp
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ServiceCard(issue: ServiceIssue, consented: Boolean, actions: HomeActions) {
    val colors = MaterialTheme.colorScheme
    val error = issue == ServiceIssue.NotResponding
    val container = if (error) colors.errorContainer else colors.inverseSurface
    val content = if (error) colors.onErrorContainer else colors.inverseOnSurface
    val strong = if (error) colors.error else colors.inversePrimary
    val onStrong = if (error) colors.onError else colors.inverseSurface
    Surface(
        shape = MaterialTheme.shapes.extraLargeIncreased,
        color = container,
        contentColor = content,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShapeBadge(
                    polygon = when (issue) {
                        ServiceIssue.NotSetUp, ServiceIssue.NotResponding -> MaterialShapes.SoftBurst
                        ServiceIssue.TurnedOff -> MaterialShapes.Cookie7Sided
                    },
                    color = strong,
                    size = 56.dp,
                ) {
                    Icon(
                        when (issue) {
                            ServiceIssue.NotSetUp -> Icons.Rounded.AccessibilityNew
                            ServiceIssue.TurnedOff -> Icons.Rounded.PowerSettingsNew
                            ServiceIssue.NotResponding -> Icons.Rounded.SyncProblem
                        },
                        contentDescription = null,
                        tint = onStrong,
                    )
                }
                Spacer(Modifier.size(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(
                            when (issue) {
                                ServiceIssue.NotSetUp -> R.string.home_service_off_title
                                ServiceIssue.TurnedOff -> R.string.home_service_turned_off_title
                                ServiceIssue.NotResponding -> R.string.home_service_stuck_title
                            },
                        ),
                        style = MaterialTheme.typography.titleLargeEmphasized,
                    )
                    Text(
                        stringResource(
                            when (issue) {
                                ServiceIssue.NotSetUp -> R.string.home_service_off_body
                                ServiceIssue.TurnedOff -> R.string.home_service_turned_off_body
                                ServiceIssue.NotResponding -> R.string.home_service_stuck_body
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = content.copy(alpha = 0.8f),
                    )
                }
            }
            Spacer(Modifier.size(16.dp))
            val primary = ButtonDefaults.buttonColors(containerColor = strong, contentColor = onStrong)
            if (issue == ServiceIssue.NotSetUp) {
                Button(
                    onClick = actions::onTurnOn,
                    shapes = ButtonDefaults.shapes(),
                    colors = primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = ButtonDefaults.MediumContainerHeight),
                    contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
                ) {
                    Text(stringResource(R.string.action_turn_on), style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight))
                }
            } else {
                // Side by side when both labels fit, otherwise one above the other.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = when {
                            issue == ServiceIssue.NotResponding || consented -> actions::onOpenAccessibility
                            // The disclosure comes first until the user has agreed to it.
                            else -> actions::onTurnOn
                        },
                        shapes = ButtonDefaults.shapes(),
                        colors = primary,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            stringResource(if (error) R.string.action_open_settings else R.string.action_turn_on_again),
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                    OutlinedButton(
                        onClick = actions::onTroubleshoot,
                        shapes = ButtonDefaults.shapes(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = content),
                        border = BorderStroke(1.dp, content.copy(alpha = 0.4f)),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.action_troubleshoot), maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveCard(status: OverlayStatus.Visible, actions: HomeActions) {
    Surface(
        shape = MaterialTheme.shapes.extraLargeIncreased,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (status.running) {
                    ContainedLoadingIndicator(modifier = Modifier.size(56.dp))
                } else {
                    ShapeBadge(MaterialShapes.Cookie9Sided, MaterialTheme.colorScheme.onSecondaryContainer, 56.dp) {
                        Icon(
                            if (status.mode == OverlayMode.Single) Icons.Rounded.AdsClick else Icons.Rounded.Route,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondaryContainer,
                        )
                    }
                }
                Spacer(Modifier.size(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (status.running) status.scriptName.ifBlank { stringResource(R.string.mode_single_title) } else stringResource(R.string.home_controls_title),
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (status.running) {
                            pluralStringResource(
                                R.plurals.action_count,
                                status.progress.actions.toInt().coerceAtMost(Int.MAX_VALUE),
                                Durations.count(status.progress.actions),
                            ) + " · " + Durations.clock(status.progress.elapsedMs)
                        } else {
                            stringResource(R.string.home_controls_body)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                    )
                }
            }
            Spacer(Modifier.size(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (status.running) {
                    Button(
                        onClick = withHaptic(HapticFeedbackType.ToggleOff, actions::onStop),
                        shapes = ButtonDefaults.shapes(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            contentColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text(stringResource(R.string.action_stop))
                    }
                }
                OutlinedButton(
                    onClick = withHaptic(onClick = actions::onHideControls),
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.4f)),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.VisibilityOff, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.action_hide_controls))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SingleCard(quick: Script?, actions: HomeActions) {
    val resources = LocalResources.current
    val container = MaterialTheme.colorScheme.primaryContainer
    val content = MaterialTheme.colorScheme.onPrimaryContainer
    val colors = onCardColors(container, content)
    ModeCard(
        container = container,
        content = content,
        badge = {
            ShapeBadge(MaterialShapes.Cookie9Sided, colors.strong, 60.dp) {
                Icon(Icons.Rounded.AdsClick, contentDescription = null, tint = colors.onStrong, modifier = Modifier.size(28.dp))
            }
        },
        title = stringResource(R.string.mode_single_title),
        body = stringResource(R.string.mode_single_body),
        details = {
            if (quick != null) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoPill(Icons.Rounded.Timer, Summaries.interval(resources, quick), colors)
                    InfoPill(
                        if (quick.stopRule.mode == StopMode.Never) Icons.Rounded.AllInclusive else Icons.Rounded.Flag,
                        Summaries.stopRule(resources, quick.stopRule),
                        colors,
                    )
                    if (quick.variation.isActive) InfoPill(Icons.Rounded.Shuffle, stringResource(R.string.summary_varied), colors)
                }
            }
        },
    ) {
        PrimaryCardButton(
            text = stringResource(R.string.action_start),
            icon = Icons.Rounded.PlayArrow,
            colors = colors,
            onClick = actions::onStartSingle,
            modifier = Modifier.weight(1f),
        )
        FilledTonalIconButton(
            onClick = actions::onSingleSettings,
            shapes = IconButtonDefaults.shapes(),
            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = colors.soft, contentColor = colors.onSoft),
            modifier = Modifier.size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide)),
        ) {
            Icon(Icons.Rounded.Tune, contentDescription = stringResource(R.string.action_settings))
        }
    }
}

@Composable
private fun SequenceCard(actions: HomeActions) {
    val container = MaterialTheme.colorScheme.tertiaryContainer
    val content = MaterialTheme.colorScheme.onTertiaryContainer
    val colors = onCardColors(container, content)
    ModeCard(
        container = container,
        content = content,
        badge = {
            ShapeBadge(MaterialShapes.Clover4Leaf, colors.strong, 60.dp) {
                Icon(Icons.Rounded.Route, contentDescription = null, tint = colors.onStrong, modifier = Modifier.size(28.dp))
            }
        },
        title = stringResource(R.string.mode_multi_title),
        body = stringResource(R.string.mode_multi_body),
    ) {
        PrimaryCardButton(
            text = stringResource(R.string.action_new_sequence),
            icon = Icons.Rounded.Add,
            colors = colors,
            onClick = actions::onNewSequence,
            modifier = Modifier.weight(1f),
        )
        FilledTonalButton(
            onClick = actions::onOpenLibrary,
            shapes = ButtonDefaults.shapes(),
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = colors.soft, contentColor = colors.onSoft),
            modifier = Modifier.heightIn(min = ButtonDefaults.MediumContainerHeight),
            contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
        ) {
            Text(stringResource(R.string.nav_sequences), style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight))
        }
    }
}

/** A medium-height button in the card's strongest colour, the card's main action. */
@Composable
private fun PrimaryCardButton(text: String, icon: ImageVector, colors: OnCardColors, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val height = ButtonDefaults.MediumContainerHeight
    Button(
        onClick = withHaptic(onClick = onClick),
        shapes = ButtonDefaults.shapes(),
        colors = ButtonDefaults.buttonColors(containerColor = colors.strong, contentColor = colors.onStrong),
        modifier = modifier.heightIn(min = height),
        contentPadding = ButtonDefaults.contentPaddingFor(height),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.iconSizeFor(height)))
        Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(height)))
        Text(text, style = ButtonDefaults.textStyleFor(height), maxLines = 1)
    }
}

@Composable
private fun ModeCard(
    container: Color,
    content: Color,
    badge: @Composable () -> Unit,
    title: String,
    body: String,
    details: @Composable () -> Unit = {},
    buttons: @Composable RowScope.() -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLargeIncreased,
        color = container,
        contentColor = content,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                badge()
                Spacer(Modifier.size(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.headlineSmallEmphasized)
                    Text(body, style = MaterialTheme.typography.bodyMedium, color = content.copy(alpha = 0.8f))
                }
            }
            Spacer(Modifier.size(16.dp))
            details()
            Spacer(Modifier.size(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = buttons,
            )
        }
    }
}

@Composable
private fun InfoPill(icon: ImageVector, text: String, colors: OnCardColors) {
    Surface(shape = MaterialTheme.shapes.extraLarge, color = colors.soft, contentColor = colors.onSoft) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.size(6.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}
