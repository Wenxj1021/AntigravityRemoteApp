package com.antigravity.remote

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.antigravity.remote.databinding.ActivityMainBinding
import com.antigravity.remote.databinding.BottomSheetMenuBinding
import com.antigravity.remote.databinding.BottomSheetWebviewInfoBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "antigravity_notifications"
    }

    private lateinit var binding: ActivityMainBinding
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null
    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>

    private val prefs by lazy {
        getSharedPreferences("antigravity_remote_prefs", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivitiesIfAvailable(application)
        super.onCreate(savedInstanceState)

        // 规避从桌面启动器返回已有任务时，系统重复创建根 Activity 导致页面重载重置的经典 Bug
        if (!isTaskRoot && intent.hasCategory(Intent.CATEGORY_LAUNCHER) && intent.action == Intent.ACTION_MAIN) {
            finish()
            return
        }

        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInsets()
        initLaunchers()
        createNotificationChannel()
        requestNotificationPermission()
        setupWebView()
        setupListeners()
        updateFabPosition()
        setupBackNavigation()

        if (savedInstanceState != null) {
            binding.webView.restoreState(savedInstanceState)
        } else {
            loadTargetUrl()
        }
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootContainer) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.rootContainer.updatePadding(
                top = systemBars.top,
                bottom = systemBars.bottom,
                left = systemBars.left,
                right = systemBars.right
            )
            updateFabPosition()
            insets
        }
    }

    private fun initLaunchers() {
        fileChooserLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val uris = if (result.resultCode == RESULT_OK) {
                result.data?.let { data ->
                    val clipData = data.clipData
                    when {
                        clipData != null -> {
                            val count = clipData.itemCount
                            Array(count) { i -> clipData.getItemAt(i).uri }
                        }
                        data.data != null -> arrayOf(data.data!!)
                        else -> null
                    }
                }
            } else null

            fileUploadCallback?.onReceiveValue(uris)
            fileUploadCallback = null
        }

        notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, R.string.notification_permission_granted_toast, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.channel_desc)
                enableLights(true)
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    fun sendNotification(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        ) {
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
        }
    }

    inner class NotificationBridge {
        @JavascriptInterface
        fun postNotification(title: String?, body: String?, tag: String?) {
            val safeTitle = if (title.isNullOrBlank()) "Antigravity Remote" else title
            val safeBody = body ?: ""
            runOnUiThread {
                sendNotification(safeTitle, safeBody)
            }
        }

        @JavascriptInterface
        fun requestPermission() {
            runOnUiThread {
                requestNotificationPermission()
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val webSettings = binding.webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.useWideViewPort = true
        webSettings.loadWithOverviewMode = true
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false
        webSettings.setSupportZoom(true)
        webSettings.allowFileAccess = false
        webSettings.allowContentAccess = true
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT

        WebView.setWebContentsDebuggingEnabled(true)

        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(binding.webView, true)

        // 注入通知桥接 JavaScript 接口
        binding.webView.addJavascriptInterface(NotificationBridge(), "AndroidNotificationBridge")

        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.progressBar.visibility = View.VISIBLE
                binding.errorContainer.visibility = View.GONE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.progressBar.visibility = View.GONE

                if (!url.isNullOrBlank() && !url.startsWith("data:") && !url.startsWith("about:")) {
                    prefs.edit().putString("last_visited_url", url).apply()
                }

                // 注入 HTML5 Notification API Polyfill，使网页通知直接桥接为安卓原生系统通知
                val notificationPolyfill = """
                    (function() {
                        if (!window.AndroidNotificationBridgeInjected) {
                            window.AndroidNotificationBridgeInjected = true;
                            window.Notification = function(title, options) {
                                options = options || {};
                                var body = options.body || "";
                                var tag = options.tag || "";
                                if (window.AndroidNotificationBridge) {
                                    window.AndroidNotificationBridge.postNotification(title, body, tag);
                                }
                            };
                            window.Notification.permission = "granted";
                            window.Notification.requestPermission = function(callback) {
                                if (window.AndroidNotificationBridge) {
                                    window.AndroidNotificationBridge.requestPermission();
                                }
                                var p = Promise.resolve("granted");
                                if (callback) callback("granted");
                                return p;
                            };
                        }
                    })();
                """.trimIndent()
                view?.evaluateJavascript(notificationPolyfill, null)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    binding.progressBar.visibility = View.GONE
                    binding.errorContainer.visibility = View.VISIBLE
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val uri = request?.url ?: return false
                val scheme = uri.scheme?.lowercase() ?: return false

                return when (scheme) {
                    "http", "https" -> false // Keep standard web browsing in WebView
                    else -> {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(
                                this@MainActivity,
                                "No app found to handle link: $uri",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        true
                    }
                }
            }
        }

        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                binding.progressBar.setProgressCompat(newProgress, true)
                if (newProgress >= 100) {
                    binding.progressBar.visibility = View.GONE
                } else {
                    binding.progressBar.visibility = View.VISIBLE
                }
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                // 授权网页申请的媒体/通知等权限
                request?.grant(request.resources)
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileUploadCallback?.onReceiveValue(null)
                fileUploadCallback = filePathCallback

                val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }

                try {
                    fileChooserLauncher.launch(intent)
                } catch (e: Exception) {
                    fileUploadCallback?.onReceiveValue(null)
                    fileUploadCallback = null
                    Toast.makeText(this@MainActivity, R.string.cannot_open_file_picker, Toast.LENGTH_SHORT).show()
                    return false
                }
                return true
            }
        }
    }

    private fun setupListeners() {
        binding.btnRetry.setOnClickListener {
            binding.errorContainer.visibility = View.GONE
            binding.webView.reload()
        }

        binding.btnSettings.setOnClickListener {
            showMenuBottomSheet()
        }

        binding.fabSettings.setOnClickListener {
            showMenuBottomSheet()
        }
    }

    private fun updateFabPosition() {
        val params = binding.fabSettings.layoutParams as FrameLayout.LayoutParams
        params.gravity = android.view.Gravity.BOTTOM or android.view.Gravity.END
        params.marginEnd = resources.getDimensionPixelSize(R.dimen.fab_margin_end)
        params.bottomMargin = resources.getDimensionPixelSize(R.dimen.fab_margin_bottom)
        binding.fabSettings.layoutParams = params
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateFabPosition()
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.webView.canGoBack()) {
                    binding.webView.goBack()
                } else {
                    finish()
                }
            }
        })
    }

    private fun getTargetUrl(): String {
        return prefs.getString("last_visited_url", getString(R.string.default_url))
            ?: getString(R.string.default_url)
    }

    private fun loadTargetUrl() {
        val url = getTargetUrl()
        binding.webView.loadUrl(url)
    }

    private fun showMenuBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val sheetBinding = BottomSheetMenuBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        dialog.behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
            isFitToContents = true
        }
        dialog.setOnShowListener {
            dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        sheetBinding.itemRefresh.setOnClickListener {
            dialog.dismiss()
            binding.errorContainer.visibility = View.GONE
            binding.webView.reload()
        }

        sheetBinding.itemTestNotification.setOnClickListener {
            dialog.dismiss()
            sendNotification(
                getString(R.string.test_notification_title),
                getString(R.string.test_notification_content)
            )
            Toast.makeText(this, R.string.test_notification_sent_toast, Toast.LENGTH_SHORT).show()
        }

        sheetBinding.itemWebViewInfo.setOnClickListener {
            dialog.dismiss()
            showWebViewInfoBottomSheet()
        }

        sheetBinding.itemLogout.setOnClickListener {
            dialog.dismiss()
            performLogout()
        }

        dialog.show()
    }

    private data class WebViewDetails(
        val providerName: String,
        val packageName: String,
        val versionName: String,
        val versionCode: String,
        val chromeKernelVersion: String,
        val isMultiProcess: Boolean?,
        val isSafeBrowsingSupported: Boolean,
        val isWebMessageListenerSupported: Boolean,
        val isDocumentStartScriptSupported: Boolean,
        val isDarkeningSupported: Boolean,
        val isDebuggingEnabled: Boolean,
        val osVersion: String,
        val deviceModel: String,
        val cpuAbi: String,
        val userAgent: String,
        val icon: Drawable?
    )

    private fun getWebViewDetails(): WebViewDetails {
        val packageInfo = try {
            WebViewCompat.getCurrentWebViewPackage(this)
                ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WebView.getCurrentWebViewPackage()
                } else null
        } catch (e: Exception) {
            null
        }

        val pm = packageManager
        val providerName = packageInfo?.applicationInfo?.loadLabel(pm)?.toString()
            ?: packageInfo?.packageName
            ?: getString(R.string.webview_status_unknown)

        val icon = try {
            packageInfo?.packageName?.let { pm.getApplicationIcon(it) }
        } catch (e: Exception) {
            null
        }

        val pkgName = packageInfo?.packageName ?: getString(R.string.webview_status_unknown)
        val verName = packageInfo?.versionName ?: getString(R.string.webview_status_unknown)
        val verCode = if (packageInfo != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toString()
            }
        } else {
            getString(R.string.webview_status_unknown)
        }

        val ua = try {
            binding.webView.settings.userAgentString
        } catch (e: Exception) {
            try {
                WebSettings.getDefaultUserAgent(this)
            } catch (e2: Exception) {
                "N/A"
            }
        }

        val chromeKernel = Regex("Chrome/([0-9.]+)").find(ua)?.groupValues?.getOrNull(1) ?: "N/A"

        val isMultiProcess = try {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROCESS)) {
                WebViewCompat.isMultiProcessEnabled()
            } else {
                null
            }
        } catch (e: Throwable) {
            null
        }

        val isSafeBrowsing = try {
            WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)
        } catch (e: Throwable) {
            false
        }

        val isWebMessage = try {
            WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)
        } catch (e: Throwable) {
            false
        }

        val isDocStartScript = try {
            WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
        } catch (e: Throwable) {
            false
        }

        val isDarkening = try {
            WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)
        } catch (e: Throwable) {
            false
        }

        val osVer = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        val device = "${Build.MANUFACTURER} ${Build.MODEL}"
        val abis = Build.SUPPORTED_ABIS?.joinToString(", ") ?: "N/A"

        return WebViewDetails(
            providerName = providerName,
            packageName = pkgName,
            versionName = verName,
            versionCode = verCode,
            chromeKernelVersion = chromeKernel,
            isMultiProcess = isMultiProcess,
            isSafeBrowsingSupported = isSafeBrowsing,
            isWebMessageListenerSupported = isWebMessage,
            isDocumentStartScriptSupported = isDocStartScript,
            isDarkeningSupported = isDarkening,
            isDebuggingEnabled = true,
            osVersion = osVer,
            deviceModel = device,
            cpuAbi = abis,
            userAgent = ua,
            icon = icon
        )
    }

    private fun showWebViewInfoBottomSheet() {
        val details = getWebViewDetails()
        val dialog = BottomSheetDialog(this)
        val infoBinding = BottomSheetWebviewInfoBinding.inflate(layoutInflater)
        dialog.setContentView(infoBinding.root)

        dialog.behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
            isFitToContents = true
        }
        dialog.setOnShowListener {
            dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        if (details.icon != null) {
            infoBinding.imgProviderIcon.setImageDrawable(details.icon)
            infoBinding.imgProviderIcon.imageTintList = null
        }
        infoBinding.tvProviderName.text = details.providerName
        infoBinding.tvPackageName.text = details.packageName
        infoBinding.tvVersionName.text = details.versionName
        infoBinding.tvVersionCode.text = details.versionCode
        infoBinding.tvChromeVersion.text = details.chromeKernelVersion

        fun formatFeature(supported: Boolean): String =
            getString(if (supported) R.string.webview_status_supported else R.string.webview_status_unsupported)

        fun formatStatus(enabled: Boolean?): String = when (enabled) {
            true -> getString(R.string.webview_status_enabled)
            false -> getString(R.string.webview_status_disabled)
            null -> getString(R.string.webview_status_unknown)
        }

        infoBinding.tvMultiProcess.text = formatStatus(details.isMultiProcess)
        infoBinding.tvSafeBrowsing.text = formatFeature(details.isSafeBrowsingSupported)
        infoBinding.tvWebMessage.text = formatFeature(details.isWebMessageListenerSupported)
        infoBinding.tvStartScript.text = formatFeature(details.isDocumentStartScriptSupported)
        infoBinding.tvDebugging.text = getString(R.string.webview_status_enabled)

        infoBinding.tvOsVersion.text = details.osVersion
        infoBinding.tvDeviceModel.text = details.deviceModel
        infoBinding.tvCpuAbi.text = details.cpuAbi
        infoBinding.tvUserAgent.text = details.userAgent

        infoBinding.btnCopyInfo.setOnClickListener {
            copyWebViewInfoToClipboard(details)
        }

        infoBinding.btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun copyWebViewInfoToClipboard(details: WebViewDetails) {
        val text = buildString {
            appendLine("=== WebView Information ===")
            appendLine("Provider: ${details.providerName}")
            appendLine("Package: ${details.packageName}")
            appendLine("Version: ${details.versionName}")
            appendLine("Version Code: ${details.versionCode}")
            appendLine("Chromium Kernel: ${details.chromeKernelVersion}")
            appendLine()
            appendLine("=== Features & Status ===")
            appendLine("Multi-Process: ${if (details.isMultiProcess == true) "Enabled" else if (details.isMultiProcess == false) "Disabled" else "Unknown"}")
            appendLine("Safe Browsing: ${if (details.isSafeBrowsingSupported) "Supported" else "Unsupported"}")
            appendLine("WebMessage Listener: ${if (details.isWebMessageListenerSupported) "Supported" else "Unsupported"}")
            appendLine("Document Start Script: ${if (details.isDocumentStartScriptSupported) "Supported" else "Unsupported"}")
            appendLine("Algorithmic Darkening: ${if (details.isDarkeningSupported) "Supported" else "Unsupported"}")
            appendLine("WebContents Debugging: Enabled")
            appendLine()
            appendLine("=== Environment ===")
            appendLine("Device: ${details.deviceModel}")
            appendLine("OS: ${details.osVersion}")
            appendLine("ABI: ${details.cpuAbi}")
            appendLine()
            appendLine("=== User Agent ===")
            appendLine(details.userAgent)
        }

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("WebView Info", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, R.string.webview_info_copied_toast, Toast.LENGTH_SHORT).show()
    }

    private fun performLogout() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.logout_confirm_title)
            .setMessage(R.string.logout_confirm_message)
            .setIcon(R.drawable.ic_logout)
            .setPositiveButton(R.string.logout) { _, _ ->
                val cookieManager = CookieManager.getInstance()
                cookieManager.removeAllCookies {
                    cookieManager.flush()
                }
                WebStorage.getInstance().deleteAllData()
                binding.webView.clearCache(true)
                binding.webView.clearHistory()
                binding.webView.clearFormData()
                prefs.edit().clear().apply()
                binding.errorContainer.visibility = View.GONE
                binding.webView.loadUrl(getString(R.string.default_url))
                Toast.makeText(this, R.string.logout_success, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.webView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        binding.webView.restoreState(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        binding.webView.onResume()
    }

    override fun onPause() {
        binding.webView.onPause()
        CookieManager.getInstance().flush()
        super.onPause()
    }

    override fun onStop() {
        CookieManager.getInstance().flush()
        super.onStop()
    }

    override fun onDestroy() {
        binding.webView.destroy()
        super.onDestroy()
    }
}
