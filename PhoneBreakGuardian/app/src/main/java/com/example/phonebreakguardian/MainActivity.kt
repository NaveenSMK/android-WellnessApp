package com.example.phonebreakguardian

import android.app.*
import android.content.*
import android.net.Uri
import android.os.*
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.example.phonebreakguardian.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.usageButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        b.overlayButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")))
        }
        b.startButton.setOnClickListener {
            val i = Intent(this, UsageMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
            b.status.text = getString(R.string.monitoring)
        }
    }
}
