package com.chatapp.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.format.Formatter
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chatapp.R
import com.chatapp.adapter.ChatAdapter
import com.chatapp.databinding.ActChatBinding
import com.chatapp.db.OfflineDatabase
import com.chatapp.models.Users
import com.chatapp.models.chat
import com.chatapp.utils.PermissionManager
import com.chatapp.wifichat.HandleSocket
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


/**
 * This is the main Activity that displays the current chat session.
 */
@SuppressLint("MissingPermission")
class ChatActivity : AppCompatActivity() {
    lateinit var binding: ActChatBinding

    // Name of the connected device
    private lateinit var mConnectedDeviceName: String

    // String buffer for outgoing messages
    private lateinit var mOutStringBuffer: StringBuffer

    // Local Bluetooth adapter
    private lateinit var mBluetoothAdapter: BluetoothAdapter

    // Member object for the chat services
    private lateinit var mChatService: ChatService

    var database: OfflineDatabase? = null

    /**
     * device: The Bluetooth Device that will receive the file contents.
     */
    private var device: BluetoothDevice? = null

    /**
     * fileURI: The URI for the file of interest on the current phone.
     */
    private var fileURI: String = ""

    private var chatList: ArrayList<chat> = arrayListOf()
    lateinit var chatAdapter: ChatAdapter

    var isConnected = false

