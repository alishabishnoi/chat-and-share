package com.bluetoothwifiofflinechattingfilesharing.utils

import android.app.Activity
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.bluetoothwifiofflinechattingfilesharing.R
import com.bluetoothwifiofflinechattingfilesharing.databinding.CustomToastBinding

class CustomToast {
    lateinit var binding: CustomToastBinding
    companion object {
        val GRAVITY_TOP = 48
        val GRAVITY_CENTER = 17
        val GRAVITY_BOTTOM = 80
        private lateinit var layoutInflater: LayoutInflater

        fun infoToastError(context: Activity, message: String, position: Int) {
            layoutInflater = LayoutInflater.from(context)
            val layout = layoutInflater.inflate(R.layout.custom_toast, (context).findViewById(R.id.custom_toast_layout))
            layout.findViewById<ImageView>(R.id.custom_toast_image).setImageDrawable(ContextCompat.getDrawable(context,
                R.drawable.icon_error
            ))
            val drawable = ContextCompat.getDrawable(context, R.drawable.toast_round_background)
            drawable?.colorFilter = PorterDuffColorFilter(ContextCompat.getColor(context,
                R.color.reddish
            ), PorterDuff.Mode.MULTIPLY)
            layout.background = drawable
            layout.findViewById<TextView>(R.id.custom_toast_message).setTextColor(Color.WHITE)
            layout.findViewById<TextView>(R.id.custom_toast_message).text = message
            val toast = Toast(context.applicationContext)
            toast.duration = Toast.LENGTH_SHORT
            if (position == GRAVITY_BOTTOM) {
                toast.setGravity(position, 0, 40)
            } else {
                toast.setGravity(position, 0, 0)
            }
            toast.view = layout //setting the view of custom toast layout
            toast.show()
        }
        fun infoToast(context: Activity, message: String, position: Int) {
            layoutInflater = LayoutInflater.from(context)
            val layout = layoutInflater.inflate(R.layout.custom_toast, (context).findViewById(R.id.custom_toast_layout))
            layout.findViewById<ImageView>(R.id.custom_toast_image).setImageDrawable(ContextCompat.getDrawable(context,
                R.drawable.icon_done
            ))
            val drawable = ContextCompat.getDrawable(context, R.drawable.toast_round_background)
            drawable?.colorFilter = PorterDuffColorFilter(ContextCompat.getColor(context,
                R.color.green
            ), PorterDuff.Mode.MULTIPLY)
            layout.background = drawable
            layout.findViewById<TextView>(R.id.custom_toast_message).setTextColor(Color.WHITE)
            layout.findViewById<TextView>(R.id.custom_toast_message).text = message
            //layout.findViewById<TextView>(R.id.custom_toast_message).typeface = ResourcesCompat.getFont(context,R.font.raleway)
            val toast = Toast(context.applicationContext)
            toast.duration = Toast.LENGTH_SHORT
            if (position == GRAVITY_BOTTOM) {
                toast.setGravity(position, 0, 180)
            } else {
                toast.setGravity(position, 0, 0)
            }
            toast.view = layout//setting the view of custom toast layout
            toast.show()
        }
    }
}