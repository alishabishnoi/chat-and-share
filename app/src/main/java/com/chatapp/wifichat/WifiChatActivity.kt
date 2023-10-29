package com.chatapp.wifichat

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener
import android.net.wifi.p2p.WifiP2pManager.PeerListListener
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.ResultReceiver
import android.provider.Settings
import android.text.format.Formatter
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.view.Window
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amulyakhare.textdrawable.TextDrawable
import com.chatapp.R
import com.chatapp.adapter.ChatAdapter
import com.chatapp.bluetooth.FilePickerHelper
import com.chatapp.databinding.ActivityWifiP2PBinding
import com.chatapp.databinding.BottomSheetBinding
import com.chatapp.db.OfflineDatabase
import com.chatapp.models.chat
import com.chatapp.utils.CustomToast
import com.chatapp.utils.PermissionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Calendar


@SuppressLint("MissingPermission")
class WifiChatActivity : AppCompatActivity() {
    private val TAG = "Wifi Chat Activity"
    lateinit var binding: ActivityWifiP2PBinding
    lateinit var wifiManager: WifiManager
    private var mManager: WifiP2pManager? = null
    private var mChannel: WifiP2pManager.Channel? = null
    private var mReceiver: BroadcastReceiver? = null
    private var mIntentFilter: IntentFilter? = null

    var CHANNEL_1_ID = "channel1"

    var isConnected = false

    private var custom_peers = ArrayList<WifiP2pDevice>()
    private lateinit var deviceNameArray: Array<String?>
    private lateinit var devicesArray: Array<WifiP2pDevice?>

    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private var serverClass1: ServerClass? = null
    private var clientClass1: ClientClass? = null
    private var isHost = false

    var socket = Socket()

    var database: OfflineDatabase? = null

    private lateinit var sheetBinding: BottomSheetBinding
    lateinit var sheetdialog: Dialog

    private var chatList: ArrayList<chat> = arrayListOf()
    private lateinit var chatAdapter: ChatAdapter

    private var mOutStringBuffer = StringBuffer()

    private var isWifiP2pEnabled = false

    var deviceName: String = ""
    var deviceAddress: String = ""

    lateinit var connectedDevice: WifiP2pDevice

