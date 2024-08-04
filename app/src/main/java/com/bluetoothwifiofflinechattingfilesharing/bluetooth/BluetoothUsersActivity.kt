package com.bluetoothwifiofflinechattingfilesharing.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bluetoothwifiofflinechattingfilesharing.adapter.UserAdapter
import com.bluetoothwifiofflinechattingfilesharing.databinding.ActMainBinding
import com.bluetoothwifiofflinechattingfilesharing.db.OfflineDatabase
import com.bluetoothwifiofflinechattingfilesharing.models.Users
import com.bluetoothwifiofflinechattingfilesharing.utils.PrefsHelper
import com.bluetoothwifiofflinechattingfilesharing.utils.ShowReview
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

    var deviceList :List<Users> = arrayListOf()
    lateinit var devicesAdapter:UserAdapter

    var database: OfflineDatabase? = null

    lateinit var binding: ActMainBinding

    private val bluetoothManager by lazy {
        applicationContext.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkPermissionsAndStartDiscovery()

        val enableBluetoothLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            Toast.makeText(this, "Bluetooth Enabled", Toast.LENGTH_SHORT).show()
        }

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { perms ->
            val canEnableBluetooth = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                perms[Manifest.permission.BLUETOOTH_CONNECT] == true
            } else true

            if(canEnableBluetooth && !isBluetoothEnabled) {
                enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                )
            )
        }

        //initialize room database to get all his methods
        database = OfflineDatabase.getDatabase(this)

        binding.recyclerUsers.layoutManager = LinearLayoutManager(this)

        getDbChats()

        // Checking if bluetooth is supported, letting user know if it isn't and closing application
        if (bluetoothAdapter == null) {
            toast("This device does not support bluetooth")
            this.finish()
            return
        }

        binding.floatingAddUsers.setOnClickListener {
            val intent= Intent(this, DeviceListActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: get users")
        getDbChats()
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
                if (bluetoothAdapter!!.isEnabled) {
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

    private fun checkPermissionsAndStartDiscovery() {
        val permissions = listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestBluetoothPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            //Toast.makeText(this, "Granted", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestBluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.BLUETOOTH] == true &&
                permissions[Manifest.permission.BLUETOOTH_ADMIN] == true &&
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (granted) {
            Toast.makeText(this, "Granted", Toast.LENGTH_SHORT).show()
            //startBluetoothDiscovery()
        }
    }
}