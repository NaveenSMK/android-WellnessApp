package com.example.phonebreakguardian

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.*
import android.hardware.*
import android.os.*
import androidx.core.app.NotificationCompat
import java.util.*

class UsageMonitorService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var lastSample = 0L
    private var usedMs = 0L
    private var lastPrompt = 0L
    private var darkWarningShown = false
    private var sensor: Sensor? = null
    private var lightValue = Float.MAX_VALUE

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
        createChannel()
        startForeground(1001, NotificationCompat.Builder(this, "guardian")
            .setContentTitle("Phone Break Guardian")
            .setContentText("Monitoring phone use")
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
        val i = Intent(this, BreakActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra("night", night)
        startActivity(i)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val c = NotificationChannel("guardian", "Phone Break Guardian",
                NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(c)
        }
    }

    override fun onBind(intent: Intent?) = null
    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        (getSystemService(SENSOR_SERVICE) as SensorManager).unregisterListener(sensorListener)
        super.onDestroy()
    }
}
