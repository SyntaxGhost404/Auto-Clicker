package io.github.syntaxghost404.tappilot.ui

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.syntaxghost404.tappilot.AppGraph
import io.github.syntaxghost404.tappilot.R
import io.github.syntaxghost404.tappilot.appGraph
import io.github.syntaxghost404.tappilot.core.data.AppSettings
import io.github.syntaxghost404.tappilot.core.data.ControlSize
import io.github.syntaxghost404.tappilot.core.data.OverlayMode
import io.github.syntaxghost404.tappilot.core.data.ThemeMode
import io.github.syntaxghost404.tappilot.core.model.Script
import io.github.syntaxghost404.tappilot.core.model.Step
import io.github.syntaxghost404.tappilot.core.model.StopRule
import io.github.syntaxghost404.tappilot.core.model.Variation
import io.github.syntaxghost404.tappilot.service.OverlayStatus
import io.github.syntaxghost404.tappilot.service.TapPilotRuntime
import io.github.syntaxghost404.tappilot.ui.components.RenameDialog
import io.github.syntaxghost404.tappilot.ui.navigation.About
import io.github.syntaxghost404.tappilot.ui.navigation.Home
import io.github.syntaxghost404.tappilot.ui.navigation.HowTo
import io.github.syntaxghost404.tappilot.ui.navigation.SequenceEditor
import io.github.syntaxghost404.tappilot.ui.navigation.Sequences
import io.github.syntaxghost404.tappilot.ui.navigation.ServiceSetup
import io.github.syntaxghost404.tappilot.ui.navigation.Settings
import io.github.syntaxghost404.tappilot.ui.navigation.SinglePoint
import io.github.syntaxghost404.tappilot.ui.navigation.TopLevelRoute
import io.github.syntaxghost404.tappilot.ui.navigation.Troubleshooting
import io.github.syntaxghost404.tappilot.ui.navigation.Welcome
import io.github.syntaxghost404.tappilot.ui.navigation.selectTab
import io.github.syntaxghost404.tappilot.ui.screens.editor.EditorActions
import io.github.syntaxghost404.tappilot.ui.screens.editor.EditorScreen
import io.github.syntaxghost404.tappilot.ui.screens.editor.EditorState
import io.github.syntaxghost404.tappilot.ui.screens.editor.EditorViewModel
import io.github.syntaxghost404.tappilot.ui.screens.help.AboutScreen
import io.github.syntaxghost404.tappilot.ui.screens.help.HowToScreen
import io.github.syntaxghost404.tappilot.ui.screens.help.TroubleshootingActions
import io.github.syntaxghost404.tappilot.ui.screens.help.TroubleshootingScreen
import io.github.syntaxghost404.tappilot.ui.screens.help.TroubleshootingState
import io.github.syntaxghost404.tappilot.ui.screens.home.HomeActions
import io.github.syntaxghost404.tappilot.ui.screens.home.HomeScreen
import io.github.syntaxghost404.tappilot.ui.screens.home.HomeState
import io.github.syntaxghost404.tappilot.ui.screens.home.HomeViewModel
import io.github.syntaxghost404.tappilot.ui.screens.sequences.SequencesActions
import io.github.syntaxghost404.tappilot.ui.screens.sequences.SequencesScreen
import io.github.syntaxghost404.tappilot.ui.screens.sequences.SequencesViewModel
import io.github.syntaxghost404.tappilot.ui.screens.settings.SettingsActions
import io.github.syntaxghost404.tappilot.ui.screens.settings.SettingsScreen
import io.github.syntaxghost404.tappilot.ui.screens.settings.SettingsViewModel
import io.github.syntaxghost404.tappilot.ui.screens.setup.ServiceSetupActions
import io.github.syntaxghost404.tappilot.ui.screens.setup.ServiceSetupScreen
import io.github.syntaxghost404.tappilot.ui.screens.setup.WelcomeScreen
import io.github.syntaxghost404.tappilot.ui.screens.single.SinglePointActions
import io.github.syntaxghost404.tappilot.ui.screens.single.SinglePointScreen
import io.github.syntaxghost404.tappilot.ui.screens.single.SinglePointViewModel
import io.github.syntaxghost404.tappilot.ui.system.SystemScreens
import io.github.syntaxghost404.tappilot.ui.theme.TapPilotTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val BACKUP_MIME = "application/json"
private val IMPORT_MIME_TYPES = arrayOf("application/json", "text/plain", "application/octet-stream")

