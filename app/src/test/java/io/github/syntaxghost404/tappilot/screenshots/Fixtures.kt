package io.github.syntaxghost404.tappilot.screenshots

import io.github.syntaxghost404.tappilot.core.model.CanvasSize
import io.github.syntaxghost404.tappilot.core.model.Script
import io.github.syntaxghost404.tappilot.core.model.Step
import io.github.syntaxghost404.tappilot.core.model.StopMode
import io.github.syntaxghost404.tappilot.core.model.StopRule
import io.github.syntaxghost404.tappilot.core.model.SwipeStep
import io.github.syntaxghost404.tappilot.core.model.TapStep
import io.github.syntaxghost404.tappilot.core.model.Variation
import io.github.syntaxghost404.tappilot.ui.navigation.TopLevelRoute
import io.github.syntaxghost404.tappilot.ui.screens.editor.EditorActions
import io.github.syntaxghost404.tappilot.ui.screens.help.TroubleshootingActions
import io.github.syntaxghost404.tappilot.ui.screens.home.HomeActions
import io.github.syntaxghost404.tappilot.ui.screens.sequences.SequencesActions
import io.github.syntaxghost404.tappilot.ui.screens.settings.SettingsActions
import io.github.syntaxghost404.tappilot.ui.screens.setup.ServiceSetupActions
import io.github.syntaxghost404.tappilot.ui.screens.single.SinglePointActions
import io.github.syntaxghost404.tappilot.core.data.ControlSize
import io.github.syntaxghost404.tappilot.core.data.ThemeMode

/** Sample data shared by the screenshot tests. Coordinates suit a 1080 x 2400 screen. */
object Fixtures {
    val canvas = CanvasSize(1080, 2400)

    val quick = Script(
        id = Script.QUICK_ID,
        name = "Single point",
        steps = listOf(TapStep(id = "q", x = 540f, y = 1300f, delayMs = 100L)),
        variation = Variation(position = true),
    )

    val sequence = Script(
        id = "daily",
        name = "Daily reward",
        canvas = canvas,
        steps = listOf<Step>(
            TapStep(id = "s1", x = 300f, y = 760f, holdMs = 10L, delayMs = 400L),
            TapStep(id = "s2", x = 780f, y = 980f, holdMs = 600L, delayMs = 250L),
            SwipeStep(id = "s3", startX = 220f, startY = 1500f, endX = 860f, endY = 1360f, durationMs = 450L, delayMs = 800L),
            TapStep(id = "s4", x = 540f, y = 1880f, holdMs = 10L, delayMs = 1_500L),
        ),
        stopRule = StopRule(mode = StopMode.Cycles, cycles = 50),
        variation = Variation(position = true, timingPercent = 15),
    )

    /** A sequence placed for a 1600 x 720 screen, clear of controls in the top-left corner. */
    val landscapeSequence = Script(
        id = "landscape",
        name = "Daily reward",
        canvas = CanvasSize(1600, 720),
        steps = listOf<Step>(
            TapStep(id = "l1", x = 1000f, y = 200f, holdMs = 10L, delayMs = 400L),
            TapStep(id = "l2", x = 1340f, y = 320f, holdMs = 600L, delayMs = 250L),
            SwipeStep(id = "l3", startX = 940f, startY = 520f, endX = 1400f, endY = 580f, durationMs = 450L, delayMs = 800L),
        ),
        stopRule = StopRule(mode = StopMode.Cycles, cycles = 50),
    )

    val library = listOf(
        sequence,
        Script(
            id = "farm",
            name = "Harvest loop",
            steps = List(6) { TapStep(id = "f$it", x = 100f * it, y = 400f) },
            stopRule = StopRule(mode = StopMode.Duration, durationMs = 30 * 60_000L),
        ),
        Script(
            id = "reader",
            name = "Page turner",
            steps = listOf(SwipeStep(id = "r1", startX = 900f, startY = 1200f, endX = 150f, endY = 1200f)),
        ),
        Script(
            id = "quiz",
            name = "Quiz streak",
            steps = List(3) { TapStep(id = "q$it", x = 200f, y = 300f * it) } +
                SwipeStep(id = "q9", startX = 1f, startY = 1f, endX = 2f, endY = 2f),
            stopRule = StopRule(mode = StopMode.Cycles, cycles = 200),
        ),
    )

