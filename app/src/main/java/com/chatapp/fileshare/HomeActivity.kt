package com.chatapp.fileshare

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.chatapp.activity.QRCaptureClass
import com.chatapp.databinding.ActivityHomeBinding
import com.chatapp.fileshare.Server.Companion.localIpAddress
import com.chatapp.utils.CustomToast
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions

class HomeActivity : AppCompatActivity() {
    lateinit var binding: ActivityHomeBinding
    var wifiManager: WifiManager? = null
    var mManager: WifiP2pManager? = null
    var mChannel: WifiP2pManager.Channel? = null
    var receiver: BroadcastReceiver? = null
    var filter: IntentFilter? = null

    companion object {
        const val MY_PERMISSIONS_REQUEST_LOCATION = 99
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.animView.playAnimation()

        wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        mManager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        mChannel = mManager!!.initialize(this, mainLooper, null)

        receiver = WifiReceiverFileShare(mManager, mChannel, this@HomeActivity)
        filter = IntentFilter()

        binding.EnableDisable.setOnClickListener(View.OnClickListener {
            Toast.makeText(this@HomeActivity, "LOCATION & HOTSPOT REQUIRED", Toast.LENGTH_LONG)
                .show()
            locationEnabled()
            val hotspot = Intent(Settings.ACTION_WIRELESS_SETTINGS)
            val intents = arrayOf(hotspot)
            startActivities(intents)
        })

        binding.RECEIVE.setOnClickListener(View.OnClickListener {
            scanQr()
            /*if (wifiManager!!.isWifiEnabled) {
                Toast.makeText(this@HomeActivity, "WIFI is required", Toast.LENGTH_LONG).show()
                val i = Intent(this@HomeActivity, Client::class.java)
                startActivity(i)
            } else {
                wifiManager!!.isWifiEnabled = true
                Toast.makeText(this@HomeActivity, "Wifi is turning ON", Toast.LENGTH_SHORT).show()
                val i = Intent(this@HomeActivity, Client::class.java)
                startActivity(i)
            }*/
        })

        binding.SERVER.setOnClickListener(View.OnClickListener {
            val c = Intent(this@HomeActivity, Server::class.java)
            startActivity(c)
            //createQR(localIpAddress)
        })
    }



    fun scanQr(){
        val options = ScanOptions()
        options.setPrompt("For Flash use volume up key")
        options.setBeepEnabled(true)
        options.setOrientationLocked(true)
        options.captureActivity = QRCaptureClass::class.java
        resultLauncher.launch(options)
    }

    //when qr code scan is complete and give result
    var resultLauncher = registerForActivityResult<ScanOptions, ScanIntentResult>(
        ScanContract()
    ) { result: ScanIntentResult ->
        if (result.contents != null) {
            val res = result.contents.toString()
            val i = Intent(this@HomeActivity, Client::class.java)
            i.putExtra("ip",res)
            startActivity(i)

        } else {
            CustomToast.infoToastError(this,"Scan Again", Gravity.BOTTOM)
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(receiver, filter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

    fun Wifiscanner(view: View?) {
        //in this we will connect to the items manually
        val file = Intent(this@HomeActivity, WifiScanner::class.java)
        startActivity(file)
    }

    private fun locationEnabled() {

        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        var gps_enabled = false
        var network_enabled = false
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (!gps_enabled && !network_enabled) {
            AlertDialog.Builder(this@HomeActivity)
                .setMessage("GPS Enable")
                .setPositiveButton("Settings") { paramDialogInterface, paramInt ->
                    startActivity(
                        Intent(
                            Settings.ACTION_LOCATION_SOURCE_SETTINGS
                        )
                    )
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }


}