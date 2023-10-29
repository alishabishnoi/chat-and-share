package com.chatapp.walkie

import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

object SocketHandler {
    @get:Synchronized
    @set:Synchronized
    var socket: Socket? = null
}