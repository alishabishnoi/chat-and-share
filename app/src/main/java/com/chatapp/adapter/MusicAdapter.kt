package com.chatapp.adapter

import android.content.Context
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chatapp.R
import com.chatapp.adapter.MusicAdapter.MusicViewHolder
import com.chatapp.models.ModelMusic
import com.chatapp.utils.FileOpener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class MusicAdapter(var context: Context, var list: List<ModelMusic>) :
    RecyclerView.Adapter<MusicViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        return MusicViewHolder(
            LayoutInflater.from(context).inflate(R.layout.music_item, parent, false)
        )
    }

    fun filterList(filesAndFolders1: List<ModelMusic>) {
        list = filesAndFolders1
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        val (mName, mPath) = list[position]
        holder.title.text = list[position].mName
        holder.title.isEnabled = true
        holder.size.text = Formatter.formatFileSize(context, list[position].mSize)
        val date = getDate(System.currentTimeMillis(), "dd MMM yyyy hh:mm")

        if (mName.lowercase(Locale.CANADA).endsWith(".mp3")) {
            holder.imageView.setImageResource(R.drawable.ic_music)
            holder.duration.text = convertToMMSS(list[position].mDuration) + " Min"
        } else if (mName.lowercase(Locale.CANADA).contains(".mkv") || mName.lowercase(Locale.CANADA)
                .contains(".mp4")
        ) {
            //holder.imageView.setImageResource(R.drawable.ic_video)
            Glide.with(context).load(mPath).skipMemoryCache(false).into(holder.imageView)
            holder.duration.text = date
        } else if (mName.lowercase(Locale.CANADA).contains(".jpg") || mName.lowercase(Locale.CANADA)
                .contains(".png")
        ) {
            //holder.imageView.setImageResource(R.drawable.ic_pictures)
            Glide.with(context).load(mPath).skipMemoryCache(false).into(holder.imageView)
            holder.duration.text = date
        } else if (mName.lowercase(Locale.CANADA).contains(".txt")) {
            holder.imageView.setImageResource(R.drawable.ic_text_file)
            holder.duration.text = date
        } else if (mName.lowercase(Locale.CANADA).contains(".pdf")) {
            holder.imageView.setImageResource(R.drawable.ic_pdf)
            holder.duration.text = date
        } else {
            holder.imageView.setImageResource(R.drawable.ic_file)
            holder.duration.text = date
        }
        holder.title.setOnClickListener {
            try {
                FileOpener.openFile(context, File(list[holder.adapterPosition].mPath))
                //FileOpener.openFile(context, );
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
    }


    fun getDate(milliSeconds: Long, dateFormat: String?): String? {
        // Create a DateFormatter object for displaying date in specified format.
        val formatter = SimpleDateFormat(dateFormat)

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        val calendar: Calendar = Calendar.getInstance()
        calendar.timeInMillis = milliSeconds
        return formatter.format(calendar.time)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class MusicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var title: TextView
        var duration: TextView
        var size: TextView
        var imageView: ImageView

        init {
            title = itemView.findViewById(R.id.music_title)
            duration = itemView.findViewById(R.id.music_duration)
            size = itemView.findViewById(R.id.music_size)
            imageView = itemView.findViewById(R.id.icon_view)
        }
    }

    companion object {
        fun convertToMMSS(duration: String): String {
            val millis = duration.toLong()
            return String.format(
                Locale.ENGLISH,
                "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1)
            )
        }
    }
}