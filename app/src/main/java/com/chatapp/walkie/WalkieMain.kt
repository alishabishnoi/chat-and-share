package com.chatapp.walkie

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Point
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener
import android.net.wifi.p2p.WifiP2pManager.PeerListListener
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.chatapp.R
import com.chatapp.databinding.ActivityWalkieMainBinding
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

@SuppressLint("MissingPermission")
class WalkieMain : AppCompatActivity(), View.OnClickListener {
    lateinit var binding: ActivityWalkieMainBinding
    private var device_points = ArrayList<Point>()

    private var wifiManager: WifiManager? = null
    private var mManager: WifiP2pManager? = null
    private var mChannel: WifiP2pManager.Channel? = null

    private var mReceiver: BroadcastReceiver? = null
    private var mIntentFilter: IntentFilter? = null

    private var custom_peers = ArrayList<CustomDevice>()
    private var serverClass: ServerClass? = null
    var clientClass: ClientClass? = null

    private var menu: Menu? = null

    companion object {
        private const val TAG = "Walkie Main"
        private const val MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1
        private const val MY_PERMISSIONS_REQUEST_REQUIRED_PERMISSION = 3
        private const val SEPRATION_DIST_THRESHOLD = 50
        private var device_count = 0
        const val PORT_USED = 9584
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWalkieMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //ask permissions
        permissionLocation

        permissionNearByDevice

        // add onClick Listeners
        binding.centerDeviceIcon.setOnClickListener(this)

        // center button position
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        device_points.add(Point(size.x / 2, size.y / 2))
        Log.d("MainActivity", size.x.toString() + "  " + size.y)

        wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        mManager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        mChannel = mManager!!.initialize(this, mainLooper, null)

        mReceiver = WifiDirectBroadcastReceiver(mManager, mChannel!!, this)

        mIntentFilter = IntentFilter()
        mIntentFilter!!.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        mIntentFilter!!.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        mIntentFilter!!.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        mIntentFilter!!.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menu_inflater = menuInflater
        menu_inflater.inflate(R.menu.walkie_menu, menu)
        this.menu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.wifi_toggle) {
            toggleWifiState()
        }
        return super.onOptionsItemSelected(item)
    }

    inner class ServerClass : Thread() {
        private var socket: Socket? = null
        private var serverSocket: ServerSocket? = null
        override fun run() {
            try {
                serverSocket = ServerSocket(PORT_USED)
                socket = serverSocket!!.accept()
                SocketHandler.socket = socket
                startActivity(Intent(applicationContext, ChatWindow::class.java))
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    inner class ClientClass internal constructor(address: InetAddress) : Thread() {
        private var clientSocket: Socket = Socket()
        private var hostAddress: String

        init {
            hostAddress = address.hostAddress!!
        }

        override fun run() {
            try {
                clientSocket.connect(InetSocketAddress(hostAddress, PORT_USED), 500)
                SocketHandler.socket = clientSocket
                startActivity(Intent(applicationContext, ChatWindow::class.java))
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mReceiver)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(mReceiver, mIntentFilter)
    }


    private fun checkLocationEnabled() {
        val lm = this@WalkieMain.getSystemService(LOCATION_SERVICE) as LocationManager
        var gps_enabled = false
        var network_enabled = false
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (_: Exception) {
        }
        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {
        }
        if (!gps_enabled && !network_enabled) {
            // notify user
            AlertDialog.Builder(this@WalkieMain).setTitle(R.string.gps_network_not_enabled_title)
                .setMessage(R.string.gps_network_not_enabled)
                .setPositiveButton(R.string.open_location_settings) { paramDialogInterface, paramInt ->
                    this@WalkieMain.startActivity(
                        Intent(
                            Settings.ACTION_LOCATION_SOURCE_SETTINGS
                        )
                    )
                }.setNegativeButton(R.string.Cancel, null).show()
        }
    }

    override fun onClick(v: View) {
        val view_id = v.id
        if (getIndexFromIdPeerList(view_id) != -1) {
            //means something in the circle list
            val idx = getIndexFromIdPeerList(view_id)
            val device = custom_peers[idx].device
            val config = WifiP2pConfig()
            config.deviceAddress = device!!.deviceAddress
            mManager!!.connect(mChannel, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Toast.makeText(
                        applicationContext, "Connected to " + device.deviceName, Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onFailure(reason: Int) {
                    Toast.makeText(
                        applicationContext,
                        "Error in connecting to " + device.deviceName,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        } else {
            binding.rippleBackground.startRippleAnimation()
            checkLocationEnabled()
            discoverDevices()
        }
    }

    private fun getIndexFromIdPeerList(id: Int): Int {
        for (device in custom_peers) {
            if (device.id == id) {
                return custom_peers.indexOf(device)
            }
        }
        return -1
    }

    private fun checkPeersListByName(deviceName: String): Int {
        for (device in custom_peers) {
            if (device.deviceName == deviceName) {
                return custom_peers.indexOf(device)
            }
        }
        return -1
    }

    private fun discoverDevices() {
        mManager!!.discoverPeers(mChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                binding.connectionStatus.text = "Discovery Started"
            }

            override fun onFailure(reason: Int) {
                binding.connectionStatus.text = "Discovery start Failed"
            }
        })
    }

    @JvmField
    var peerListListener = PeerListListener { peersList ->
        Log.d("DEVICE_NAME", "Listener called" + peersList.deviceList.size)
        if (peersList.deviceList.isNotEmpty()) {

            // first make a list of all devices already present
            val device_already_present = ArrayList<CustomDevice>()
            for (device in peersList.deviceList) {
                val idx = checkPeersListByName(device.deviceName)
                if (idx != -1) {
                    // device already in list
                    device_already_present.add(custom_peers[idx])
                }
            }
            if (device_already_present.size == peersList.deviceList.size) {
                // all discovered devices already present
                return@PeerListListener
            }

            // clear previous views
            clear_all_device_icons()

            // this will remove all devices no longer in range
            custom_peers.clear()
            // add all devices in range
            custom_peers.addAll(device_already_present)

            // add all already present devices to the view
            for (d in device_already_present) {
                binding.rippleBackground.addView(d.icon_view)
            }
            for (device in peersList.deviceList) {
                if (checkPeersListByName(device.deviceName) == -1) {
                    // device not already present
                    val tmp_device = createNewDevice(device.deviceName)
                    binding.rippleBackground.addView(tmp_device)
                    foundDevice(tmp_device)
                    val tmp_device_obj = CustomDevice()
                    tmp_device_obj.deviceName = device.deviceName
                    tmp_device_obj.id = tmp_device.id
                    tmp_device_obj.device = device
                    tmp_device_obj.icon_view = tmp_device
                    custom_peers.add(tmp_device_obj)
                }
            }
        }
        if (peersList.deviceList.size == 0) {
            Toast.makeText(applicationContext, "No Peers Found", Toast.LENGTH_SHORT).show()
        }
    }

    fun clear_all_device_icons() {
        if (!custom_peers.isEmpty()) {
            for (d in custom_peers) {
                binding.rippleBackground.removeView(findViewById(d.id))
            }
        }
    }

    @JvmField
    var connectionInfoListener = ConnectionInfoListener { info ->
        val groupOwnerAddress = info.groupOwnerAddress
        if (info.groupFormed && info.isGroupOwner) {
            binding.connectionStatus.text = "HOST"
            serverClass = ServerClass()
            serverClass!!.start()
        } else if (info.groupFormed) {
            binding.connectionStatus.text = "CLIENT"
            clientClass = ClientClass(groupOwnerAddress)
            clientClass!!.start()
        }
    }

    private fun generateRandomPosition(): Point {
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val SCREEN_WIDTH = size.x
        val SCREEN_HEIGHT = size.y
        val height_start = SCREEN_HEIGHT / 2 - 300
        var x = 0
        var y = 0
        do {
            x = (Math.random() * SCREEN_WIDTH).toInt()
            y = (Math.random() * height_start).toInt()
        } while (checkPositionOverlap(Point(x, y)))
        val new_point = Point(x, y)
        device_points.add(new_point)
        return new_point
    }

    private fun checkPositionOverlap(new_p: Point): Boolean {
        //  if overlap, then return true, else return false
        if (!device_points.isEmpty()) {
            for (p in device_points) {
                val distance = Math.sqrt(
                    Math.pow(
                        (new_p.x - p.x).toDouble(), 2.0
                    ) + Math.pow((new_p.y - p.y).toDouble(), 2.0)
                ).toInt()
                Log.d(TAG, distance.toString() + "")
                if (distance < SEPRATION_DIST_THRESHOLD) {
                    return true
                }
            }
        }
        return false
    }

    //this is show  a device in the ripple background
    private fun createNewDevice(device_name: String?): View {
        val device1 = LayoutInflater.from(this).inflate(R.layout.device_icon, null)
        val new_point = generateRandomPosition()
        val params = RelativeLayout.LayoutParams(350, 350)
        params.setMargins(new_point.x, new_point.y, 0, 0)
        device1.layoutParams = params
        val txt_device1 = device1.findViewById<TextView>(R.id.myImageViewText)
        val device_id = System.currentTimeMillis().toInt() + device_count++
        txt_device1.text = device_name
        device1.id = device_id
        device1.setOnClickListener(this)
        device1.visibility = View.INVISIBLE
        return device1
    }

    //it will show the animation
    private fun foundDevice(foundDevice: View) {
        val animatorSet = AnimatorSet()
        animatorSet.duration = 400
        animatorSet.interpolator = AccelerateDecelerateInterpolator()
        val animatorList = ArrayList<Animator>()
        val scaleXAnimator = ObjectAnimator.ofFloat(foundDevice, "ScaleX", 0f, 1.2f, 1f)
        animatorList.add(scaleXAnimator)
        val scaleYAnimator = ObjectAnimator.ofFloat(foundDevice, "ScaleY", 0f, 1.2f, 1f)
        animatorList.add(scaleYAnimator)
        animatorSet.playTogether(animatorList)
        foundDevice.visibility = View.VISIBLE
        animatorSet.start()
    }

    private fun toggleWifiState() {
        if (wifiManager!!.isWifiEnabled) {
            wifiManager!!.isWifiEnabled = false
            menu!!.findItem(R.id.wifi_toggle).title = "Turn Wifi On"
        } else {
            wifiManager!!.isWifiEnabled = true
            menu!!.findItem(R.id.wifi_toggle).title = "Turn Wifi Off"
        }
    }

    private val permissionLocation: Unit
        get() {
            if ((ContextCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED)
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ), MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION
                )
            }
        }

    private val permissionNearByDevice: Unit
        get() {
            if ((ContextCompat.checkSelfPermission(
                    this, Manifest.permission.NEARBY_WIFI_DEVICES
                ) != PackageManager.PERMISSION_GRANTED)
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.NEARBY_WIFI_DEVICES
                    ), MY_PERMISSIONS_REQUEST_REQUIRED_PERMISSION
                )
            }
        }

    inner class CustomDevice internal constructor() {
        var id = 0
        var deviceName: String? = null
        var device: WifiP2pDevice? = null
        var icon_view: View? = null
    }


}