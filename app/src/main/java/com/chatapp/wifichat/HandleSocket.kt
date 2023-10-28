package com.chatapp.wifichat

import java.net.Socket

object HandleSocket {
    @get:Synchronized
    @set:Synchronized
    var socket: Socket? = null

    @get:Synchronized
    @set:Synchronized
    var activityStatus:String="start"
}