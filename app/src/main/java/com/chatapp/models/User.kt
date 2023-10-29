package com.chatapp.models

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "users")
data class Users(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name="name") val otherDeviceName: String,
    @ColumnInfo(name="lastMsg") val lastMsg: String,
    @ColumnInfo(name="address") val otherDeviceAddress: String,
    //@TypeConverters(DataConverter::class)
    @ColumnInfo(name="device") val thisDeviceAddress: String,
    @ColumnInfo(name="image") val image: Int,
    @ColumnInfo(name="lastSeen") val lastSeen: Long,

    )