package com.chatapp.activity

import android.annotation.SuppressLint
import android.app.Dialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.chatapp.R
import com.chatapp.bluetooth.BluetoothUsersActivity
import com.chatapp.databinding.ActivitySplashBinding
import com.chatapp.fileshare.HomeActivity
import com.chatapp.utils.CustomToast
import com.chatapp.walkie.WalkieMain
import com.chatapp.wifichat.WifiChatActivity


class Splash : AppCompatActivity() {
    lateinit var binding: ActivitySplashBinding
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManagerCompat

    var CHANNEL_1_ID = "channel1"
    val CHANNEL_2_ID = "channel2"

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createNotificationChannels()

        binding.button.setOnClickListener {
            val intent=Intent(this, BluetoothUsersActivity::class.java)
            startActivity(intent)
        }

        binding.button2.setOnClickListener {
            val intent=Intent(this, WifiChatActivity::class.java)
            startActivity(intent)
        }

        binding.btnWalkie.setOnClickListener {
            val intent=Intent(this, WalkieMain::class.java)
            startActivity(intent)
        }

        binding.btnShare.setOnClickListener {



            val intent=Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }
    }

    private fun createNotificationChannels() {
        val channel1 = NotificationChannel(
            CHANNEL_1_ID,
            "Channel 1",
            NotificationManager.IMPORTANCE_HIGH
        )
        channel1.description = "This is Channel 1"

        val channel2 = NotificationChannel(
            CHANNEL_2_ID,
            "Channel 2",
            NotificationManager.IMPORTANCE_LOW
        )
        channel2.description = "This is Channel 2"

        val manager = getSystemService(
            NotificationManager::class.java
        )
        manager.createNotificationChannel(channel1)
        manager.createNotificationChannel(channel2)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.bottom_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_share -> {
                val intent2 = Intent(Intent.ACTION_SEND)
                intent2.type = "text/plain"
                intent2.putExtra(Intent.EXTRA_SUBJECT, "Check my App")
                intent2.putExtra(
                    Intent.EXTRA_TEXT,
                    "https://play.google.com/store/apps/details?id=$packageName"
                )
                startActivity(Intent.createChooser(intent2, "Share Via"))
                return true
            }

            R.id.nav_rate_us -> {

                //SettingsPopup(this).show(item.actionView!!)

                showRatingDialog()
                return true
            }

            R.id.nav_policy -> {
                val uri =
                    Uri.parse("https://sites.google.com/view/smarttoolboxpolicy/home")
                val i = Intent(Intent.ACTION_VIEW, uri)
                try {
                    startActivity(i)
                } catch (e: Exception) {
                    CustomToast.infoToastError(this,"Error", CustomToast.GRAVITY_BOTTOM)
                }
                return true
            }

            R.id.nav_feed_back -> {

                val intent = Intent(Intent.ACTION_SEND)
                intent.putExtra(Intent.EXTRA_EMAIL,"teammukam4people@gmail.com")
                intent.putExtra(Intent.EXTRA_SUBJECT, "Feedback")
                intent.putExtra(Intent.EXTRA_TEXT, "write your suggestion")
                intent.setType("message/rfc822")
                startActivity(Intent.createChooser(intent, "Send Email using:"));
                return true
            }



        }
        return false
    }

    fun showRatingDialog(){
        var rate: Double
        val dialog2 = Dialog(this)
        dialog2.setContentView(R.layout.dialog_rating)
        dialog2.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val bar = dialog2.findViewById<RatingBar>(R.id.rating_bar)
        val submitRating = dialog2.findViewById<TextView>(R.id.submitRating)
        val showRatingMsg = dialog2.findViewById<TextView>(R.id.showRatingMsg)
        val rating = bar.numStars.toString()


        bar.onRatingBarChangeListener = RatingBar.OnRatingBarChangeListener() { ratingBar, v, b ->
            rate = v.toDouble()
            var msg: String? = null
            when (rate.toInt()) {
                1 -> msg = resources.getString(R.string.more_rating_Ok)
                2 -> msg = resources.getString(R.string.more_rating_nice)
                3 -> msg = resources.getString(R.string.more_rating_good)
                4 -> msg = resources.getString(R.string.more_rating_very_good)
                5 -> msg = resources.getString(R.string.more_rating_awesome)
            }
            showRatingMsg.text = rate.toString()+"⭐"+" "+msg
        }

        //to update review in review collection
        submitRating.setOnClickListener {
            val uri =
                Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            val i = Intent(Intent.ACTION_VIEW, uri)
            try {
                startActivity(i)
            } catch (e: java.lang.Exception) {
                CustomToast.infoToastError(this,"Error", CustomToast.GRAVITY_BOTTOM)
            }

        }
        dialog2.show()
    }


}