package com.bluetoothwifiofflinechattingfilesharing.fileshare

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bluetoothwifiofflinechattingfilesharing.bluetooth.FilePickerHelper
import com.bluetoothwifiofflinechattingfilesharing.databinding.ActivityClientBinding
import com.bluetoothwifiofflinechattingfilesharing.utils.CustomToast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.OutputStream
import java.net.Socket
import java.nio.ByteBuffer

class Client : AppCompatActivity() {
    private val TAG = "Client"
    var binding: ActivityClientBinding? = null
    var thread: Thread? = null
    var SERVER_IP: String? = null
    var path: String? = null
    var SERVER_PORT = 5050

    var file_path: String? = null

    val REQUEST_GET = 1

    var bytes: ByteArray = ByteArray(4)
    var file_get: File? = null

    // private var output: DataOutputStream? = null
    private var output: OutputStream? = null

    //private var input: DataInputStream? = null
    private var input: InputStream? = null
    private var f_input: ObjectInputStream? = null
    private var f_output: FileOutputStream? = null
    private lateinit var rThread: readThread

    var isConnected = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClientBinding.inflate(layoutInflater)
        setContentView(binding!!.root)


        doSomethingMemoryIntensive()


        val ip = intent.getStringExtra("ip").toString().trim { it <= ' ' }
        Toast.makeText(this, ip, Toast.LENGTH_SHORT).show()

        Log.d(TAG, "onCreate: client ip $ip")


        Handler().postDelayed({
            thread = Thread(Thread1(ip))
            thread!!.start()
        }, 3000)


        binding!!.btnConnect.setOnClickListener {
            binding!!.tvMessages.text = ""
            SERVER_IP = binding!!.etIP.text.toString().trim { it <= ' ' }

            //SERVER_PORT = binding!!.etPort.text.toString().trim { it <= ' ' }.toInt()

        }
        binding!!.btnSend.setOnClickListener {

            if (isConnected){
                CoroutineScope(Dispatchers.IO).launch {
                    writeFile()
                }
            }else{
                CustomToast.infoToastError(this,"Not Connected", Gravity.BOTTOM)
            }
        }
        binding!!.FILES.setOnClickListener {
            val myimageIntent = Intent(Intent.ACTION_GET_CONTENT)
            myimageIntent.type = "*/*"
            startActivityForResult(myimageIntent, Server.REQUEST_GET)
        }

