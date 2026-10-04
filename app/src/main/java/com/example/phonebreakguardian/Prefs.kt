package com.example.phonebreakguardian

import android.content.Context

/**
 * Small wrapper around SharedPreferences (on-device storage) so the app
 * remembers whether the user turned monitoring on, across app restarts
 * and device reboots. Nothing here ever leaves the device.
 */
object Prefs {
    private const val FILE = "guardian_prefs"
    private const val KEY_MONITORING_ENABLED = "monitoring_enabled"

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun setMonitoringEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_MONITORING_ENABLED, enabled).apply()
    }

    fun isMonitoringEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_MONITORING_ENABLED, false)
}