    object NoHome : HomeActions {
        override fun onSelectTab(tab: TopLevelRoute) = Unit
        override fun onTurnOn() = Unit
        override fun onOpenAccessibility() = Unit
        override fun onTroubleshoot() = Unit
        override fun onStartSingle() = Unit
        override fun onSingleSettings() = Unit
        override fun onNewSequence() = Unit
        override fun onOpenLibrary() = Unit
        override fun onOpenSequence(id: String) = Unit
        override fun onStartSequence(id: String) = Unit
        override fun onStop() = Unit
        override fun onHideControls() = Unit
    }

    object NoSequences : SequencesActions {
        override fun onSelectTab(tab: TopLevelRoute) = Unit
        override fun onNew() = Unit
        override fun onOpen(id: String) = Unit
        override fun onStart(id: String) = Unit
        override fun onRename(script: Script) = Unit
        override fun onDuplicate(id: String) = Unit
        override fun onExport(selection: List<Script>?) = Unit
        override fun onImport() = Unit
        override fun onDelete(id: String) = Unit
    }

    object NoEditor : EditorActions {
        override fun onBack() = Unit
        override fun onStart() = Unit
        override fun onRename() = Unit
        override fun onDuplicate() = Unit
        override fun onExport() = Unit
        override fun onDelete() = Unit
        override fun onStopRule(rule: StopRule) = Unit
        override fun onVariation(variation: Variation) = Unit
        override fun onUpdateStep(stepId: String, transform: (Step) -> Step) = Unit
        override fun onDeleteStep(stepId: String) = Unit
        override fun onMoveStep(stepId: String, offset: Int) = Unit
    }

    object NoSingle : SinglePointActions {
        override fun onBack() = Unit
        override fun onStart() = Unit
        override fun onInterval(ms: Long) = Unit
        override fun onStopRule(rule: StopRule) = Unit
        override fun onVariation(variation: Variation) = Unit
        override fun onResetTarget() = Unit
    }

    object NoSettings : SettingsActions {
        override fun onSelectTab(tab: TopLevelRoute) = Unit
        override fun onTheme(mode: ThemeMode) = Unit
        override fun onDynamicColor(enabled: Boolean) = Unit
        override fun onControlSize(size: ControlSize) = Unit
        override fun onMarkerSize(dp: Int) = Unit
        override fun onTapFeedback(enabled: Boolean) = Unit
        override fun onKeepScreenOn(enabled: Boolean) = Unit
        override fun onStopOnScreenOff(enabled: Boolean) = Unit
        override fun onHapticFeedback(enabled: Boolean) = Unit
        override fun onDefaultDelay(ms: Long) = Unit
        override fun onDefaultHold(ms: Long) = Unit
        override fun onDefaultSwipe(ms: Long) = Unit
        override fun onStopRun() = Unit
        override fun onExportAll() = Unit
        override fun onImport() = Unit
        override fun onHowTo() = Unit
        override fun onTroubleshooting() = Unit
        override fun onAbout() = Unit
    }

    object NoSetup : ServiceSetupActions {
        override fun onBack() = Unit
        override fun onAgree() = Unit
        override fun onOpenAccessibility() = Unit
        override fun onOpenAppInfo() = Unit
        override fun onDone() = Unit
        override fun onNotNow() = Unit
    }

    object NoTroubleshooting : TroubleshootingActions {
        override fun onBack() = Unit
        override fun onOpenAccessibility() = Unit
        override fun onOpenBattery() = Unit
        override fun onOpenAppInfo() = Unit
    }
}