    private lateinit var permissionManager: PermissionManager
    private val permission1 = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    companion object {
        // Debugging
        const val TAG = "Chat Activity"
        private const val D = true

        // Message types sent from the BluetoothChatService Handler
        const val MESSAGE_STATE_CHANGE = 1
        const val MESSAGE_READ = 2
        const val MESSAGE_WRITE = 3
        const val MESSAGE_DEVICE_NAME = 4
        const val MESSAGE_TOAST = 5
        const val MESSAGE_PROGRESS_SENT = 6
        const val MESSAGE_PROGRESS_READ = 7

        // Key names received from the BluetoothChatService Handler
        const val DEVICE_NAME = "device_name"
        const val TOAST = "toast"

        // Intent request codes
        private const val REQUEST_CONNECT_DEVICE_SECURE = 1
        private const val REQUEST_CONNECT_DEVICE_INSECURE = 2
        private const val REQUEST_ENABLE_BT = 3
        private const val REQUEST_FILE_PICK = 4
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (D) Log.e(TAG, "+++ ON CREATE +++")
        // Set up the window layout
        binding = ActChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter.equals(null)) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show()
            finish()
        }

        permissionManager = PermissionManager.getInstance(this)

        //initialize room database to get all his methods
        database = OfflineDatabase.getDatabase(this)


        if (!mBluetoothAdapter.isEnabled) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
            // Otherwise, setup the chat session
        } else {
            // Update the user textview
            //get the bluetooth device from the list actvity intent

            device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

            binding.chatRecyclerView.layoutManager = LinearLayoutManager(
                this, RecyclerView.VERTICAL, false
            )
            chatAdapter = ChatAdapter(this, chatList, device!!.name)
            binding.chatRecyclerView.adapter = chatAdapter
            setupChat(device)
        }


    }

    fun setRecycler(sender: String, receiver: String, msg: String?) {
        val time = getDate(System.currentTimeMillis(), "hh:mm:ss")
        val chatModel =
            chat(0, "", sender, receiver, R.drawable.happyicon, msg!!, time!!, "", false)
        chatList.add(chatModel)
        chatAdapter.notifyItemInserted(chatList.size - 1)
        binding.chatRecyclerView.scrollToPosition(chatList.size - 1)
        insertBlueChat(device!!.name, receiver, msg, time)
    }

    private fun getDate(milliSeconds: Long, dateFormat: String?): String? {
        // Create a DateFormatter object for displaying date in specified format.
        val formatter = SimpleDateFormat(dateFormat, Locale.US)

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        val calendar: Calendar = Calendar.getInstance()
        calendar.timeInMillis = milliSeconds
        return formatter.format(calendar.time)
    }

    public override fun onStart() {
        super.onStart()
        if (D) Log.e(TAG, "++ ON START ++")
        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
            // Otherwise, setup the chat session
        } else {
            if (mChatService.equals(null)) setupChat(device)
        }

    }

    @Synchronized
    public override fun onResume() {
        super.onResume()
        if (D) Log.e(TAG, "+ ON RESUME +")
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (!mChatService.equals(null)) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == ChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start()
            } else {
                Log.d("========", "onResume: state not null")
            }
        }

        binding.iPickFile.setOnClickListener {
            if (!permissionManager.methodCheckPermission(permission1)) {
                permissionManager.methodAskPermission(
                    this, permission1, 100
                )
            } else {
                filePicker()
            }

        }
    }

    private fun setupChat(device: BluetoothDevice?) {
        Log.d(TAG, "setupChat()")

        // Initialize the compose field with a listener for the return key
        binding.mOutEditText.setOnEditorActionListener(mWriteListener)

        // Initialize the send button with a listener that for click events
        binding.mSendButton.setOnClickListener(View.OnClickListener {
            // Send a message using content of the edit text widget
            val message = binding.mOutEditText.text.toString()
            sendMessage(message)

        })
        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = ChatService(this, mHandler)
        // Initialize the buffer for outgoing messages
        mOutStringBuffer = StringBuffer()

        connectDevice(intent, true)

    }

    @Synchronized
    public override fun onPause() {
        super.onPause()
        if (D) Log.e(TAG, "- ON PAUSE -")
        HandleSocket.activityBlueStatus = "pause"
    }

    public override fun onStop() {
        super.onStop()
        if (D) Log.e(TAG, "-- ON STOP --")
        HandleSocket.activityBlueStatus = "stop"
    }

    public override fun onDestroy() {
        super.onDestroy()
        // Stop the Bluetooth chat services
        HandleSocket.activityBlueStatus = "destroy"

        if (mChatService.equals(null)) mChatService.stop()
        isConnected = false
        if (D) Log.e(TAG, "--- ON DESTROY ---")
    }

    private fun ensureDiscoverable() {
        if (D) Log.d(TAG, "ensure discoverable")
        if (mBluetoothAdapter.scanMode != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            startActivity(discoverableIntent)
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private fun sendMessage(message: String) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != ChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show()
            return
        }
        // Check that there's actually something to send
        if (message.isNotEmpty()) {
            // Get the message bytes and tell the BluetoothChatService to write
            val finalMessage = "msg#$message"
            val send = finalMessage.toByteArray()
            //mChatService.write(send)
            mChatService.writeFile("fileURI", send)
            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0)
            binding.mOutEditText.setText(mOutStringBuffer)
        } else {
            send()
        }
    }

    // The action listener for the EditText widget, to listen for the return key
    private val mWriteListener =
        OnEditorActionListener { view, actionId, event -> // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.action == KeyEvent.ACTION_UP) {
                val message = view.text.toString()
                sendMessage(message)
            }
            if (D) Log.i(TAG, "END onEditorAction")
            true
        }

    private fun setStatusSubTitle(resId: Int) {
        val actionBar = supportActionBar
        actionBar!!.setSubtitle(resId)
    }

    private fun setStatusSubTitle(subTitle: CharSequence) {
        val actionBar = supportActionBar
        actionBar!!.subtitle = subTitle
    }

    private fun setStatusTitle(tit: CharSequence) {
        val actionBar = supportActionBar
        actionBar!!.title = tit
    }

    // The Handler that gets information back from the BluetoothChatService
    @SuppressLint("HandlerLeak")
    private val mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MESSAGE_STATE_CHANGE -> {
                    if (D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1)
                    when (msg.arg1) {
                        ChatService.STATE_CONNECTED -> {
                            setStatusTitle(mConnectedDeviceName)
                            setStatusSubTitle("online")
                            //chatList.clear()
                            isConnected = true
                            //insert into  database this device
                            insertBlueUser(
                                device!!.name,
                                device!!.address,
                                device!!,
                                "last ",
                                System.currentTimeMillis()
                            )
                        }

                        ChatService.STATE_CONNECTING -> setStatusSubTitle(R.string.title_connecting)

                        ChatService.STATE_LISTEN -> setStatusSubTitle(
                            "Listening"
                        )

                        ChatService.STATE_NONE -> setStatusSubTitle(
                            R.string.title_not_connected
                        )
                    }
                }

                //this will show send message
                MESSAGE_WRITE -> {
                    val writeBuf = msg.obj as ByteArray
                    // construct a string from the buffer
                    val writeMessage = writeBuf.toString(Charsets.UTF_8)
                    Log.d("--bluetooth Msg Write", "handler write: $writeMessage")
                    setRecycler(mConnectedDeviceName, "me", writeMessage)

                }

                //this will show received message
                MESSAGE_READ -> {
                    val readBuf = msg.obj as ByteArray
                    // construct a string from the valid bytes in the buffer
                    val readMessage = readBuf.toString(Charsets.UTF_8)
                    Log.d("--bluetooth Msg Read", "handleMess read: $readMessage")
                    setRecycler("me", mConnectedDeviceName, readMessage)
                }

                MESSAGE_PROGRESS_SENT -> {
                    val readBuff = msg.obj as Int

                    binding.progressFileSent.progress = readBuff
                    binding.tvFileSent.text = readBuff.toString()

                    Log.d("''''''progress''''''", "send Progress: $readBuff")
                    if (readBuff > 98) {
                        binding.layBottom.visibility = View.VISIBLE
                        binding.layoutFile.visibility = View.GONE
                    }
                }

                MESSAGE_PROGRESS_READ -> {
                    val readBuff = msg.obj as Int

                    binding.progressFileSent.progress = readBuff
                    binding.tvFileSent.text = readBuff.toString()

                    Log.d("''''''progress''''''", "read Progress: $readBuff")

                    if (readBuff > 98) {
                        binding.layBottom.visibility = View.VISIBLE
                        binding.layoutFile.visibility = View.GONE
                    }
                }

                MESSAGE_DEVICE_NAME -> {
                    // save the connected device's name
                    mConnectedDeviceName = msg.data.getString(DEVICE_NAME).toString()
                    Toast.makeText(
                        applicationContext,
                        "Connected to $mConnectedDeviceName",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                MESSAGE_TOAST -> Toast.makeText(
                    applicationContext, msg.data.getString(TOAST), Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (D) Log.d(TAG, "onActivityResult $resultCode")
        when (requestCode) {
            REQUEST_CONNECT_DEVICE_SECURE ->                 // When DeviceListActivity returns with a device to connect
                if (resultCode == RESULT_OK) {
                    connectDevice(data, true)
                }

            REQUEST_CONNECT_DEVICE_INSECURE ->                 // When DeviceListActivity returns with a device to connect
                if (resultCode == RESULT_OK) {
                    connectDevice(data, false)
                }

            REQUEST_ENABLE_BT ->                 // When the request to enable Bluetooth returns
                if (resultCode == RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat(device)
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled")
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show()
                    finish()
                }

            REQUEST_FILE_PICK -> {
                if (resultCode == RESULT_OK) {
                    val selectedFile = data?.data //The uri with a location of the file

                    // Helper method, refer to FilePickerHelper getPath() method
                    val selectedFilePath = FilePickerHelper.getPath(this, selectedFile!!)

                    // Update the global variable of the fileURI with the actual URI
                    fileURI = selectedFilePath!!

                } else if (resultCode == RESULT_CANCELED) {
                    toast("File choosing cancelled")
                } else {
                    toast("Error with choosing this file")
                }
            }
        }
    }

    private fun fileProgressWriting(fileName: String, fileSize: String) {
        binding.layBottom.visibility = View.GONE
        binding.layoutFile.visibility = View.VISIBLE


        binding.tvFileName.text = fileName
        binding.tvFileTotalSize.text = fileSize
    }

    private fun fileProgressReading(fileName: String, fileSize: String) {
        binding.layBottom.visibility = View.GONE
        binding.layoutFile.visibility = View.VISIBLE

        //binding.progressFileSent.progress=progressReading
        //binding.tvFileSent.text=progressReading.toString()
        binding.tvFileName.text = fileName
        binding.tvFileTotalSize.text = fileSize
    }

    private fun connectDevice(data: Intent?, secure: Boolean) {
        // Get the device MAC address
        val address = data!!.extras!!.getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS)
        // Get the BluetoothDevice object
        val device = mBluetoothAdapter.getRemoteDevice(address)

        Log.d(
            "----connect device",
            "connect method: ${device.name + "\n" + device.address + " --" + device.bondState} "
        )

        // Attempt to connect to the device
        if (device!!.bondState == 10) {
            //means this is non bonded device
            if (createBond(device)) {
                mChatService.connect(device, secure)
            }

        } else {
            mChatService.connect(device, secure)
        }


    }

    @Throws(Exception::class)
    fun createBond(btDevice: BluetoothDevice?): Boolean {
        Log.d(TAG, "createBond: ${device!!.name}")
        val class1 = Class.forName("android.bluetooth.BluetoothDevice")
        val createBondMethod = class1.getMethod("createBond")
        return createBondMethod.invoke(btDevice) as Boolean
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.bluetooth_chat, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val serverIntent: Intent?
        if (item.itemId == R.id.secure_connect_scan) {
            if (mChatService.getState() != ChatService.STATE_CONNECTED) {
                Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show()
            } else {
                // Launch the DeviceListActivity to see devices and do scan
                serverIntent = Intent(this, DeviceListActivity::class.java)
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE)
            }

            return true
        }

        if (item.itemId == R.id.disconnect) {
            /// Ensure this device is discoverable by others
            ensureDiscoverable()
            return true
        }
        return false
    }

    private fun filePicker() {
        val mimeTypes: Array<String> = arrayOf("image/*", "video/*", "application/pdf", "audio/*")

        // create the intent to open file picker, add the desired file types to our picker and select the option that
        // the files be openable on the phone
        val intent = Intent().setType("*/*")
            .setAction(Intent.ACTION_GET_CONTENT) // ACTION_GET_CONTENT refers to the built-in file picker
            .addCategory(Intent.CATEGORY_OPENABLE)
            .putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)

        // Open file picker, and add a requestCode to refer to correct Activity result in startActivityForResult()
        startActivityForResult(Intent.createChooser(intent, "Choose a file"), REQUEST_FILE_PICK)
    }

    /**
     * Opens a dialog to confirm with the user that they wish to send the file. This helps the user double check what
     * they have selected before sending. This dialog also serves as a preventive measure against accidental sending.
     *
     * @return void
     */
    private fun send() {
        if (fileURI == "") {
            toast("Please choose a file first")
        } else {
            // Double check with user
            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder.setTitle("Sending Confirmation")
            alertDialogBuilder.setMessage("Are you sure you wish to send this file?")
            alertDialogBuilder.setPositiveButton(R.string.send) { _, _ -> checkLessThan5MB(fileURI) }
            alertDialogBuilder.setNegativeButton("Cancel") { _, _ -> toast("File sending cancelled") }
            alertDialogBuilder.show()
        }
    }

    /**
     * Before starting SendActivity, this method checks the file size to be sure that it is less than 5MB. It is
     * calculated by getting the number of bytes in a Kilobyte (1024 B / 1 KB) * (1024 KB / 1 MB) to convert to 1 MB
     * and finally multiply by 5 to get the number of bytes in 5MB.
     *
     * This value is then compared with the number of bytes in the file of interest, if it is less than 5MB, it is okay
     * to send. If it is greater than 5MB a dialog is displayed and sending is cancelled.
     *
     * @return void
     */
    private fun checkLessThan5MB(fileURI: String) {
        // Create File object to convert to bytes
        val file = File(fileURI)

        // Check with 5 MB Limit
        if (file.readBytes().size > (1024 * 1024 * 5)) {
            // Dialog the user and send back to file picker activity
            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder.setTitle("File too large")
            alertDialogBuilder.setMessage("This file is larger than the 5MB Limit")
            alertDialogBuilder.setPositiveButton("OK") { _, _ ->
                toast("File sending failed")
            }
            alertDialogBuilder.show()
        } else {
            // Start the bluetooth client to send file
            CoroutineScope(Dispatchers.IO).launch {

                val fileSize = File(fileURI).length()

                val size = Formatter.formatFileSize(this@ChatActivity, fileSize)

                val finalMessage = "file#$fileURI#$size"
                val send = finalMessage.toByteArray()
                Log.d("---send file detail", "checkLessThan5MB: $finalMessage")
                //now send file
                mChatService.writeFile(fileURI, send)

                runOnUiThread {
                    mOutStringBuffer.setLength(0)
                    binding.mOutEditText.setText(mOutStringBuffer)
                }

            }
        }
    }

    fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val mDialog = MaterialAlertDialogBuilder(this)
        mDialog.setPositiveButton("Close") { dialogInterface, _ ->
            dialogInterface.dismiss()
            super.onBackPressed()
        }.setNegativeButton("Cancel") { dialogInterface, _ ->
            dialogInterface.dismiss()
        }.setMessage("You chat session will be disconnected")
            .setTitle("Exit Chat").create()
        mDialog.show()
    }

    fun insertBlueUser(
        sender: String,
        address: String,
        device: BluetoothDevice,
        msg: String,
        lastSeen: Long
    ) {
        val oirModel = Users(0, sender, msg, address, device, R.drawable.happyicon, lastSeen)

        CoroutineScope(Dispatchers.IO).launch{
            database!!.insertDao().insertUser(oirModel)
            Log.d(TAG, "insertSqliteUser: user is inserted into database")
        }
    }

    fun getDbChats() {
        CoroutineScope(Dispatchers.IO).launch {
            val list = database!!.insertDao().getAllChats()
            chatList = ArrayList(list)
        }
    }

    private fun insertBlueChat(sender: String, receiver: String, msg: String, time: String) {
        val oirModel = chat(
            0, "bluetooth", sender, receiver, R.drawable.happyicon, msg, time, "", true
        )

        CoroutineScope(Dispatchers.IO).launch {
            database!!.insertDao().insertChats(oirModel)
        }
    }

    fun deleteRoomData() {
        CoroutineScope(Dispatchers.IO).launch {
            database!!.clearAllTables()
        }
    }
}