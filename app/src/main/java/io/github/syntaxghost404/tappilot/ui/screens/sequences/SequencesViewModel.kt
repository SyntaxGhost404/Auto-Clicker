package io.github.syntaxghost404.tappilot.ui.screens.sequences

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.syntaxghost404.tappilot.AppGraph
import io.github.syntaxghost404.tappilot.R
import io.github.syntaxghost404.tappilot.core.data.BackupManager
import io.github.syntaxghost404.tappilot.core.data.ImportResult
import io.github.syntaxghost404.tappilot.core.model.Script
import io.github.syntaxghost404.tappilot.ui.UiMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Library operations shared by the Sequences tab, the editor and Settings. */
class SequencesViewModel(private val graph: AppGraph) : ViewModel() {
    val scripts: StateFlow<List<Script>?> = graph.scripts.scripts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = _messages.receiveAsFlow()

    /** Scripts waiting to be exported once the user picks a destination; null means all. */
    private var pendingExport: List<Script>? = null

    fun rename(id: String, name: String) = viewModelScope.launch { graph.scripts.rename(id, name) }

    fun duplicate(id: String, suffix: String) = viewModelScope.launch { graph.scripts.duplicate(id, suffix) }

    fun delete(id: String) = viewModelScope.launch {
        val removed = graph.scripts.delete(id) ?: return@launch
        _messages.send(
            UiMessage(
                text = { it.getString(R.string.sequence_deleted, removed.name) },
                actionLabel = { it.getString(R.string.action_undo) },
                onAction = { viewModelScope.launch { graph.scripts.restore(removed) } },
            ),
        )
    }

    fun prepareExport(selection: List<Script>?) {
        pendingExport = selection
    }

    fun export(uri: Uri) = viewModelScope.launch {
        val result = graph.backups.export(uri, pendingExport)
        pendingExport = null
        val message = result.fold(
            onSuccess = { count -> UiMessage({ it.getQuantityString(R.plurals.export_success, count, count) }) },
            onFailure = { error ->
                if (error is BackupManager.NothingToExport) {
                    UiMessage({ it.getString(R.string.export_nothing) })
                } else {
                    UiMessage({ it.getString(R.string.export_failed) })
                }
            },
        )
        _messages.send(message)
    }

    fun import(uri: Uri) = viewModelScope.launch {
        val message = when (val result = graph.backups.import(uri)) {
            is ImportResult.Success -> {
                val count = result.scripts.size
                UiMessage({ it.getQuantityString(R.plurals.import_success, count, count) })
            }
            is ImportResult.Failure -> UiMessage({ res ->
                res.getString(
                    when (result.reason) {
                        ImportResult.Reason.NotABackup -> R.string.import_not_backup
                        ImportResult.Reason.UnsupportedVersion -> R.string.import_newer_version
                        ImportResult.Reason.Empty -> R.string.import_empty
                        ImportResult.Reason.Unreadable -> R.string.import_failed
                    },
                )
            })
        }
        _messages.send(message)
    }

    fun exportFileName(selection: List<Script>?): String {
        val base = selection?.singleOrNull()?.name
            ?.replace(Regex("[^A-Za-z0-9 _-]"), "")
            ?.trim()
            ?.replace(' ', '-')
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }
            ?: "tap-pilot-sequences"
        return "$base.json"
    }
}
