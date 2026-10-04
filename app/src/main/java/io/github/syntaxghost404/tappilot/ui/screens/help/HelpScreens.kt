package io.github.syntaxghost404.tappilot.ui.screens.help

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AdsClick
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloseFullscreen
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.PhonelinkSetup
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import io.github.syntaxghost404.tappilot.BuildConfig
import io.github.syntaxghost404.tappilot.R
import io.github.syntaxghost404.tappilot.overlay.SHOW_SEQUENCES_BUTTON
import io.github.syntaxghost404.tappilot.ui.ServiceState
import io.github.syntaxghost404.tappilot.ui.components.DetailScaffold
import io.github.syntaxghost404.tappilot.ui.components.FormCard
import io.github.syntaxghost404.tappilot.ui.components.SectionTitle
import io.github.syntaxghost404.tappilot.ui.components.ShapeBadge

@Composable
private fun HelpColumn(padding: PaddingValues, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(padding)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) { content() }
}

@Composable
fun HowToScreen(onBack: () -> Unit) {
    DetailScaffold(title = stringResource(R.string.howto_title), onBack = onBack) { padding ->
        HelpColumn(padding) {
            ModeExplainer(
                badge = MaterialShapes.Cookie9Sided,
                icon = Icons.Rounded.AdsClick,
                title = stringResource(R.string.howto_single_title),
                body = stringResource(R.string.howto_single_body),
                primary = true,
            )
            Spacer(Modifier.size(12.dp))
            ModeExplainer(
                badge = MaterialShapes.Clover4Leaf,
                icon = Icons.Rounded.Route,
                title = stringResource(R.string.howto_multi_title),
                body = stringResource(R.string.howto_multi_body),
                primary = false,
            )
            SectionTitle(stringResource(R.string.howto_controls_title), Modifier.padding(top = 20.dp))
            val controls = buildList {
                add(Icons.Rounded.PlayArrow to R.string.howto_control_play)
                add(Icons.Rounded.AdsClick to R.string.howto_control_add_tap)
                add(Icons.Rounded.Swipe to R.string.howto_control_add_swipe)
                add(Icons.AutoMirrored.Rounded.Undo to R.string.howto_control_remove)
                add(Icons.Rounded.Tune to R.string.howto_control_settings)
                if (SHOW_SEQUENCES_BUTTON) add(Icons.Rounded.FolderOpen to R.string.howto_control_sequences)
                add(Icons.Rounded.CloseFullscreen to R.string.howto_control_minimize)
                add(Icons.Rounded.Close to R.string.howto_control_close)
                add(Icons.Rounded.OpenWith to R.string.howto_control_drag)
            }
            controls.forEachIndexed { index, (icon, text) ->
                SegmentedListItem(
                    shapes = ListItemDefaults.segmentedShapes(index = index, count = controls.size),
                    leadingContent = {
                        ShapeBadge(
                            if (index == 0) MaterialShapes.Cookie9Sided else MaterialShapes.Circle,
                            if (index == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                            40.dp,
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = if (index == 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    },
                ) { Text(stringResource(text), style = MaterialTheme.typography.bodyLarge) }
            }
        }
    }
}

@Composable
private fun ModeExplainer(
    badge: RoundedPolygon,
    icon: ImageVector,
    title: String,
    body: String,
    primary: Boolean,
) {
    val container = if (primary) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
    val content = if (primary) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
    val accent = content
    val onAccent = container
    FormCard(color = container) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShapeBadge(badge, accent, 48.dp) { Icon(icon, contentDescription = null, tint = onAccent) }
            Spacer(Modifier.size(16.dp))
            Text(title, style = MaterialTheme.typography.titleLargeEmphasized)
        }
        Spacer(Modifier.size(12.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge)
    }
}

data class TroubleshootingState(val service: ServiceState, val batteryOptimized: Boolean)

interface TroubleshootingActions {
    fun onBack()
    fun onOpenAccessibility()
    fun onOpenBattery()
    fun onOpenAppInfo()
}

@Composable
fun TroubleshootingScreen(state: TroubleshootingState, actions: TroubleshootingActions) {
    DetailScaffold(title = stringResource(R.string.settings_troubleshooting), onBack = actions::onBack) { padding ->
        HelpColumn(padding) {
            val serviceOk = state.service == ServiceState.Connected
            StatusItem(
                index = 0,
                ok = serviceOk,
                okIcon = Icons.Rounded.CheckCircle,
                badIcon = Icons.Rounded.ErrorOutline,
                title = stringResource(R.string.ts_service),
                status = stringResource(
                    when (state.service) {
                        ServiceState.Connected -> R.string.ts_service_connected
                        ServiceState.Starting -> R.string.ts_service_starting
                        ServiceState.Stuck -> R.string.ts_service_stuck
                        ServiceState.Off -> R.string.ts_service_off
                    },
                ),
                hint = stringResource(R.string.ts_service_hint),
                action = stringResource(R.string.action_open_settings),
                onAction = actions::onOpenAccessibility,
            )
            StatusItem(
                index = 1,
                ok = !state.batteryOptimized,
                okIcon = Icons.Rounded.BatteryFull,
                badIcon = Icons.Rounded.BatteryAlert,
                title = stringResource(R.string.ts_battery),
                status = stringResource(if (state.batteryOptimized) R.string.ts_battery_optimized else R.string.ts_battery_unrestricted),
                hint = null,
                action = stringResource(R.string.ts_battery_action),
                onAction = actions::onOpenBattery,
            )
            Spacer(Modifier.size(12.dp))
            TipCard(Icons.Rounded.Lock, stringResource(R.string.ts_restricted), stringResource(R.string.setup_restricted_body)) {
                FilledTonalButton(onClick = actions::onOpenAppInfo, shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(R.string.action_app_info))
                }
            }
            TipCard(Icons.Rounded.PhonelinkSetup, stringResource(R.string.ts_oem), stringResource(R.string.ts_oem_body))
            TipCard(Icons.Rounded.WarningAmber, stringResource(R.string.ts_overlap), stringResource(R.string.ts_overlap_body))
            TipCard(Icons.Rounded.TouchApp, stringResource(R.string.ts_not_registering), stringResource(R.string.ts_not_registering_body))
        }
    }
}

@Composable
private fun StatusItem(
    index: Int,
    ok: Boolean,
    okIcon: ImageVector,
    badIcon: ImageVector,
    title: String,
    status: String,
    hint: String?,
    action: String,
    onAction: () -> Unit,
) {
    Surface(
        shape = ListItemDefaults.segmentedShapes(index = index, count = 2).shape,
        color = ListItemDefaults.segmentedColors().containerColor,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShapeBadge(
                    if (ok) MaterialShapes.Cookie9Sided else MaterialShapes.SoftBurst,
                    if (ok) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                    44.dp,
                ) {
                    Icon(
                        if (ok) okIcon else badIcon,
                        contentDescription = null,
                        tint = if (ok) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
                Spacer(Modifier.size(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMediumEmphasized)
                    Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (hint != null) {
                Text(
                    hint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            FilledTonalButton(
                onClick = onAction,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.padding(top = 12.dp),
            ) { Text(action) }
        }
    }
}

@Composable
private fun TipCard(icon: ImageVector, title: String, body: String, action: @Composable () -> Unit = {}) {
    FormCard(modifier = Modifier.padding(top = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.size(12.dp))
            Text(title, style = MaterialTheme.typography.titleMediumEmphasized)
        }
        Spacer(Modifier.size(8.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.padding(top = 8.dp)) { action() }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    DetailScaffold(title = stringResource(R.string.settings_about), onBack = onBack) { padding ->
        HelpColumn(padding) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ShapeBadge(MaterialShapes.Cookie12Sided, MaterialTheme.colorScheme.primary, 120.dp) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
                        modifier = Modifier.size(150.dp),
                    )
                }
                Spacer(Modifier.size(16.dp))
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmallEmphasized)
                Text(
                    stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            SectionTitle(stringResource(R.string.privacy_title))
            FormCard { Text(stringResource(R.string.privacy_body), style = MaterialTheme.typography.bodyLarge) }
            SectionTitle(stringResource(R.string.about_title), Modifier.padding(top = 12.dp))
            FormCard { Text(stringResource(R.string.about_body), style = MaterialTheme.typography.bodyLarge) }
        }
    }
}
