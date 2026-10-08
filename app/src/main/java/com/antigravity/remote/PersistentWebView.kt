package com.antigravity.remote

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.webkit.WebView

/**
 * 后台保活 WebView
 *
 * 核心原理：
 * 1. 视窗可见性：重写 onWindowVisibilityChanged 与 onVisibilityChanged 始终上报 View.VISIBLE，
 *    阻止 Chromium 内核进入休眠节流与冻结长连接。
 * 2. 焦点支持：在触摸模式下开启 isFocusableInTouchMode，使 Chromium 内核能够正常获得原生视图焦点，
 *    完全保留 WebView 默认的 onCheckIsTextEditor() 与 onCreateInputConnection()，
 *    确保富文本编辑与系统输入法双向通道畅通无阻，避免输入内容丢失。
 */
class PersistentWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
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
