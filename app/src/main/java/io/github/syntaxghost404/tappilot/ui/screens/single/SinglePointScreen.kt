package io.github.syntaxghost404.tappilot.ui.screens.single

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MediumExtendedFloatingActionButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.syntaxghost404.tappilot.AppGraph
import io.github.syntaxghost404.tappilot.R
import io.github.syntaxghost404.tappilot.core.model.Script
import io.github.syntaxghost404.tappilot.core.model.StopRule
import io.github.syntaxghost404.tappilot.core.model.TapStep
import io.github.syntaxghost404.tappilot.core.model.Timing
import io.github.syntaxghost404.tappilot.core.model.Variation
import io.github.syntaxghost404.tappilot.ui.components.DetailScaffold
import io.github.syntaxghost404.tappilot.ui.components.DurationField
import io.github.syntaxghost404.tappilot.ui.components.FormCard
import io.github.syntaxghost404.tappilot.ui.components.SectionTitle
import io.github.syntaxghost404.tappilot.ui.components.StopRuleEditor
import io.github.syntaxghost404.tappilot.ui.components.VariationEditor
import io.github.syntaxghost404.tappilot.ui.withHaptic
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SinglePointViewModel(private val graph: AppGraph) : ViewModel() {
    val quick: StateFlow<Script?> = graph.scripts.quick
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun edit(transform: (Script) -> Script) {
        viewModelScope.launch { graph.scripts.edit(Script.QUICK_ID, transform) }
    }

    fun setInterval(ms: Long) = edit { script -> script.copy(steps = script.steps.map { it.withDelay(ms) }) }

    fun setStopRule(rule: StopRule) = edit { it.copy(stopRule = rule) }

    fun setVariation(variation: Variation) = edit { it.copy(variation = variation) }

    /** Forgets the saved target position so it starts in the middle of the screen next time. */
    fun resetTarget() = edit { script ->
        script.copy(steps = script.steps.map { if (it is TapStep) it.copy(x = -1f, y = -1f) else it })
    }
}

interface SinglePointActions {
    fun onBack()
    fun onStart()
    fun onInterval(ms: Long)
    fun onStopRule(rule: StopRule)
    fun onVariation(variation: Variation)
    fun onResetTarget()
}

@Composable
fun SinglePointScreen(quick: Script?, actions: SinglePointActions, snackbarHostState: SnackbarHostState) {
    DetailScaffold(
        title = stringResource(R.string.mode_single_title),
        subtitle = stringResource(R.string.mode_single_body),
        onBack = actions::onBack,
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            MediumExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.action_start)) },
                icon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
                onClick = withHaptic(onClick = actions::onStart),
            )
        },
    ) { padding ->
        if (quick == null) {
            Box(Modifier.fillMaxSize())
            return@DetailScaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FormCard {
                DurationField(
                    valueMs = quick.steps.firstOrNull()?.delayMs ?: Timing.DEFAULT_INTERVAL_MS,
                    onValueChange = actions::onInterval,
                    label = stringResource(R.string.field_interval),
                    supportingText = stringResource(R.string.field_interval_support),
                    warnBelowMs = Timing.FAST_DELAY_WARNING_MS,
                )
            }
            SectionTitle(stringResource(R.string.editor_when_to_stop), Modifier.padding(top = 16.dp))
            FormCard {
                StopRuleEditor(
                    rule = quick.stopRule,
                    onChange = actions::onStopRule,
                    cyclesSupport = stringResource(R.string.stop_cycles_support_single),
                )
            }
            SectionTitle(stringResource(R.string.variation_title), Modifier.padding(top = 16.dp))
            FormCard { VariationEditor(variation = quick.variation, onChange = actions::onVariation) }
            Spacer(Modifier.size(8.dp))
            OutlinedButton(onClick = actions::onResetTarget, shapes = ButtonDefaults.shapes()) {
                Icon(Icons.Rounded.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.action_reset_target))
            }
        }
    }
}
