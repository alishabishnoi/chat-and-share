package com.bluetoothwifiofflinechattingfilesharing.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bluetoothwifiofflinechattingfilesharing.R
import com.bluetoothwifiofflinechattingfilesharing.adapter.devicesAdapter
import com.bluetoothwifiofflinechattingfilesharing.databinding.ActDeviceListBinding
import java.io.IOException
import java.util.UUID

/**
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 */
@SuppressLint("MissingPermission")
class DeviceListActivity : AppCompatActivity(), finishInterface {
    lateinit var binding: ActDeviceListBinding

    // Member fields
    private lateinit var mBtAdapter: BluetoothAdapter

    var deviceList: ArrayList<BluetoothDevice> = arrayListOf()
    lateinit var devicesAdapter: devicesAdapter

    private var isDiscovering: Boolean = false

    companion object {
        // Debugging
        private const val TAG = "DeviceListActivity"
        private const val D = true

        // Return Intent extra
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

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter()

        if (!mBtAdapter.isEnabled) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, 1112)
            // Otherwise, setup the chat session
        } else {
            // Get a set of currently paired devices
            val pairedDevices = mBtAdapter.bondedDevices

            // If there are paired devices, add each one to the ArrayAdapter
            if (pairedDevices.size > 0) {
                for (device in pairedDevices) {
                    val deviceType = device.bluetoothClass.deviceClass
                    if (deviceType == BluetoothClass.Device.PHONE_SMART) {
                        //Only show mobile devices
                        deviceList.add(device)
                    }

                }
            } else {
                Toast.makeText(this, "No  Device Found", Toast.LENGTH_SHORT).show()
            }
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        devicesAdapter = devicesAdapter(this, deviceList, this@DeviceListActivity)
        binding.recyclerView.adapter = devicesAdapter

        // Initialize the button to perform device discovery
        startDiscovery()
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
    fun startDiscovery() {
        if (mBtAdapter == null) {
            Log.e("BluetoothHelper", "Bluetooth is not supported on this device")
            return
        }

        if (D) Log.d(TAG, "doDiscovery()")
        // Indicate scanning in the title
        setTitle(R.string.scanning)
        // Turn on sub-title for new devices
        // If we're already discovering, stop it

        if (mBtAdapter.isDiscovering) {
            mBtAdapter.cancelDiscovery()
        }
        //deviceList.clear()
        isDiscovering = true

        // Register for broadcasts when a device is discovered
        // Create an IntentFilter and add multiple actions
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        this.registerReceiver(mReceiver, filter)

        mBtAdapter.startDiscovery()
        Log.d("BluetoothHelper", "Started discovery")
    }

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND == action) {
                // Get the BluetoothDevice object from the Intent
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                // If it's already paired, skip it, because it's been listed already
                if (device!!.bondState != BluetoothDevice.BOND_BONDED) {
                    Log.d(
                        TAG,
                        "onReceive: ${device.getName() + "\n" + device.getAddress() + " --" + device.bondState} "
                    )
                }

                val deviceType = device.bluetoothClass.deviceClass
                if (deviceType == BluetoothClass.Device.PHONE_SMART) {
                    //Only show mobile devices
                    deviceList.add(device)
                    devicesAdapter.notifyItemInserted(deviceList.size - 1)
                }

                // When discovery is finished, change the Activity title
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED == action) {
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val bondState =
                    intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
                val previousBondState = intent.getIntExtra(
                    BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,
                    BluetoothDevice.BOND_NONE
                )

                when (bondState) {
                    BluetoothDevice.BOND_BONDED -> {
                        val deviceType = device!!.bluetoothClass.deviceClass
                        if (deviceType == BluetoothClass.Device.PHONE_SMART ) {
                            //first check if this device is already exist or not
                            if (deviceList.contains(device)){
                                //first remove the earlier
                                deviceList.remove(device)
                                //Only show mobile devices
                                deviceList.add(device)
                                devicesAdapter.notifyItemInserted(deviceList.size - 1)
                            }
                        }
                        // Device is now paired
                        Toast.makeText(context, "Paired with ${device.name}", Toast.LENGTH_SHORT)
                            .show()
                    }

                    BluetoothDevice.BOND_BONDING -> {
                        // Device is pairing
                        Toast.makeText(context, "Pairing with ${device?.name}", Toast.LENGTH_SHORT)
                            .show()
                    }

                    BluetoothDevice.BOND_NONE -> {
                        // Device is not paired
                        if (previousBondState == BluetoothDevice.BOND_BONDING) {
                            Toast.makeText(
                                context,
                                "Pairing with ${device?.name} failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                isDiscovering = false
                setTitle(R.string.select_device)
                Toast.makeText(this@DeviceListActivity, "Discovery Finish", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    override fun finish1() {
        finish()
    }


}