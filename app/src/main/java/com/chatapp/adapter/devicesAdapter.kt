package com.chatapp.adapter

import android.annotation.SuppressLint
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.EXTRA_DEVICE
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chatapp.R
import com.chatapp.bluetooth.ChatActivity
import com.chatapp.bluetooth.DeviceListActivity.Companion.EXTRA_DEVICE_ADDRESS
import com.chatapp.bluetooth.finishInterface

@SuppressLint("MissingPermission")
class devicesAdapter(val context: Context, var list: ArrayList<BluetoothDevice>,val finishInterface: finishInterface) : RecyclerView.Adapter<devicesAdapter.AppViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        return AppViewHolder(
            LayoutInflater.from(context).inflate(R.layout.document_item, parent, false)
        )
    }


    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.deviceName.setText(list[position].name)
        holder.deviceAddress.setText(list[position].address)

        holder.connect.setOnClickListener {
            // Get the device MAC address, which is the last 17 chars in the View
            // Create the result Intent and include the MAC address
            val intent = Intent(context, ChatActivity::class.java)
            //intent.putExtra(EXTRA_DEVICE, list[position])
            intent.putExtra(EXTRA_DEVICE_ADDRESS, list[position].address)
            // Set result and finish this Activity
            context.startActivity(intent)
            finishInterface.finish1()
        }

        if (list[position].bondState==10){
            holder.connect.text="pair"
            holder.connect.setTextColor(context.resources.getColor(R.color.reddish))
        }

        list[position].bluetoothClass.let {
            when(it.deviceClass){
                BluetoothClass.Device.COMPUTER_DESKTOP ->{
                    Log.d(ChatActivity.TAG, "computer desktop")
                    holder.appIcon.setImageResource(R.drawable.ic_device_laptop)
                }
                BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET ->{
                    Log.d(ChatActivity.TAG, "computer desktop")
                    holder.appIcon.setImageResource(R.drawable.ic_device_headset)
                }

                BluetoothClass.Device.COMPUTER_LAPTOP ->{
                    Log.d(ChatActivity.TAG, "computer laptop")
                    holder.appIcon.setImageResource(R.drawable.ic_device_laptop)
                }

                BluetoothClass.Device.PHONE_SMART ->{
                    Log.d(ChatActivity.TAG, "computer desktop")
                    holder.appIcon.setImageResource(R.drawable.ic_device_phone)
                }

                BluetoothClass.Device.Major.UNCATEGORIZED->{
                    Log.d(ChatActivity.TAG, "uncategorized")
                    holder.appIcon.setImageResource(R.drawable.ic_device_headset)
                }

                else -> {
                    Log.d(ChatActivity.TAG, "device ${it.deviceClass}")
                }
            }
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