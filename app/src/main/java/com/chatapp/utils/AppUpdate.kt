package com.chatapp.utils

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.graphics.Color
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

class AppUpdate(val context: Context,val activity:Activity) {

    var UPDATE_CODE = 103

    var appUpdateManager: AppUpdateManager? = null
    var listener: InstallStateUpdatedListener

    init {
        //update listner
        listener =
            InstallStateUpdatedListener { InstallState: InstallState ->
                if (InstallState.installStatus() == InstallStatus.DOWNLOADED) {
                    popup()
                }
            }

    }

    //check for app update
    fun inAppUp() {
        appUpdateManager = AppUpdateManagerFactory.create(context)
        val task: com.google.android.play.core.tasks.Task<AppUpdateInfo> = appUpdateManager!!.getAppUpdateInfo()
        task.addOnSuccessListener { appUpdateInfo: AppUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                try {
                    appUpdateManager!!.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.FLEXIBLE,
                        activity,
                        UPDATE_CODE
                    )
                } catch (e: IntentSender.SendIntentException) {
                    e.printStackTrace()
                }
            }
        }
        appUpdateManager!!.registerListener(listener)
    }

    //app update complete snack bar
    private fun popup() {
        val snackbar = Snackbar.make(activity.findViewById(android.R.id.content),
            "App Update installation almost done", Snackbar.LENGTH_INDEFINITE
        )
            .setAction("Reload") { v: View? -> appUpdateManager?.completeUpdate() }
        snackbar.setTextColor(Color.parseColor("#FF0000"))
        snackbar.show()
    }

}