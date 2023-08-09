@file:Suppress("HasPlatformType")

package com.ncorti.kotlin.template.app.utils

import android.annotation.TargetApi
import android.app.ResourcesManager
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Environment
import android.util.ArrayMap
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ncorti.kotlin.template.app.R
import dalvik.system.DexClassLoader
import java.io.File
import java.lang.ref.WeakReference

/**
 * 插件化工具类
 */
private const val TAG = "PluginUtils"

fun appendToArray(src: Array<String>?, target: String): Array<String> {
    val resultArray = Array((src?.size ?: 0) + 1) { "" }
    if (src != null) {
        System.arraycopy(src, 0, resultArray, 0, src.size)
    }
    resultArray[resultArray.size - 1] = target
    return resultArray
}

fun Context.getContextImpl(): Context {
    var innerContext = this
    while (innerContext is ContextWrapper && innerContext.baseContext != null) {
        innerContext = innerContext.baseContext
    }
    return innerContext
}

fun Context.getLoadedApk() = Reflector.with(getContextImpl()).field("mPackageInfo").get<Any?>(this)

/**
 * 将 plugin apk 资源加入到 Resources 对象中
 *
 * 前提
 * 各个[ApplicationInfo]在各个 Context 中都是同一个对象
 * 各个 ContextImpl 中的[LoadedAPk]对象也是同一个对象
 * 所以，只需要修改一个对象就都修改了
 *
 * @return 成功则返回 true，失败则 false
 */
fun addPluginPathToSystem(context: Context, pluginFile: File): Boolean {
    return runCatching {
        val applicationInfo: ApplicationInfo = context.applicationInfo
        val splitSourceDirs = applicationInfo.splitSourceDirs
        val newSourceDirs = appendToArray(splitSourceDirs, pluginFile.absolutePath)
        applicationInfo.splitSourceDirs = newSourceDirs

        val loadedApk = context.getLoadedApk()
        Reflector.with(loadedApk).field("mSplitResDirs")
            .set(appendToArray(splitSourceDirs, pluginFile.absolutePath))
    }.isSuccess
}

fun getResourcesMapping(): ArrayMap<Any, WeakReference<Any>> {
    val resourceManager =
        Reflector.on("android.app.ResourcesManager").method("getInstance").call<Any>(null)
    return Reflector.with(resourceManager).field("mResourceImpls").get(resourceManager)
}

/**
 * 根据 resName 获取 drawable 资源的 resId
 */
fun getPluginDrawableResId(
    context: Context,
    pluginPath: String,
    apkPackageName: String,
    resName: String,
): Int {
    val optimizedDirectoryFile = context.getDir("dex", AppCompatActivity.MODE_PRIVATE)
    val dexClassLoader = DexClassLoader(
        pluginPath,
        optimizedDirectoryFile.path,
        null,
        ClassLoader.getSystemClassLoader(),
    )
    // 通过使用apk自己的类加载器，反射出R类中相应的内部类进而获取我们需要的资源id
    return Reflector.on("$apkPackageName.R\$drawable", true, dexClassLoader).field(resName)
        .get(R.id::class.java)
}

@TargetApi(24)
fun Resources.getResourcesImpl() = Reflector.with(this).field("getImpl").get<Any>(this)

fun Context.mockPlugin(pluginFile: File) {
    addPluginPathToSystem(this, pluginFile)
    val originalMapping = getResourcesMapping()
    originalMapping.forEach {
        val key = it.key
        val value = it.value
        Log.i(TAG, "mockPlugin before: $key ->$value")
    }
    if (Build.VERSION.SDK_INT >= 28 || (Build.VERSION.SDK_INT >= 27 && Build.VERSION.PREVIEW_SDK_INT != 0)) {
        val newApplicationResources =
            applicationContext.createConfigurationContext(Configuration()).resources.getResourcesImpl()
        val newActivityResources =
            this.createConfigurationContext(resources.configuration).resources.getResourcesImpl()
        val newResourceMapping = mutableMapOf<Any, WeakReference<Any>>()
        newResourceMapping.putAll(originalMapping)
        originalMapping.forEach {
            val value = it.value.get() ?: return@forEach
            // map new key to old ResourcesImpl
            if (value == newApplicationResources) {
                val oldApplicationResImpl = applicationContext.resources.getResourcesImpl()
                val oldKey =
                    newResourceMapping.entries.firstOrNull { entry -> entry.value.get() == oldApplicationResImpl }?.key
                oldKey?.let {
                    newResourceMapping.remove(oldKey)
                }
                newResourceMapping[it.key] = WeakReference(oldApplicationResImpl)
            } else if (value == newActivityResources) {
                val oldActivityResImpl = resources.getResourcesImpl()
                val oldKey =
                    newResourceMapping.entries.firstOrNull { entry -> entry.value.get() == oldActivityResImpl }?.key
                oldKey?.let {
                    newResourceMapping.remove(oldKey)
                }
                newResourceMapping[it.key] = WeakReference(oldActivityResImpl)
            }
        }
        originalMapping.clear()
        originalMapping.putAll(newResourceMapping)
    }
    ResourcesManager.getInstance()
        .appendLibAssetForMainAssetPath(applicationInfo.publicSourceDir, "$packageName.test")
    originalMapping.forEach {
        val key = it.key
        val value = it.value
        Log.i(TAG, "mockPlugin after: $key ->$value")
    }
}
