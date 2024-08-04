package com.bluetoothwifiofflinechattingfilesharing.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bluetoothwifiofflinechattingfilesharing.R
import com.bluetoothwifiofflinechattingfilesharing.bluetooth.ChatActivity
import com.bluetoothwifiofflinechattingfilesharing.bluetooth.DeviceListActivity.Companion.EXTRA_DEVICE_ADDRESS
import com.bluetoothwifiofflinechattingfilesharing.models.Users
import java.text.SimpleDateFormat
import java.util.Calendar

@SuppressLint("MissingPermission")
class UserAdapter(val context: Context, var list: List<Users>) : RecyclerView.Adapter<UserAdapter.AppViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        return AppViewHolder(
            LayoutInflater.from(context).inflate(R.layout.users_item, parent, false)
        )
    }


    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val lastMessage=list[position].lastMsg.split("#")[1]
        holder.lastMessage.text =lastMessage
        holder.deviceAddress.text = list[position].otherDeviceName

        holder.lastSeen.text=getDate(System.currentTimeMillis(), "hh:mm:ss")

        holder.card.setOnClickListener {
            // Get the device MAC address, which is the last 17 chars in the View
            // Create the result Intent and include the MAC address
            val intent = Intent(context, ChatActivity::class.java)
            //intent.putExtra(EXTRA_DEVICE, list[position].otherDeviceAddress)
            intent.putExtra(EXTRA_DEVICE_ADDRESS, list[position].otherDeviceAddress)
            // Set result and finish this Activity
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun getDate(milliSeconds: Long, dateFormat: String?): String? {
        // Create a DateFormatter object for displaying date in specified format.
        val formatter = SimpleDateFormat(dateFormat)

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        val calendar: Calendar = Calendar.getInstance()
        calendar.timeInMillis = milliSeconds
        return formatter.format(calendar.time)
    }



    class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var lastMessage: TextView
        lateinit var deviceAddress: TextView
        lateinit var lastSeen: TextView
        var appIcon: ImageView
        lateinit var card:ConstraintLayout

        init {
            lastMessage = itemView.findViewById<TextView>(R.id.tvLastMsg)
            deviceAddress = itemView.findViewById<TextView>(R.id.tvAddress)
            lastSeen = itemView.findViewById<TextView>(R.id.tvLastSeen)
            appIcon = itemView.findViewById<ImageView>(R.id.ivPhoto)
            card = itemView.findViewById<ConstraintLayout>(R.id.cardCategory)
        }
    }


}