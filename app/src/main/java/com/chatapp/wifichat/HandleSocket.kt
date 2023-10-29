package com.chatapp.wifichat

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.chatapp.DirectReplyReceiver
import com.chatapp.R
import java.net.Socket

object HandleSocket {
    @get:Synchronized
    @set:Synchronized
    var socket: Socket? = null

    @get:Synchronized
    @set:Synchronized
    var activityStatus:String="start"

    @get:Synchronized
    @set:Synchronized
    var activityBlueStatus:String="start"


}