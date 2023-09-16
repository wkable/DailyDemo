package com.ncorti.kotlin.template.app.oom

import android.os.Process
import java.io.File

/**
 * OOM 工具类
 */
private const val MAX_THREAD_FILE = "/proc/sys/kernel/threads-max"
private val limitFile = "/proc/${Process.myPid()}/limits"
private val statusFile = "/proc/${Process.myPid()}/status"
private val fdDir = "/proc/${Process.myPid()}/fd"

// Max open files            32768                32768                files
private val fdRegex: Regex = "\\s{3,}".toRegex()
private val statusRegex = "\\s+".toRegex()

// 没有权限读取
val maxThreadCount by lazy {
    runCatching { File(MAX_THREAD_FILE).readText().toInt() }.getOrElse { 0 }
}

val maxFdCount by lazy {
    File(limitFile).readLines().firstOrNull { it.contains("Max open files") }?.let {
        runCatching { it.trim().split(fdRegex)[1].toInt() }.getOrNull()
    } ?: 0
}

/**
 * 查看当前进程中的线程数
 * 通过查看 /proc/[pid]/status 中的 `Threads: Xxx`
 */
val currentThreadCount: Int
    get() = File(statusFile).readLines().firstOrNull { it.contains("Threads:") }?.let {
        runCatching { it.split(statusRegex)[1].toInt() }.getOrNull()
    } ?: 0

/**
 * 获取当前进程下的 fd 数量
 * /proc/[pid]/fd 该目录下列出了所有打开的 fd 以及具体的场景
 */
val currentFdCount: Int
    get() = File(fdDir).listFiles()?.size ?: 0
