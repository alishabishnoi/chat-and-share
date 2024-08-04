package com.bluetoothwifiofflinechattingfilesharing.fileshare

import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bluetoothwifiofflinechattingfilesharing.adapter.MusicAdapter
import com.bluetoothwifiofflinechattingfilesharing.databinding.ActivityFileListBinding
import com.bluetoothwifiofflinechattingfilesharing.models.ModelMusic
import com.bluetoothwifiofflinechattingfilesharing.utils.CustomToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FileListActivity : AppCompatActivity() {
    lateinit var binding: ActivityFileListBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFileListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true);
        supportActionBar!!.setDisplayShowHomeEnabled(true);

        //action bar title
        title = "Files Received"


        binding.rvPublicPhotos.setLayoutManager(LinearLayoutManager(this))


        lifecycleScope.launch {
            binding.rvPublicPhotos.setAdapter(MusicAdapter(this@FileListActivity, loadMusicFromInternalStorage()))
        }
    }

    private suspend fun loadMusicFromInternalStorage(): List<ModelMusic> {
        return withContext(Dispatchers.IO) {
            val files = getExternalFilesDir("received")!!.listFiles()
            if (files!!.isEmpty()){
                CustomToast.infoToastError(this@FileListActivity,"No File Found", Gravity.BOTTOM)
            }
            //&& it.name.endsWith(".mp3")
            files.filter { it.canRead() && it.isFile  }?.map {
                val bytes = it.readBytes()
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ModelMusic(it.name,it.path,it.length(),it.length().toString(),it.lastModified())
            } ?: listOf()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}