package com.chatapp.wifichat

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
import com.chatapp.bluetooth.ChatActivity


class ReplyReceiverWifi : BroadcastReceiver() {

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

            val i = Intent("android.intent.action.SmsReceiver")
            i.putExtra("incomingPhoneNumber", replyText)
            //now send this reply to the service or writer
            LocalBroadcastManager.getInstance(context).sendBroadcast(i)

            ReadingService().sendChannel1Notification(context, replyText)


        }else{
            Log.d("remote input null", "000")
        }


    }


}