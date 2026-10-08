package com.antigravity.remote

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.webkit.WebView

/**
 * 后台保活 WebView
 *
 * 核心原理：
 * Android 系统在 Activity 进入后台（onStop）时，会通过 View 树向 WebView 发送
 * onWindowVisibilityChanged(View.GONE)。
 * Chromium 内核接收到 GONE 后会将页面状态转入 Hidden，触发定时器深度节流（最慢 1 分钟执行一次）、
 * 强制挂起 WebRTC 数据流及长轮询连接，导致挂后台时无法实时接收 Agent 响应及弹出操作提醒。
 *
 * 通过在此重写视图可见性通知，始终向底层 Chromium 内核上报 View.VISIBLE，
 * 阻止内核进入休眠降级逻辑，实现真正实时的后台消息接收。
 */
class PersistentWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    override fun onWindowVisibilityChanged(visibility: Int) {
        // 关键：始终向底层 Chromium 汇报 View.VISIBLE
        super.onWindowVisibilityChanged(View.VISIBLE)
    }

    override fun dispatchWindowVisibilityChanged(visibility: Int) {
        super.dispatchWindowVisibilityChanged(View.VISIBLE)
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, View.VISIBLE)
    }

    override fun dispatchVisibilityChanged(changedView: View, visibility: Int) {
        super.dispatchVisibilityChanged(changedView, View.VISIBLE)
    }
}
