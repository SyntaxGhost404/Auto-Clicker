package io.github.syntaxghost404.tappilot.overlay

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.os.Build
import android.os.SystemClock
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat
import io.github.syntaxghost404.tappilot.AppGraph
import io.github.syntaxghost404.tappilot.R
import io.github.syntaxghost404.tappilot.core.data.AppSettings
import io.github.syntaxghost404.tappilot.core.data.OverlayMode
import io.github.syntaxghost404.tappilot.core.data.defaultQuickScript
import io.github.syntaxghost404.tappilot.core.engine.GestureDispatcher
import io.github.syntaxghost404.tappilot.core.engine.GesturePlan
import io.github.syntaxghost404.tappilot.core.engine.GesturePlanner
import io.github.syntaxghost404.tappilot.core.engine.RunListener
import io.github.syntaxghost404.tappilot.core.engine.RunOutcome
import io.github.syntaxghost404.tappilot.core.engine.RunProgress
import io.github.syntaxghost404.tappilot.core.engine.ScriptRunner
import io.github.syntaxghost404.tappilot.core.engine.SwipePlan
import io.github.syntaxghost404.tappilot.core.engine.TapPlan
import io.github.syntaxghost404.tappilot.core.model.CanvasSize
import io.github.syntaxghost404.tappilot.core.model.Script
import io.github.syntaxghost404.tappilot.core.model.Step
import io.github.syntaxghost404.tappilot.core.model.SwipeStep
import io.github.syntaxghost404.tappilot.core.model.TapStep
import io.github.syntaxghost404.tappilot.overlay.ui.ControlActions
import io.github.syntaxghost404.tappilot.overlay.ui.ControlBar
import io.github.syntaxghost404.tappilot.overlay.ui.DialogActions
import io.github.syntaxghost404.tappilot.overlay.ui.MarkerState
import io.github.syntaxghost404.tappilot.overlay.ui.OverlayDialogHost
import io.github.syntaxghost404.tappilot.overlay.ui.PathLayer
import io.github.syntaxghost404.tappilot.overlay.ui.TargetMarker
import io.github.syntaxghost404.tappilot.service.OverlayStatus
import io.github.syntaxghost404.tappilot.service.TapPilotRuntime
import io.github.syntaxghost404.tappilot.ui.ProvideHaptics
import io.github.syntaxghost404.tappilot.ui.theme.TapPilotTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Owns everything drawn over other apps: the floating controls, one small window per target
 * handle, a full-screen non-touchable layer for paths and tap ripples, and dialogs.
 *
 * Targets are separate windows (rather than one full-screen editor) so the app underneath stays
 * usable around them while the controls are open.
 */
