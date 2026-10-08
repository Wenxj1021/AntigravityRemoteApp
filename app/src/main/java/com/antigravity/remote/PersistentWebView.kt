package com.antigravity.remote

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.webkit.WebView

/**
 * 后台保活 WebView
 *
 * 核心原理：
 * 1. 视窗可见性：重写 onWindowVisibilityChanged 与 onVisibilityChanged 始终上报 View.VISIBLE，
 *    阻止 Chromium 内核进入休眠节流与冻结长连接。
 * 2. 输入对焦与输入法绑定：显式开启 isFocusableInTouchMode 并声明 onCheckIsTextEditor = true，
 *    确保用户点击对话框输入区域时，原生 View 树与 InputMethodManager 能够将焦点指派给本 WebView，
 *    无缝呼起系统软键盘。
 */
class PersistentWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        isClickable = true
    }

    override fun onCheckIsTextEditor(): Boolean {
        // 向系统 InputMethodManager 明确标识本组件支持文本输入交互
        return true
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val connection = super.onCreateInputConnection(outAttrs)
        if (outAttrs.imeOptions == 0) {
            outAttrs.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        }
        return connection
    }

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
