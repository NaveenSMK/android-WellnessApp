package com.example.phonebreakguardian

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.phonebreakguardian.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Whatever the user chooses, just refresh the screen to reflect it.
            refreshStatus()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.usageButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        b.overlayButton.setOnClickListener {
            // Repurposed: grants the full-screen alert permission (Android 14+)
            // used to show the break screen reliably. Not needed on older versions.
            if (Build.VERSION.SDK_INT >= 34) {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        }

        b.startButton.setOnClickListener {
            if (Prefs.isMonitoringEnabled(this)) {
                stopMonitoring()
            } else {
                startMonitoringIfReady()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun startMonitoringIfReady() {
        if (!hasUsageAccess()) {
            b.status.text = getString(R.string.need_usage_access)
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            return
        }

        // Ask for notification permission (Android 13+) before starting, since
        // without it the ongoing + break-alert notifications won't be shown.
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            return
        }

        val i = Intent(this, UsageMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
        Prefs.setMonitoringEnabled(this, true)
        refreshStatus()
    }

    private fun stopMonitoring() {
        stopService(Intent(this, UsageMonitorService::class.java))
        Prefs.setMonitoringEnabled(this, false)
        refreshStatus()
    }

    private fun refreshStatus() {
        val monitoring = Prefs.isMonitoringEnabled(this)
        b.startButton.text =
            if (monitoring) getString(R.string.stop_monitoring) else getString(R.string.start_monitoring)

        b.status.text = when {
            monitoring -> getString(R.string.monitoring)
            !hasUsageAccess() -> getString(R.string.need_usage_access)
            Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED -> getString(R.string.need_notifications)
            else -> ""
        }
    }

    private fun hasUsageAccess(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
