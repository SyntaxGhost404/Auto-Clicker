package io.github.syntaxghost404.tappilot

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.github.syntaxghost404.tappilot.core.data.OverlayMode
import io.github.syntaxghost404.tappilot.ui.TapPilotRoot

class MainActivity : ComponentActivity() {
    private var pendingStart by mutableStateOf<OverlayMode?>(null)
    private var ready = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        splash.setKeepOnScreenCondition { !ready }
        if (savedInstanceState == null) handleIntent(intent)
        publishShortcuts()
        setContent {
            TapPilotRoot(
                pendingStart = pendingStart,
                onPendingStartHandled = { pendingStart = null },
                onReady = { ready = true },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        pendingStart = when (intent?.action) {
            ACTION_START_SINGLE -> OverlayMode.Single
            ACTION_START_MULTI -> OverlayMode.Multi
            else -> null
        }
    }

    private fun publishShortcuts() {
        fun shortcut(id: String, short: Int, long: Int, icon: Int, action: String) =
            ShortcutInfoCompat.Builder(this, id)
                .setShortLabel(getString(short))
                .setLongLabel(getString(long))
                .setIcon(IconCompat.createWithResource(this, icon))
                .setIntent(Intent(this, MainActivity::class.java).setAction(action))
                .build()
        runCatching {
            ShortcutManagerCompat.setDynamicShortcuts(
                this,
                listOf(
                    shortcut("single", R.string.shortcut_single_short, R.string.shortcut_single_long, R.drawable.ic_shortcut_single, ACTION_START_SINGLE),
                    shortcut("multi", R.string.shortcut_multi_short, R.string.shortcut_multi_long, R.drawable.ic_shortcut_multi, ACTION_START_MULTI),
                ),
            )
        }
    }

    companion object {
        const val ACTION_START_SINGLE = "io.github.syntaxghost404.tappilot.action.START_SINGLE"
        const val ACTION_START_MULTI = "io.github.syntaxghost404.tappilot.action.START_MULTI"
    }
}
