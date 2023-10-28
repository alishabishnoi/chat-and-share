package com.chatapp.fileshare

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chatapp.activity.FileListActivity
import com.chatapp.bluetooth.ChatService
import com.chatapp.bluetooth.FilePickerHelper
import com.chatapp.databinding.ActivityServerBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.ObjectOutputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.nio.ByteBuffer
import java.nio.file.Files
import java.util.Objects
import java.util.concurrent.Executors

class Server : AppCompatActivity() {
    private val TAG = "Server"
    lateinit var binding: ActivityServerBinding
    var serverSocket: ServerSocket? = null
    var thread: Thread? = null
    lateinit var message: String
    lateinit var path: String
    var flag = 0
    var file_get: File? = null
    private lateinit var rThread: readThread

    companion object {
        var SERVER_IP = ""
        const val SERVER_PORT = 5050
        var file_path: String? = null

        const val REQUEST_GET = 1


        val localIpAddress: String
            get() {
                var ip = ""
                try {
                    val en = NetworkInterface.getNetworkInterfaces()
                    while (en.hasMoreElements()) {
                        val intf = en.nextElement()
                        val enumIpAddr = intf.inetAddresses
                        while (enumIpAddr.hasMoreElements()) {
                            val inetAddress = enumIpAddr.nextElement()
                            if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                                ip += """
                                ${inetAddress.getHostAddress()}
                                
                                """.trimIndent()
                            }
                        }
                    }
                } catch (ex: SocketException) {
                    ex.printStackTrace()
                }
                return ip
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //get ip address of the device
        SERVER_IP = localIpAddress

        Log.d(TAG, "onCreate: server ip $SERVER_IP")

        createQR(SERVER_IP)

        //start the thread to connect to the client
        thread = Thread(Thread1())
        thread!!.start()

        path = ""

        binding.send.setOnClickListener(View.OnClickListener {

            CoroutineScope(Dispatchers.IO).launch {
                writeFile()
            }

            //Thread(writeThread(message)).start()
            if (flag == 1) {
                Toast.makeText(this@Server, "Transferring", Toast.LENGTH_SHORT).show()

            }

        })
        binding.FILES.setOnClickListener(View.OnClickListener {
            val myimageIntent = Intent(Intent.ACTION_GET_CONTENT)
            myimageIntent.type = "*/*"
            startActivityForResult(myimageIntent, REQUEST_GET)
        })

        /*binding.showpath.setOnClickListener(View.OnClickListener {
            val intent = intent
            path = intent.getStringExtra("file").toString()
            binding.filepath.text = "Path: $file_path"
            flag = 1
        })*/

        binding.showFiles.setOnClickListener {
            val c = Intent(this, FileListActivity::class.java)
            startActivity(c)
        }
    }

    fun createQR(text:String){
        //create qrcode in image
        val writer = MultiFormatWriter()
        var matrix: BitMatrix? = null
        try {
            matrix = writer.encode(
                text, BarcodeFormat.QR_CODE,
                300, 300
            )
        } catch (e: WriterException) {
            e.printStackTrace()
        }
        val encoder = BarcodeEncoder()
        val bitmap: Bitmap = encoder.createBitmap(matrix)


        binding.imageView.visibility=View.VISIBLE
        binding.imageView.setImageBitmap(bitmap)
    }

    //private var output: DataOutputStream? = null
    private var output: OutputStream? = null
    //private var input: DataInputStream? = null
    private var input: InputStream? = null

    private var f_input: BufferedInputStream? = null
    private var f_output: ObjectOutputStream? = null

    internal inner class Thread1 : Runnable {
        override fun run() {
            val socket: Socket
            try {
                serverSocket = ServerSocket(SERVER_PORT)
                runOnUiThread {
                    binding.tvmessages.text = "Not Connected"
                    //binding.ipText.text = SERVER_IP
                    //binding.portText.text = "Port :" + (SERVER_PORT.toString() + "")
                }
                try {
                    socket = serverSocket!!.accept()
                    socket.soTimeout = 300000
                    //output = DataOutputStream(socket.getOutputStream())
                    output = socket.getOutputStream()
                    //input = DataInputStream(socket.getInputStream())
                    input = socket.getInputStream()
                    //f_input = BufferedInputStream(FileInputStream(file_get))
                    //f_output = ObjectOutputStream(socket.getOutputStream())

                    rThread=readThread(input!!)
                    rThread.start()

                    runOnUiThread { binding.tvmessages.text = "Connected\n" }
                    //Thread(readThread(input)).start()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


    private inner class readThread(var inputStream: InputStream?) : Thread() {
        override fun run() {
            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    getFile(inputStream!!)
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

            sendFile(output!!,file_path!!)

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }



    /*internal inner class writeThread(private val message: String) : Runnable {
        override fun run() {
            try {

                sendFile(output!!, file_path!!)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            runOnUiThread {
                binding.tvmessages.append("Server: $message\n")
                binding.etMessage.setText("")
            }
        }
    }*/

    fun sendFile(outputStream: OutputStream, filePath: String) {

        //first get the file size if it 0 then send message
        val selectedFileSize = File(filePath).length().toInt()

        val prefix=filePath.split("/")
        val name=prefix[prefix.size-1]

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
        val fileNameSizeBuffer = ByteArray(4) // Only 4 bytes needed for this operation, int => 4 bytes
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
        }else{
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



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_GET && resultCode == RESULT_OK) {
            val fullPhoto = data!!.data
            file_path = FilePickerHelper.getPath(this, fullPhoto!!).toString()

            Log.d(TAG, "onActivityResult:server $file_path ")

        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val mDialog = MaterialAlertDialogBuilder(this)
        mDialog.setPositiveButton("Close") { dialogInterface, _ ->
            dialogInterface.dismiss()
            super.onBackPressed()
        }.setNegativeButton("Cancel") { dialogInterface, _ ->
            dialogInterface.dismiss()
        }.setMessage("Do you want to exiting file sending")
            .setTitle("Exit Sharing").create()
        mDialog.show()
    }
}