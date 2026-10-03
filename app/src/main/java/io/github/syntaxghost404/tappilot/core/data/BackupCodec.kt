package io.github.syntaxghost404.tappilot.core.data

import io.github.syntaxghost404.tappilot.core.model.Script
import io.github.syntaxghost404.tappilot.core.model.Step
import io.github.syntaxghost404.tappilot.core.model.SwipeStep
import io.github.syntaxghost404.tappilot.core.model.TapStep
import io.github.syntaxghost404.tappilot.core.model.Timing
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable

/** File format used to move scripts between devices. */
@Serializable
data class ScriptBackup(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val exportedAt: Long = 0L,
    val scripts: List<Script> = emptyList(),
) {
    companion object {
        const val FORMAT = "tap-pilot.scripts"
        const val VERSION = 1
    }
}

sealed interface ImportResult {
    data class Success(val scripts: List<Script>) : ImportResult
    data class Failure(val reason: Reason) : ImportResult

    enum class Reason { NotABackup, UnsupportedVersion, Empty, Unreadable }
}

object BackupCodec {
    fun encode(scripts: List<Script>, exportedAt: Long): String =
        LibraryJson.encodeToString(ScriptBackup.serializer(), ScriptBackup(exportedAt = exportedAt, scripts = scripts))

    /**
     * Parses a backup and sanitizes every value so a hand-edited or corrupted file can never
     * produce gestures the platform would reject. Imported scripts get fresh ids so they never
     * overwrite existing ones.
     */
    fun decode(text: String, now: Long, newId: () -> String = Script::newId): ImportResult {
        val backup = try {
            LibraryJson.decodeFromString(ScriptBackup.serializer(), text)
        } catch (e: SerializationException) {
            return ImportResult.Failure(ImportResult.Reason.NotABackup)
        } catch (e: IllegalArgumentException) {
            return ImportResult.Failure(ImportResult.Reason.NotABackup)
        }
        if (backup.format != ScriptBackup.FORMAT) return ImportResult.Failure(ImportResult.Reason.NotABackup)
        if (backup.version > ScriptBackup.VERSION) return ImportResult.Failure(ImportResult.Reason.UnsupportedVersion)

        val scripts = backup.scripts
            .filter { it.id != Script.QUICK_ID }
            .map { script ->
                script.copy(
                    id = newId(),
                    name = script.name.trim().take(MAX_NAME_LENGTH).ifEmpty { "Imported sequence" },
                    steps = script.steps.mapNotNull { sanitize(it, newId) },
                    stopRule = script.stopRule.normalized(),
                    variation = script.variation.normalized(),
                    createdAt = now,
                    updatedAt = now,
                    lastRunAt = 0L,
                )
            }
        return if (scripts.isEmpty()) ImportResult.Failure(ImportResult.Reason.Empty) else ImportResult.Success(scripts)
    }

    private fun sanitize(step: Step, newId: () -> String): Step? = when (step) {
        is TapStep -> if (!finite(step.x, step.y)) null else step.copy(
            id = newId(),
            x = step.x.coerceAtLeast(0f),
            y = step.y.coerceAtLeast(0f),
            holdMs = step.holdMs.coerceIn(Timing.MIN_HOLD_MS, Timing.MAX_GESTURE_MS),
            delayMs = step.delayMs.coerceIn(0L, Timing.MAX_DELAY_MS),
        )
        is SwipeStep -> if (!finite(step.startX, step.startY, step.endX, step.endY)) null else step.copy(
            id = newId(),
            startX = step.startX.coerceAtLeast(0f),
            startY = step.startY.coerceAtLeast(0f),
            endX = step.endX.coerceAtLeast(0f),
            endY = step.endY.coerceAtLeast(0f),
            durationMs = step.durationMs.coerceIn(Timing.MIN_SWIPE_MS, Timing.MAX_GESTURE_MS),
            delayMs = step.delayMs.coerceIn(0L, Timing.MAX_DELAY_MS),
        )
    }

    private fun finite(vararg values: Float) = values.all { it.isFinite() }

    const val MAX_NAME_LENGTH = 60
}
