package com.chatapp.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.chatapp.adapter.UserAdapter
import com.chatapp.databinding.ActMainBinding
import com.chatapp.utils.CustomToast
import com.chatapp.utils.PermissionManager
import com.chatapp.db.OfflineDatabase
import com.chatapp.models.Users
import com.chatapp.utils.PrefsHelper
import com.chatapp.utils.ShowReview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class BluetoothUsersActivity : AppCompatActivity() {

    private val TAG = "BluetoothUsersActivity"
    /**
     * REQUEST_ENABLE_BLUETOOTH: constant for requesting bluetooth be turned on.
     */
    private val REQUEST_ENABLE_BLUETOOTH = 1
    private var MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 0
    private var MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 0
    private var MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 0

    /**
     * m_bluetoothAdapter: The Bluetooth Adapter of this phone.
     */
    private var m_bluetoothAdapter: BluetoothAdapter? = null

    var deviceList :List<Users> = arrayListOf()
    lateinit var devicesAdapter:UserAdapter

    var database: OfflineDatabase? = null

    private lateinit var permissionManager: PermissionManager
    private val permission1 = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )


    @RequiresApi(Build.VERSION_CODES.S)
    private val permission2= arrayOf(Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN)

    lateinit var binding: ActMainBinding

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissionManager = PermissionManager.getInstance(this)

        if (!permissionManager.methodCheckPermission(permission1)) {
            permissionManager.methodAskPermission(
                this, permission1, 100
            )
        }

        /*if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this,permission2,200)
        }*/


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!permissionManager.methodCheckPermission(permission2)) {
                permissionManager.methodAskPermission(
                    this, permission2, 200
                )
            }
        }



        //initialize room database to get all his methods
        database = OfflineDatabase.getDatabase(this)

        binding.recyclerUsers.layoutManager = LinearLayoutManager(this)

        getDbChats()

        //init only once in all app
        PrefsHelper.Builder()
            .setContext(this)
            .setMode(ContextWrapper.MODE_PRIVATE)
            .setPrefsName(packageName)
            .setUseDefaultSharedPreference(true)
            .build()

        //every 5th time show for review
        val opened = PrefsHelper.getInt("OPEN", 0)

        //first get previous value
        if (opened == 8){
            PrefsHelper.putInt("OPEN", 1)

            //initiateRatingFlow()
            val rating = ShowReview(this@BluetoothUsersActivity, this)
            //this will show the rating dialog every 5th time
            Handler().postDelayed({
                rating.initiateRatingFlow()
            }, 3000)
        }else{
            val newVal=opened+1
            PrefsHelper.putInt("OPEN", newVal)
        }

        // Getting Bluetooth Adapter
        m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // Checking if bluetooth is supported, letting user know if it isn't and closing application
        if (m_bluetoothAdapter == null) {
            toast("This device does not support bluetooth")
            this.finish()
            return
        }

        // Request to enable bluetooth for user if not enabled, this ensures the user's bluetooth is on for the rest
        // of the process
        if (!m_bluetoothAdapter!!.isEnabled) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH)
        }

        binding.floatingAddUsers.setOnClickListener {
            val intent= Intent(this, DeviceListActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * This method is called when either bluetooth discovery is enabled or when bluetooth is enabled, the methods
     * that call this method send a request code as a parameter that can be referred to in this method.
     *
     * This method is used to notify the user the bluetooth status on their phone.
     *
     * There are two cases seen in this method:
     *  RESULT_OK - Activity ran correctly, it has proper data to be read by the method.
     *
     *  RESULT_CANCELLED - The user cancelled before bluetooth could be set up.
     *  This is usually done by denying the requests that pop up when "Entering the Zone"
     *
     *  @param requestCode - code for the action that was undertaken.
     *  @param resultCode - result of that action
     *  @param data - data only exists, if resultCode = RESULT_OK
     *  @return void
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Check which action has occurred
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {

            // Check status and notify respectively
            if (resultCode == Activity.RESULT_OK) {
                if (m_bluetoothAdapter!!.isEnabled) {
                    toast("Bluetooth has been enabled")
                } else {
                    toast("Bluetooth has been disabled")
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                toast("Bluetooth has been cancelled")
            }
        } else if (requestCode.equals(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)) {
            if (resultCode == Activity.RESULT_CANCELED) {
                toast("Bluetooth has been cancelled")
            }
        }
    }

    fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && permissionManager.handleRequestResult(grantResults)) {
            CustomToast.infoToast(this, "Permission Granted", CustomToast.GRAVITY_BOTTOM)
        }

        if (requestCode == 200 && permissionManager.handleRequestResult(grantResults)) {
            CustomToast.infoToast(this, "Nearby Permission Granted", CustomToast.GRAVITY_BOTTOM)
        }

    }

    fun getDbChats() {
        GlobalScope.launch {

            var list:List<Users> = emptyList()

            val job=launch {
                deviceList=database!!.insertDao().getAllUsers()
            }
            job.join()

            Log.d(TAG, "getDbChats: ${deviceList.toString()}")

            runOnUiThread {
                devicesAdapter = UserAdapter(this@BluetoothUsersActivity, deviceList)
                binding.recyclerUsers.adapter = devicesAdapter
            }
        }


    }



    fun deleteRoomData() {
        GlobalScope.launch(Dispatchers.IO) {
            database!!.clearAllTables()
        }
    }




}