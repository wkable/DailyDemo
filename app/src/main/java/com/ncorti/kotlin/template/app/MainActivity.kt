@file:Suppress("unused")

package com.ncorti.kotlin.template.app

import android.os.Bundle
import android.os.Environment
import android.os.Handler.Callback
import android.os.Message
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.ncorti.kotlin.template.app.databinding.ActivityMainBinding
import com.ncorti.kotlin.template.app.utils.Reflector
import com.ncorti.kotlin.template.app.utils.SdPermissionHelper
import com.ncorti.kotlin.template.app.utils.StorageResult
import com.ncorti.kotlin.template.app.utils.getPluginDrawableResId
import com.ncorti.kotlin.template.app.utils.hookActivityThreadH
import com.ncorti.kotlin.template.app.utils.mockPlugin
import com.ncorti.kotlin.template.app.utils.parsePluginApk
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var sdPermissionHelper: SdPermissionHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ensureSdCardPrivilege()
        hookActivityThreadH(HCallback())
    }

    override fun onDestroy() {
        super.onDestroy()
        sdPermissionHelper?.dispose()
    }

    private fun ensureSdCardPrivilege() {
        if (!SdPermissionHelper.hasStoragePermission) {
            sdPermissionHelper = SdPermissionHelper(this) {
                when (it) {
                    is StorageResult.PermissionResult -> {
                        if (it.isSuccess) {
                            initPluginResources()
                        }
                    }

                    else -> Unit
                }
            }
            sdPermissionHelper?.requestStoragePermission()
        } else {
            initPluginResources()
        }
    }

    private fun initPluginResources() {
        val file = File(Environment.getExternalStorageDirectory(), "test.apk")
        mockPlugin(file)
        val drawable = ResourcesCompat.getDrawable(
            resources,
            getPluginDrawableResId(
                context = this,
                pluginPath = file.absolutePath,
                apkPackageName = "com.example.composedemo",
                resName = "scenery",
            ),
            theme,
        )
        binding.image.setImageDrawable(drawable)

        val packageInfo = parsePluginApk(file)
        Log.i(TAG, "initPluginResources: activity=> ${packageInfo.activities.joinToString()}")
        Log.i(TAG, "initPluginResources: service=> ${packageInfo.services.joinToString()}")
    }

    private class HCallback : Callback {
        override fun handleMessage(msg: Message): Boolean {
            when (msg.what) {
                EXECUTE_TRANSACTION -> {
                    val lifecycleItem = Reflector.with(msg.obj)
                        .method("getLifecycleStateRequest").call<Any?>() ?: return false
                    Log.i(TAG, "handleMessage: $lifecycleItem,${lifecycleItem::class.java}")
                    runCatching {
                        val lifecycleState =
                            Reflector.with(lifecycleItem).method("getTargetState").call<Int>()
                        Log.i(TAG, "handleMessage: $lifecycleState->$lifecycleState")
                    }
                }
            }
            return true
        }
    }

    companion object {

        private const val TAG = "MainActivity"
        private const val EXECUTE_TRANSACTION = 159
    }
}
