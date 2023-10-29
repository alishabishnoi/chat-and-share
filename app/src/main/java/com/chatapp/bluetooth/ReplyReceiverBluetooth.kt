package com.chatapp.bluetooth

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.chatapp.Message
import com.chatapp.R
import com.chatapp.wifichat.ReadingService
import com.chatapp.wifichat.ReplyReceiverWifi


class ReplyReceiverBluetooth : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val reply = intent.getStringExtra("reply")



        //Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        val remoteInput: Bundle? = RemoteInput.getResultsFromIntent(intent)
        if (remoteInput != null) {
            val replyText = remoteInput.getCharSequence("key_text_reply").toString()

            Log.d("reply receiver", "onReceive: $replyText")
            val answer = Message(replyText, null)

            val person = Person.Builder().setName("Me").build()
            val message = NotificationCompat.MessagingStyle.Message(
                replyText, System.currentTimeMillis(), person
            )

            val i = Intent("android.intent.action.BluetoothReply")
            i.putExtra("bluetoothReply", replyText)
            //now send this reply to the service or writer
            LocalBroadcastManager.getInstance(context).sendBroadcast(i)

            sendChannel1Notification(context, replyText)


        }else{
            Log.d("remote input null", "000")
        }


    }

    @SuppressLint("MissingPermission")
    fun sendChannel1Notification(context: Context?, message: String) {
        val activityIntent = Intent(context, ChatActivity::class.java)
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

        val person = Person.Builder().setName("sender").build()
        val notificationStyle = NotificationCompat.MessagingStyle(person)
            .addMessage(message, System.currentTimeMillis(), person)

        val notification: Notification = NotificationCompat.Builder(context!!, "channel1")
            .setSmallIcon(R.drawable.ic_bluetooth)
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