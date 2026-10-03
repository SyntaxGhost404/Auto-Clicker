package io.github.syntaxghost404.tappilot.ui.system

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import io.github.syntaxghost404.tappilot.service.TapPilotAccessibilityService

/** Shortcuts into system settings pages, each with a safe fallback. */
object SystemScreens {
    fun openAccessibilitySettings(context: Context) {
        val component = ComponentName(context, TapPilotAccessibilityService::class.java).flattenToString()
        // The fragment-args extras ask Settings to scroll to and highlight our entry where supported.
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .putExtra(EXTRA_FRAGMENT_ARG_KEY, component)
            .putExtra(EXTRA_SHOW_FRAGMENT_ARGS, Bundle().apply { putString(EXTRA_FRAGMENT_ARG_KEY, component) })
        launch(context, intent) || launch(context, Intent(Settings.ACTION_SETTINGS))
    }

    fun openAppInfo(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        launch(context, intent) || launch(context, Intent(Settings.ACTION_SETTINGS))
    }

    fun openBatterySettings(context: Context) {
        if (!launch(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))) openAppInfo(context)
    }

    fun isBatteryOptimized(context: Context): Boolean {
        val power = context.getSystemService(PowerManager::class.java) ?: return false
        return !power.isIgnoringBatteryOptimizations(context.packageName)
    }

    private fun launch(context: Context, intent: Intent): Boolean = try {
        if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }

    private const val EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key"
    private const val EXTRA_SHOW_FRAGMENT_ARGS = ":settings:show_fragment_args"
}
