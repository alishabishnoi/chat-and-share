package com.chatapp.bluetooth

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.text.format.Formatter
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.chatapp.Constants
import com.chatapp.DirectReplyReceiver
import com.chatapp.R
import com.chatapp.wifichat.HandleSocket
import com.chatapp.wifichat.WifiChatActivity
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.UUID

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
@SuppressLint("MissingPermission")
class ChatService(val context: Context?, private val mHandler: Handler) {
    // Member fields
    private val mAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var mSecureAcceptThread: AcceptThread? = null
    private var mInsecureAcceptThread: AcceptThread? = null
    private var mConnectThread: ConnectThread? = null
    private var mConnectedThread: ConnectedThread? = null

    companion object {
        // Debugging
        private const val TAG = "BluetoothChatService"

        // Name for the SDP record when creating server socket
        private const val NAME_SECURE = "BluetoothChatSecure"
        private const val NAME_INSECURE = "BluetoothChatInsecure"

        // Unique UUID for this application
        private val MY_UUID_SECURE = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
        private val MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")

        // Constants that indicate the current connection state
        const val STATE_NONE = 0 // we're doing nothing
        const val STATE_LISTEN = 1 // now listening for incoming connections
        const val STATE_CONNECTING = 2 // now initiating an outgoing connection
        const val STATE_CONNECTED = 3 // now connected to a remote device


    }

    private var mstate = 0
    private var mNewState = 0


    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param context The UI Activity Context
     * @param handler A Handler to send messages back to the UI Activity
     */
    init {
        mstate = STATE_NONE
        mNewState = mstate
    }

    /**
     * Return the current connection state.
     */
    @Synchronized
    fun getState(): Int {
        return mstate
    }

