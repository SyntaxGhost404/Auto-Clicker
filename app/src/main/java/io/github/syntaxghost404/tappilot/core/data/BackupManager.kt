package io.github.syntaxghost404.tappilot.core.data

import android.content.Context
import android.net.Uri
import io.github.syntaxghost404.tappilot.core.model.Script
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** Reads and writes backup files chosen through the system file picker. */
class BackupManager(private val context: Context, private val scripts: ScriptRepository) {

    /** Writes [selection] (or every sequence) to [uri]. Returns how many were written. */
    suspend fun export(uri: Uri, selection: List<Script>? = null): Result<Int> = withContext(Dispatchers.IO) {
        val list = selection ?: scripts.all()
        if (list.isEmpty()) return@withContext Result.failure(NothingToExport())
        try {
            val stream = context.contentResolver.openOutputStream(uri, "wt")
                ?: return@withContext Result.failure(IOException("No output stream"))
            stream.use { it.write(BackupCodec.encode(list, System.currentTimeMillis()).encodeToByteArray()) }
            Result.success(list.size)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: SecurityException) {
            Result.failure(e)
        }
    }

    suspend fun import(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val text = try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bytes = input.readNBytesCompat(MAX_BACKUP_BYTES + 1)
                if (bytes.size > MAX_BACKUP_BYTES) null else bytes.decodeToString()
            }
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        } ?: return@withContext ImportResult.Failure(ImportResult.Reason.Unreadable)

        val result = BackupCodec.decode(text, System.currentTimeMillis())
        if (result is ImportResult.Success) scripts.addAll(result.scripts)
        result
    }

    class NothingToExport : Exception()

    private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
        val buffer = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(8 * 1024)
        while (buffer.size() < limit) {
            val read = read(chunk, 0, minOf(chunk.size, limit - buffer.size()))
            if (read < 0) break
            buffer.write(chunk, 0, read)
        }
        return buffer.toByteArray()
    }

    private companion object {
        /** Backups are tiny; anything larger is not one of ours. */
        const val MAX_BACKUP_BYTES = 4 * 1024 * 1024
    }
}
