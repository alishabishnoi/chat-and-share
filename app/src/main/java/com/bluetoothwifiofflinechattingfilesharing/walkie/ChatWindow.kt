package com.bluetoothwifiofflinechattingfilesharing.walkie

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bluetoothwifiofflinechattingfilesharing.databinding.ActivityChatWindowBinding
import java.io.IOException
import java.io.OutputStream

class ChatWindow : AppCompatActivity(), View.OnClickListener {
    lateinit var binding: ActivityChatWindowBinding

    private var micRecorder: MicRecorder? = null
    var outputStream: OutputStream? = null
    var t: Thread? = null

    companion object {
        private const val MESSAGE_READ = 1
        private const val isRecording = false
        private const val MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityChatWindowBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissionAudio

        binding.sendBtn.setOnClickListener(this)

        //val socket = SocketHandler.socket
        try {
            //outputStream = socket!!.getOutputStream()
            Log.e("OUTPUT_SOCKET", "SUCCESS")
            val intent=Intent(applicationContext, AudioStreamingService::class.java)
            startService(intent)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private val permissionAudio: Unit
        get() {
            if ((ContextCompat.checkSelfPermission(
                    this, Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED)

            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.RECORD_AUDIO
                    ), MY_PERMISSIONS_REQUEST_RECORD_AUDIO
                )
            }
        }

    override fun onClick(v: View) {
        if (binding.sendBtn.text.toString() == "TALK") {
            // stream audio
            binding.sendBtn.text = "OVER"
            micRecorder = MicRecorder()

            //run the runable in thread
            t = Thread(micRecorder)
            if (micRecorder != null) {
                MicRecorder.keepRecording = true
            }
            //start thread
            t!!.start()

            // start animation
            binding.rippleBackground.startRippleAnimation()
        } else if (binding.sendBtn.text.toString() == "OVER") {
            binding.sendBtn.text = "TALK"
            if (micRecorder != null) {
                MicRecorder.keepRecording = false
            }

            // stop animation
            binding.rippleBackground.clearAnimation()
            binding.rippleBackground.stopRippleAnimation()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (micRecorder != null) {
            MicRecorder.keepRecording = false
        }
    }


}