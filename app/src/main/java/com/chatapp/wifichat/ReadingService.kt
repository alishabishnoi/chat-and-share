package com.chatapp.wifichat

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager.WakeLock
import android.os.ResultReceiver
import android.text.format.Formatter
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.chatapp.DirectReplyReceiver
import com.chatapp.Message
import com.chatapp.R
import com.chatapp.bluetooth.FileHelper
import com.chatapp.walkie.SocketHandler
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors


class ReadingService : Service() {
    private val TAG = "ReadingService"
    private var serviceEnabled = true
    private var message = 1
    private var progress = 2
    private var serverResult: ResultReceiver? = null

    private var wakeLock: WakeLock? = null

    var CHANNEL_1_ID = "channel1"
    val ACTION_TOGGLE = "toogle"
    val ACTION = "android.intent.action.SmsReceiver"

    var MESSAGES: ArrayList<Message> = ArrayList()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        /*val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ExampleApp:Wakelock"
        )
        wakeLock!!.acquire(10 * 60 * 1000L *//*10 minutes*//*)*/

        MESSAGES.add(Message("Hello", "me"))
        MESSAGES.add(Message("Hi!", "Jenny"))

        Log.d(TAG, "Wakelock acquired")

        val broadcastIntent = Intent(this, ReadingService::class.java)
        broadcastIntent.action = ACTION_TOGGLE
        val actionIntent = PendingIntent.getService(
            this,
            0, broadcastIntent, PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_1_ID)
            .setContentTitle("Chat Service")
            .setContentText("Running...")
            .setSmallIcon(R.drawable.app_icon)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.app_icon))
            .addAction(R.drawable.app_icon, "Stop Chat", actionIntent)
            .build()
        startForeground(1, notification)
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serverResult = intent!!.extras!!["clientResult"] as ResultReceiver?
        startStreaming()
        /*if (intent!!.action == ACTION_TOGGLE) {
            stopForeground(true)
            stopSelf()
            this.stopService(intent)
            Toast.makeText(this, "Chat Stopped", Toast.LENGTH_SHORT).show()
            //send a broadcast to activity that service is stopped
            val intentStop = Intent(ACTION)
            intentStop.putExtra("stop", "stop")
            LocalBroadcastManager.getInstance(this).sendBroadcast(intentStop)
        } else {
            serverResult = intent.extras!!["clientResult"] as ResultReceiver?
        }*/
        //return START_NOT_STICKY
        return super.onStartCommand(intent, flags, startId)
    }

    fun startStreaming() {
        Log.d(TAG, "onStartCommand: getting file")

        /*val rThread = readThread()
        rThread.start()*/

        //run on bg thread
        val executorService = Executors.newSingleThreadExecutor()
        //val handler = Handler(Looper.getMainLooper())
        executorService.execute {
            Log.d(TAG, "executor: ")
            //val inputStream = HandleSocket.socket!!.getInputStream()
            val inputStream = SocketHandler.socket!!.getInputStream()
            val buffer = ByteArray(1024)
            //var bytes_read=inputStream!!.read(buffer,0,buffer.size)
            while (serviceEnabled) {
                Log.d(TAG, "while: ")
                // Read from the InputStream
                getFile(inputStream)
                //inputStream = socket!!.getInputStream()
                //bytes_read=inputStream!!.read(buffer,0,buffer.size)
            }
        }

        /*val runnable = Runnable {
            try {


            } catch (e: IOException) {
                e.printStackTrace()
                sendActivityMessage(e.message)
            }


        }
        Thread(runnable).start()*/
    }

    private inner class readThread : Thread() {
        override fun run() {
            try {
                //inputStream = socket!!.getInputStream()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            //run on bg thread
            val executorService = Executors.newSingleThreadExecutor()
            //val handler = Handler(Looper.getMainLooper())
            executorService.execute {
                Log.d(TAG, "executor: ")
                /*while (socket != null) {
                    Log.d(TAG, "while: ")
                    try {
                        // Read from the InputStream
                        getFile(inputStream!!)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }*/
            }
        }


    }

    private fun sendActivityMessage(message1: String?) {
        val b = Bundle()
        b.putString("message", message1)
        serverResult!!.send(message, b)
    }

    fun sendActivityProgress(progress1: String?) {
        val b = Bundle()
        b.putString("progress", progress1)
        serverResult!!.send(progress, b)
    }


    override fun onDestroy() {
        serviceEnabled = false

        //releaseCPU()

        //Signal that the service was stopped
        //serverResult.send(port, new Bundle());
        stopSelf()
        sendActivityMessage("serviceStoppedByOther")
    }

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }


    /*@Deprecated("Deprecated in Java")
    override fun onHandleIntent(intent: Intent?) {
        *//*if (intent!!.action== ACTION_TOGGLE){
            stopForeground(true)
            stopSelf()
            this.stopService(intent)
            Toast.makeText(this, "Chat Stopped", Toast.LENGTH_SHORT).show()
            //send a broadcast to activity that service is stopped
            val intentStop=Intent(ACTION)
            intentStop.putExtra("stop","stop")
            LocalBroadcastManager.getInstance(this).sendBroadcast(intentStop)
        }*//*
        try {
            serverResult = intent!!.extras!!["clientResult"] as ResultReceiver?

            inputStream = socket!!.getInputStream()

            Log.d(TAG, "executor: ")
            while (socket != null) {
                Log.d(TAG, "while: ")
                try {
                    // Read from the InputStream
                    getFile(inputStream!!)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        } catch (e: IOException) {
            Log.d(TAG, "onStartCommand: io exception ${e.message}")
            sendActivityMessage(e.message)
        } catch (e: Exception) {
            Log.d(TAG, "onStartCommand:exception ${e.message}")
            sendActivityMessage(e.message)
        }
    }*/

    fun releaseCPU() {
        if (wakeLock != null) {
            if (wakeLock!!.isHeld) {
                wakeLock!!.release()
            }
        }

    }

    /*
    * this method we are reading the input stream
    * 1. we will check the incoming message - if that contain "msg" means there in no file otherwise
    * 2. we will get "file" so we will also read other details of file from the stream
    * */
    fun getFile(inputStream: InputStream) {

        /*val size = inputStream.read()
        if (size == 0) {
            Log.d(TAG, "input stream 0")
        } else {
            Log.d(TAG, "input stream--- ${size}")
        }*/

        if (FileHelper.isExternalStorageWritable()) {
            val totalFileNameSizeInBytes: Int
            val totalFileSizeInBytes: Int

            // File name string size or say message string size
            val fileNameSizeBuffer =
                ByteArray(4) // Only 4 bytes needed for this operation, int => 4 bytes
            Log.d(TAG, "getFile: started")
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

                Log.d("** reading message 2", fileName)

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
                        val progressReading =
                            (totalBytesRead.toFloat() / totalFileSizeInBytes.toFloat() * 100).toInt()
                        Log.d("** file reading", "reading $progressReading")

                        //sendActivityProgress(progressReading.toString())

                        /*runOnUiThread {
                            binding.progressFileSent.progress = progressReading
                            binding.tvFileSent.text = progressReading.toString()
                        }*/
                    }
                    baos.flush()

                    val saveFile = FileHelper.getPublicStorageDir(fileName)
                    if (saveFile.exists()) {
                        saveFile.delete()
                    }

                    WifiChatActivity.fileCreated =
                        File(this.getExternalFilesDir("received"), name[name.size - 1])
                    val fos = FileOutputStream(WifiChatActivity.fileCreated.path)
                    fos.write(baos.toByteArray())
                    fos.close()

                    //Log.d(TAG, "file saved ${WifiP2P.fileCreated.path}")
                    //runOnUiThread { toast("File Received !") }

                    val finalMessage = "file#${WifiChatActivity.fileCreated.path}#${
                        Formatter.formatFileSize(
                            this, WifiChatActivity.fileCreated.length()
                        )
                    }"
                    val send = finalMessage.toByteArray()

                    sendActivityMessage(finalMessage)

                    // Send the obtained bytes to the UI Activity
                    if (HandleSocket.activityStatus == "pause" || HandleSocket.activityStatus == "stop") {
                        sendChannel1Notification(this, finalMessage)
                    }

                    //handler.obtainMessage(WifiP2P.STATE_MESSAGE_RECEIVED, totalFileNameSizeInBytes, -1, send).sendToTarget()

                    /*runOnUiThread {
                        binding.layBottom.visibility = View.VISIBLE
                        binding.layoutFile.visibility = View.GONE
                    }*/


                    //Thread.sleep(5000)
                } else {
                    Log.d("** reading file", "it was a message")
                    // Send the obtained bytes to the UI Activity
                    if (HandleSocket.activityStatus == "pause" || HandleSocket.activityStatus == "stop") {
                        sendChannel1Notification(this, fileName)
                    } else {
                        sendActivityMessage(fileName)
                    }
                    //handler.obtainMessage(WifiP2P.STATE_MESSAGE_RECEIVED, totalFileNameSizeInBytes, -1, fileName.toByteArray()).sendToTarget()
                }
            } else {
                Log.d("** reading message1 ", "   ")
            }


        } else {
            Log.d(TAG, "getFile: no writeable")
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

        val notification: Notification = NotificationCompat.Builder(context!!, CHANNEL_1_ID)
            .setSmallIcon(R.drawable.app_icon)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.app_icon))
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