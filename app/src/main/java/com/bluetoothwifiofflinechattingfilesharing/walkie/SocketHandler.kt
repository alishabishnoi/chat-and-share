package com.bluetoothwifiofflinechattingfilesharing.walkie

import java.net.Socket

object SocketHandler {
    @get:Synchronized
    @set:Synchronized
    var socket: Socket? = null
}