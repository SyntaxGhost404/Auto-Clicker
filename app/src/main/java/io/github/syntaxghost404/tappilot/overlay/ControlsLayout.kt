package io.github.syntaxghost404.tappilot.overlay

import io.github.syntaxghost404.tappilot.core.data.ControlSize
import io.github.syntaxghost404.tappilot.core.data.OverlayMode

/**
 * How the floating controls sit on the current screen. The toolbar runs vertically whenever it fits
 * the screen's height and turns horizontal when it does not, as on a phone held sideways. Only if
 * neither direction fits is it scaled below the chosen control size.
 */
internal data class ControlsLayout(val vertical: Boolean, val scale: Float) {

    companion object {
        val Default = ControlsLayout(vertical = true, scale = 1f)

        // Geometry of ControlBar in dp at scale 1. The Material floating toolbar pads its 48 dp
        // buttons by 8 dp at each end and keeps its 56 dp play button 8 dp away. Collapsed, the
        // play button grows to 80 dp. ControlBar adds 6 dp all round for the shadows.
        const val EDGE_DP = 6f
        const val BUTTON_DP = 48f
        const val TOOLBAR_PADDING_DP = 8f
        const val FAB_GAP_DP = 8f
        const val FAB_DP = 56f
        const val COLLAPSED_FAB_DP = 80f

        /** Thickness of the controls, and the side of the square they shrink to while running. */
        const val COLLAPSED_DP = COLLAPSED_FAB_DP + 2 * EDGE_DP

        /** Space kept free at each end of the screen when deciding whether the toolbar fits. */
        const val SCREEN_MARGIN_DP = 8f
        const val MIN_SCALE = 0.6f

        /** Buttons on the expanded toolbar besides the play button. */
        fun buttonCount(mode: OverlayMode): Int = toolbarItems(mode).size

        /** Length of the expanded controls along the toolbar, in dp at scale 1. */
        fun lengthDp(mode: OverlayMode): Float =
            2 * EDGE_DP + FAB_DP + FAB_GAP_DP + 2 * TOOLBAR_PADDING_DP + buttonCount(mode) * BUTTON_DP

        fun choose(screenWidthDp: Float, screenHeightDp: Float, mode: OverlayMode, size: ControlSize): ControlsLayout {
            val length = lengthDp(mode)
            val preferred = size.scale
            val roomDown = screenHeightDp - 2 * SCREEN_MARGIN_DP
            val roomAcross = screenWidthDp - 2 * SCREEN_MARGIN_DP
            return when {
                length * preferred <= roomDown -> ControlsLayout(vertical = true, scale = preferred)
                length * preferred <= roomAcross -> ControlsLayout(vertical = false, scale = preferred)
                else -> {
                    val vertical = roomDown >= roomAcross
                    val room = if (vertical) roomDown else roomAcross
                    ControlsLayout(vertical, scale = (room / length).coerceIn(MIN_SCALE, preferred))
                }
            }
        }
    }
}

/** The floating toolbar's buttons besides the play button. */
internal enum class ToolbarItem { AddTap, AddSwipe, RemoveLast, Settings, Sequences, Minimize, Close }

/**
 * The saved-sequences button is kept out of the toolbar, which offers [ToolbarItem.Minimize] in
 * its place. Its button, help entry, dialog and action all remain; set this to bring them back.
 */
internal const val SHOW_SEQUENCES_BUTTON = false

/** The buttons of the expanded toolbar, in order. Minimized, sequences keep only Minimize. */
internal fun toolbarItems(mode: OverlayMode): List<ToolbarItem> = when (mode) {
    OverlayMode.Single -> listOf(ToolbarItem.Settings, ToolbarItem.Close)
    OverlayMode.Multi -> buildList {
        add(ToolbarItem.AddTap)
        add(ToolbarItem.AddSwipe)
        add(ToolbarItem.RemoveLast)
        add(ToolbarItem.Settings)
        if (SHOW_SEQUENCES_BUTTON) add(ToolbarItem.Sequences)
        add(ToolbarItem.Minimize)
        add(ToolbarItem.Close)
    }
}

/** How much each control size scales the floating controls. */
internal val ControlSize.scale: Float
    get() = when (this) {
        ControlSize.Compact -> 0.86f
        ControlSize.Regular -> 1f
        ControlSize.Large -> 1.16f
    }
