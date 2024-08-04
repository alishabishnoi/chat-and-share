package com.bluetoothwifiofflinechattingfilesharing.models

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "chats")
data class chat(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name="type") val type: String,
    @ColumnInfo(name="sender") val sender: String,
    @ColumnInfo(name="receiver") val receiver: String,
    @ColumnInfo(name="image") val image: Int,
    @ColumnInfo(name="msg") val msg: String,
    @ColumnInfo(name="timestamp") val timestamp: String,
    @ColumnInfo(name="file") val file: String,
    @ColumnInfo(name="isSeen") val isSeen: Boolean,

)