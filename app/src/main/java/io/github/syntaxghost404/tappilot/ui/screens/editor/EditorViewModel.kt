package io.github.syntaxghost404.tappilot.ui.screens.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.syntaxghost404.tappilot.AppGraph
import io.github.syntaxghost404.tappilot.core.model.Script
import io.github.syntaxghost404.tappilot.core.model.Step
import io.github.syntaxghost404.tappilot.core.model.StopRule
import io.github.syntaxghost404.tappilot.core.model.Variation
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface EditorState {
    data object Loading : EditorState
    data object Missing : EditorState
    data class Loaded(val script: Script) : EditorState
}

/** Edits a saved sequence. Every change is written straight to storage. */
class EditorViewModel(private val graph: AppGraph, private val scriptId: String) : ViewModel() {
    val state: StateFlow<EditorState> = graph.scripts.script(scriptId)
        .map { script -> if (script == null) EditorState.Missing else EditorState.Loaded(script) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EditorState.Loading)

    private fun edit(transform: (Script) -> Script) {
        viewModelScope.launch { graph.scripts.edit(scriptId, transform) }
    }

    fun setStopRule(rule: StopRule) = edit { it.copy(stopRule = rule) }

    fun setVariation(variation: Variation) = edit { it.copy(variation = variation) }

    fun rename(name: String) = viewModelScope.launch { graph.scripts.rename(scriptId, name) }

    fun updateStep(stepId: String, transform: (Step) -> Step) = edit { it.updateStep(stepId, transform) }

    fun deleteStep(stepId: String) = edit { script -> script.copy(steps = script.steps.filterNot { it.id == stepId }) }

    /** Moves a step one place up (-1) or down (+1). */
    fun moveStep(stepId: String, offset: Int) = edit { script ->
        val from = script.steps.indexOfFirst { it.id == stepId }
        val to = from + offset
        if (from < 0 || to !in script.steps.indices) {
            script
        } else {
            script.copy(steps = script.steps.toMutableList().apply { add(to, removeAt(from)) })
        }
    }
}
