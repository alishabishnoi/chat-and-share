package com.bluetoothwifiofflinechattingfilesharing.activity

import android.Manifest
import android.app.Dialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bluetoothwifiofflinechattingfilesharing.R
import com.bluetoothwifiofflinechattingfilesharing.bluetooth.BluetoothUsersActivity
import com.bluetoothwifiofflinechattingfilesharing.databinding.ActivitySplashBinding
import com.bluetoothwifiofflinechattingfilesharing.fileshare.HomeActivity
import com.bluetoothwifiofflinechattingfilesharing.utils.AppUpdate
import com.bluetoothwifiofflinechattingfilesharing.utils.CustomToast
import com.bluetoothwifiofflinechattingfilesharing.utils.PrefsHelper
import com.bluetoothwifiofflinechattingfilesharing.utils.ShowReview
import com.bluetoothwifiofflinechattingfilesharing.walkie.WalkieMain
import com.bluetoothwifiofflinechattingfilesharing.wifichat.WifiChatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivitySplashBinding
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManagerCompat

    var CHANNEL_1_ID = "channel1"
    val CHANNEL_2_ID = "channel2"

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //init only once in all app
        PrefsHelper.Builder()
            .setContext(this)
            .setMode(ContextWrapper.MODE_PRIVATE)
            .setPrefsName(packageName)
            .setUseDefaultSharedPreference(true)
            .build()

        //every 5th time show for review
        val opened = PrefsHelper.getInt("OPEN", 0)

        //first get previous value
        if (opened == 8){
            PrefsHelper.putInt("OPEN", 1)

            //initiateRatingFlow()
            val rating = ShowReview(this, this)
            //this will show the rating dialog every 5th time
            Handler().postDelayed({
                rating.initiateRatingFlow()
            }, 3000)
        }else{
            val newVal=opened+1
            PrefsHelper.putInt("OPEN", newVal)
        }

        //to check and update if available
        AppUpdate(this,this)

        // checking if android version is greater than oreo(API 26) or not
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //to show daily notification
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                createNotificationChannels()
            } else {
                // Permission is not granted.
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }


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

    val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // PERMISSION GRANTED
            Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
        } else {
            // PERMISSION NOT GRANTED
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
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

            R.id.nav_theme->{
                val mDialog = MaterialAlertDialogBuilder(this)
                mDialog.setPositiveButton(getString(R.string.day)) { dialogInterface, _ ->
                    dialogInterface.cancel()
                    dialogInterface.dismiss()
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

                }.setNegativeButton(getString(R.string.night)) { dialogInterface, _ ->
                    dialogInterface.cancel()
                    dialogInterface.dismiss()
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

                }
                    .setMessage(getString(R.string.more_theme_msg))
                    .setTitle(getString(R.string.more_theme_head))
                    .create()
                mDialog.show()
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