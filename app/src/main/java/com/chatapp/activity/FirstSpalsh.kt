package com.chatapp.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import com.chatapp.R
import com.chatapp.databinding.ActivityFirstSpalshBinding
import com.chatapp.databinding.ActivityHomeBinding
import com.chatapp.fileshare.Server

class FirstSpalsh : AppCompatActivity() {
    lateinit var binding: ActivityFirstSpalshBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFirstSpalshBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Handler().postDelayed({
            val c = Intent(this, Splash::class.java)
            startActivity(c)
            finish()
        }, 2500)
    }
}