package io.github.syntaxghost404.tappilot.overlay

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.syntaxghost404.tappilot.core.data.AppSettings
import io.github.syntaxghost404.tappilot.core.data.OverlayMode
import io.github.syntaxghost404.tappilot.core.engine.RunProgress
import io.github.syntaxghost404.tappilot.core.model.Script

/** Which part of a step a marker handle controls. */
internal enum class HandlePart { Tap, SwipeStart, SwipeEnd }

internal data class Handle(val stepId: String, val part: HandlePart) {
    val key: String get() = "$stepId/${part.name}"
}

/** Dialogs shown above the floating controls. */
internal sealed interface OverlayDialog {
    data class EditStep(val stepId: String) : OverlayDialog
    data object Settings : OverlayDialog
    data object OpenSequence : OverlayDialog
}

/**
 * A short ring animation drawn where a gesture landed. [startedAtNanos] is stamped by the path
 * layer on the first frame that draws it, so it always shares the frame clock's time base.
 */
internal data class Pulse(val id: Long, val x: Float, val y: Float, val swipe: Boolean, val startedAtNanos: Long = UNSTARTED) {
    companion object {
        const val UNSTARTED = -1L
    }
}

/**
 * Observable state of the open floating controls. Lives on the main thread and is read by every
 * overlay window's composition.
 */
@Stable
internal class OverlaySession(
    val mode: OverlayMode,
    script: Script,
    settings: AppSettings,
    /** Whether [script] already exists in the library. New sequences are saved once they have a step. */
    persisted: Boolean,
) {
    var script by mutableStateOf(script)
    var settings by mutableStateOf(settings)
    var persisted by mutableStateOf(persisted)
    var running by mutableStateOf(false)
    /** [android.os.SystemClock.elapsedRealtime] when the current run started. */
    var runStartedAt by mutableLongStateOf(0L)
    var progress by mutableStateOf(RunProgress())
    var activeStepId by mutableStateOf<String?>(null)
    var dialog by mutableStateOf<OverlayDialog?>(null)
    /** Direction and scale of the floating controls on the current screen. */
    var controlsLayout by mutableStateOf(ControlsLayout.Default)
    /** Bumped each time a run reaches its limit, so the play button can celebrate briefly. */
    var finishedCount by mutableIntStateOf(0)
    val pulses = mutableStateListOf<Pulse>()

    /** The last version written to storage, used to skip redundant saves. */
    var lastSaved: Script? = if (persisted) script else null

    val isMulti: Boolean get() = mode == OverlayMode.Multi
}
