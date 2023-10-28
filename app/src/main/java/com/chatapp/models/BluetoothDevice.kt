package com.chatapp.models

import androidx.annotation.NonNull

data class BluetoothDevice(

    var name:String = "",

    var address:String = "",

    var image:String = "",
    var isPaired:Boolean=false
)