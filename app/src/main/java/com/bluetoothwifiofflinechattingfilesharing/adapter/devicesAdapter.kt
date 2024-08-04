package com.bluetoothwifiofflinechattingfilesharing.adapter

import android.annotation.SuppressLint
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bluetoothwifiofflinechattingfilesharing.R
import com.bluetoothwifiofflinechattingfilesharing.bluetooth.ChatActivity
import com.bluetoothwifiofflinechattingfilesharing.bluetooth.DeviceListActivity.Companion.EXTRA_DEVICE_ADDRESS
import com.bluetoothwifiofflinechattingfilesharing.bluetooth.finishInterface
import java.io.IOException
import java.util.UUID

@SuppressLint("MissingPermission")
class devicesAdapter(
    val context: Context,
    var list: ArrayList<BluetoothDevice>,
    val finishInterface: finishInterface
) : RecyclerView.Adapter<devicesAdapter.AppViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        return AppViewHolder(
            LayoutInflater.from(context).inflate(R.layout.document_item, parent, false)
        )
    }


    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.deviceName.text = list[position].name
        holder.deviceAddress.text = list[position].address

        holder.connect.setOnClickListener {


            //first we will check if device is connect or not
            if (list[position].bondState == 10) {
                try {
                    val method = list[position].javaClass.getMethod("createBond")
                    method.invoke(list[position])
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                /*val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard UUID for SPP
                connectToDevice(list[position],uuid)*/
            } else {
                // Get the device MAC address, which is the last 17 chars in the View
                // Create the result Intent and include the MAC address
                val intent = Intent(context, ChatActivity::class.java)
                //intent.putExtra(EXTRA_DEVICE, list[position])
                intent.putExtra(EXTRA_DEVICE_ADDRESS, list[position].address)
                // Set result and finish this Activity
                context.startActivity(intent)
                finishInterface.finish1()
            }

        }

        if (list[position].bondState == 10) {
            holder.connect.text = "Pair"
            holder.connect.setTextColor(context.resources.getColor(R.color.reddish))
        }

        list[position].bluetoothClass.let {
            when (it.deviceClass) {
                BluetoothClass.Device.COMPUTER_DESKTOP -> {
                    Log.d(ChatActivity.TAG, "computer desktop")
                    holder.appIcon.setImageResource(R.drawable.ic_device_laptop)
                }

                BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET -> {
                    Log.d(ChatActivity.TAG, "computer desktop")
                    holder.appIcon.setImageResource(R.drawable.ic_device_headset)
                }

                BluetoothClass.Device.COMPUTER_LAPTOP -> {
                    Log.d(ChatActivity.TAG, "computer laptop")
                    holder.appIcon.setImageResource(R.drawable.ic_device_laptop)
                }

                BluetoothClass.Device.PHONE_SMART -> {
                    Log.d(ChatActivity.TAG, "computer desktop")
                    holder.appIcon.setImageResource(R.drawable.ic_device_phone)
                }

                BluetoothClass.Device.Major.UNCATEGORIZED -> {
                    Log.d(ChatActivity.TAG, "uncategorized")
                    holder.appIcon.setImageResource(R.drawable.ic_device_headset)
                }

                else -> {
                    Log.d(ChatActivity.TAG, "device ${it.deviceClass}")
                }
            }
        }


    }

    fun connectToDevice(device: BluetoothDevice, uuid: UUID): BluetoothSocket? {
        return try {
            val socket = device.createRfcommSocketToServiceRecord(uuid)
            socket.connect()
            Log.d("BluetoothHelper", "Connected to device: ${device.name}")
            socket
        } catch (e: IOException) {
            Log.e("BluetoothHelper", "Failed to connect: ${e.message}")
            null
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }


    class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var deviceName: TextView
        lateinit var deviceAddress: TextView
        lateinit var connect: TextView
        var appIcon: ImageView

        init {
            deviceName = itemView.findViewById<TextView>(R.id.tvLastMsg)
            deviceAddress = itemView.findViewById<TextView>(R.id.tvAddress)
            connect = itemView.findViewById<TextView>(R.id.tvLastSeen)
            appIcon = itemView.findViewById<ImageView>(R.id.ivPhoto)
        }
    }


}