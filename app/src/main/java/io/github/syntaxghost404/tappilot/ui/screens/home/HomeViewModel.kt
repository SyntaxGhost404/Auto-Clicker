package io.github.syntaxghost404.tappilot.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.syntaxghost404.tappilot.AppGraph
import io.github.syntaxghost404.tappilot.core.model.Script
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(graph: AppGraph) : ViewModel() {
    val quick: StateFlow<Script?> = graph.scripts.quick
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val recent: StateFlow<List<Script>> = graph.scripts.scripts
        .map { it.take(RECENT_COUNT) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private companion object {
        const val RECENT_COUNT = 3
    }
}
