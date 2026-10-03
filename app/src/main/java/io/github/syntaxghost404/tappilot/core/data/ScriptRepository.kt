package io.github.syntaxghost404.tappilot.core.data

import androidx.datastore.core.DataStore
import io.github.syntaxghost404.tappilot.core.model.Script
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ScriptRepository(
    private val store: DataStore<ScriptLibrary>,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    /** Saved sequences, most recently used or edited first. */
    val scripts: Flow<List<Script>> = store.data
        .map { library -> library.scripts.sortedByDescending { maxOf(it.updatedAt, it.lastRunAt) } }
        .distinctUntilChanged()

    val quick: Flow<Script> = store.data.map { it.quick }.distinctUntilChanged()

    fun script(id: String): Flow<Script?> = store.data
        .map { library -> if (id == Script.QUICK_ID) library.quick else library.scripts.firstOrNull { it.id == id } }
        .distinctUntilChanged()

    suspend fun get(id: String): Script? = script(id).first()

    /** Inserts or replaces [script], stamping it as edited now. Returns the stored version. */
    suspend fun save(script: Script): Script {
        val now = clock()
        var stored = script
        store.updateData { library ->
            if (script.id == Script.QUICK_ID) {
                stored = script.copy(updatedAt = now)
                library.copy(quick = stored)
            } else {
                val index = library.scripts.indexOfFirst { it.id == script.id }
                stored = script.copy(
                    createdAt = if (script.createdAt == 0L) now else script.createdAt,
                    updatedAt = now,
                )
                val updated = library.scripts.toMutableList()
                if (index >= 0) updated[index] = stored else updated += stored
                library.copy(scripts = updated)
            }
        }
        return stored
    }

    suspend fun rename(id: String, name: String) = edit(id) { it.copy(name = name.trim().take(BackupCodec.MAX_NAME_LENGTH)) }

    suspend fun markRun(id: String) {
        val now = clock()
        store.updateData { library ->
            if (id == Script.QUICK_ID) {
                library.copy(quick = library.quick.copy(lastRunAt = now))
            } else {
                library.copy(scripts = library.scripts.map { if (it.id == id) it.copy(lastRunAt = now) else it })
            }
        }
    }

    suspend fun duplicate(id: String, copySuffix: String): Script? {
        val original = get(id) ?: return null
        val now = clock()
        val copy = original.copy(
            id = Script.newId(),
            name = "${original.name} $copySuffix".take(BackupCodec.MAX_NAME_LENGTH),
            steps = original.steps.map { step -> step.withId(Script.newId()) },
            createdAt = now,
            updatedAt = now,
            lastRunAt = 0L,
        )
        store.updateData { it.copy(scripts = it.scripts + copy) }
        return copy
    }

    /** Removes a script and returns it so the caller can offer an undo. */
    suspend fun delete(id: String): Script? {
        var removed: Script? = null
        store.updateData { library ->
            removed = library.scripts.firstOrNull { it.id == id }
            library.copy(scripts = library.scripts.filterNot { it.id == id })
        }
        return removed
    }

    suspend fun restore(script: Script) {
        store.updateData { library ->
            if (library.scripts.any { it.id == script.id }) library else library.copy(scripts = library.scripts + script)
        }
    }

    suspend fun addAll(imported: List<Script>) {
        store.updateData { library -> library.copy(scripts = library.scripts + imported) }
    }

    suspend fun all(): List<Script> = scripts.first()

    /** The first unused name of the form "<base> N". */
    suspend fun nextName(base: String): String {
        val taken = store.data.first().scripts.map { it.name }.toSet()
        var n = taken.size + 1
        while ("$base $n" in taken) n++
        return "$base $n"
    }

    /** Applies [transform] to the stored script atomically, so rapid edits never overwrite each other. */
    suspend fun edit(id: String, transform: (Script) -> Script) {
        val now = clock()
        store.updateData { library ->
            if (id == Script.QUICK_ID) {
                library.copy(quick = transform(library.quick).copy(updatedAt = now))
            } else {
                library.copy(scripts = library.scripts.map { if (it.id == id) transform(it).copy(updatedAt = now) else it })
            }
        }
    }
}
