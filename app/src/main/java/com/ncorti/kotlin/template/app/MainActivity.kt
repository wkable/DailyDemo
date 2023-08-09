@file:Suppress("unused")

package com.ncorti.kotlin.template.app

import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.ncorti.kotlin.template.app.databinding.ActivityMainBinding
import com.ncorti.kotlin.template.app.utils.getPluginDrawableResId
import com.ncorti.kotlin.template.app.utils.mockPlugin
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var sdPermissionHelper: SdPermissionHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ensureSdCardPrivilege()
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
    }

    companion object {

        private const val TAG = "MainActivity"
    }
}
