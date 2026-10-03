package io.github.syntaxghost404.tappilot.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.syntaxghost404.tappilot.core.model.Timing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode { System, Light, Dark }

enum class ControlSize { Compact, Regular, Large }

enum class OverlayMode { Single, Multi }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = true,
    val markerSizeDp: Int = DEFAULT_MARKER_DP,
    val controlSize: ControlSize = ControlSize.Regular,
    val tapFeedback: Boolean = true,
    val keepScreenOn: Boolean = true,
    val stopOnScreenOff: Boolean = true,
    val defaultDelayMs: Long = Timing.DEFAULT_DELAY_MS,
    val defaultHoldMs: Long = Timing.DEFAULT_HOLD_MS,
    val defaultSwipeMs: Long = Timing.DEFAULT_SWIPE_MS,
    val onboardingDone: Boolean = false,
    val lastMode: OverlayMode = OverlayMode.Single,
    val lastScriptId: String? = null,
    /** Last position of the floating controls, or -1 when never moved. */
    val controlsX: Int = -1,
    val controlsY: Int = -1,
) {
    companion object {
        const val MIN_MARKER_DP = 40
        const val MAX_MARKER_DP = 88
        const val DEFAULT_MARKER_DP = 56
    }
}

class SettingsRepository(private val store: DataStore<Preferences>) {
    val settings: Flow<AppSettings> = store.data.map { it.toSettings() }.distinctUntilChanged()

    suspend fun current(): AppSettings = settings.first()

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[THEME] = mode.name }
    suspend fun setDynamicColor(enabled: Boolean) = edit { it[DYNAMIC] = enabled }
    suspend fun setMarkerSize(dp: Int) =
        edit { it[MARKER] = dp.coerceIn(AppSettings.MIN_MARKER_DP, AppSettings.MAX_MARKER_DP) }
    suspend fun setControlSize(size: ControlSize) = edit { it[CONTROL_SIZE] = size.name }
    suspend fun setTapFeedback(enabled: Boolean) = edit { it[FEEDBACK] = enabled }
    suspend fun setKeepScreenOn(enabled: Boolean) = edit { it[KEEP_ON] = enabled }
    suspend fun setStopOnScreenOff(enabled: Boolean) = edit { it[STOP_SCREEN_OFF] = enabled }
    suspend fun setDefaultDelay(ms: Long) = edit { it[DEFAULT_DELAY] = ms.coerceIn(0L, Timing.MAX_DELAY_MS) }
    suspend fun setDefaultHold(ms: Long) =
        edit { it[DEFAULT_HOLD] = ms.coerceIn(Timing.MIN_HOLD_MS, Timing.MAX_GESTURE_MS) }
    suspend fun setDefaultSwipe(ms: Long) =
        edit { it[DEFAULT_SWIPE] = ms.coerceIn(Timing.MIN_SWIPE_MS, Timing.MAX_GESTURE_MS) }
    suspend fun setOnboardingDone(done: Boolean) = edit { it[ONBOARDED] = done }

    suspend fun rememberSession(mode: OverlayMode, scriptId: String?) = edit {
        it[LAST_MODE] = mode.name
        if (scriptId == null) it.remove(LAST_SCRIPT) else it[LAST_SCRIPT] = scriptId
    }

    suspend fun setControlsPosition(x: Int, y: Int) = edit {
        it[CONTROLS_X] = x
        it[CONTROLS_Y] = y
    }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        store.edit { block(it) }
    }

    private fun Preferences.toSettings() = AppSettings(
        themeMode = enumOr(this[THEME], ThemeMode.System),
        dynamicColor = this[DYNAMIC] ?: true,
        markerSizeDp = this[MARKER] ?: AppSettings.DEFAULT_MARKER_DP,
        controlSize = enumOr(this[CONTROL_SIZE], ControlSize.Regular),
        tapFeedback = this[FEEDBACK] ?: true,
        keepScreenOn = this[KEEP_ON] ?: true,
        stopOnScreenOff = this[STOP_SCREEN_OFF] ?: true,
        defaultDelayMs = this[DEFAULT_DELAY] ?: Timing.DEFAULT_DELAY_MS,
        defaultHoldMs = this[DEFAULT_HOLD] ?: Timing.DEFAULT_HOLD_MS,
        defaultSwipeMs = this[DEFAULT_SWIPE] ?: Timing.DEFAULT_SWIPE_MS,
        onboardingDone = this[ONBOARDED] ?: false,
        lastMode = enumOr(this[LAST_MODE], OverlayMode.Single),
        lastScriptId = this[LAST_SCRIPT],
        controlsX = this[CONTROLS_X] ?: -1,
        controlsY = this[CONTROLS_Y] ?: -1,
    )

    private inline fun <reified E : Enum<E>> enumOr(name: String?, fallback: E): E =
        name?.let { runCatching { enumValueOf<E>(it) }.getOrNull() } ?: fallback

    private companion object {
        val THEME = stringPreferencesKey("theme_mode")
        val DYNAMIC = booleanPreferencesKey("dynamic_color")
        val MARKER = intPreferencesKey("marker_size_dp")
        val CONTROL_SIZE = stringPreferencesKey("control_size")
        val FEEDBACK = booleanPreferencesKey("tap_feedback")
        val KEEP_ON = booleanPreferencesKey("keep_screen_on")
        val STOP_SCREEN_OFF = booleanPreferencesKey("stop_on_screen_off")
        val DEFAULT_DELAY = longPreferencesKey("default_delay_ms")
        val DEFAULT_HOLD = longPreferencesKey("default_hold_ms")
        val DEFAULT_SWIPE = longPreferencesKey("default_swipe_ms")
        val ONBOARDED = booleanPreferencesKey("onboarding_done")
        val LAST_MODE = stringPreferencesKey("last_mode")
        val LAST_SCRIPT = stringPreferencesKey("last_script_id")
        val CONTROLS_X = intPreferencesKey("controls_x")
        val CONTROLS_Y = intPreferencesKey("controls_y")
    }
}
