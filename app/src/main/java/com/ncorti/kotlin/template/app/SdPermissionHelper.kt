package com.ncorti.kotlin.template.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.ncorti.kotlin.template.app.utils.appContext
import com.ncorti.kotlin.template.library.android.ToastUtil

class SdPermissionHelper(
    caller: ActivityResultCaller,
    private val resultCallback: (StorageResult) -> Unit = {},
) {

    private var requestForFullStorage: ActivityResultLauncher<Intent>? = null
    private var requestForStoragePermission: ActivityResultLauncher<String>? = null

    init {
        registerForFullStoragePermission(caller)
        registerForStoragePermission(caller)
    }

    fun requestStoragePermission() {
        ensureSdCardPrivilege()
    }

    fun dispose() {
        requestForFullStorage?.unregister()
        requestForStoragePermission?.unregister()
    }

    private fun registerForStoragePermission(caller: ActivityResultCaller) {
        if (hasStoragePermission) return
        val requestPermission = ActivityResultContracts.RequestPermission()
        requestForStoragePermission =
            caller.registerForActivityResult(requestPermission) { permissionGranted ->
                Log.i(TAG, "requestPermissionResult:$permissionGranted")
                resultCallback.invoke(StorageResult.PermissionResult(permissionGranted))
            }
    }

    private fun registerForFullStoragePermission(caller: ActivityResultCaller) {
        if (Build.VERSION.SDK_INT >= 30 && !hasFullStoragePermission) {
            val requestForStorage = ActivityResultContracts.StartActivityForResult()
            val activityResultLauncher = caller.registerForActivityResult(requestForStorage) {
                val permissionGranted = hasFullStoragePermission
                resultCallback.invoke(StorageResult.FullStorageResult(permissionGranted))
                Log.i(TAG, "ensureSdCardPrivilege: $permissionGranted")
                if (permissionGranted) {
                    requestForFullStoragePermission()
                }
            }
            requestForFullStorage = activityResultLauncher
        }
    }

    private fun ensureSdCardPrivilege() {
        if (Build.VERSION.SDK_INT >= 30) {
            if (!hasFullStoragePermission) {
                runCatching {
                    requestForFullStorage?.launch(
                        Intent(
                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            Uri.parse("package:${appContext.packageName}"),
                        ),
                    )
                }.apply {
                    if (isFailure) {
                        resultCallback.invoke(
                            StorageResult.FullStorageResult(
                                false,
                                "no activity to handle the Intent",
                            ),
                        )
                        ToastUtil.showToast(
                            appContext,
                            "request for full storage permission failed for:${exceptionOrNull()?.message}",
                        )
                    }
                }
            } else {
                requestForFullStoragePermission()
            }
        } else {
            requestForFullStoragePermission()
        }
    }

    private fun requestForFullStoragePermission() {
        val externalStoragePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (ActivityCompat.checkSelfPermission(
                appContext, externalStoragePermission,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestForStoragePermission?.launch(externalStoragePermission)
        }
    }

    companion object {

        val hasStoragePermission: Boolean
            get() = ActivityCompat.checkSelfPermission(
                appContext, Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED

        val hasFullStoragePermission: Boolean
            @RequiresApi(Build.VERSION_CODES.R)
            get() = Environment.isExternalStorageManager()

        private const val TAG = "SdPermissionHelper"
    }
}

sealed class StorageResult {
    class FullStorageResult(val isSuccess: Boolean, val failMessage: String = "") : StorageResult()

    class PermissionResult(val isSuccess: Boolean, val failMessage: String = "") : StorageResult()
}
