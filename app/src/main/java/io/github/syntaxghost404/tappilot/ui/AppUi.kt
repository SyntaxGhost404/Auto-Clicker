package io.github.syntaxghost404.tappilot.ui

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.syntaxghost404.tappilot.AppGraph
import io.github.syntaxghost404.tappilot.appGraph
import io.github.syntaxghost404.tappilot.service.TapPilotRuntime

/** Creates a view model scoped to the current navigation entry, wired to the app graph. */
@Composable
inline fun <reified VM : ViewModel> graphViewModel(key: String? = null, noinline create: (AppGraph) -> VM): VM {
    val graph = LocalContext.current.appGraph
    return viewModel(key = key, factory = viewModelFactory { initializer { create(graph) } })
}

/** A one-off message for a snackbar, resolved against resources when shown. */
class UiMessage(
    val text: (Resources) -> String,
    val actionLabel: ((Resources) -> String)? = null,
    val onAction: (() -> Unit)? = null,
)

enum class ServiceState {
    /** Running and ready to perform gestures. */
    Connected,

    /** Switched on in settings, but the system has not (re)connected it. */
    Stuck,

    Off,
}

@Composable
fun rememberServiceState(): ServiceState {
    val context = LocalContext.current
    val connected by TapPilotRuntime.serviceConnected.collectAsStateWithLifecycle(TapPilotRuntime.isServiceConnected)
    var enabled by remember { mutableStateOf(TapPilotRuntime.isServiceEnabled(context)) }
    LifecycleResumeEffect(Unit) {
        enabled = TapPilotRuntime.isServiceEnabled(context)
        onPauseOrDispose { }
    }
    return when {
        connected -> ServiceState.Connected
        enabled -> ServiceState.Stuck
        else -> ServiceState.Off
    }
}
