package com.bluetoothwifiofflinechattingfilesharing.walkie

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log

/**
 * A BroadcastReceiver that notifies of important wifi p2p events.
 */
@SuppressLint("MissingPermission")
class WifiDirectBroadcastReceiver(
    private val mManager: WifiP2pManager?,
    private val mChannel: WifiP2pManager.Channel,
    private val mActivity: WalkieMain
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION == action) {
            //Indicates whether Wi-Fi Direct is enabled
            // UI update to indicate wifi p2p status.
            val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                Log.d("broadcast wifi--", "WIFI_ ON")
            } else {
                Log.d("broadcast wifi--", "WIFI_ OFF")
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION == action) {
            //Indicates that the available peer list has changed.
            // request available peers from the wifi p2p manager. This is an
            // asynchronous call and the calling activity is notified with a
            // callback on PeerListListener.onPeersAvailable()
            if (mManager != null) {
                mManager.requestPeers(mChannel, mActivity.peerListListener)
                Log.e("broadcast wifi--", "WIFI P2P peers changed called")
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION == action) {
            //Indicates the state of Wi-Fi Direct connectivity has changed
            if (mManager == null) {
                return
            }
            val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
            if (networkInfo != null && networkInfo.isConnected) {
                mManager.requestConnectionInfo(mChannel, mActivity.connectionInfoListener)
            } else {
                mActivity.binding.connectionStatus.text = "Device Disconnected"
                mActivity.clear_all_device_icons()
                mActivity.binding.rippleBackground.stopRippleAnimation()
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION == action) {
            //Indicates this device's configuration details have changed
            //means wither is it connected or disconnected
            Log.d("broadcast wifi--", "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION ")

        }
    }
}