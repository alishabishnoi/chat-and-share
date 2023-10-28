package com.chatapp.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.chatapp.R
import com.chatapp.adapter.devicesAdapter
import com.chatapp.databinding.ActDeviceListBinding

/**
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 */
@SuppressLint("MissingPermission")
class DeviceListActivity : AppCompatActivity() {
    lateinit var binding: ActDeviceListBinding

    // Member fields
    private lateinit var mBtAdapter: BluetoothAdapter

    var deviceList :ArrayList<BluetoothDevice> = arrayListOf()
    lateinit var devicesAdapter: devicesAdapter

    companion object {
        // Debugging
        private const val TAG = "DeviceListActivity"
        private const val D = true

        // Return Intent extra
        @JvmField
        var EXTRA_DEVICE_ADDRESS = "device_address"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActDeviceListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true);
        supportActionBar!!.setDisplayShowHomeEnabled(true);

        // Set result CANCELED in case the user backs out
        setResult(RESULT_CANCELED)

        // Register for broadcasts when a device is discovered
        var filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        this.registerReceiver(mReceiver, filter)

        // Register for broadcasts when discovery has finished
        filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        this.registerReceiver(mReceiver, filter)

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter()

        // Get a set of currently paired devices
        val pairedDevices = mBtAdapter.bondedDevices

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size > 0) {
            for (device in pairedDevices) {

                deviceList.add(device)
            }
        } else {
            Toast.makeText(this, "No  Device Found", Toast.LENGTH_SHORT).show()
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        devicesAdapter = devicesAdapter(this, deviceList)
        binding.recyclerView.adapter = devicesAdapter

        // Initialize the button to perform device discovery
        doDiscovery()
    }

    //to exit on back btn from action bar
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        // Make sure we're not doing discovery anymore
        if (!mBtAdapter.equals(null)) {
            mBtAdapter.cancelDiscovery()
        }
        // Unregister broadcast listeners
        unregisterReceiver(mReceiver)
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private fun doDiscovery() {
        if (D) Log.d(TAG, "doDiscovery()")
        // Indicate scanning in the title
        setTitle(R.string.scanning)
        // Turn on sub-title for new devices
        // If we're already discovering, stop it
        if (mBtAdapter.isDiscovering) {
            mBtAdapter.cancelDiscovery()
        }
        // Request discover from BluetoothAdapter
        mBtAdapter.startDiscovery()
    }

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND == action) {
                // Get the BluetoothDevice object from the Intent
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                // If it's already paired, skip it, because it's been listed already
                if (device!!.bondState != BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "onReceive: ${device.getName() + "\n" + device.getAddress() +" --"+device.bondState} ")

                    deviceList.add(device)
                    devicesAdapter.notifyItemInserted(deviceList.size-1)
                }

                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                setTitle(R.string.select_device)
                Toast.makeText(this@DeviceListActivity, "Discovery Finish", Toast.LENGTH_SHORT).show()
            }
        }
    }





}