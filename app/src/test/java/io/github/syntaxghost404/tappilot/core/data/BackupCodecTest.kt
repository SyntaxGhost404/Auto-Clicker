package io.github.syntaxghost404.tappilot.core.data

import io.github.syntaxghost404.tappilot.core.model.Script
import io.github.syntaxghost404.tappilot.core.model.StopMode
import io.github.syntaxghost404.tappilot.core.model.StopRule
import io.github.syntaxghost404.tappilot.core.model.SwipeStep
import io.github.syntaxghost404.tappilot.core.model.TapStep
import io.github.syntaxghost404.tappilot.core.model.Timing
import io.github.syntaxghost404.tappilot.core.model.Variation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {
    private val original = Script(
        id = "original-id",
        name = "Daily reward",
        steps = listOf(
            TapStep(id = "a", x = 120f, y = 340f, holdMs = 20L, delayMs = 500L),
            SwipeStep(id = "b", startX = 100f, startY = 900f, endX = 900f, endY = 900f, durationMs = 400L, delayMs = 1_000L),
        ),
        stopRule = StopRule(mode = StopMode.Cycles, cycles = 25),
        variation = Variation(position = true, timingPercent = 15),
    )

    @Test
    fun `round trips scripts with fresh ids`() {
        val text = BackupCodec.encode(listOf(original), exportedAt = 1L)
        var counter = 0
        val result = BackupCodec.decode(text, now = 99L) { "new-${counter++}" }

        assertTrue(result is ImportResult.Success)
        val imported = (result as ImportResult.Success).scripts.single()
        assertNotEquals(original.id, imported.id)
        assertEquals(original.name, imported.name)
        assertEquals(original.stopRule, imported.stopRule)
        assertEquals(original.variation, imported.variation)
        assertEquals(original.steps.map { it.withId("") }, imported.steps.map { it.withId("") })
        assertEquals(99L, imported.createdAt)
    }

    @Test
    fun `rejects files that are not backups`() {
        assertEquals(ImportResult.Failure(ImportResult.Reason.NotABackup), BackupCodec.decode("hello", now = 0L))
        assertEquals(
            ImportResult.Failure(ImportResult.Reason.NotABackup),
            BackupCodec.decode("""{"format":"something-else","version":1,"scripts":[]}""", now = 0L),
        )
    }

    @Test
    fun `rejects backups from a newer version`() {
        val text = """{"format":"${ScriptBackup.FORMAT}","version":${ScriptBackup.VERSION + 1},"scripts":[]}"""
        assertEquals(ImportResult.Failure(ImportResult.Reason.UnsupportedVersion), BackupCodec.decode(text, now = 0L))
    }

    @Test
    fun `reports an empty backup`() {
        val text = BackupCodec.encode(emptyList(), exportedAt = 0L)
        assertEquals(ImportResult.Failure(ImportResult.Reason.Empty), BackupCodec.decode(text, now = 0L))
    }

    @Test
    fun `sanitizes out of range values`() {
        val text = """
            {"format":"${ScriptBackup.FORMAT}","version":1,"scripts":[{
              "id":"x","name":"   ",
              "steps":[
                {"type":"tap","id":"t","x":-50,"y":20,"holdMs":0,"delayMs":-5},
                {"type":"swipe","id":"s","startX":1,"startY":2,"endX":3,"endY":4,"durationMs":999999999,"delayMs":10}
              ],
              "stopRule":{"mode":"Cycles","cycles":-3},
              "variation":{"position":false,"timingPercent":400}
            }]}
        """.trimIndent()

        val script = (BackupCodec.decode(text, now = 0L) as ImportResult.Success).scripts.single()
        val tap = script.steps[0] as TapStep
        val swipe = script.steps[1] as SwipeStep

        assertEquals("Imported sequence", script.name)
        assertEquals(0f, tap.x)
        assertEquals(Timing.MIN_HOLD_MS, tap.holdMs)
        assertEquals(0L, tap.delayMs)
        assertEquals(Timing.MAX_GESTURE_MS, swipe.durationMs)
        assertEquals(1, script.stopRule.cycles)
        assertEquals(Timing.MAX_TIMING_VARIATION, script.variation.timingPercent)
    }

    @Test
    fun `ignores unknown fields from future versions`() {
        val text = """
            {"format":"${ScriptBackup.FORMAT}","version":1,"futureField":true,"scripts":[{
              "name":"Future","colour":"teal",
              "steps":[{"type":"tap","x":5,"y":6,"pressure":0.4}]
            }]}
        """.trimIndent()
        val script = (BackupCodec.decode(text, now = 0L) as ImportResult.Success).scripts.single()
        assertEquals("Future", script.name)
        assertEquals(1, script.steps.size)
    }
}
