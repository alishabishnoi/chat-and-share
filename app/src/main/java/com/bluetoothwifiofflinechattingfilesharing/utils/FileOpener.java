package com.bluetoothwifiofflinechattingfilesharing.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;

public class FileOpener {

    public static void openFile(Context context, File file) throws IOException{

        Uri url= FileProvider.getUriForFile(context,"com.bluetoothwifiofflinechattingfilesharing.fileprovider", file);
        Intent intent=new Intent(Intent.ACTION_VIEW);

        if (url.toString().contains(".doc") || url.toString().contains(".docx")) {
            // Word document
            intent.setDataAndType(url, "application/msword");
        } else if (url.toString().contains(".pdf")) {
            // PDF file
            intent.setDataAndType(url, "application/pdf");
        } else if (url.toString().contains(".ppt") || url.toString().contains(".pptx")) {
            // Powerpoint file
            intent.setDataAndType(url, "application/vnd.ms-powerpoint");
        } else if (url.toString().contains(".xls") || url.toString().contains(".xlsx")) {
            // Excel file
            intent.setDataAndType(url, "application/vnd.ms-excel");
        } else if (url.toString().contains(".zip")) {
            // ZIP file
            intent.setDataAndType(url, "application/zip");
        } else if (url.toString().contains(".rar")){
            // RAR file
            intent.setDataAndType(url, "application/x-rar-compressed");
        } else if (url.toString().contains(".rtf")) {
            // RTF file
            intent.setDataAndType(url, "application/rtf");
        } else if (url.toString().contains(".wav") || url.toString().contains(".mp3")) {
            // WAV audio file
            intent.setDataAndType(url, "audio/x-wav");
        } else if (url.toString().contains(".gif")) {
            // GIF file
            intent.setDataAndType(url, "image/gif");
        } else if (url.toString().contains(".jpg") || url.toString().contains(".jpeg") || url.toString().contains(".png")) {
            // JPG file
            intent.setDataAndType(url, "image/jpeg");
        } else if (url.toString().contains(".txt")) {
            // Text file
            intent.setDataAndType(url, "text/plain");
        } else if (url.toString().contains(".3gp") || url.toString().contains(".mpg") ||
                url.toString().contains(".mpeg") ||
                url.toString().contains(".mpe") ||
                url.toString().contains(".mp4") ||
                url.toString().contains(".avi")) {
            // Video files
            intent.setDataAndType(url, "video/*");
        } else {
            intent.setDataAndType(url, "*/*");
        }

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        //context.startActivity(Intent.createChooser(intent, "Open File Using..."));
        context.startActivity(intent);



    }


}
