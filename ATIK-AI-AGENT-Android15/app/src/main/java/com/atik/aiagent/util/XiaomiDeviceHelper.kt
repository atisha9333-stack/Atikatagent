package com.atik.aiagent.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * Helper for Redmi Turbo 3 (HyperOS / Android 15, China ROM) permissions & battery guidance.
 * Uses only public Android intents with safe fallbacks.
 */
object XiaomiDeviceHelper {

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun openNotificationListenerSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openAppDetailsSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openBatteryOptimizationSettings(context: Context) {
        val directIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (runCatching { context.startActivity(directIntent) }.isFailure) {
            openAppDetailsSettings(context)
        }
    }

    fun openXiaomiAutostartSettings(context: Context) {
        // Public Xiaomi Security Center Autostart activity if present on HyperOS/MIUI, falls back to App Details
        val xiaomiIntent = Intent().apply {
            setClassName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (runCatching { context.startActivity(xiaomiIntent) }.isFailure) {
            openAppDetailsSettings(context)
        }
    }

    fun deviceSummary(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL} • Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
    }
}