@Composable
fun TapPilotRoot(pendingStart: OverlayMode?, onPendingStartHandled: () -> Unit, onReady: () -> Unit) {
    val graph = LocalContext.current.appGraph
    val settings by graph.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    val loaded = settings ?: return
    LaunchedEffect(Unit) { onReady() }
    TapPilotTheme(themeMode = loaded.themeMode, dynamicColor = loaded.dynamicColor) {
        ProvideHaptics(loaded.hapticFeedback) {
            AppNavigation(graph, loaded, pendingStart, onPendingStartHandled)
        }
    }
}

@Composable
private fun MessagesEffect(messages: Flow<UiMessage>, host: SnackbarHostState) {
    val resources = LocalResources.current
    LaunchedEffect(messages) {
        messages.collect { message ->
            val result = host.showSnackbar(
                message = message.text(resources),
                actionLabel = message.actionLabel?.invoke(resources),
                duration = if (message.onAction != null) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) message.onAction?.invoke()
        }
    }
}

@Composable
private fun AppNavigation(
    graph: AppGraph,
    settings: AppSettings,
    pendingStart: OverlayMode?,
    onPendingStartHandled: () -> Unit,
) {
    val backStack = rememberNavBackStack(if (settings.onboardingDone) Home else Welcome)
    // Read through a state so that screens already on the back stack see changes.
    val latestSettings by rememberUpdatedState(settings)
    val activity = LocalActivity.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun startControls(mode: OverlayMode, scriptId: String? = null) {
        if (TapPilotRuntime.openControls(mode, scriptId)) {
            activity?.moveTaskToBack(true)
        } else {
            backStack.add(ServiceSetup)
        }
    }

    fun finishSetup() {
        scope.launch { graph.settings.setOnboardingDone(true) }
        backStack.clear()
        backStack.add(Home)
    }

    fun selectTab(tab: TopLevelRoute) = backStack.selectTab(tab)

    LaunchedEffect(pendingStart) {
        val mode = pendingStart ?: return@LaunchedEffect
        // When the process was just started the system may still be binding the service.
        withTimeoutOrNull(2_000) { TapPilotRuntime.serviceConnected.first { it } }
        startControls(mode)
        onPendingStartHandled()
    }

    val slide = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val enterFade = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val exitFade = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    val fadeThrough: () -> ContentTransform = { fadeIn(enterFade) togetherWith fadeOut(exitFade) }
    val topLevel = NavDisplay.transitionSpec { fadeThrough() } +
        NavDisplay.popTransitionSpec { fadeThrough() } +
        NavDisplay.predictivePopTransitionSpec { fadeThrough() }

    NavDisplay(
        backStack = backStack,
        onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        transitionSpec = {
            (slideInHorizontally(slide) { it / 5 } + fadeIn(enterFade)) togetherWith
                (slideOutHorizontally(slide) { -it / 10 } + fadeOut(exitFade))
        },
        popTransitionSpec = {
            (slideInHorizontally(slide) { -it / 10 } + fadeIn(enterFade)) togetherWith
                (slideOutHorizontally(slide) { it / 5 } + fadeOut(exitFade))
        },
        predictivePopTransitionSpec = {
            (slideInHorizontally(slide) { -it / 10 } + fadeIn(enterFade)) togetherWith
                (slideOutHorizontally(slide) { it / 5 } + fadeOut(exitFade))
        },
        entryProvider = entryProvider<NavKey> {
            entry<Welcome> {
                WelcomeScreen(onGetStarted = { backStack.add(ServiceSetup) })
            }

            entry<ServiceSetup> {
                val service = rememberServiceState()
                ServiceSetupScreen(
                    service = service,
                    consented = latestSettings.accessibilityConsent,
                    actions = remember {
                        object : ServiceSetupActions {
                            override fun onBack() {
                                if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) else finishSetup()
                            }
                            override fun onAgree() {
                                graph.ioScope.launch { graph.settings.setAccessibilityConsent(true) }
                            }
                            override fun onOpenAccessibility() = SystemScreens.openAccessibilitySettings(context)
                            override fun onOpenAppInfo() = SystemScreens.openAppInfo(context)
                            override fun onDone() = finishSetup()
                            override fun onNotNow() = finishSetup()
                        }
                    },
                )
            }

            entry<Home>(metadata = topLevel) {
                val viewModel = graphViewModel { HomeViewModel(it) }
                val quick by viewModel.quick.collectAsStateWithLifecycle()
                val recent by viewModel.recent.collectAsStateWithLifecycle()
                val status by TapPilotRuntime.status.collectAsStateWithLifecycle()
                val service = rememberServiceState()
                HomeScreen(
                    state = HomeState(
                        service = service,
                        status = status,
                        quick = quick,
                        recent = recent,
                        serviceSetUp = latestSettings.serviceSetUp,
                        consented = latestSettings.accessibilityConsent,
                    ),
                    actions = remember {
                        object : HomeActions {
                            override fun onSelectTab(tab: TopLevelRoute) = selectTab(tab)
                            override fun onTurnOn() {
                                backStack.add(ServiceSetup)
                            }
                            override fun onOpenAccessibility() = SystemScreens.openAccessibilitySettings(context)
                            override fun onTroubleshoot() {
                                backStack.add(Troubleshooting)
                            }
                            override fun onStartSingle() = startControls(OverlayMode.Single)
                            override fun onSingleSettings() {
                                backStack.add(SinglePoint)
                            }
                            override fun onNewSequence() = startControls(OverlayMode.Multi)
                            override fun onOpenLibrary() = selectTab(Sequences)
                            override fun onOpenSequence(id: String) {
                                backStack.add(SequenceEditor(id))
                            }
                            override fun onStartSequence(id: String) = startControls(OverlayMode.Multi, id)
                            override fun onStop() = TapPilotRuntime.stopRun()
                            override fun onHideControls() = TapPilotRuntime.closeControls()
                        }
                    },
                )
            }

            entry<Sequences>(metadata = topLevel) {
                val viewModel = graphViewModel { SequencesViewModel(it) }
                val scripts by viewModel.scripts.collectAsStateWithLifecycle()
                val snackbar = remember { SnackbarHostState() }
                MessagesEffect(viewModel.messages, snackbar)
                var renaming by remember { mutableStateOf<Script?>(null) }
                val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_MIME)) { uri ->
                    if (uri != null) viewModel.export(uri)
                }
                val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    if (uri != null) viewModel.import(uri)
                }
                val copySuffix = stringResource(R.string.copy_suffix)
                SequencesScreen(
                    scripts = scripts,
                    snackbarHostState = snackbar,
                    actions = remember {
                        object : SequencesActions {
                            override fun onSelectTab(tab: TopLevelRoute) = selectTab(tab)
                            override fun onNew() = startControls(OverlayMode.Multi)
                            override fun onOpen(id: String) {
                                backStack.add(SequenceEditor(id))
                            }
                            override fun onStart(id: String) = startControls(OverlayMode.Multi, id)
                            override fun onRename(script: Script) {
                                renaming = script
                            }
                            override fun onDuplicate(id: String) {
                                viewModel.duplicate(id, copySuffix)
                            }
                            override fun onExport(selection: List<Script>?) {
                                viewModel.prepareExport(selection)
                                exporter.launch(viewModel.exportFileName(selection))
                            }
                            override fun onImport() = importer.launch(IMPORT_MIME_TYPES)
                            override fun onDelete(id: String) {
                                viewModel.delete(id)
                            }
                        }
                    },
                )
                renaming?.let { script ->
                    RenameDialog(
                        initial = script.name,
                        onConfirm = { name ->
                            viewModel.rename(script.id, name)
                            renaming = null
                        },
                        onDismiss = { renaming = null },
                    )
                }
            }

            entry<SequenceEditor> { key ->
                val viewModel = graphViewModel(key = key.scriptId) { EditorViewModel(it, key.scriptId) }
                val library = graphViewModel(key = "library-${key.scriptId}") { SequencesViewModel(it) }
                val state by viewModel.state.collectAsStateWithLifecycle()
                val snackbar = remember { SnackbarHostState() }
                MessagesEffect(library.messages, snackbar)
                var renaming by remember { mutableStateOf(false) }
                var confirmDelete by remember { mutableStateOf(false) }
                val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_MIME)) { uri ->
                    if (uri != null) library.export(uri)
                }
                val copySuffix = stringResource(R.string.copy_suffix)
                val script = (state as? EditorState.Loaded)?.script
                EditorScreen(
                    state = state,
                    snackbarHostState = snackbar,
                    actions = object : EditorActions {
                        override fun onBack() {
                            backStack.removeAt(backStack.lastIndex)
                        }
                        override fun onStart() = startControls(OverlayMode.Multi, key.scriptId)
                        override fun onRename() {
                            renaming = true
                        }
                        override fun onDuplicate() {
                            scope.launch {
                                graph.scripts.duplicate(key.scriptId, copySuffix)?.let { backStack.add(SequenceEditor(it.id)) }
                            }
                        }
                        override fun onExport() {
                            val selection = script?.let(::listOf) ?: return
                            library.prepareExport(selection)
                            exporter.launch(library.exportFileName(selection))
                        }
                        override fun onDelete() {
                            confirmDelete = true
                        }
                        override fun onStopRule(rule: StopRule) {
                            viewModel.setStopRule(rule)
                        }
                        override fun onVariation(variation: Variation) {
                            viewModel.setVariation(variation)
                        }
                        override fun onUpdateStep(stepId: String, transform: (Step) -> Step) {
                            viewModel.updateStep(stepId, transform)
                        }
                        override fun onDeleteStep(stepId: String) {
                            viewModel.deleteStep(stepId)
                        }
                        override fun onMoveStep(stepId: String, offset: Int) {
                            viewModel.moveStep(stepId, offset)
                        }
                    },
                )
                if (renaming && script != null) {
                    RenameDialog(
                        initial = script.name,
                        onConfirm = { name ->
                            viewModel.rename(name)
                            renaming = false
                        },
                        onDismiss = { renaming = false },
                    )
                }
                if (confirmDelete && script != null) {
                    AlertDialog(
                        onDismissRequest = { confirmDelete = false },
                        title = { Text(stringResource(R.string.action_delete)) },
                        text = { Text(script.name) },
                        confirmButton = {
                            Button(
                                onClick = {
                                    confirmDelete = false
                                    graph.ioScope.launch { graph.scripts.delete(script.id) }
                                    backStack.removeAt(backStack.lastIndex)
                                },
                                shapes = ButtonDefaults.shapes(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError,
                                ),
                            ) { Text(stringResource(R.string.action_delete)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) }
                        },
                    )
                }
            }

            entry<SinglePoint> {
                val viewModel = graphViewModel { SinglePointViewModel(it) }
                val quick by viewModel.quick.collectAsStateWithLifecycle()
                val snackbar = remember { SnackbarHostState() }
                val resetMessage = stringResource(R.string.target_reset_done)
                SinglePointScreen(
                    quick = quick,
                    snackbarHostState = snackbar,
                    actions = remember {
                        object : SinglePointActions {
                            override fun onBack() {
                                backStack.removeAt(backStack.lastIndex)
                            }
                            override fun onStart() = startControls(OverlayMode.Single)
                            override fun onInterval(ms: Long) {
                                viewModel.setInterval(ms)
                            }
                            override fun onStopRule(rule: StopRule) {
                                viewModel.setStopRule(rule)
                            }
                            override fun onVariation(variation: Variation) {
                                viewModel.setVariation(variation)
                            }
                            override fun onResetTarget() {
                                viewModel.resetTarget()
                                scope.launch { snackbar.showSnackbar(resetMessage) }
                            }
                        }
                    },
                )
            }

            entry<Settings>(metadata = topLevel) {
                val viewModel = graphViewModel { SettingsViewModel(it) }
                val library = graphViewModel(key = "settings-library") { SequencesViewModel(it) }
                val current by viewModel.settings.collectAsStateWithLifecycle()
                val status by TapPilotRuntime.status.collectAsStateWithLifecycle()
                val snackbar = remember { SnackbarHostState() }
                MessagesEffect(library.messages, snackbar)
                val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_MIME)) { uri ->
                    if (uri != null) library.export(uri)
                }
                val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    if (uri != null) library.import(uri)
                }
                SettingsScreen(
                    settings = current,
                    runActive = (status as? OverlayStatus.Visible)?.running == true,
                    snackbarHostState = snackbar,
                    actions = remember {
                        object : SettingsActions {
                            override fun onSelectTab(tab: TopLevelRoute) = selectTab(tab)
                            override fun onTheme(mode: ThemeMode) {
                                viewModel.setTheme(mode)
                            }
                            override fun onDynamicColor(enabled: Boolean) {
                                viewModel.setDynamicColor(enabled)
                            }
                            override fun onControlSize(size: ControlSize) {
                                viewModel.setControlSize(size)
                            }
                            override fun onMarkerSize(dp: Int) {
                                viewModel.setMarkerSize(dp)
                            }
                            override fun onTapFeedback(enabled: Boolean) {
                                viewModel.setTapFeedback(enabled)
                            }
                            override fun onKeepScreenOn(enabled: Boolean) {
                                viewModel.setKeepScreenOn(enabled)
                            }
                            override fun onStopOnScreenOff(enabled: Boolean) {
                                viewModel.setStopOnScreenOff(enabled)
                            }
                            override fun onHapticFeedback(enabled: Boolean) {
                                viewModel.setHapticFeedback(enabled)
                            }
                            override fun onDefaultDelay(ms: Long) {
                                viewModel.setDefaultDelay(ms)
                            }
                            override fun onDefaultHold(ms: Long) {
                                viewModel.setDefaultHold(ms)
                            }
                            override fun onDefaultSwipe(ms: Long) {
                                viewModel.setDefaultSwipe(ms)
                            }
                            override fun onStopRun() = TapPilotRuntime.stopRun()
                            override fun onExportAll() {
                                library.prepareExport(null)
                                exporter.launch(library.exportFileName(null))
                            }
                            override fun onImport() = importer.launch(IMPORT_MIME_TYPES)
                            override fun onHowTo() {
                                backStack.add(HowTo)
                            }
                            override fun onTroubleshooting() {
                                backStack.add(Troubleshooting)
                            }
                            override fun onAbout() {
                                backStack.add(About)
                            }
                        }
                    },
                )
            }

            entry<HowTo> { HowToScreen(onBack = { backStack.removeAt(backStack.lastIndex) }) }

            entry<Troubleshooting> {
                val service = rememberServiceState()
                var batteryOptimized by remember { mutableStateOf(SystemScreens.isBatteryOptimized(context)) }
                LifecycleResumeEffect(Unit) {
                    batteryOptimized = SystemScreens.isBatteryOptimized(context)
                    onPauseOrDispose { }
                }
                TroubleshootingScreen(
                    state = TroubleshootingState(service, batteryOptimized),
                    actions = remember {
                        object : TroubleshootingActions {
                            override fun onBack() {
                                backStack.removeAt(backStack.lastIndex)
                            }
                            override fun onOpenAccessibility() = SystemScreens.openAccessibilitySettings(context)
                            override fun onOpenBattery() = SystemScreens.openBatterySettings(context)
                            override fun onOpenAppInfo() = SystemScreens.openAppInfo(context)
                        }
                    },
                )
            }

            entry<About> { AboutScreen(onBack = { backStack.removeAt(backStack.lastIndex) }) }
        },
    )
}
