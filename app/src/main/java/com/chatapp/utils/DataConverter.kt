package com.chatapp.utils

import android.bluetooth.BluetoothDevice
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

internal class DataConverter {
    @TypeConverter
    fun fromBluetoothDevice(value: BluetoothDevice?): String? {
        if (value == null) {
            return null
        }
        val gson = Gson()
        val type = object : TypeToken<BluetoothDevice?>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toBluetoothDevice(value: String?): BluetoothDevice? {
        if (value == null) {
            return null
        }
        val gson = Gson()
        val type = object : TypeToken<BluetoothDevice?>() {}.type
        return gson.fromJson(value, type)
    }
}