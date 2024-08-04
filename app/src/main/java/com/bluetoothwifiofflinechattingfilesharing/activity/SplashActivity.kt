package com.bluetoothwifiofflinechattingfilesharing.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.bluetoothwifiofflinechattingfilesharing.databinding.ActivityFirstSpalshBinding

class SplashActivity : AppCompatActivity() {
    lateinit var binding: ActivityFirstSpalshBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFirstSpalshBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Handler().postDelayed(
            {
                val c = Intent(this, MainActivity::class.java)
                startActivity(c)
                finish()
            },
            2500
        )


    }
}