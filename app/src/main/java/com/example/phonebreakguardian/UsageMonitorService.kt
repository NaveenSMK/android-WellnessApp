package com.example.phonebreakguardian

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.*
import android.hardware.*
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.*

class UsageMonitorService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var lastSample = 0L
    private var usedMs = 0L
    private var lastPrompt = 0L
    private var darkWarningShown = false
    private var sensor: Sensor? = null
    private var lightValue = Float.MAX_VALUE

    companion object {
        private const val ONGOING_CHANNEL = "guardian"
        private const val ALERT_CHANNEL = "guardian_alerts"
        private const val ONGOING_NOTIFICATION_ID = 1001
        private const val ALERT_NOTIFICATION_ID = 1002
    }

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(e: SensorEvent) { lightValue = e.values[0] }
        override fun onAccuracyChanged(s: Sensor?, a: Int) {}
    }

    private val tick = object : Runnable {
        override fun run() {
            sampleUsage()
            handler.postDelayed(this, 15_000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannels()
        startForeground(ONGOING_NOTIFICATION_ID, NotificationCompat.Builder(this, ONGOING_CHANNEL)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.monitoring_notification))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true).build())

        val sm = getSystemService(SENSOR_SERVICE) as SensorManager
        sensor = sm.getDefaultSensor(Sensor.TYPE_LIGHT)
        sensor?.let { sm.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_NORMAL) }
        lastSample = System.currentTimeMillis()
        handler.post(tick)
    }

    private fun sampleUsage() {
        val now = System.currentTimeMillis()
        val delta = (now - lastSample).coerceAtMost(30_000L)
        lastSample = now

        val usm = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 20_000, now)
        val active = stats.maxByOrNull { it.lastTimeUsed }?.packageName ?: ""
        if (active != packageName && active.isNotBlank()) {
            usedMs += delta
        }

        val thirtyMinutes = 30 * 60 * 1000L
        if (usedMs >= thirtyMinutes && now - lastPrompt > thirtyMinutes) {
            lastPrompt = now
            usedMs = 0
            launchBreak(false)
        }

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val night = hour >= 22 || hour < 6
        if (night && lightValue < 10f && !darkWarningShown) {
            darkWarningShown = true
            launchBreak(true)
        }
        if (!night || lightValue >= 20f) darkWarningShown = false
    }

    private fun launchBreak(night: Boolean) {
        // Launching an Activity straight from a background service is blocked
        // on modern Android. Instead, post a high-priority notification with a
        // full-screen intent: the OS-recommended pattern for alarm/call-style
        // alerts, which reliably opens BreakActivity even if the permission
        // isn't granted (it just falls back to a tappable heads-up notification).
        val contentIntent = Intent(this, BreakActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra("night", night)

        val pendingIntent = PendingIntent.getActivity(
            this, if (night) 1 else 0, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (night) getString(R.string.alert_title_dark) else getString(R.string.alert_title_break)

        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL)
            .setContentTitle(title)
            .setContentText(getString(R.string.alert_body))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // NotificationManagerCompat.notify() silently no-ops if POST_NOTIFICATIONS
        // isn't granted (Android 13+) rather than crashing, so this is always safe.
        NotificationManagerCompat.from(this).notify(ALERT_NOTIFICATION_ID, notification)
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(ONGOING_CHANNEL, "Phone Break Guardian", NotificationManager.IMPORTANCE_LOW)
            )
            nm.createNotificationChannel(
                NotificationChannel(ALERT_CHANNEL, "Break Alerts", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    override fun onBind(intent: Intent?) = null
    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        (getSystemService(SENSOR_SERVICE) as SensorManager).unregisterListener(sensorListener)
        super.onDestroy()
    }
}
