package io.github.syntaxghost404.tappilot.service

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import io.github.syntaxghost404.tappilot.MainActivity
import io.github.syntaxghost404.tappilot.R
import io.github.syntaxghost404.tappilot.appGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Quick Settings tile that shows or hides the floating controls from anywhere. */
class ControlsTileService : TileService() {
    private var listening: CoroutineScope? = null

    override fun onStartListening() {
        super.onStartListening()
        listening?.cancel()
        listening = MainScope().also { scope ->
            scope.launch {
                combine(TapPilotRuntime.serviceConnected, TapPilotRuntime.status) { connected, status -> connected to status }
                    .collect { (connected, status) -> render(connected, status) }
            }
        }
    }

    override fun onStopListening() {
        listening?.cancel()
        listening = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        if (TapPilotRuntime.status.value is OverlayStatus.Visible) {
            TapPilotRuntime.closeControls()
            return
        }
        if (!TapPilotRuntime.isServiceConnected) {
            openApp()
            return
        }
        val open = {
            MainScope().launch {
                val settings = appGraph.settings.current()
                TapPilotRuntime.openControls(settings.lastMode, settings.lastScriptId)
                TapPilotRuntime.collapseShade()
            }
        }
        if (isLocked) unlockAndRun { open() } else open()
    }

    private fun render(connected: Boolean, status: OverlayStatus) {
        val tile = qsTile ?: return
        tile.label = getString(R.string.tile_label)
        tile.state = when {
            !connected -> Tile.STATE_INACTIVE
            status is OverlayStatus.Visible -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when {
                !connected -> getString(R.string.tile_off)
                status is OverlayStatus.Visible && status.running -> getString(R.string.tile_running)
                status is OverlayStatus.Visible -> status.scriptName.ifBlank { getString(R.string.shortcut_single_short) }
                else -> getString(R.string.tile_ready)
            }
        }
        tile.updateTile()
    }

    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    companion object {
        fun requestRefresh(context: Context) {
            runCatching {
                requestListeningState(context, ComponentName(context, ControlsTileService::class.java))
            }
        }
    }
}
