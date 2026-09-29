package com.example.phonebreakguardian

import android.app.*
import android.os.*
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.phonebreakguardian.databinding.ActivityBreakBinding
import kotlin.random.Random

class BreakActivity : AppCompatActivity() {
    private lateinit var b: ActivityBreakBinding
    private var expected = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        b = ActivityBreakBinding.inflate(layoutInflater)
        setContentView(b.root)

        val night = intent.getBooleanExtra("night", false)
        b.warning.text = if (night) {
            "🌙 Please turn on a light\n\nUsing a phone in a dark environment may cause eye strain. Take regular breaks.\n\n🌙 விளக்கை எரிய வைக்கவும்\n\nஇருட்டான சூழலில் கைபேசியைப் பயன்படுத்துவது கண்களுக்கு சிரமத்தை ஏற்படுத்தலாம். போதுமான வெளிச்சத்தில் பயன்படுத்தி, இடையிடையே ஓய்வு எடுக்கவும்."
        } else {
            "🛑 Time for a break\n\nYou have used your phone for 30 minutes. Please rest your eyes for a few minutes.\n\n🛑 ஓய்வு நேரம்\n\n30 நிமிடங்களாக கைபேசியைப் பயன்படுத்துகிறீர்கள். சில நிமிடங்கள் கண்களுக்கு ஓய்வு கொடுக்கவும்."
        }
        newQuestion()
        b.submit.setOnClickListener {
            val answer = b.answer.text.toString().toIntOrNull()
            if (answer == expected) {
                finish()
            } else {
                b.result.text = "Try again / மீண்டும் முயற்சிக்கவும்"
                newQuestion()
            }
        }
    }

    private fun newQuestion() {
        val a = Random.nextInt(2, 13)
        val c = Random.nextInt(2, 13)
        if (Random.nextBoolean()) {
            expected = a + c
            b.question.text = "$a + $c = ?"
        } else {
            expected = a * c
            b.question.text = "$a × $c = ?"
        }
        b.answer.text.clear()
    }
}
