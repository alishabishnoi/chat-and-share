package com.chatapp.models

import android.bluetooth.BluetoothDevice
import androidx.annotation.Keep
import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.bumptech.glide.load.model.ByteArrayLoader
import com.chatapp.utils.DataConverter

@Keep
@Entity(tableName = "users")
data class Users(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name="name") val name: String,
    @ColumnInfo(name="lastMsg") val lastMsg: String,
    @ColumnInfo(name="address") val address: String,
    @TypeConverters(DataConverter::class)
    @ColumnInfo(name="device") val device: BluetoothDevice,
    @ColumnInfo(name="image") val image: Int,
    @ColumnInfo(name="lastSeen") val lastSeen: Long,

    )