    /**
     * Update UI title according to the current state of the chat connection
     */
    @Synchronized
    private fun updateUserInterfaceTitle() {
        Log.d(TAG, "updateUserInterfaceTitle() $mNewState -> $mstate")
        mNewState = mstate
        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, mNewState, -1).sendToTarget()
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    @Synchronized
    fun start() {
        Log.d(TAG, "start")
        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }
        // Start the thread to listen on a BluetoothServerSocket
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = AcceptThread(true)
            mSecureAcceptThread!!.start()
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = AcceptThread(false)
            mInsecureAcceptThread!!.start()
        }
        // Update UI title
        updateUserInterfaceTitle()
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    @Synchronized
    fun connect(device: BluetoothDevice, secure: Boolean) {
        Log.d(TAG, "connect to: $device")
        // Cancel any thread attempting to make a connection
        if (mstate == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread!!.cancel()
                mConnectThread = null
            }
        }
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }
        // Start the thread to connect with the given device
        mConnectThread = ConnectThread(device, secure)
        mConnectThread!!.start()
        // Update UI title
        updateUserInterfaceTitle()
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    @Synchronized
    fun connected(socket: BluetoothSocket?, device: BluetoothDevice, socketType: String) {
        Log.d(
            TAG, "connected, Socket Type:$socketType"
        )
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }
        // Cancel the accept thread because we only want to connect to one device
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread!!.cancel()
            mSecureAcceptThread = null
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread!!.cancel()
            mInsecureAcceptThread = null
        }
        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = ConnectedThread(socket!!)
        mConnectedThread!!.start()

        // Send the name of the connected device back to the UI Activity
        val msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME)
        val bundle = Bundle()
        bundle.putString(Constants.DEVICE_NAME, device.name)
        msg.data = bundle
        mHandler.sendMessage(msg)
        // Update UI title
        updateUserInterfaceTitle()
    }

    /**
     * Stop all threads
     */
    @Synchronized
    fun stop() {
        Log.d(TAG, "stop")
        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread!!.cancel()
            mSecureAcceptThread = null
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread!!.cancel()
            mInsecureAcceptThread = null
        }
        mstate = STATE_NONE
        // Update UI title
        updateUserInterfaceTitle()
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread.writeFile
     */
    fun writeFile(fileUri: String, buffer: ByteArray?) {
        // Create temporary object
        var r: ConnectedThread?
        // Synchronize a copy of the ConnectedThread
        synchronized(this) {
            if (mstate != STATE_CONNECTED) return
            r = mConnectedThread
        }
        // Perform the write unsynchronized
        r!!.writeFile(fileUri, buffer)
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private fun connectionFailed() {
        // Send a failure message back to the Activity
        val msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putString(Constants.TOAST, "Unable to connect device")
        msg.data = bundle
        mHandler.sendMessage(msg)
        mstate = STATE_NONE
        // Update UI title
        updateUserInterfaceTitle()
        // Start the service over to restart listening mode
        start()
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private fun connectionLost() {
        // Send a failure message back to the Activity
        val msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putString(Constants.TOAST, "Device connection was lost")
        msg.data = bundle
        mHandler.sendMessage(msg)
        mstate = STATE_NONE
        // Update UI title
        updateUserInterfaceTitle()
        // Start the service over to restart listening mode
        start()
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private inner class AcceptThread(secure: Boolean) : Thread() {
        // The local server socket
        private val mmServerSocket: BluetoothServerSocket?
        private val mSocketType: String

        //this is constructor of this class
        init {
            var tmp: BluetoothServerSocket? = null
            mSocketType = if (secure) "Secure" else "Insecure"
            // Create a new listening server socket
            try {
                tmp = if (secure) {
                    mAdapter.listenUsingRfcommWithServiceRecord(
                        NAME_SECURE, MY_UUID_SECURE
                    )
                } else {
                    mAdapter.listenUsingInsecureRfcommWithServiceRecord(
                        NAME_INSECURE, MY_UUID_INSECURE
                    )
                }
            } catch (e: IOException) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e)
            }
            mmServerSocket = tmp
            mstate = STATE_LISTEN
        }

        //default method of Thread class
        override fun run() {
            Log.d(
                TAG, "Socket Type: " + mSocketType + "BEGIN mAcceptThread" + this
            )
            name = "AcceptThread$mSocketType"
            var socket: BluetoothSocket? = null

            // Listen to the server socket if we're not connected
            while (mstate != STATE_CONNECTED) {
                socket = try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    mmServerSocket!!.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e)
                    break
                }
                // If a connection was accepted
                if (socket != null) {
                    synchronized(this) {
                        when (mstate) {
                            STATE_LISTEN, STATE_CONNECTING ->                                 // Situation normal. Start the connected thread.
                                connected(
                                    socket, socket.remoteDevice, mSocketType
                                )

                            STATE_NONE, STATE_CONNECTED ->                                 // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close()
                                } catch (e: IOException) {
                                    Log.e(
                                        TAG, "Could not close unwanted socket", e
                                    )
                                }

                            else -> {}
                        }
                    }
                }
            }
            Log.i(
                TAG, "END mAcceptThread, socket Type: $mSocketType"
            )
        }

        fun cancel() {
            Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this)
            try {
                mmServerSocket!!.close()
            } catch (e: IOException) {
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e)
            }
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private inner class ConnectThread(private val mmDevice: BluetoothDevice, secure: Boolean) :
        Thread() {
        private val mmSocket: BluetoothSocket?
        private val mSocketType: String

        init {
            var tmp: BluetoothSocket? = null
            mSocketType = if (secure) "Secure" else "Insecure"
            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = if (secure) {
                    mmDevice.createRfcommSocketToServiceRecord(
                        MY_UUID_SECURE
                    )
                } else {
                    mmDevice.createInsecureRfcommSocketToServiceRecord(
                        MY_UUID_INSECURE
                    )
                }
            } catch (e: IOException) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e)
            }
            mmSocket = tmp
            mstate = STATE_CONNECTING
        }

        override fun run() {
            Log.i(
                TAG, "BEGIN mConnectThread SocketType:$mSocketType"
            )
            name = "ConnectThread$mSocketType"
            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery()
            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket!!.connect()
            } catch (e: IOException) {
                // Close the socket
                try {
                    mmSocket!!.close()
                } catch (e2: IOException) {
                    Log.e(
                        TAG,
                        "unable to close() " + mSocketType + " socket during connection failure",
                        e2
                    )
                }
                connectionFailed()
                return
            }
            // Reset the ConnectThread because we're done
            synchronized(this) { mConnectThread = null }
            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType)
        }

        fun cancel() {
            try {
                mmSocket!!.close()
            } catch (e: IOException) {
                Log.e(
                    TAG, "close() of connect $mSocketType socket failed", e
                )
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private inner class ConnectedThread(
        private val mmSocket: BluetoothSocket
    ) : Thread() {
        private var mmInStream: InputStream? = null
        private var mmOutStream: OutputStream? = null

        init {
            // Get the BluetoothSocket input and output streams
            try {
                mmInStream = mmSocket.inputStream
                mmOutStream = mmSocket.outputStream
            } catch (e: IOException) {
                Log.e(TAG, "temp sockets not created", e)
            }
            mstate = STATE_CONNECTED
        }

        override fun run() {
            Log.i(TAG, "BEGIN mConnectedThread")

            // Keep listening to the InputStream while connected
            while (mstate == STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    Log.d("**chat service", "reading 1st")

                    //now get anything from the stream
                    getFile(mmInStream!!)

                } catch (e: IOException) {
                    Log.e(TAG, "disconnected", e)
                    connectionLost()
                    break
                }
            }
            Log.d("-----disconnect", "run: dissconnect")
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        fun writeFile(fileUri: String, buffer: ByteArray?) {
            Log.d("chat service writing file", "write file: called")
            try {
                putFile(fileUri,mmOutStream!!,buffer)
            } catch (e: IOException) {
                Log.e(TAG, "Exception during write", e)
            }
        }

        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, "close() of connect socket failed", e)
            }
        }
    }

    /*
    * this method will do two work
    * 1. if user have not selected any file then it will send message
    * 2. if user have selected any file then it will send file and its all detail
    * */
    fun putFile(fileUri: String, outputStream: OutputStream, messageByteArray: ByteArray?) {
        //first get the file size if it 0 then send message
        val selectedFileSize = File(fileUri).length().toInt()

        mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, messageByteArray).sendToTarget()

        val message = messageByteArray!!.toString(Charsets.UTF_8)
        val prefix = message.split("#")
        val name = prefix[1].split("/")

        Log.d(
            "** put file method",
            "sending - size-$selectedFileSize: message ${messageByteArray.toString(Charsets.UTF_8)}"
        )

        val fileBytes: ByteArray
        // Int -> 4 bytes required to represent it
        val fileNameSize = ByteBuffer.allocate(4)
        val fileSize = ByteBuffer.allocate(4)

        fileNameSize.putInt(messageByteArray.size) // Get number of bytes and put that number in

        outputStream.write(fileNameSize.array())    // The int containing number of bytes in the file name string
        //outputStream.write(file.name.toByteArray()) // The file name string turned to bytes
        outputStream.write(messageByteArray) // The file name string turned to bytes

        //now if we have selected file then we will add extra details in output stream otherwise not
        if (selectedFileSize != 0) {
            val buffer = ByteArray(1024)
            // Create a File object containing the file contents, and convert into byte array
            val file = File(fileUri)

            fileBytes = ByteArray(file.length().toInt())
            // Same thing for the size of the actual file, put the size in an int, represented in 4 bytes
            fileSize.putInt(fileBytes.size)

            outputStream.write(fileSize.array())        // The int containing number of bytes in the actual file

            val bis = BufferedInputStream(FileInputStream(file))

            //runOnUiThread {fileProgressWriting(name[name.size - 1], prefix[2]) }


            var bytesRead: Int
            var totalBytesRead = 0
            bytesRead = bis.read(buffer, 0, buffer.size)
            while (bytesRead != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                if (totalBytesRead == fileBytes.size) {
                    break
                }

                bytesRead = bis.read(buffer, 0, buffer.size)
                val progressWriting = (totalBytesRead.toFloat() / fileBytes.size.toFloat() * 100).toInt()
                Log.d("** file writing", "writing $progressWriting")


                //mHandler.obtainMessage(Constants.MESSAGE_PROGRESS_READ, -1, -1, progressWriting).sendToTarget()

                /*runOnUiThread {
                    binding.progressFileSent.progress = progressWriting
                    binding.tvFileSent.text = progressWriting.toString()
                }*/
            }

            bis.close()

            Log.d("** file sent", "sent details: ${messageByteArray.toString(Charsets.UTF_8)}")

            /*runOnUiThread {
                binding.layBottom.visibility = View.VISIBLE
                binding.layoutFile.visibility = View.GONE
            }*/

            //set it zero so that next time it will not read it
            //fileUri = ""

            // If no exception has been thrown, then successful sending

            //Thread.sleep(5000)// Add delay of 5 seconds before closing the socket, this
        }

    }



    /*
    * this method we are reading the input stream
    * 1. we will check the incoming message - if that contain "msg" means there in no file otherwise
    * 2. we will get "file" so we will also read other details of file from the stream
    * */
    fun getFile(inputStream: InputStream) {
        if (FileHelper.isExternalStorageWritable()) {
            val totalFileNameSizeInBytes: Int
            val totalFileSizeInBytes: Int

            // File name string size or say message string size
            val fileNameSizeBuffer =
                ByteArray(4) // Only 4 bytes needed for this operation, int => 4 bytes
            inputStream.read(fileNameSizeBuffer, 0, 4)
            var fileSizeBuffer = ByteBuffer.wrap(fileNameSizeBuffer)
            totalFileNameSizeInBytes = fileSizeBuffer.int

            var prefix = listOf<String>()
            var fileName = ""

            var name = listOf<String>()

            //first check if we are sending something from there
            if (totalFileNameSizeInBytes > 0) {
                // Actual String of file name or say our message sent from other side
                val fileNameBuffer = ByteArray(1024)
                inputStream.read(fileNameBuffer, 0, totalFileNameSizeInBytes)
                fileName = String(fileNameBuffer, 0, totalFileNameSizeInBytes)
                prefix = fileName.split("#")
                name = prefix[1].split("/")

                Log.d("** reading message", fileName)
            }

            //if there is a file then only we will read next otherwise means it was a message
            if (prefix[0] == "file") {
                // File size integer bytes
                val fileSizebuffer = ByteArray(4) // int => 4 bytes
                inputStream.read(fileSizebuffer, 0, 4)
                fileSizeBuffer = ByteBuffer.wrap(fileSizebuffer)
                totalFileSizeInBytes = fileSizeBuffer.int

                //runOnUiThread { fileProgressReading(name[name.size - 1], prefix[2]) }


                // The actual file bytes
                val baos = ByteArrayOutputStream()  // this will write file to our storage
                val buffer = ByteArray(1024)
                var read: Int
                var totalBytesRead = 0
                read = inputStream.read(buffer, 0, buffer.size)
                while (read != -1) {
                    baos.write(buffer, 0, read)
                    totalBytesRead += read
                    if (totalBytesRead == totalFileSizeInBytes) {
                        break
                    }
                    read = inputStream.read(buffer, 0, buffer.size)
                    val progressReading = (totalBytesRead.toFloat() / totalFileSizeInBytes.toFloat() * 100).toInt()
                    Log.d("** file reading", "reading $progressReading")

                    //mHandler.obtainMessage(Constants.MESSAGE_PROGRESS_READ, totalFileNameSizeInBytes, -1, progressReading).sendToTarget()

                    /*runOnUiThread {
                        binding.progressFileSent.progress = progressReading
                        binding.tvFileSent.text = progressReading.toString()
                    }*/
                }
                baos.flush()

                /*val saveFile = FileHelper.getPublicStorageDir(fileName)
                if (saveFile.exists()) {
                    saveFile.delete()
                }*/

                WifiChatActivity.fileCreated = File(context!!.getExternalFilesDir("received"), name[name.size - 1])
                val fos = FileOutputStream(WifiChatActivity.fileCreated.path)
                fos.write(baos.toByteArray())
                fos.close()

                Log.d(TAG, "file saved ${WifiChatActivity.fileCreated.path}")
                //runOnUiThread { toast("File Received !") }

                val finalMessage = "file#${WifiChatActivity.fileCreated.path}#${
                    Formatter.formatFileSize(
                        context, WifiChatActivity.fileCreated.length()
                    )
                }"
                val send = finalMessage.toByteArray()

                // Send the obtained bytes to the UI Activity
                mHandler.obtainMessage(Constants.MESSAGE_READ, totalFileNameSizeInBytes, -1, send).sendToTarget()

                /*runOnUiThread {
                    binding.layBottom.visibility = View.VISIBLE
                    binding.layoutFile.visibility = View.GONE
                }*/


                //Thread.sleep(5000)
            } else {
                Log.d("** reading file", "it was a message")
                // Send the obtained bytes to the UI Activity
                if (HandleSocket.activityBlueStatus == "pause" || HandleSocket.activityBlueStatus == "stop") {
                    sendChannel1Notification(context, prefix[1])
                } else {
                    mHandler.obtainMessage(
                        Constants.MESSAGE_READ, totalFileNameSizeInBytes, -1, fileName.toByteArray()
                    ).sendToTarget()
                }

            }


        }
    }

    @SuppressLint("MissingPermission")
    fun sendChannel1Notification(context: Context?, message: String) {
        val activityIntent = Intent(context, WifiChatActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            context, 0, activityIntent, PendingIntent.FLAG_MUTABLE
        )
        val remoteInput = RemoteInput.Builder("key_text_reply")
            .setLabel("Your answer...")
            .build()
        val replyPendingIntent: PendingIntent?
        val replyIntent = Intent(context, DirectReplyReceiver::class.java)
        replyIntent.putExtra("reply", message)
        replyPendingIntent = PendingIntent.getBroadcast(
            context,
            0, replyIntent, PendingIntent.FLAG_MUTABLE
        )
        val replyAction: NotificationCompat.Action = NotificationCompat.Action.Builder(
            R.drawable.ic_send,
            "Reply",
            replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        /*val messagingStyle = NotificationCompat.MessagingStyle("Me")
        messagingStyle.conversationTitle = "Group Chat"

        for (chatMessage in MESSAGES) {
            val notificationMessage = NotificationCompat.MessagingStyle.Message(
                chatMessage.text,
                chatMessage.timestamp,
                chatMessage.sender
            )
            messagingStyle.addMessage(notificationMessage)
        }*/

        val person = Person.Builder().setName("sender").build()
        val notificationStyle = NotificationCompat.MessagingStyle(person)
            .addMessage(message, System.currentTimeMillis(), person)

        val notification: Notification = NotificationCompat.Builder(context!!, "channel1")
            .setSmallIcon(R.drawable.app_icon)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_bluetooth))
            //.setStyle(messagingStyle)
            .setStyle(notificationStyle)
            .addAction(replyAction)
            .setColor(Color.BLUE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        val notificationManager = NotificationManagerCompat.from(context)
        //we need to change this id for different notification
        notificationManager.notify(2, notification)
    }


}