        binding!!.showFiles.setOnClickListener {
            val c = Intent(this, FileListActivity::class.java)
            startActivity(c)
        }
    }

    inner class Thread1(val ip: String) : Runnable {
        override fun run() {
            val socket: Socket
            try {
                socket = Socket(ip, SERVER_PORT)
                //output = DataOutputStream(socket.getOutputStream())
                output = socket.getOutputStream()
                //input = DataInputStream(socket.getInputStream())
                input = socket.getInputStream()
                //f_input = ObjectInputStream(socket.getInputStream())
                //f_output = FileOutputStream(file_get)
                rThread = readThread(input!!)
                rThread.start()

                runOnUiThread {
                    binding!!.tvMessages.text = "Connected\n"
                    isConnected = true
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


    internal inner class readThread(val inputStream: InputStream) : Thread() {
        override fun run() {
            // Keep listening to the InputStream while connected
            while (true) {
                try {

                    getFile(inputStream)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

        }


    }

    /**
     * Write to the connected OutStream.
     *
     * @param buffer The bytes to write
     */
    fun writeFile() {
        Log.d("writeFIle called^^^^^", "write file: called")
        try {

            // Create File object to convert to bytes
            val file = File(file_path!!)

            if (file.readBytes().size > (1024 * 1024 * 100)) {
                // Dialog the user and send back to file picker activity
                val alertDialogBuilder = AlertDialog.Builder(this)
                alertDialogBuilder.setTitle("File too large")
                alertDialogBuilder.setMessage("This file is larger than the 100 MB Limit")
                alertDialogBuilder.setPositiveButton("OK") { _, _ ->
                    Toast.makeText(this, "File sending failed", Toast.LENGTH_SHORT).show()
                }
                alertDialogBuilder.show()
            } else {
                sendFile(output!!, file_path!!)
            }


        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_GET && resultCode == RESULT_OK) {
            val fullPhoto = data!!.data
            file_path = FilePickerHelper.getPath(this, fullPhoto!!).toString()
            Log.d(TAG, "onActivityResult:client $file_path ")

        }
    }

    fun sendFile(outputStream: OutputStream, filePath: String) {
        //first get the file size if it 0 then send message
        val selectedFileSize = File(filePath).length().toInt()

        val prefix = filePath.split("/")
        val name = prefix[prefix.size - 1]

        val send = name.toByteArray()

        // Int -> 4 bytes required to represent it
        val fileNameSize = ByteBuffer.allocate(4)

        fileNameSize.putInt(send.size) // Get number of bytes and put that number in

        outputStream.write(fileNameSize.array())    // The int containing number of bytes in the file name string
        outputStream.write(send) // The file name string turned to bytes

        //now if we have selected file then we will add extra details in output stream otherwise not
        val buffer1 = ByteArray(4096)
        // Create a File object containing the file contents, and convert into byte array
        val file = File(filePath)

        val fileBytes = ByteArray(file.length().toInt())

        // Int -> 4 bytes required to represent it
        val fileSize = ByteBuffer.allocate(4)

        // Same thing for the size of the actual file, put the size in an int, represented in 4 bytes
        fileSize.putInt(fileBytes.size)

        outputStream.write(fileSize.array())        // The int containing number of bytes in the actual file

        val bis = BufferedInputStream(FileInputStream(file))

        var bytesRead: Int
        var totalBytesRead2 = 0
        bytesRead = bis.read(buffer1, 0, buffer1.size)
        while (bytesRead != -1) {
            outputStream.write(buffer1, 0, bytesRead)
            totalBytesRead2 += bytesRead
            if (totalBytesRead2 == fileBytes.size) {
                break
            }

            bytesRead = bis.read(buffer1, 0, buffer1.size)
            val progressWriting =
                (totalBytesRead2.toFloat() / fileBytes.size.toFloat() * 100).toInt()
            Log.d("** file writing", "writing $progressWriting")

        }

        bis.close()

        Log.d("** file sent", "sent details: ")
        runOnUiThread { Toast.makeText(this, "file Sent", Toast.LENGTH_SHORT).show() }
    }

    fun getFile(inputStream: InputStream) {

        val totalFileNameSizeInBytes: Int
        val totalFileSizeInBytes: Int

        // File name string size or say message string size
        val fileNameSizeBuffer =
            ByteArray(4) // Only 4 bytes needed for this operation, int => 4 bytes
        inputStream.read(fileNameSizeBuffer, 0, 4)
        var fileSizeBuffer = ByteBuffer.wrap(fileNameSizeBuffer)
        totalFileNameSizeInBytes = fileSizeBuffer.int

        var fileName = ""

        //first check if we are sending something from there
        if (totalFileNameSizeInBytes > 0) {
            // Actual String of file name or say our message sent from other side
            val fileNameBuffer = ByteArray(1024)
            inputStream.read(fileNameBuffer, 0, totalFileNameSizeInBytes)
            fileName = String(fileNameBuffer, 0, totalFileNameSizeInBytes)

            Log.d("** reading message 2", fileName)
        } else {
            Log.d("** reading message1 ", "   ")
        }

        // File size integer bytes
        val fileSizebuffer = ByteArray(4) // int => 4 bytes
        inputStream.read(fileSizebuffer, 0, 4)
        fileSizeBuffer = ByteBuffer.wrap(fileSizebuffer)
        totalFileSizeInBytes = fileSizeBuffer.int

        // The actual file bytes
        val baos = ByteArrayOutputStream()  // this will write file to our storage
        val buffer = ByteArray(4096)
        var totalBytesRead = 0
        var read = inputStream.read(buffer, 0, buffer.size)
        while (read != -1) {
            baos.write(buffer, 0, read)
            totalBytesRead += read
            if (totalBytesRead == totalFileSizeInBytes) {
                break
            }
            read = inputStream.read(buffer, 0, buffer.size)
            val progressReading =
                (totalBytesRead.toFloat() / totalFileSizeInBytes.toFloat() * 100).toInt()
            Log.d("** file reading", "reading $progressReading")
        }
        baos.flush()

        val fileCreated = File(this.getExternalFilesDir("received"), fileName)
        val fos = FileOutputStream(fileCreated.path)
        fos.write(baos.toByteArray())
        fos.close()

        runOnUiThread { Toast.makeText(this, "file Received", Toast.LENGTH_SHORT).show() }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {

        val mDialog = MaterialAlertDialogBuilder(this)
        mDialog.setPositiveButton("Close") { dialogInterface, _ ->
            dialogInterface.dismiss()
            super.onBackPressed()
        }.setNegativeButton("Cancel") { dialogInterface, _ ->
            dialogInterface.dismiss()
        }.setMessage("Do you want to exiting file sending").setTitle("Exit Sharing").create()
        mDialog.show()
    }

    fun doSomethingMemoryIntensive() {

        Log.d(TAG, "avial mem: ${getAvailableMemory().availMem}")
        Log.d(TAG, "total mem: ${getAvailableMemory().totalMem}")
        Log.d(TAG, "thres mem: ${getAvailableMemory().threshold}")

        // Before doing something that requires a lot of memory,
        // check whether the device is in a low memory state.
        if (!getAvailableMemory().lowMemory) {
            // Do memory intensive work.
        }
    }

    // Get a MemoryInfo object for the device's current memory status.
    private fun getAvailableMemory(): ActivityManager.MemoryInfo {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return ActivityManager.MemoryInfo().also { memoryInfo ->
            activityManager.getMemoryInfo(memoryInfo)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isConnected=false
    }
}