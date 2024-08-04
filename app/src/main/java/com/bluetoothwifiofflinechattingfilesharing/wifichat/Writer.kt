package com.bluetoothwifiofflinechattingfilesharing.wifichat

import android.annotation.SuppressLint
import android.os.Handler
import android.util.Log
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer

/*
* This class will write bytes of audio into output stream
* stream an play the voice
* */

@SuppressLint("MissingPermission")
class Writer(
    val handler: Handler,
    val fileUri: String,
    val byteArray: ByteArray,
    val outputStream: OutputStream
) : Runnable {
    private val TAG = "Writer"

    var keepRecording = true

    override fun run() {
        try {
            //will will send the audio byte thorough output stream into  socket
            //val outputStream = HandleSocket.socket!!.getOutputStream()


            if (keepRecording) {
                try {
                    Log.e("AUDIO", "STARTED RECORDING")
                    putFile(fileUri, outputStream, byteArray)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /*
    * this method will do two work
    * 1. if user have not selected any file then it will send message
    * 2. if user have selected any file then it will send file and its all detail
    * */
    private fun putFile(fileUri: String, outputStream: OutputStream, messageByteArray: ByteArray?) {
        Log.d(TAG, "outFile: started")
        //first get the file size if it 0 then send message
        val selectedFileSize = File(fileUri).length().toInt()

        handler.obtainMessage(WifiChatActivity.STATE_MESSAGE_SEND, -1, -1, messageByteArray)
            .sendToTarget()

        val message = messageByteArray!!.toString(Charsets.UTF_8)
        val prefix = message.split("#")
        //val name = prefix[1].split("/")


        val fileBytes: ByteArray
        // Int -> 4 bytes required to represent it
        val fileNameSize = ByteBuffer.allocate(4)
        val fileSize = ByteBuffer.allocate(4)

        fileNameSize.putInt(messageByteArray.size) // Get number of bytes and put that number in

        outputStream.write(fileNameSize.array())    // The int containing number of bytes in the file name string
        outputStream.write(messageByteArray) // The file name string turned to bytes

        Log.d(
            "** put file method",
            "sending - size-$selectedFileSize: message ${messageByteArray.toString(Charsets.UTF_8)}"
        )

        //now if we have selected file then we will add extra details in output stream otherwise not
        if (selectedFileSize != 0) {
            val buffer = ByteArray(1024)
            // Create a File object containing the file contents, and convert into byte array
            val file = File(fileUri)

            fileBytes = ByteArray(file.length().toInt())
            // Same thing for the size of the actual file, put the size in an int, represented in 4 bytes
            fileSize.putInt(fileBytes.size)

            outputStream.write(fileSize.array())        // The int containing number of bytes in the actual file

            val bis = BufferedInputStream(FileInputStream(file))

            //runOnUiThread { fileProgressWriting(name[name.size - 1], prefix[2]) }


            var bytesRead: Int
            var totalBytesRead = 0
            bytesRead = bis.read(buffer, 0, buffer.size)
            while (bytesRead != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                if (totalBytesRead == fileBytes.size) {
                    break
                }

                bytesRead = bis.read(buffer, 0, buffer.size)
                val progressWriting =
                    (totalBytesRead.toFloat() / fileBytes.size.toFloat() * 100).toInt()
                Log.d("** file writing", "writing $progressWriting")

                //handler.obtainMessage(WifiP2P.STATE_PROGRESS, -1, -1, progressWriting).sendToTarget()

            }

            bis.close()

            Log.d("** file sent", "sent details: ${messageByteArray.toString(Charsets.UTF_8)}")

        }

    }
}