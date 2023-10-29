package com.chatapp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.amulyakhare.textdrawable.TextDrawable
import com.bumptech.glide.Glide
import com.chatapp.utils.FileOpener
import com.chatapp.R
import com.chatapp.models.chat
import java.io.File
import java.io.IOException
import java.util.Locale

class ChatAdapter(
    private val context: Context,
    private val chatList: ArrayList<chat>,
    private val userId: String
) :
    RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    private val MESSAGE_TYPE_LEFT = 0
    private val MESSAGE_TYPE_RIGHT = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == MESSAGE_TYPE_RIGHT) {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_right, parent, false)
            ViewHolder(view)
        } else {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_left, parent, false)
            ViewHolder(view)
        }

    }

    override fun getItemCount(): Int {
        return chatList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chat = chatList[position]

        val prefix = chat.msg.split("#")

        //Log.d("on bind adapter", "onBindViewHolder: ${chat.message}")

        if (prefix[0].equals("file")) {
            //means this is file
            holder.layout.visibility = View.VISIBLE
            holder.txtUserName.visibility = View.GONE
            val name=prefix[1].split("/")

            holder.txtFileName.text = name[name.size-1]

            if (prefix[1].lowercase(Locale.CANADA).endsWith(".mp3")) {
                holder.imageView.setImageResource(R.drawable.ic_music)
            } else if (prefix[1].contains(".mkv") || prefix[1]
                    .contains(".mp4")
            ) {
                //holder.imageView.setImageResource(R.drawable.ic_video)
                Glide.with(context).load(prefix[1]).skipMemoryCache(false).into(holder.imageView)

            } else if (prefix[1].contains(".jpg") || prefix[1]
                    .contains(".png")
            ) {
                //holder.imageView.setImageResource(R.drawable.ic_pictures)
                Glide.with(context).load(prefix[1]).skipMemoryCache(false).into(holder.imageView)
            } else if (prefix[1].contains(".txt")) {
                holder.imageView.setImageResource(R.drawable.ic_text_file)
            } else if (prefix[1].contains(".pdf")) {
                holder.imageView.setImageResource(R.drawable.ic_pdf)
            } else {
                holder.imageView.setImageResource(R.drawable.ic_file)
            }
            //holder.txtFileTotal.text = Formatter.formatFileSize(context, prefix[2].length.toLong())
            holder.txtFileTotal.text = prefix[2]

            holder.imageView.setOnClickListener{
                val recvFile = File(prefix[1])

                try {
                    //FileOpener.openFile(context, File(selectedFile.toURI()))
                    FileOpener.openFile(context, recvFile)
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
            }
        } else {
            if (prefix[0].equals("msg")){
                if (prefix[1] != ""){
                    holder.txtUserName.text = prefix[1]
                    holder.layout.visibility = View.GONE
                }

            }

        }

        holder.txtTime.text=chat.timestamp
        val name=chat.sender.toCharArray()
        val drawable = TextDrawable.builder().buildRound(name[0].toString(), R.color.colorAccent)
        holder.imageUser.setImageDrawable(drawable)



        //Glide.with(context).load(chat.).placeholder(R.drawable.profile_image).into(holder.imgUser)

    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val txtUserName: TextView = view.findViewById(R.id.tvMessage)
        val txtTime: TextView = view.findViewById(R.id.tvTime)
        val txtFileName: TextView = view.findViewById(R.id.tvFileName)
        val txtFileTotal: TextView = view.findViewById(R.id.tvFileTotalSize)
        val layout: LinearLayout = view.findViewById(R.id.layoutFile)
        val imageView: ImageView = view.findViewById(R.id.iFileType)
        val imageUser: ImageView = view.findViewById(R.id.userImage)

        //val imgUser: CircleImageView = view.findViewById(R.id.userImage)
    }



    override fun getItemViewType(position: Int): Int {
        //this method is to know that who have send the message and which side to show
        return if (chatList[position].receiver == userId) {
            MESSAGE_TYPE_RIGHT
        } else {
            MESSAGE_TYPE_LEFT
        }

    }
}