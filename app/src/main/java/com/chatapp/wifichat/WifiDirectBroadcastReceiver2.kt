package com.chatapp.wifichat

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import android.widget.Toast

class WifiDirectBroadcastReceiver2(
    private val mManager: WifiP2pManager?,
    private val mChannel: WifiP2pManager.Channel,
    private val mActivity: WifiChatActivity
) : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION == action) {
            val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                //Toast.makeText(context, "WIFI is On", Toast.LENGTH_SHORT).show()

                // Wifi Direct mode is enabled
                mActivity.setIsWifiP2pEnabled(true)
            } else {
                Toast.makeText(context, "WIFI is OFF", Toast.LENGTH_SHORT).show()
                // Wifi Direct mode is enabled
                mActivity.setIsWifiP2pEnabled(false)
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION == action) {
            //call wifip2pManager.reqPeer to get list of peers
            if (mManager != null) {
                mManager.requestPeers(mChannel, mActivity.peerListListener)
                Log.e("DEVICE_NAME", "WIFI P2P peers changed called")
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION == action) {
            Log.e("new connection listen", "WIFI_P2P_CONNECTION_CHANGED_ACTION")
            //respond to new connections or disconnects
            if (mManager != null) {
                val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                if (networkInfo != null && networkInfo.isConnected) {
                    mManager.requestConnectionInfo(mChannel, mActivity.connectionInfoListener)
                } else {
                    mActivity.setStatusSubTitle("Device Disconnected")
                    mActivity.isConnected=false
                }
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION == action) {

            //Indicates this device's configuration details have changed
            //means wither is it connected or disconnected

            val device= intent.getParcelableExtra<WifiP2pDevice>(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
            mActivity.deviceName=device!!.deviceName
            //mActivity.deviceAddress= device.deviceAddress
            Log.e("connected device --receiver", device.primaryDeviceType+" "+device.secondaryDeviceType)
        }
    }
}