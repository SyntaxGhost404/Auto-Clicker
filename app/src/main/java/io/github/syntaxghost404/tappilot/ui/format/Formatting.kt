package io.github.syntaxghost404.tappilot.ui.format

import android.content.res.Resources
import androidx.annotation.StringRes
import io.github.syntaxghost404.tappilot.R
import io.github.syntaxghost404.tappilot.core.model.Script
import io.github.syntaxghost404.tappilot.core.model.StopMode
import io.github.syntaxghost404.tappilot.core.model.StopRule
import java.text.NumberFormat

/** Units offered next to duration fields. The number the user typed keeps its meaning per unit. */
enum class TimeUnitChoice(val millis: Long, @StringRes val label: Int) {
    Milliseconds(1L, R.string.unit_ms),
    Seconds(1_000L, R.string.unit_s),
    Minutes(60_000L, R.string.unit_min);

    companion object {
        /** The largest unit that represents [ms] exactly, so "1500" stays in milliseconds. */
        fun bestFor(ms: Long, allowed: List<TimeUnitChoice> = entries): TimeUnitChoice =
            allowed.sortedByDescending { it.millis }.firstOrNull { ms >= it.millis && ms % it.millis == 0L }
                ?: allowed.minBy { it.millis }
    }
}

object Durations {
    private fun number(value: Double, maxFraction: Int = 1): String =
        NumberFormat.getNumberInstance().apply {
            maximumFractionDigits = maxFraction
            minimumFractionDigits = 0
        }.format(value)

    /** Short human form: "250 ms", "1.5 s", "2 min 30 s", "1 h 5 min". */
    fun format(res: Resources, ms: Long): String {
        val safe = ms.coerceAtLeast(0L)
        return when {
            safe < 1_000L -> res.getString(R.string.duration_ms, number(safe.toDouble(), 0))
            safe < 60_000L -> res.getString(R.string.duration_s, number(safe / 1_000.0, if (safe % 100L == 0L) 1 else 3))
            safe < 3_600_000L -> {
                val minutes = safe / 60_000L
                val seconds = (safe % 60_000L) / 1_000L
                if (seconds == 0L) {
                    res.getString(R.string.duration_min, number(minutes.toDouble(), 0))
                } else {
                    res.getString(R.string.duration_min_s, number(minutes.toDouble(), 0), number(seconds.toDouble(), 0))
                }
            }
            else -> {
                val hours = safe / 3_600_000L
                val minutes = (safe % 3_600_000L) / 60_000L
                if (minutes == 0L) {
                    res.getString(R.string.duration_h, number(hours.toDouble(), 0))
                } else {
                    res.getString(R.string.duration_h_min, number(hours.toDouble(), 0), number(minutes.toDouble(), 0))
                }
            }
        }
    }

    /** Stopwatch form used while running: "4:05" or "1:02:03". */
    fun clock(ms: Long): String {
        val totalSeconds = ms.coerceAtLeast(0L) / 1_000L
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
    }

    fun count(value: Long): String = NumberFormat.getIntegerInstance().format(value)
}

object Summaries {
    fun stopRule(res: Resources, rule: StopRule): String = when (rule.mode) {
        StopMode.Never -> res.getString(R.string.summary_forever)
        StopMode.Duration -> res.getString(R.string.summary_for, Durations.format(res, rule.durationMs))
        StopMode.Cycles -> res.getQuantityString(R.plurals.summary_cycles, rule.cycles, Durations.count(rule.cycles.toLong()))
    }

    fun interval(res: Resources, script: Script): String {
        val delay = script.steps.firstOrNull()?.delayMs ?: 0L
        return res.getString(R.string.summary_every, Durations.format(res, delay))
    }

    fun composition(res: Resources, script: Script): String {
        val parts = buildList {
            if (script.tapCount > 0) add(res.getQuantityString(R.plurals.tap_count, script.tapCount, script.tapCount))
            if (script.swipeCount > 0) add(res.getQuantityString(R.plurals.swipe_count, script.swipeCount, script.swipeCount))
        }
        return if (parts.isEmpty()) res.getQuantityString(R.plurals.step_count, 0, 0) else parts.joinToString(" · ")
    }
}
