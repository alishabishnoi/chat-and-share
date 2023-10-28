package com.chatapp.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;

public class PermissionManager {
    private static PermissionManager instance=null;
    private Context context;

    public PermissionManager() {
    }

    public static PermissionManager getInstance(Context context){
        if (instance==null){
            instance=new PermissionManager();
        }
        instance.init(context);
        return instance;
    }

    public void init(Context context){
        this.context=context;
    }

    public  boolean methodCheckPermission(String[] permissions){
        int noOfPermissions=permissions.length;
        int noOfPermissionsGranted=0;
        boolean isLocationPermissionGranted;

        for (int i=0;i<noOfPermissions ;i++){
            if (ContextCompat.checkSelfPermission(context,permissions[i])== PermissionChecker.PERMISSION_GRANTED){
                noOfPermissionsGranted=noOfPermissionsGranted+1;
            }
        }

        if (noOfPermissionsGranted==noOfPermissions){
            isLocationPermissionGranted=true;

        }else {
            isLocationPermissionGranted=false;
        }

        return isLocationPermissionGranted;
    }

    public  boolean methodCheckLocation(String[] permissions){
        int noOfPermissions=permissions.length;
        int noOfPermissionsGranted=0;
        boolean isLocationPermissionGranted;

        for (int i=0;i<noOfPermissions ;i++){
            if (ContextCompat.checkSelfPermission(context,permissions[i])== PermissionChecker.PERMISSION_GRANTED){
                noOfPermissionsGranted=noOfPermissionsGranted+1;
            }
        }

        if (noOfPermissionsGranted==noOfPermissions){
            isLocationPermissionGranted=true;

        }else {
            isLocationPermissionGranted=false;
        }

        return isLocationPermissionGranted;

    }

    public void methodAskPermission(Activity activity, String[] permissions, int i){
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)){
            Toast.makeText(activity, "Permission needed", Toast.LENGTH_SHORT).show();
        }
        ActivityCompat.requestPermissions(activity,permissions, 100);
    }

    public boolean handleRequestResult(int[] grantResults){
        if (grantResults.length>0){

            return grantResults[0] != PackageManager.PERMISSION_DENIED;
        }

        return false;
    }
}