    private lateinit var permissionManager: PermissionManager
    private val storagePermission = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private val locationPermission = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION
    )

    fun setIsWifiP2pEnabled(isWifiP2pEnabled: Boolean) {
        this.isWifiP2pEnabled = isWifiP2pEnabled
    }

    /**
     * fileURI: The URI for the file of interest on the current phone.
     * this will be selected by user
     */
    private var fileURI: String = ""

    companion object {
        private const val MY_PERMISSIONS_REQUEST_REQUIRED_PERMISSION = 3
        private const val REQUEST_FILE_PICK = 4

        private val REQUEST_STORAGE = 101
        private val REQUEST_LOCATION = 102
        private val REQUEST_NEARBY = 103

        private const val D = true
        lateinit var fileCreated: File

        private const val REQUEST_SERVICE_MESSAGE = 1

        const val STATE_LISTENING = 1
        const val STATE_CONNECTING = 2
        const val STATE_CONNECTED = 3
        const val STATE_CONNECTION_FAILED = 4
        const val STATE_NO_PEERS = 5
        const val STATE_MESSAGE_RECEIVED = 6
        const val STATE_MESSAGE_SEND = 7
        const val STATE_PROGRESS = 8
    }

    /*this will be called from the broadcast
    *call wifi2pManager.reqPeer to get list of peers
    */
    var peerListListener = PeerListListener { peersList ->
        Log.d(TAG, "Peers Listener called--list size" + peersList.deviceList.size)
        if (peersList.deviceList.isNotEmpty()) {

            // this will remove all devices no longer in range
            custom_peers.clear()

            // add all devices in range
            custom_peers.addAll(peersList.deviceList)
            deviceNameArray = arrayOfNulls(peersList.deviceList.size)
            devicesArray = arrayOfNulls(peersList.deviceList.size)

            /* old way
            var index = 0
            for (device in peersList.deviceList) {
                deviceNameArray[index] = device.deviceName
                devicesArray[index] = device
                index++
            }*/

            for ((index, device) in peersList.deviceList.withIndex()) {
                deviceNameArray[index] = device.deviceName
                devicesArray[index] = device
            }

            val adapter = ArrayAdapter(
                applicationContext, android.R.layout.simple_list_item_1, deviceNameArray
            )
            sheetBinding.peerListView.adapter = adapter


        } else {
            val message = Message.obtain()
            message.what = STATE_NO_PEERS
            handler.sendMessage(message)
        }
    }

    //respond to new connections or disconnects and called from the broadcast receiver
    var connectionInfoListener = ConnectionInfoListener { wifiP2pInfo ->
        //a host can connect as many device
        val level = WifiManager.calculateSignalLevel(wifiManager.connectionInfo.rssi, 5)
        Log.d(TAG, "level of signal:$level ")
        val groupOwnerAddress = wifiP2pInfo.groupOwnerAddress
        if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
            setStatusSubTitle("online")

            isHost = true
            isConnected = true
            //it is host make it server
            serverClass1 = ServerClass()
            serverClass1!!.start() //because it is thread
            //if sheet is showing then hide it
            sheetdialog.dismiss()
            //now get the  chats of this user
            //getDbChats()
        } else if (wifiP2pInfo.groupFormed) {
            setStatusSubTitle("online")
            isHost = false
            isConnected = true
            clientClass1 = ClientClass(groupOwnerAddress)
            clientClass1!!.start()

            //if sheet is showing then hide it
            sheetdialog.dismiss()

            //getDbChats()
        }
    }

    private fun initP2p(): Boolean {
        // Device capability definition check
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)) {
            toast("Wi-Fi Direct is not supported by this device")
            return false
        }

        // Hardware capability check
        wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        if (!wifiManager.isP2pSupported) {
            toast("Wifi Off")
            val panelIntent = Intent(Settings.Panel.ACTION_WIFI)
            startActivityForResult(panelIntent, 1)
            return false
        }
        mManager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        if (mManager == null) {
            toast("Cannot get Wi-Fi Direct system service")
            return false
        }
        mChannel = mManager!!.initialize(this, mainLooper, null)
        if (mChannel == null) {
            toast("Cannot initialize Wi-Fi Direct")
            return false
        } else {
            if (mManager != null && mChannel != null) {
                removeGroup()
            }
        }

        return true
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWifiP2PBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (D) Log.e(TAG, "+++ ON CREATE +++")
        HandleSocket.activityStatus = "create"

        permissionManager = PermissionManager.getInstance(this)

        if (!permissionManager.methodCheckPermission(locationPermission)) {
            permissionManager.methodAskPermission(
                this, locationPermission, REQUEST_LOCATION
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!permissionManager.methodCheckPermission(arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES))) {
                permissionManager.methodAskPermission(
                    this, arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES), REQUEST_NEARBY
                )
            }
        }

        binding.writeMsg.setSelection(0)

        checkLocationEnabled()

        //initialize room database to get all his methods
        database = OfflineDatabase.getDatabase(this)

        if (!initP2p()) {
            finish()
        }

        sheetdialog = Dialog(this)
        sheetdialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        //using binding
        sheetBinding = BottomSheetBinding.inflate(layoutInflater)
        sheetdialog.setContentView(sheetBinding.root)

        mIntentFilter = IntentFilter()
        mIntentFilter!!.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        mIntentFilter!!.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        mIntentFilter!!.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        mIntentFilter!!.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)

        binding.chatRecyclerView.layoutManager = LinearLayoutManager(
            this, RecyclerView.VERTICAL, false
        )
        chatAdapter = ChatAdapter(this, chatList, "me")
        binding.chatRecyclerView.adapter = chatAdapter

        //this receiver will listen for reply
        val filter = IntentFilter()
        filter.addAction("android.intent.action.SmsReceiver")
        LocalBroadcastManager.getInstance(this).registerReceiver(mServiceReceiver, filter)

        //to send message
        binding.sendButton.setOnClickListener {
            if (isConnected) {
                val msg = binding.writeMsg.text.toString()
                if (msg.isNotEmpty() && isHost) {
                    //means we are writing from server class
                    val finalMessage = "msg#$msg"
                    val send = finalMessage.toByteArray()
                    writeWriter("", send)

                    //empty the edittext and buffer otherwise it will add last chars in the next message
                    runOnUiThread {
                        mOutStringBuffer.setLength(0)
                        binding.writeMsg.setText(mOutStringBuffer)
                    }

                } else if (msg.isNotEmpty()) {
                    //this time we are writing from client class
                    val finalMessage = "msg#$msg"
                    val send = finalMessage.toByteArray()

                    writeWriter("", send)

                    runOnUiThread {
                        mOutStringBuffer.setLength(0)
                        binding.writeMsg.setText(mOutStringBuffer)
                    }
                }

                /*//this send message is also a blocking call so do in bg
                val executorService = Executors.newSingleThreadExecutor()

                executorService.execute {

                }*/
            } else {
                toast("You are not connected")
            }

        }

        binding.iPickFile.setOnClickListener {
            if (isConnected) {
                if (!permissionManager.methodCheckPermission(storagePermission)) {
                    permissionManager.methodAskPermission(
                        this, storagePermission, REQUEST_LOCATION
                    )
                } else {
                    filePicker()
                }

            } else {
                toast("You are not connected")
            }

        }
    }

    private fun send(fileURI: String) {
        // Create File object to convert to bytes
        val file = File(fileURI)
        if (this.fileURI == "") {
            toast("Please choose a file first")
        } else {// Check with 5 MB Limit
            if (file.readBytes().size > (1024 * 1024 * 100)) {
                // Dialog the user and send back to file picker activity
                val alertDialogBuilder = AlertDialog.Builder(this)
                alertDialogBuilder.setTitle("File too large")
                alertDialogBuilder.setMessage("This file is larger than the 100 MB Limit")
                alertDialogBuilder.setPositiveButton("OK") { _, _ ->
                    toast("File sending failed")
                }
                alertDialogBuilder.show()
            } else {
                val fileSize = File(fileURI).length()

                val size = Formatter.formatFileSize(this@WifiChatActivity, fileSize)

                val finalMessage = "file#${fileURI}#$size"
                val send = finalMessage.toByteArray()
                Log.d("---send file detail", "message with file : $finalMessage")

                if (isHost) {
                    writeWriter(fileURI, send)
                    //serverClass1!!.serverWriting(send)
                } else {
                    writeWriter(fileURI, send)
                    //clientClass1!!.clientWriting(send)
                }
            }

        }
    }

    private fun filePicker() {
        val mimeTypes: Array<String> = arrayOf("image/*", "video/*", "application/pdf", "audio/*")

        // create the intent to open file picker, add the desired file types to our picker and select the option that
        // the files be openable on the phone
        val intent = Intent().setType("*/*")
            .setAction(Intent.ACTION_GET_CONTENT) // ACTION_GET_CONTENT refers to the built-in file picker
            .addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)

        // Open file picker, and add a requestCode to refer to correct Activity result in startActivityForResult()
        startActivityForResult(
            Intent.createChooser(intent, "Choose a file"), REQUEST_FILE_PICK
        )
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (D) Log.d(TAG, "onActivityResult $resultCode")
        when (requestCode) {
            REQUEST_FILE_PICK -> {
                if (resultCode == RESULT_OK) {
                    val selectedFile = data?.data //The uri with a location of the file

                    // Helper method, refer to FilePickerHelper getPath() method
                    fileURI = FilePickerHelper.getPath(this, selectedFile!!).toString()

                    //send file in ui thread because it will show dialog
                    send(fileURI)

                } else if (resultCode == RESULT_CANCELED) {
                    toast("File choosing cancelled")
                } else {
                    toast("Error with choosing this file")
                }
            }
        }
    }

    private fun setRecycler(sender: String, receiver: String, msg: String?) {

        val time = getDate(System.currentTimeMillis(), "hh:mm aa")
        val chatModel = chat(0, "", sender, receiver, R.drawable.happyicon, msg!!, time!!, "", true)
        chatList.add(chatModel)
        chatAdapter.notifyItemInserted(chatList.size - 1)
        binding.chatRecyclerView.scrollToPosition(chatList.size - 1)

        if (isConnected) {
            insertSqlite("deviceName", deviceName, msg, time)
        }

        //chatAdapter.notifyDataSetChanged()
    }

    fun setStatusTitle(title: String) {
        val actionBar = supportActionBar
        actionBar!!.title = title
    }

    fun setStatusSubTitle(subTitle: CharSequence) {
        val actionBar = supportActionBar
        actionBar!!.subtitle = subTitle
    }

    override fun onStart() {
        super.onStart()
        if (D) Log.e(TAG, "+++ ON START +++")

    }

    override fun onResume() {
        super.onResume()
        if (D) Log.e(TAG, "+++ ON RESUME +++")
        HandleSocket.activityStatus = "resume"
        //make receiver private nhi toh ye yellow hoga
        mReceiver = WifiDirectBroadcastReceiver2(mManager, mChannel!!, this)
        registerReceiver(mReceiver, mIntentFilter)
    }

    override fun onPause() {
        super.onPause()
        if (D) Log.e(TAG, "+++ ON PAUSE +++")
        HandleSocket.activityStatus = "pause"
    }

    override fun onStop() {
        super.onStop()
        if (D) Log.e(TAG, "+++ ON STOP +++")
        HandleSocket.activityStatus = "stop"
    }

    override fun onDestroy() {
        super.onDestroy()
        if (D) Log.e(TAG, "+++ ON DESTROY +++")
        HandleSocket.activityStatus = "destroy"

        if (mReceiver != null) {
            unregisterReceiver(mReceiver)
        }

        isConnected = false

        //stop service
        val broadcastIntent = Intent(this, ReadingService::class.java)
        broadcastIntent.action = "toogle"
        stopService(broadcastIntent)

        try {
            if (!mServiceReceiver.equals(null)) {
                unregisterReceiver(mServiceReceiver)
            }

        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    inner class ClientClass(hostAddress: InetAddress) : Thread() {
        private var hostAddress: String


        init {
            //get the server address
            this.hostAddress = hostAddress.hostAddress!!
            //client will create socket and connect with the host or say server
        }

        override fun run() {
            Log.d(TAG, "client class run ")
            try {
                socket.connect(InetSocketAddress(hostAddress, 8889))
                //set this socket to handle in service
                HandleSocket.socket = socket

                //now get stream from this socket
                inputStream = socket.getInputStream()
                outputStream = socket.getOutputStream()

                /*//run on bg thread
                val executorService = Executors.newSingleThreadExecutor()
                //val handler = Handler(Looper.getMainLooper())
                executorService.execute {
                    Log.d(TAG, "executor: ")
                    while (!socket.equals(null)) {
                        Log.d(TAG, "while: ")
                        try {
                            // Read from the InputStream
                            getFile(inputStream!!)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }*/


                startServices()

            } catch (e: IOException) {
                e.printStackTrace()
            }


        }


    }

    inner class ServerClass : Thread() {

        private var serverSocket: ServerSocket? = null

        override fun run() {
            Log.d(TAG, "server class run")
            try {
                serverSocket = ServerSocket(8889)
                //first connect this socket to  server socket the get and out the stream
                socket = serverSocket!!.accept()

                HandleSocket.socket = socket
                //set this socket to handle in service

                inputStream = socket.getInputStream()
                outputStream = socket.getOutputStream()

                startServices()


            } catch (e: IOException) {
                e.printStackTrace()
            }


        }


    }




    private fun writeWriter(fileURI: String, send: ByteArray) {
        val writer = Writer(handler, fileURI, send, outputStream!!)
        val t = Thread(writer)

        writer.keepRecording = true

        //start thread
        t.start()

        this@WifiChatActivity.fileURI = ""

    }

    fun startServices() {
        val readingServiceIntent = Intent(applicationContext, ReadingService::class.java)
        //readingServiceIntent.putExtra("message", REQUEST_SERVICE_MESSAGE)
        readingServiceIntent.putExtra("clientResult", object : ResultReceiver(null) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle) {
                if (resultCode == REQUEST_SERVICE_MESSAGE) {
                    val message = resultData["message"] as String?
                    Log.d("from reading service", "onReceiveResult: $message")
                    if (message != null) {
                        if (message == "serviceStoppedByOther") {
                            //send this message to the other
                            val ms = "serviceStoppedByOther"
                            val finalMessage = "msg#$ms"
                            val send = finalMessage.toByteArray()

                            writeWriter("", send)
                        } else {
                            handler.obtainMessage(
                                STATE_MESSAGE_RECEIVED,
                                message.toByteArray().size,
                                -1,
                                message.toByteArray()
                            ).sendToTarget()
                        }


                    }

                }

                /*if (resultCode == REQUEST_SERVICE_PROGRESS) {
                    val progress=resultData["progress"] as String?
                    Log.d("from reading service", "onReceiveResult: $progress")
                    binding.progressFileSent.progress=progress!!.toInt()
                    binding.tvFileSent.text= progress

                    Log.d("''''''progress''''''", "reading Progress: $progress")
                }*/
            }
        })
        startForegroundService(readingServiceIntent)
        Log.d(TAG, "-----------------: started services")
    }

    private val mServiceReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            val stop = intent.getStringExtra("stop")
            //Extract your data - better to use constants...
            val replyText = intent.getStringExtra("incomingPhoneNumber")

            if (stop.equals("stop")) {
                //means service is stopped
                isConnected = false
                setStatusSubTitle("Disconnected")
                toast("Search New Peers")
            } else {
                val finalMessage = "msg#$replyText"

                Log.d(TAG, "onReceive: reply $replyText ")
                //now we have reply and we can pass write it to writer
                if (isConnected) {
                    writeWriter("", finalMessage.toByteArray())
                } else {
                    toast("You are not Connected")
                }
            }


        }
    }

    var handler = Handler { msg ->
        when (msg.what) {
            STATE_LISTENING -> setStatusSubTitle("Listening")
            STATE_CONNECTING -> setStatusSubTitle("Connecting")
            STATE_CONNECTED -> setStatusSubTitle("Connected")
            STATE_CONNECTION_FAILED -> setStatusSubTitle("Connection Failed")

            STATE_MESSAGE_RECEIVED -> {
                val readBuff = msg.obj as ByteArray

                val receivedMessage = String(readBuff, 0, readBuff.size)
                Log.d("''''''read''''''", "handleMessage: $receivedMessage")
                val perfix = receivedMessage.split("#")[1]
                if (perfix == "serviceStoppedByOther") {
                    //stop service
                    val broadcastIntent = Intent(this, ReadingService::class.java)
                    broadcastIntent.action = "toogle"
                    startService(broadcastIntent)
                } else {
                    setRecycler("sender", "me", receivedMessage)
                }


            }

            STATE_MESSAGE_SEND -> {
                val readBuff = msg.obj as ByteArray

                val sentMessage = readBuff.toString(Charsets.UTF_8)
                Log.d("''''''write''''''", "handleMessage: $sentMessage")
                setRecycler("me", "sender", sentMessage)

            }

            STATE_PROGRESS -> {
                val readBuff = msg.obj as Int

                binding.progressFileSent.progress = readBuff
                binding.tvFileSent.text = readBuff.toString()

                //val progress = readBuff.toString(Charsets.UTF_8)
                Log.d("''''''progress''''''", "send Progress: $readBuff")

            }
        }
        true
    }


    private fun showSheet() {
        if (isWifiP2pEnabled) {
            sheetBinding.imgWifi.setImageResource(R.drawable.ic_wifi_on)
        } else {
            sheetBinding.imgWifi.setImageResource(R.drawable.ic_wifi_off)
        }

        if (checkLocationEnabled()) {
            sheetBinding.imgLocation.setImageResource(R.drawable.ic_location_on)
        } else {
            sheetBinding.imgLocation.setImageResource(R.drawable.ic_location_off)
        }

        //when we click on an item from the peers list we will try to connect
        sheetBinding.peerListView.onItemClickListener =
            OnItemClickListener { adapterView, view, i, l ->
                val device = devicesArray[i]
                val config = WifiP2pConfig()
                config.deviceAddress = device!!.deviceAddress
                mManager!!.connect(mChannel, config, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        setStatusTitle(device.deviceName)
                        connectedDevice = device
                        isConnected = true
                        val message = Message.obtain()
                        message.what = STATE_CONNECTING
                        handler.sendMessage(message)
                        sheetdialog.dismiss()
                    }

                    override fun onFailure(i: Int) {
                        val message = Message.obtain()
                        message.what = STATE_CONNECTION_FAILED
                        handler.sendMessage(message)
                    }
                })
            }

        sheetBinding.imgWifi.setOnClickListener {
            if (mManager != null && mChannel != null) {
                // Since this is the system wireless settings activity, it's
                // not going to send us a result. We will be notified by
                // WiFiDeviceBroadcastReceiver instead.
                //startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                removeGroup()
            } else {
                Log.e(
                    TAG, "channel or manager is null"
                )
            }
        }

        sheetBinding.tMyName.text = deviceName
        if (deviceName != "") {
            val name = deviceName.toCharArray()
            val drawable =
                TextDrawable.builder().buildRound(name[0].toString(), R.color.colorAccent)
            sheetBinding.deviceImg.setImageDrawable(drawable)
        }

        //sheetBinding.tMyAddress.text = deviceAddress


        sheetdialog.show()
        sheetdialog.window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        sheetdialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        sheetdialog.window!!.attributes.windowAnimations = R.style.DialogAnimation
        sheetdialog.window!!.setGravity(Gravity.BOTTOM)
    }

    private fun checkLocationEnabled(): Boolean {
        val lm = this.getSystemService(LOCATION_SERVICE) as LocationManager
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
            AlertDialog.Builder(this).setTitle(R.string.gps_network_not_enabled_title)
                .setMessage(R.string.gps_network_not_enabled)
                .setPositiveButton(R.string.open_location_settings) { paramDialogInterface, paramInt ->
                    this.startActivity(
                        Intent(
                            Settings.ACTION_LOCATION_SOURCE_SETTINGS
                        )
                    )
                }.setNegativeButton(R.string.Cancel, null).show()
        }

        return gps_enabled
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION && permissionManager.handleRequestResult(grantResults)) {
            CustomToast.infoToast(this, "Location Permission Granted", CustomToast.GRAVITY_BOTTOM)
        }

        if (requestCode == REQUEST_NEARBY && permissionManager.handleRequestResult(grantResults)) {
            CustomToast.infoToast(this, "Nearby Permission Granted", CustomToast.GRAVITY_BOTTOM)
        }

        if (requestCode == REQUEST_STORAGE && permissionManager.handleRequestResult(grantResults)) {
            CustomToast.infoToast(this, "Storage Permission Granted", CustomToast.GRAVITY_BOTTOM)
        }

    }


    fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    /**
     * Return date in specified format.
     * @param milliSeconds Date in milliseconds
     * @param dateFormat Date format
     * @return String representing date in specified format
     */
    fun getDate(milliSeconds: Long, dateFormat: String?): String? {
        // Create a DateFormatter object for displaying date in specified format.
        val formatter = SimpleDateFormat(dateFormat)

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        val calendar: Calendar = Calendar.getInstance()
        calendar.timeInMillis = milliSeconds
        return formatter.format(calendar.time)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.wifi_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.secure_connect_scan) {
            if (!isConnected) {
                mManager!!.discoverPeers(mChannel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        val message = Message.obtain()
                        message.what = STATE_LISTENING
                        handler.sendMessage(message)
                        showSheet()

                    }

                    override fun onFailure(reason: Int) {
                        val message = Message.obtain()
                        message.what = STATE_CONNECTION_FAILED
                        handler.sendMessage(message)
                    }
                })
            } else {
                toast("You are already Connected")
            }

            return true
        }

        if (item.itemId == R.id.disconnect) {
            /// Ensure this device is discoverable by others
            if (isConnected) {
                disconnectDevice(connectedDevice)
            }
            return true
        }
        return false
    }

    fun disconnectDevice(device: WifiP2pDevice) {
        if (device.status == WifiP2pDevice.INVITED || device.status == WifiP2pDevice.CONNECTED) {
            mManager!!.cancelConnect(mChannel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d(TAG, "cancelConnect: onSuccess " + device.deviceName)
                }

                override fun onFailure(reason: Int) {
                    Log.d(TAG, "cancelConnect: onFailure " + device.deviceName)

                }
            })
        } else {
            Log.d(TAG, "cancelConnect: already disconnected " + device.status + device.deviceName)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {

        val mDialog = MaterialAlertDialogBuilder(this)
        mDialog.setPositiveButton("Close") { dialogInterface, _ ->
            dialogInterface.dismiss()
            super.onBackPressed()
        }.setNegativeButton("Cancel") { dialogInterface, _ ->
            dialogInterface.dismiss()
        }.setMessage("You chat session will be disconnected").setTitle("Exit Chat").create()
        mDialog.show()
    }

    private fun removeGroup() {
        mManager!!.removeGroup(mChannel, object : WifiP2pManager.ActionListener {
            override fun onFailure(reasonCode: Int) {
                Log.d(
                    TAG, "Disconnect failed. Reason :$reasonCode"
                )
            }

            override fun onSuccess() {
                Log.d(
                    TAG, "group removed successfully"
                )
            }
        })
    }

    fun getDbChats() {
        GlobalScope.launch {
            val list = database!!.insertDao().getAllChats()
            chatList = ArrayList(list)
            runOnUiThread {
                chatAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun insertSqlite(sender: String, receiver: String, msg: String, time: String) {
        val oirModel = chat(
            0, "wifi", sender, receiver, R.drawable.happyicon, msg, time, "", true
        )

        GlobalScope.launch(Dispatchers.IO) {
            database!!.insertDao().insertChats(oirModel)
        }
    }

    fun deleteRoomData() {
        GlobalScope.launch(Dispatchers.IO) {
            database!!.clearAllTables()
        }
    }
}