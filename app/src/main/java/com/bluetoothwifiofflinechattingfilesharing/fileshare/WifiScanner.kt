package com.bluetoothwifiofflinechattingfilesharing.fileshare

import android.Manifest
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.AdapterView.OnItemClickListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bluetoothwifiofflinechattingfilesharing.databinding.ActivityWifiScannerBinding

/*
* this class will show list of available devices near you*/
class WifiScanner : AppCompatActivity() {
    lateinit var binding: ActivityWifiScannerBinding
    private var wifiManager: WifiManager? = null
    private val CODE_LOCATION = 1
    private lateinit var receiverWifi: WifiReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWifiScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        if (!wifiManager!!.isWifiEnabled) {
            Toast.makeText(applicationContext, "Turning WiFi ON...", Toast.LENGTH_LONG).show()
            wifiManager!!.isWifiEnabled = true
        }
        binding.scanBtn.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    this@WifiScanner,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this@WifiScanner,
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    CODE_LOCATION
                )
            } else {
                wifiManager!!.startScan()
            }
        }

        binding.wifiList.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            Toast.makeText(
                baseContext,
                binding.wifiList.getItemAtPosition(position).toString() + "",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        receiverWifi = WifiReceiver(wifiManager!!, binding.wifiList, this@WifiScanner)
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        registerReceiver(receiverWifi, intentFilter)
        wifi
    }

    private val wifi: Unit
        get() {
            Toast.makeText(this@WifiScanner, "version> = marshmallow", Toast.LENGTH_SHORT)
                .show()
            if (ContextCompat.checkSelfPermission(
                    this@WifiScanner,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(this@WifiScanner, "location turned off", Toast.LENGTH_SHORT)
                    .show()
                ActivityCompat.requestPermissions(
                    this@WifiScanner,
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    CODE_LOCATION
                )
            } else {
                Toast.makeText(this@WifiScanner, "location turned on", Toast.LENGTH_SHORT)
                    .show()
                wifiManager!!.startScan()
            }
        }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiverWifi)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CODE_LOCATION -> if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this@WifiScanner, "permission granted", Toast.LENGTH_SHORT).show()
                wifiManager!!.startScan()
            } else {
                Toast.makeText(this@WifiScanner, "permission not granted", Toast.LENGTH_SHORT)
                    .show()
                return
            }
        }
    }

}