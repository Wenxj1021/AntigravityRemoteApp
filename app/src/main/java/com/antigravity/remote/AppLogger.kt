package com.antigravity.remote

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * 应用内部轻量级运行日志记录器
 * 采用并发双端队列环形缓冲区存储最新的运行、保活、网络及通知日志，
 * 供用户在菜单中直观排查状态与一键复制。
 */
object AppLogger {

    data class LogEntry(
        val timestamp: Long,
        val level: String,
        val tag: String,
        val message: String
    ) {
        fun format(): String {
            val timeStr = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
            return "[$timeStr] [$level/$tag] $message"
        }
    }

    private const val MAX_LOGS = 600
    private val logs = ConcurrentLinkedDeque<LogEntry>()

    fun i(tag: String, message: String) = add("INFO", tag, message)
    fun d(tag: String, message: String) = add("DEBUG", tag, message)
    fun w(tag: String, message: String) = add("WARN", tag, message)
    fun e(tag: String, message: String) = add("ERROR", tag, message)

    private fun add(level: String, tag: String, message: String) {
        logs.add(LogEntry(System.currentTimeMillis(), level, tag, message))
        while (logs.size > MAX_LOGS) {
            logs.pollFirst()
        }
    }

    fun getAllLogs(): List<LogEntry> = logs.toList()

    fun getAllLogsText(): String {
        return logs.joinToString("\n") { it.format() }
    }

    fun getCount(): Int = logs.size

    fun clear() {
        logs.clear()
        i("Logger", "日志已清空")
    }
}
