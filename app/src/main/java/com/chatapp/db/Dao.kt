package com.chatapp.db

import androidx.annotation.Keep
import androidx.room.*
import androidx.room.Dao
import com.chatapp.models.Users
import com.chatapp.models.chat

@Keep
@Dao
interface Dao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertChats(model: chat)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertUser(model: Users)


    @Query("SELECT * from chats")
    fun getAllChats():List<chat>

    @Query("SELECT * from users")
    fun getAllUsers():List<Users>

    @Query("SELECT * from users Where address LIKE :address ")
    fun getOneUser(address :String): Users

    @Query("SELECT * from users Where address LIKE :address ")
    fun isUserExist(address :String): Int

    @Query("SELECT id,type,sender,receiver,image,msg,timestamp,file,isSeen from chats Where type LIKE :type1")
    fun getOneChat(type1 :String): List<chat>

    @Delete
    fun delete(model: chat)

    @Delete
    fun deleteUsers(model: Users)

    //@Query("UPDATE resultTable SET time=:time,date=:date,`right`=:right, questions=:question WHERE setNo=:set")
    //fun updateResult(set: String,time :String,date :String ,right:String,question:String)
}