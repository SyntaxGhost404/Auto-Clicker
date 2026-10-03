package io.github.syntaxghost404.tappilot.service

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import io.github.syntaxghost404.tappilot.core.data.OverlayMode
import io.github.syntaxghost404.tappilot.core.engine.RunProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow

/** What the floating controls are doing right now, as seen from the app. */
sealed interface OverlayStatus {
    data object Hidden : OverlayStatus

    data class Visible(
        val mode: OverlayMode,
        val scriptId: String?,
        val scriptName: String,
        val running: Boolean,
        val progress: RunProgress,
    ) : OverlayStatus
}

/**
 * The bridge between the app UI and the accessibility service, which live in the same process.
 * The service registers itself while it is connected; the UI only talks to this object.
 */
object TapPilotRuntime {
    private val service = MutableStateFlow<TapPilotAccessibilityService?>(null)
    private val _status = MutableStateFlow<OverlayStatus>(OverlayStatus.Hidden)

    val serviceConnected: Flow<Boolean> = service.map { it != null }
    val status: StateFlow<OverlayStatus> = _status.asStateFlow()

    val isServiceConnected: Boolean get() = service.value != null

    internal fun attach(instance: TapPilotAccessibilityService) {
        service.value = instance
    }

    internal fun detach(instance: TapPilotAccessibilityService) {
        if (service.value === instance) service.value = null
        _status.value = OverlayStatus.Hidden
    }

    internal fun publish(status: OverlayStatus) {
        _status.value = status
    }

    /** Shows the floating controls. Returns false when the accessibility service is not running. */
    fun openControls(mode: OverlayMode, scriptId: String? = null): Boolean {
        val instance = service.value ?: return false
        instance.openControls(mode, scriptId)
        return true
    }

    fun closeControls() {
        service.value?.closeControls()
    }

    /** Stops a running script but keeps the controls on screen. */
    fun stopRun() {
        service.value?.stopRun()
    }

    /** Closes the notification shade so freshly opened controls are visible. */
    fun collapseShade() {
        service.value?.collapseShade()
    }

    /** Whether the user has switched the service on in system settings (it may not be bound yet). */
    fun isServiceEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val ours = ComponentName(context, TapPilotAccessibilityService::class.java)
        val splitter = TextUtils.SimpleStringSplitter(':').apply { setString(enabled) }
        return splitter.any { ComponentName.unflattenFromString(it) == ours }
    }
}
