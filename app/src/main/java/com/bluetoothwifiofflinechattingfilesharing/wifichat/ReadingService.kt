package com.bluetoothwifiofflinechattingfilesharing.wifichat

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
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.os.ResultReceiver
import android.text.format.Formatter
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bluetoothwifiofflinechattingfilesharing.Message
import com.bluetoothwifiofflinechattingfilesharing.R
import com.bluetoothwifiofflinechattingfilesharing.bluetooth.FileHelper
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
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
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ExampleApp:Wakelock"
        )
        wakeLock!!.acquire(10 * 60 * 1000L )

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
        if (intent!!.action == ACTION_TOGGLE) {
            Log.d(TAG, "on stop Command: from chat activity on destroy")
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
            startStreaming()
        }
        return START_NOT_STICKY
        //return super.onStartCommand(intent, flags, startId)
    }

    private fun startStreaming() {
        Log.d(TAG, "onStartCommand: getting file")


        //run on bg thread
        val executorService = Executors.newSingleThreadExecutor()
        executorService.execute {
            Log.d(TAG, "executor: ")
            val inputStream = HandleSocket.socket!!.getInputStream()
            while (serviceEnabled) {
                Log.d(TAG, "while: ")
                // Read from the InputStream
                getFile(inputStream!!)
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

        sendActivityMessage("serviceStoppedByOther")

        releaseCPU()

        //Signal that the service was stopped
        //stopSelf()

    }

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    private fun releaseCPU() {
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
    private fun getFile(inputStream: InputStream) {
        if (FileHelper.isExternalStorageWritable()) {
            Log.d(TAG, "getFile: started")
            val totalFileNameSizeInBytes: Int
            val totalFileSizeInBytes: Int

            // File name string size or say message string size
            val fileNameSizeBuffer = ByteArray(4) // Only 4 bytes needed for this operation, int => 4 bytes
            inputStream.read(fileNameSizeBuffer, 0, 4)
            var fileSizeBuffer = ByteBuffer.wrap(fileNameSizeBuffer)
            totalFileNameSizeInBytes = fileSizeBuffer.int


            val prefix: List<String>
            var fileName = ""

            val name: List<String>

            //first check if we are sending something from there
            if (totalFileNameSizeInBytes > 0) {
                // Actual String of file name or say our message sent from other side
                val fileNameBuffer = ByteArray(1024)
                inputStream.read(fileNameBuffer, 0, totalFileNameSizeInBytes)
                fileName = String(fileNameBuffer, 0, totalFileNameSizeInBytes)
                prefix = fileName.split("#")
                name = prefix[1].split("/")

                Log.d("** reading message 1", fileName)

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

                    val finalMessage = "file#${WifiChatActivity.fileCreated.path}#${
                        Formatter.formatFileSize(
                            this, WifiChatActivity.fileCreated.length()
                        )
                    }"
                    val send = finalMessage.toByteArray()

                    sendActivityMessage(finalMessage)

                    /*// Send the obtained bytes to the UI Activity
                    if (HandleSocket.activityStatus == "pause" || HandleSocket.activityStatus == "stop") {
                        HandleSocket.sendChannel1Notification(this, finalMessage)
                    }*/
                } else {
                    Log.d("** reading file", "it was a message")
                    // Send the obtained bytes to the UI Activity
                    sendActivityMessage(fileName)
                    if (HandleSocket.activityStatus == "pause" || HandleSocket.activityStatus == "stop") {
                        sendChannel1Notification(this, prefix[1])
                    }
                }
            }
            else {
                Log.d("** reading message 2", "file size is empty")
            }


        } else {
            Log.d(TAG, "getFile:Storage not writeable")
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
        val replyIntent = Intent(context, ReplyReceiverWifi::class.java)
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
            .setSmallIcon(R.drawable.ic_wifi_on)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_wifi_on))
            //.setStyle(messagingStyle)
            .setStyle(notificationStyle)
            .addAction(replyAction)
            .setColor(Color.BLUE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            //.setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        val notificationManager = NotificationManagerCompat.from(context)
        //we need to change this id for different notification
        notificationManager.notify(2, notification)
    }



}