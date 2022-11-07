package nl.storegear.android.mlbarcodescanner.util

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionUtils {
    private const val TAG = "MLBarcodeScanner"
    private const val PERMISSION_REQUESTS = 1

    fun allRuntimePermissionsGranted(activity: Activity, requiredPermissions: Array<String>): Boolean {
        for (permission in requiredPermissions) {
            permission.let {
                if (!isPermissionGranted(activity, it)) {
                    return false
                }
            }
        }
        return true
    }

    fun getRuntimePermissions(activity: Activity, requiredPermissions: Array<String>) {
        val permissionsToRequest = ArrayList<String>()
        for (permission in requiredPermissions) {
            permission.let {
                if (!isPermissionGranted(activity, it)) {
                    permissionsToRequest.add(permission)
                }
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, permissionsToRequest.toTypedArray(), PERMISSION_REQUESTS)
        }
    }

    private fun isPermissionGranted(context: Context, permission: String): Boolean {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission granted: $permission")
            return true
        }
        Log.i(TAG, "Permission NOT granted: $permission")
        return false
    }
}