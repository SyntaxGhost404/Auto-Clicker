package io.github.syntaxghost404.tappilot.core.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import io.github.syntaxghost404.tappilot.core.model.Script
import io.github.syntaxghost404.tappilot.core.model.StopRule
import io.github.syntaxghost404.tappilot.core.model.TapStep
import io.github.syntaxghost404.tappilot.core.model.Timing
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

/** Everything the user has saved: their sequences plus the single-point setup. */
@Serializable
data class ScriptLibrary(
    val scripts: List<Script> = emptyList(),
    val quick: Script = defaultQuickScript(),
)

/** The single-point setup is stored as a one-step script so it shares the engine. */
fun defaultQuickScript(): Script = Script(
    id = Script.QUICK_ID,
    name = "Single point",
    steps = listOf(TapStep(id = "quick-target", x = -1f, y = -1f, delayMs = Timing.DEFAULT_INTERVAL_MS)),
    stopRule = StopRule(),
)

internal val LibraryJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
    classDiscriminator = "type"
}

internal object ScriptLibrarySerializer : Serializer<ScriptLibrary> {
    override val defaultValue: ScriptLibrary = ScriptLibrary()

    override suspend fun readFrom(input: InputStream): ScriptLibrary = try {
        LibraryJson.decodeFromString(ScriptLibrary.serializer(), input.readBytes().decodeToString())
    } catch (e: SerializationException) {
        throw CorruptionException("Saved scripts could not be read", e)
    } catch (e: IllegalArgumentException) {
        throw CorruptionException("Saved scripts could not be read", e)
    }

    override suspend fun writeTo(t: ScriptLibrary, output: OutputStream) {
        output.write(LibraryJson.encodeToString(ScriptLibrary.serializer(), t).encodeToByteArray())
    }
}
