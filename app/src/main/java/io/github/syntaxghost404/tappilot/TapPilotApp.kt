package io.github.syntaxghost404.tappilot

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import io.github.syntaxghost404.tappilot.core.data.BackupManager
import io.github.syntaxghost404.tappilot.core.data.ScriptLibrary
import io.github.syntaxghost404.tappilot.core.data.ScriptLibrarySerializer
import io.github.syntaxghost404.tappilot.core.data.ScriptRepository
import io.github.syntaxghost404.tappilot.core.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class TapPilotApp : Application() {
    val graph: AppGraph by lazy { AppGraph(this) }
}

/** Process-wide singletons. The accessibility service and the activity share one instance. */
class AppGraph(context: Context) {
    val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val scripts = ScriptRepository(
        DataStoreFactory.create(
            serializer = ScriptLibrarySerializer,
            corruptionHandler = ReplaceFileCorruptionHandler { ScriptLibrary() },
            scope = ioScope,
            produceFile = { context.dataStoreFile("scripts.json") },
        ),
    )

    val settings = SettingsRepository(
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            scope = ioScope,
            produceFile = { context.preferencesDataStoreFile("settings") },
        ),
        installId = context.firstInstallTime(),
    )

    val backups = BackupManager(context.applicationContext, scripts)
}

val Context.appGraph: AppGraph get() = (applicationContext as TapPilotApp).graph

/** When the app was installed on this phone. Updates keep it; a new installation gets a new one. */
private fun Context.firstInstallTime(): Long = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).firstInstallTime
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0).firstInstallTime
    }
}.getOrDefault(0L)
