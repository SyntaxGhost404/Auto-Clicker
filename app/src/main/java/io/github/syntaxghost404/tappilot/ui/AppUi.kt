package io.github.syntaxghost404.tappilot.ui

import android.content.res.Resources
import android.os.Process
import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay

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

    /** Switched on in settings, and the system is still connecting it, as just after the app starts. */
    Starting,

    /** Switched on in settings, but the system has not (re)connected it. */
    Stuck,

    Off,
}

@Composable
fun rememberServiceState(): ServiceState {
    val context = LocalContext.current
    val connected by TapPilotRuntime.serviceConnected.collectAsStateWithLifecycle(TapPilotRuntime.isServiceConnected)
    var enabled by remember { mutableStateOf(ServiceStartup.seen(TapPilotRuntime.isServiceEnabled(context))) }
    // Read again whenever the service connects or disconnects, as when it is switched off in settings.
    LifecycleResumeEffect(connected) {
        enabled = ServiceStartup.seen(TapPilotRuntime.isServiceEnabled(context))
        onPauseOrDispose { }
    }
    var overdue by remember { mutableStateOf(ServiceStartup.graceLeftMs() == 0L) }
    LaunchedEffect(enabled, connected) {
        val left = ServiceStartup.graceLeftMs()
        overdue = left == 0L
        if (left > 0) {
            delay(left)
            overdue = true
        }
    }
    return when {
        connected -> ServiceState.Connected
        !enabled -> ServiceState.Off
        overdue -> ServiceState.Stuck
        else -> ServiceState.Starting
    }
}

/**
 * Gives a service that is switched on time to connect before it counts as [ServiceState.Stuck]: the
 * system binds it a moment after the app starts, or after the user switches it on. Shared by every
 * screen, so moving between them does not restart the wait.
 */
internal object ServiceStartup {
    const val GRACE_MS = 3_000L

    private var clock: () -> Long = SystemClock::elapsedRealtime

    /** When the service was last seen switching on. If it was already on, the process start. */
    private var onSince = Process.getStartElapsedRealtime()
    private var wasEnabled = true

    /** Records whether the service is [enabled] in settings, and returns it. */
    fun seen(enabled: Boolean): Boolean {
        if (enabled && !wasEnabled) onSince = clock()
        wasEnabled = enabled
        return enabled
    }

    fun graceLeftMs(): Long = (onSince + GRACE_MS - clock()).coerceAtLeast(0L)

    /** Starts over as a new process would, where a service already on is waited for from the start. */
    @VisibleForTesting
    fun reset(clock: () -> Long = SystemClock::elapsedRealtime, processStart: Long = Process.getStartElapsedRealtime()) {
        this.clock = clock
        onSince = processStart
        wasEnabled = true
    }
}
