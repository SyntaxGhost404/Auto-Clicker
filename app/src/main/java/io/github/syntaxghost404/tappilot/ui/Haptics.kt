package io.github.syntaxghost404.tappilot.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Applies the user's haptic setting to [content]. Off, every vibration in it is silenced, including
 * Compose's own, such as on long press. On, haptics go through the system's touch feedback, so the
 * phone's own vibration settings still apply.
 */
@Composable
fun ProvideHaptics(enabled: Boolean, content: @Composable () -> Unit) {
    val haptics = if (enabled) LocalHapticFeedback.current else NoHaptics
    CompositionLocalProvider(LocalHapticFeedback provides haptics, content = content)
}

/** [onClick], led by a [type] haptic: a light click unless the press means more, like stopping a run. */
@Composable
fun withHaptic(type: HapticFeedbackType = HapticFeedbackType.ContextClick, onClick: () -> Unit): () -> Unit {
    val haptics = LocalHapticFeedback.current
    return {
        haptics.performHapticFeedback(type)
        onClick()
    }
}

private object NoHaptics : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = Unit
}
