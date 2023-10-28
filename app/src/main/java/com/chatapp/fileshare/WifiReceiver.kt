package com.chatapp.fileshare

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast

@SuppressLint("MissingPermission")
class WifiReceiver(var wifiManager: WifiManager, var wifiDeviceList: ListView, var wactivity: WifiScanner)
    : BroadcastReceiver() {

    var sb: StringBuilder? = null

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION == action) {
            sb = StringBuilder()
            val wifiList = wifiManager.scanResults
            val deviceList = ArrayList<String>()
            for (scanResult in wifiList) {
                sb!!.append("\n").append(scanResult.SSID).append(" - ")
                    .append(scanResult.capabilities)
                deviceList.add(scanResult.SSID + " - " + scanResult.capabilities)
            }
            Toast.makeText(context, sb, Toast.LENGTH_SHORT).show()
            val arrayAdapter: ArrayAdapter<*> = ArrayAdapter<Any?>(
                context,
                android.R.layout.simple_list_item_1,
                deviceList.toTypedArray()
            )
            wifiDeviceList.adapter = arrayAdapter
        }
        wifiDeviceList.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            val i = Intent(wactivity, HomeActivity::class.java)
        }
    }
}