internal class OverlayManager(
    private val service: AccessibilityService,
    private val graph: AppGraph,
    dispatcher: GestureDispatcher,
) {
    private val context: Context = ContextThemeWrapper(service, R.style.Theme_TapPilot)
    private val windowManager: WindowManager = service.getSystemService(WindowManager::class.java)
    private val scope = MainScope()
    private val runner = ScriptRunner(dispatcher, SystemClock::elapsedRealtime)

    /** The open session, if any. Internal so integration tests can observe it. */
    internal var session: OverlaySession? = null
        private set
    private var owner: OverlayLifecycleOwner? = null
    private var sessionScope: CoroutineScope? = null
    private var controls: OverlayWindow? = null
    private var pathLayer: OverlayWindow? = null
    private var dialogWindow: OverlayWindow? = null
    private val markers = LinkedHashMap<String, MarkerWindow>()
    private var runJob: Job? = null
    private var screenOffReceiver: BroadcastReceiver? = null
    private var lastStatusAt = 0L
    private var lastPulseAt = 0L
    private var pulseIds = 0L

    internal val markerWindowCount: Int get() = markers.size
    internal val controlsShown: Boolean get() = controls?.isShown == true

    /** The floating controls' root view and its window's top-left corner, for integration tests. */
    internal val controlsView: View? get() = controls?.view
    internal val controlsOrigin: Point? get() = controls?.params?.let { Point(it.x, it.y) }
    internal val markerViews: List<View> get() = markers.values.map { it.window.view }

    private val density: Float get() = context.resources.displayMetrics.density
    private fun dp(value: Float): Int = (value * density).roundToInt()

    // region Public API

    fun open(mode: OverlayMode, scriptId: String?) {
        scope.launch {
            close()
            val settings = graph.settings.current()
            val canvas = displaySize()
            val session = when (mode) {
                OverlayMode.Single -> {
                    val quick = graph.scripts.get(Script.QUICK_ID) ?: defaultQuickScript()
                    OverlaySession(mode, prepareQuick(quick, canvas), settings, persisted = true)
                }
                OverlayMode.Multi -> {
                    val saved = scriptId?.let { graph.scripts.get(it) }
                    if (saved != null) {
                        OverlaySession(mode, saved.fittedTo(canvas), settings, persisted = true)
                    } else {
                        OverlaySession(mode, Script(name = "", canvas = canvas), settings, persisted = false)
                    }
                }
            }
            graph.settings.rememberSession(mode, session.script.id.takeIf { session.persisted && mode == OverlayMode.Multi })
            start(session)
        }
    }

    fun close() {
        val s = session ?: return
        stopRun()
        flush(s)
        sessionScope?.cancel()
        sessionScope = null
        hideDialogWindow()
        markers.values.forEach { it.window.remove() }
        markers.clear()
        controls?.remove()
        controls = null
        pathLayer?.remove()
        pathLayer = null
        owner?.destroy()
        owner = null
        session = null
        unregisterScreenOff()
        publishStatus(force = true)
    }

    fun dispose() {
        close()
        scope.cancel()
    }

    fun onDisplayChanged() {
        val s = session ?: return
        val canvas = displaySize()
        if (s.script.canvas != canvas) s.script = s.script.fittedTo(canvas)
        s.controlsLayout = controlsLayoutFor(s)
        pathLayer?.resize(canvas.width, canvas.height)
        syncMarkers()
        keepControlsOnScreen()
    }

    // endregion

    // region Session lifecycle

    private fun start(s: OverlaySession) {
        s.controlsLayout = controlsLayoutFor(s)
        val lifecycle = OverlayLifecycleOwner().also { it.start() }
        owner = lifecycle
        session = s
        val childScope = CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext[Job]))
        sessionScope = childScope

        pathLayer = createPathLayer(s).also { it.show() }
        syncMarkers()
        val controlsWindow = createControls(s)
        controls = controlsWindow
        if (!controlsWindow.show()) {
            toast(R.string.service_lost)
            close()
            return
        }

        childScope.launch {
            graph.settings.settings.drop(1).collect { applySettings(s, it) }
        }
        @OptIn(FlowPreview::class)
        childScope.launch {
            snapshotFlow { s.script }
                .drop(1)
                .debounce(AUTOSAVE_DEBOUNCE_MS)
                .collect { persist(s, it) }
        }
        childScope.launch {
            snapshotFlow { s.dialog != null }
                .distinctUntilChanged()
                .collect { visible -> if (visible) showDialogWindow(s) else hideDialogWindow() }
        }
        publishStatus(force = true)
    }

    /** Places the single-point target in the middle of the screen the first time it is used. */
    private fun prepareQuick(quick: Script, canvas: CanvasSize): Script {
        val target = quick.steps.firstOrNull() as? TapStep
            ?: return defaultQuickScript().copy(canvas = canvas).let { prepareQuick(it, canvas) }
        if (target.x >= 0f && target.y >= 0f) return quick.fittedTo(canvas)
        val centred = target.copy(x = canvas.width / 2f, y = canvas.height / 2f)
        return quick.copy(steps = listOf(centred), canvas = canvas)
    }

    private fun applySettings(s: OverlaySession, settings: AppSettings) {
        val previous = s.settings
        s.settings = settings
        if (previous.markerSizeDp != settings.markerSizeDp) syncMarkers()
        if (previous.controlSize != settings.controlSize) s.controlsLayout = controlsLayoutFor(s)
        if (previous.keepScreenOn != settings.keepScreenOn) controls?.setKeepScreenOn(settings.keepScreenOn)
    }

    // endregion

    // region Persistence

    private suspend fun persist(s: OverlaySession, script: Script) {
        if (script == s.lastSaved) return
        if (s.mode == OverlayMode.Multi && !s.persisted && script.steps.isEmpty()) return
        val named = if (script.name.isBlank()) {
            script.copy(name = graph.scripts.nextName(context.getString(R.string.default_sequence_name)))
        } else {
            script
        }
        graph.scripts.save(named)
        s.lastSaved = named
        if (!s.persisted) {
            s.persisted = true
            graph.settings.rememberSession(s.mode, named.id)
        }
        if (session === s && s.script.id == named.id && s.script.name != named.name) {
            s.script = s.script.copy(name = named.name)
            s.lastSaved = s.script
        }
        publishStatus(force = true)
    }

    /** Saves pending changes immediately, outliving the session if it is closing. */
    private fun flush(s: OverlaySession) {
        val script = s.script
        if (script == s.lastSaved) return
        if (s.mode == OverlayMode.Multi && !s.persisted && script.steps.isEmpty()) return
        s.lastSaved = script
        val defaultName = context.getString(R.string.default_sequence_name)
        graph.ioScope.launch {
            val named = if (script.name.isBlank()) script.copy(name = graph.scripts.nextName(defaultName)) else script
            graph.scripts.save(named)
        }
    }

    // endregion

    // region Editing

    internal val controlActions = object : ControlActions {
        override fun onToggleRun() = toggleRun()
        override fun onAddTap() = addTap()
        override fun onAddSwipe() = addSwipe()
        override fun onRemoveLast() = removeLast()
        override fun onOpenSettings() {
            session?.dialog = OverlayDialog.Settings
        }
        override fun onOpenSequences() {
            session?.dialog = OverlayDialog.OpenSequence
        }
        override fun onToggleMinimized() {
            session?.let { it.toolbarMinimized = !it.toolbarMinimized }
        }
        override fun onClose() = close()
    }

    internal val dialogActions = object : DialogActions {
        override fun dismiss() {
            session?.dialog = null
        }

        override fun updateScript(transform: (Script) -> Script) {
            val s = session ?: return
            s.script = transform(s.script)
        }

        override fun deleteStep(stepId: String) {
            val s = session ?: return
            s.dialog = null
            s.script = s.script.copy(steps = s.script.steps.filterNot { it.id == stepId })
            syncMarkers()
        }

        override fun openSequence(id: String?) = loadSequence(id)
    }

    private fun addTap() {
        val s = session ?: return
        val canvas = displaySize()
        val cascade = dp(28f) * (s.script.steps.size % 6)
        val step = TapStep(
            x = (canvas.width / 2f + cascade).coerceAtMost(canvas.width - 1f),
            y = (canvas.height / 2f + cascade).coerceAtMost(canvas.height - 1f),
            holdMs = s.settings.defaultHoldMs,
            delayMs = s.settings.defaultDelayMs,
        )
        s.script = s.script.copy(steps = s.script.steps + step, canvas = canvas)
        syncMarkers()
    }

    private fun addSwipe() {
        val s = session ?: return
        val canvas = displaySize()
        val cascade = dp(28f) * (s.script.steps.size % 6)
        val y = (canvas.height * 0.4f + cascade).coerceAtMost(canvas.height - 1f)
        val step = SwipeStep(
            startX = canvas.width * 0.28f,
            startY = y,
            endX = canvas.width * 0.72f,
            endY = y,
            durationMs = s.settings.defaultSwipeMs,
            delayMs = s.settings.defaultDelayMs,
        )
        s.script = s.script.copy(steps = s.script.steps + step, canvas = canvas)
        syncMarkers()
    }

    private fun removeLast() {
        val s = session ?: return
        if (s.script.steps.isEmpty()) return
        s.script = s.script.copy(steps = s.script.steps.dropLast(1))
        syncMarkers()
    }

    private fun loadSequence(id: String?) {
        val s = session ?: return
        if (!s.isMulti) return
        stopRun()
        flush(s)
        scope.launch {
            val canvas = displaySize()
            val loaded = id?.let { graph.scripts.get(it) }?.fittedTo(canvas)
            if (session !== s) return@launch
            s.dialog = null
            s.lastSaved = loaded
            s.persisted = loaded != null
            s.script = loaded ?: Script(name = "", canvas = canvas)
            syncMarkers()
            graph.settings.rememberSession(OverlayMode.Multi, loaded?.id)
            publishStatus(force = true)
        }
    }

    // endregion

    // region Running

    private fun toggleRun() {
        if (runJob?.isActive == true) {
            haptic(controls?.view, HapticFeedbackConstantsCompat.TOGGLE_OFF)
            stopRun()
        } else {
            startRun()
        }
    }

    private fun startRun() {
        val s = session ?: return
        val childScope = sessionScope ?: return
        if (s.script.steps.isEmpty()) {
            haptic(controls?.view, HapticFeedbackConstantsCompat.REJECT)
            toast(R.string.overlay_need_target)
            return
        }
        haptic(controls?.view, HapticFeedbackConstantsCompat.TOGGLE_ON)
        s.dialog = null
        moveControlsAwayFromTargets(s)
        val script = s.script
        val canvas = displaySize()
        val planner = GesturePlanner(canvas, positionSpreadPx(s.settings), Random.Default)
        runJob = childScope.launch {
            s.running = true
            s.runStartedAt = SystemClock.elapsedRealtime()
            s.progress = RunProgress()
            setMarkersTouchable(false)
            registerScreenOff(s)
            publishStatus(force = true)
            if (s.persisted) graph.ioScope.launch { graph.scripts.markRun(script.id) }
            try {
                val result = runner.run(script, planner, runListener(s, script))
                when (result.outcome) {
                    RunOutcome.Finished -> {
                        s.finishedCount++
                        haptic(controls?.view, HapticFeedbackConstantsCompat.CONFIRM)
                    }
                    RunOutcome.Rejected -> {
                        haptic(controls?.view, HapticFeedbackConstantsCompat.REJECT)
                        toast(R.string.service_lost)
                    }
                    RunOutcome.NothingToRun -> {
                        haptic(controls?.view, HapticFeedbackConstantsCompat.REJECT)
                        toast(R.string.overlay_need_target)
                    }
                }
            } finally {
                s.running = false
                s.activeStepId = null
                setMarkersTouchable(true)
                unregisterScreenOff()
                publishStatus(force = true)
            }
        }
    }

    fun stopRun() {
        runJob?.cancel()
        runJob = null
    }

    private fun runListener(s: OverlaySession, script: Script) = object : RunListener {
        override fun onAction(stepIndex: Int, plan: GesturePlan) {
            s.activeStepId = script.steps.getOrNull(stepIndex)?.id
            if (s.settings.tapFeedback) addPulse(s, plan)
        }

        override fun onProgress(progress: RunProgress) {
            s.progress = progress
            publishStatus()
        }
    }

    private fun addPulse(s: OverlaySession, plan: GesturePlan) {
        val now = SystemClock.uptimeMillis()
        if (now - lastPulseAt < MIN_PULSE_GAP_MS) return
        lastPulseAt = now
        val pulse = when (plan) {
            is TapPlan -> Pulse(pulseIds++, plan.x, plan.y, swipe = false)
            is SwipePlan -> Pulse(pulseIds++, plan.startX, plan.startY, swipe = true)
        }
        if (s.pulses.size >= MAX_PULSES) s.pulses.removeAt(0)
        s.pulses.add(pulse)
    }

    private fun positionSpreadPx(settings: AppSettings): Float = dp(settings.markerSizeDp.toFloat()) * 0.5f * 0.6f

    private fun registerScreenOff(s: OverlaySession) {
        if (!s.settings.stopOnScreenOff || screenOffReceiver != null) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) = stopRun()
        }
        ContextCompat.registerReceiver(
            service,
            receiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        screenOffReceiver = receiver
    }

    private fun unregisterScreenOff() {
        screenOffReceiver?.let { runCatching { service.unregisterReceiver(it) } }
        screenOffReceiver = null
    }

    /**
     * While running, injected taps land on whatever window is on top, including our controls. If
     * any target sits under the collapsed controls, slide them somewhere clear.
     */
    private fun moveControlsAwayFromTargets(s: OverlaySession) {
        val window = controls ?: return
        // Collapsed, the controls are just the play button's square at their top-left corner.
        val side = dp(ControlsLayout.COLLAPSED_DP * s.controlsLayout.scale)
        val canvas = displaySize()
        val margin = dp(12f)
        val points = targetPoints(s.script)
        fun clearAt(x: Int, y: Int): Boolean {
            val rect = Rect(x - margin, y - margin, x + side + margin, y + side + margin)
            return points.none { rect.contains(it.x.roundToInt(), it.y.roundToInt()) }
        }
        val x = window.params.x
        val y = window.params.y
        if (clearAt(x, y)) return
        val edge = dp(12f)
        val farX = if (x + side / 2 < canvas.width / 2) canvas.width - side - edge else edge
        val candidates = listOf(
            farX to y,
            x to dp(96f),
            farX to dp(96f),
            x to canvas.height - side - dp(96f),
            farX to canvas.height - side - dp(96f),
        )
        candidates.firstOrNull { (cx, cy) -> clearAt(cx, cy) }?.let { (cx, cy) ->
            val (clampedX, clampedY) = clampControls(cx, cy, side to side)
            window.moveTo(clampedX, clampedY)
        }
    }

    private fun targetPoints(script: Script): List<PointF> = script.steps.flatMap { step ->
        when (step) {
            is TapStep -> listOf(PointF(step.x, step.y))
            is SwipeStep -> (0..SWIPE_SAMPLES).map { i ->
                val t = i / SWIPE_SAMPLES.toFloat()
                PointF(step.startX + (step.endX - step.startX) * t, step.startY + (step.endY - step.startY) * t)
            }
        }
    }

    // endregion

    // region Windows

    private fun composeView(content: @Composable () -> Unit): ComposeView = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(owner!!))
        setContent(content)
    }

    @Composable
    private fun Themed(s: OverlaySession, content: @Composable () -> Unit) {
        TapPilotTheme(themeMode = s.settings.themeMode, dynamicColor = s.settings.dynamicColor) {
            ProvideHaptics(s.settings.hapticFeedback, content)
        }
    }

    private fun createPathLayer(s: OverlaySession): OverlayWindow {
        val canvas = displaySize()
        val root = BackAwareFrame(context) {}
        owner!!.attachTo(root)
        root.addView(composeView { Themed(s) { PathLayer(s) } })
        val params = OverlayWindow.params(canvas.width, canvas.height, touchable = false)
        return OverlayWindow(windowManager, root, params)
    }

    private fun createControls(s: OverlaySession): OverlayWindow {
        lateinit var window: OverlayWindow
        var startX = 0
        var startY = 0
        val frame = DragFrame(
            context,
            interceptOnlyDrags = true,
            measureUnbounded = true,
            listener = object : DragFrame.Listener {
                override fun onDragStart() {
                    startX = window.params.x
                    startY = window.params.y
                    haptic(window.view, HapticFeedbackConstantsCompat.GESTURE_THRESHOLD_ACTIVATE)
                }

                override fun onDrag(totalDx: Float, totalDy: Float) {
                    val (x, y) = clampControls(startX + totalDx.roundToInt(), startY + totalDy.roundToInt())
                    window.moveTo(x, y)
                }

                override fun onDragEnd() {
                    haptic(window.view, HapticFeedbackConstantsCompat.GESTURE_END)
                    val x = window.params.x
                    val y = window.params.y
                    graph.ioScope.launch { graph.settings.setControlsPosition(x, y) }
                }
            },
        )
        owner!!.attachTo(frame)
        frame.addView(
            composeView { Themed(s) { ControlBar(s, controlActions) } },
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT),
        )
        // Stay on screen whenever the controls change size: when they turn or rescale with the
        // screen or the control size, and when they expand again after a run.
        frame.addOnLayoutChangeListener { view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) {
                view.post { keepControlsOnScreen() }
            }
        }
        val params = OverlayWindow.params(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            touchable = true,
        )
        val canvas = displaySize()
        val settings = s.settings
        val savedX = if (settings.controlsX >= 0) settings.controlsX else dp(8f)
        val savedY = if (settings.controlsY >= 0) settings.controlsY else (canvas.height * 0.22f).roundToInt()
        val (x, y) = clampControls(savedX, savedY, expandedControlsSize(s))
        params.x = x
        params.y = y
        window = OverlayWindow(windowManager, frame, params)
        window.setKeepScreenOn(settings.keepScreenOn)
        return window
    }

    /** Keeps controls of the given [size] on screen; by default, their current size. */
    private fun clampControls(x: Int, y: Int, size: Pair<Int, Int> = controlsSize()): Pair<Int, Int> {
        val canvas = displaySize()
        val (width, height) = size
        return x.coerceIn(0, (canvas.width - width).coerceAtLeast(0)) to
            y.coerceIn(0, (canvas.height - height).coerceAtLeast(0))
    }

    private fun keepControlsOnScreen() {
        val window = controls ?: return
        val (x, y) = clampControls(window.params.x, window.params.y)
        window.moveTo(x, y)
    }

    /** The controls' measured size, or their expanded size until they have been measured. */
    private fun controlsSize(): Pair<Int, Int> {
        val view = controls?.view
        if (view != null && view.width > 0 && view.height > 0) return view.width to view.height
        return session?.let(::expandedControlsSize) ?: (0 to 0)
    }

    private fun expandedControlsSize(s: OverlaySession): Pair<Int, Int> {
        val layout = s.controlsLayout
        val length = dp(ControlsLayout.lengthDp(s.mode) * layout.scale)
        val thickness = dp(ControlsLayout.COLLAPSED_DP * layout.scale)
        return if (layout.vertical) thickness to length else length to thickness
    }

    private fun controlsLayoutFor(s: OverlaySession): ControlsLayout {
        val canvas = displaySize()
        return ControlsLayout.choose(canvas.width / density, canvas.height / density, s.mode, s.settings.controlSize)
    }

    /** Keeps the controls above newly added target windows. */
    private fun raiseControls() {
        val window = controls ?: return
        if (!window.isShown) return
        window.remove()
        window.show()
    }

    private fun desiredHandles(script: Script): List<Pair<Handle, Int>> = script.steps.flatMapIndexed { index, step ->
        when (step) {
            is TapStep -> listOf(Handle(step.id, HandlePart.Tap) to index + 1)
            is SwipeStep -> listOf(
                Handle(step.id, HandlePart.SwipeStart) to index + 1,
                Handle(step.id, HandlePart.SwipeEnd) to index + 1,
            )
        }
    }

    /** Adds, removes and repositions target windows to match the session's script. */
    private fun syncMarkers() {
        val s = session ?: return
        val desired = desiredHandles(s.script)
        val keys = desired.map { it.first.key }.toSet()
        markers.keys.filterNot { it in keys }.forEach { key -> markers.remove(key)?.window?.remove() }
        var added = false
        val sizePx = dp(s.settings.markerSizeDp.toFloat())
        for ((handle, number) in desired) {
            val marker = markers[handle.key] ?: createMarker(s, handle).also {
                markers[handle.key] = it
                it.window.params.width = sizePx
                it.window.params.height = sizePx
                positionMarker(s, it, sizePx)
                it.window.setTouchable(!s.running)
                it.window.show()
                added = true
            }
            marker.state.number = number
            marker.window.resize(sizePx, sizePx)
            if (!marker.dragging) positionMarker(s, marker, sizePx)
        }
        if (added) raiseControls()
    }

    private fun positionMarker(s: OverlaySession, marker: MarkerWindow, sizePx: Int) {
        val point = pointOf(s.script, marker.handle) ?: return
        marker.window.moveTo((point.x - sizePx / 2f).roundToInt(), (point.y - sizePx / 2f).roundToInt())
    }

    private fun pointOf(script: Script, handle: Handle): PointF? {
        val step = script.steps.firstOrNull { it.id == handle.stepId } ?: return null
        return when (step) {
            is TapStep -> PointF(step.x, step.y)
            is SwipeStep -> if (handle.part == HandlePart.SwipeEnd) PointF(step.endX, step.endY) else PointF(step.startX, step.startY)
        }
    }

    private fun movePoint(script: Script, handle: Handle, x: Float, y: Float): Script = script.copy(
        steps = script.steps.map { step: Step ->
            if (step.id != handle.stepId) {
                step
            } else {
                when (step) {
                    is TapStep -> step.copy(x = x, y = y)
                    is SwipeStep -> if (handle.part == HandlePart.SwipeEnd) {
                        step.copy(endX = x, endY = y)
                    } else {
                        step.copy(startX = x, startY = y)
                    }
                }
            }
        },
    )

    private fun createMarker(s: OverlaySession, handle: Handle): MarkerWindow {
        val state = MarkerState(handle)
        lateinit var marker: MarkerWindow
        var start = PointF()
        val frame = DragFrame(
            context,
            interceptOnlyDrags = false,
            listener = object : DragFrame.Listener {
                override fun onPressChanged(pressed: Boolean) {
                    state.pressed = pressed
                }

                override fun onDragStart() {
                    marker.dragging = true
                    start = pointOf(s.script, handle) ?: PointF()
                    haptic(marker.window.view, HapticFeedbackConstantsCompat.GESTURE_THRESHOLD_ACTIVATE)
                }

                override fun onDrag(totalDx: Float, totalDy: Float) {
                    val canvas = displaySize()
                    val x = (start.x + totalDx).coerceIn(0f, canvas.width - 1f)
                    val y = (start.y + totalDy).coerceIn(0f, canvas.height - 1f)
                    s.script = movePoint(s.script, handle, x, y).copy(canvas = canvas)
                    val size = marker.window.params.width
                    marker.window.moveTo((x - size / 2f).roundToInt(), (y - size / 2f).roundToInt())
                }

                override fun onDragEnd() {
                    marker.dragging = false
                    haptic(marker.window.view, HapticFeedbackConstantsCompat.GESTURE_END)
                }

                override fun onTap() {
                    if (s.running) return
                    s.dialog = if (s.isMulti) OverlayDialog.EditStep(handle.stepId) else OverlayDialog.Settings
                }
            },
        )
        owner!!.attachTo(frame)
        frame.addView(composeView { Themed(s) { TargetMarker(state, s) } })
        val params = OverlayWindow.params(0, 0, touchable = true)
        marker = MarkerWindow(handle, OverlayWindow(windowManager, frame, params), state)
        return marker
    }

    private fun setMarkersTouchable(touchable: Boolean) {
        markers.values.forEach { it.window.setTouchable(touchable) }
    }

    private fun showDialogWindow(s: OverlaySession) {
        if (dialogWindow != null) return
        val root = BackAwareFrame(context) { s.dialog = null }
        owner!!.attachTo(root)
        root.addView(composeView { Themed(s) { OverlayDialogHost(s, graph, dialogActions) } })
        val params = OverlayWindow.params(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            touchable = true,
            focusable = true,
            noLimits = false,
        )
        dialogWindow = OverlayWindow(windowManager, root, params).also { it.show() }
    }

    private fun hideDialogWindow() {
        dialogWindow?.remove()
        dialogWindow = null
    }

    // endregion

    private fun publishStatus(force: Boolean = false) {
        val s = session
        if (s == null) {
            TapPilotRuntime.publish(OverlayStatus.Hidden)
            return
        }
        val now = SystemClock.uptimeMillis()
        if (!force && now - lastStatusAt < STATUS_THROTTLE_MS) return
        lastStatusAt = now
        TapPilotRuntime.publish(
            OverlayStatus.Visible(
                mode = s.mode,
                scriptId = s.script.id.takeIf { s.persisted },
                scriptName = s.script.name,
                running = s.running,
                progress = s.progress,
            ),
        )
    }

    /** Plays [feedback], a [HapticFeedbackConstantsCompat] value, if the user wants haptics. */
    private fun haptic(view: View?, feedback: Int) {
        if (view != null && session?.settings?.hapticFeedback == true) ViewCompat.performHapticFeedback(view, feedback)
    }

    private fun toast(@StringRes text: Int) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    private fun displaySize(): CanvasSize {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.maximumWindowMetrics.bounds
            CanvasSize(bounds.width(), bounds.height())
        } else {
            val point = Point()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealSize(point)
            CanvasSize(point.x, point.y)
        }
    }

    private class MarkerWindow(val handle: Handle, val window: OverlayWindow, val state: MarkerState) {
        var dragging = false
    }

    private companion object {
        const val AUTOSAVE_DEBOUNCE_MS = 400L
        const val STATUS_THROTTLE_MS = 250L
        const val MIN_PULSE_GAP_MS = 70L
        const val MAX_PULSES = 12
        const val SWIPE_SAMPLES = 12
    }
}
