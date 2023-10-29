package com.chatapp.db

import android.content.Context
import androidx.annotation.Keep
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bumptech.glide.load.model.ByteArrayLoader.Converter
import com.chatapp.models.Users
import com.chatapp.models.chat
import com.chatapp.utils.DataConverter

@Keep
@Database(entities = [chat::class,Users::class] ,version = 1)
//abstract method or class means without body
//@TypeConverters(DataConverter::class)
abstract class OfflineDatabase : RoomDatabase() {
    abstract fun insertDao(): Dao

    companion object{
        private var INSTANCE: OfflineDatabase? = null
        fun getDatabase(context: Context): OfflineDatabase {
            if (INSTANCE ==null){
                synchronized(this){
                    INSTANCE = Room.databaseBuilder(context,
                        OfflineDatabase::class.java
                        , "chatDb" )
                        .build()
                }

            }
            return INSTANCE!!
        }
    }
}