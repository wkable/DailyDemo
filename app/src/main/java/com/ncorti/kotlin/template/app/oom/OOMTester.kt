package com.ncorti.kotlin.template.app.oom

import android.os.Environment
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class OOMTester {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    fun getThreadInformation() {
        coroutineScope.launch {
            while (isActive) {
                Log.i(TAG, "getThreadInformation: maxFdCount: $maxFdCount")
                Log.i(TAG, "getThreadInformation: maxThreadCount: $maxThreadCount")
                Log.i(TAG, "getThreadInformation: fd count:$currentFdCount")
                Log.i(TAG, "getThreadInformation: thread count:$currentThreadCount")
                delay(5000)
            }
        }
    }

    fun mockTooManyFd() {
        coroutineScope.launch {
            val rootDir = File(Environment.getExternalStorageDirectory(), "tmpFolder")
            if (rootDir.exists()) {
                rootDir.deleteRecursively()
            }
            rootDir.mkdirs()
            val list = mutableListOf<File>()
            var count = 0
            while (true) {
                val file = File(rootDir, "file-$count.txt")
                file.createNewFile()
                file.inputStream()
                list.add(file)
                count++
                Log.i(TAG, "mockTooManyFd: $count")
                delay(10)
            }
        }
    }

    /**
     * OutOfMemoryError "Could not allocate JNI Env: Failed anonymous mmap(0x0, 8192, 0x3, 0x22, -1, 0)
     *
     * 线程过多导致的 OOM 抛出的异常
     */
    fun mockTooManyThreads() {
        coroutineScope.launch {
            val list = mutableListOf<Thread>()
            var count = 0
            while (true) {
                list.add(
                    object : Thread() {
                        override fun run() {
                            sleep(100 * 1000 * 1000)
                        }
                    }.apply { start() },
                )
                Log.i(TAG, "mockTooManyThreads: count: $count")
                count++
                delay(10)
            }
        }
    }

    companion object {

        private const val TAG = "OOMTester"
    }
}
