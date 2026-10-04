package io.github.syntaxghost404.tappilot.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import io.github.syntaxghost404.tappilot.appGraph
import io.github.syntaxghost404.tappilot.core.data.OverlayMode
import io.github.syntaxghost404.tappilot.core.engine.DispatchResult
import io.github.syntaxghost404.tappilot.core.engine.GestureDispatcher
import io.github.syntaxghost404.tappilot.core.engine.GesturePlan
import io.github.syntaxghost404.tappilot.core.engine.SwipePlan
import io.github.syntaxghost404.tappilot.core.engine.TapPlan
import io.github.syntaxghost404.tappilot.overlay.OverlayManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Performs the taps and swipes the user sets up. It observes no accessibility events and reads no
 * window content: its only capabilities are drawing the floating controls and dispatching gestures.
 */
class TapPilotAccessibilityService : AccessibilityService(), GestureDispatcher {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var overlay: OverlayManager? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlay = OverlayManager(this, appGraph, dispatcher = this)
        TapPilotRuntime.attach(this)
        ControlsTileService.requestRefresh(this)
        // Remembered so that, if the phone later switches the service off, the app can say so
        // instead of treating it as a first-time setup.
        appGraph.ioScope.launch { appGraph.settings.markServiceSetUp() }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        overlay?.onDisplayChanged()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        shutDown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        shutDown()
        super.onDestroy()
    }

    private fun shutDown() {
        overlay?.dispose()
        overlay = null
        TapPilotRuntime.detach(this)
        ControlsTileService.requestRefresh(this)
    }

    fun openControls(mode: OverlayMode, scriptId: String?) {
        overlay?.open(mode, scriptId)
    }

    fun closeControls() {
        overlay?.close()
    }

    fun stopRun() {
        overlay?.stopRun()
    }

    fun collapseShade() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
        } else {
            @Suppress("DEPRECATION")
            sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
        }
    }

    override suspend fun dispatch(plan: GesturePlan): DispatchResult {
        val gesture = runCatching { plan.toGestureDescription() }.getOrNull() ?: return DispatchResult.Rejected
        return withTimeoutOrNull(plan.durationMs + CALLBACK_GRACE_MS) {
            suspendCancellableCoroutine { continuation ->
                val callback = object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        if (continuation.isActive) continuation.resume(DispatchResult.Completed)
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        if (continuation.isActive) continuation.resume(DispatchResult.Cancelled)
                    }
                }
                val accepted = dispatchGesture(gesture, callback, mainHandler)
                if (!accepted && continuation.isActive) continuation.resume(DispatchResult.Rejected)
            }
        } ?: DispatchResult.Cancelled
    }

    private fun GesturePlan.toGestureDescription(): GestureDescription {
        val path = Path()
        when (this) {
            is TapPlan -> path.moveTo(x, y)
            is SwipePlan -> {
                path.moveTo(startX, startY)
                path.lineTo(endX, endY)
            }
        }
        return GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, durationMs))
            .build()
    }

    private companion object {
        /** How long to wait for the system's completion callback beyond the gesture itself. */
        const val CALLBACK_GRACE_MS = 1_500L
    }
}
