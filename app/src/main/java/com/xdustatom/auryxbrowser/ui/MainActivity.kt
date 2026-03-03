package com.xdustatom.auryxbrowser.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.xdustatom.auryxbrowser.AuryxApp
import com.xdustatom.auryxbrowser.R
import com.xdustatom.auryxbrowser.data.BookmarkEntity
import com.xdustatom.auryxbrowser.data.DownloadEntity
import com.xdustatom.auryxbrowser.data.HistoryEntity
import com.xdustatom.auryxbrowser.util.SettingsStore
import com.xdustatom.auryxbrowser.web.BrowserTab
import com.xdustatom.auryxbrowser.web.ContentBlocker
import com.xdustatom.auryxbrowser.web.TabMeta
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

class MainActivity : AppCompatActivity() {
    private lateinit var urlInput: TextInputEditText
    private lateinit var progress: ProgressBar
    private lateinit var settings: SettingsStore
    private lateinit var blocker: ContentBlocker
    private var defaultUa: String = ""

    private val tabs = mutableListOf<BrowserTab>()
    private val counter = AtomicLong(0)
    private var currentIndex = -1

    private val tabsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult
        when {
            data.getBooleanExtra("new", false) -> addTab(false, "about:blank")
            data.hasExtra("select") -> switchToTab(data.getLongExtra("select", -1))
            data.hasExtra("close") -> closeTab(data.getLongExtra("close", -1))
        }
    }
    private val bookmarkLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val url = it.data?.getStringExtra("url") ?: return@registerForActivityResult
        loadInput(url)
    }
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            lifecycleScope.launch { (application as AuryxApp).db.downloadDao().updateStatus(id, "Completato") }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        settings = SettingsStore(this)
        blocker = ContentBlocker(this)
        defaultUa = WebView(this).settings.userAgentString

        urlInput = findViewById(R.id.urlInput)
        progress = findViewById(R.id.progressBar)
        findViewById<ImageButton>(R.id.goButton).setOnClickListener { loadInput(urlInput.text.toString()) }
        findViewById<ImageButton>(R.id.backButton).setOnClickListener { currentTab()?.webView?.goBack() }
        findViewById<ImageButton>(R.id.forwardButton).setOnClickListener { currentTab()?.webView?.goForward() }
        findViewById<ImageButton>(R.id.reloadButton).setOnClickListener { currentTab()?.webView?.reload() }
        findViewById<ImageButton>(R.id.homeButton).setOnClickListener { loadHomePage() }
        findViewById<ImageButton>(R.id.tabsButton).setOnClickListener { openTabs() }

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar).setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_incognito -> addTab(true, "about:blank")
                R.id.menu_bookmark -> addBookmark()
                R.id.menu_bookmarks -> bookmarkLauncher.launch(Intent(this, BookmarksActivity::class.java))
                R.id.menu_downloads -> startActivity(Intent(this, DownloadsActivity::class.java))
                R.id.menu_settings -> startActivity(Intent(this, SettingsActivity::class.java))
                R.id.menu_clear -> confirmClearData()
                R.id.menu_desktop -> toggleDesktopMode()
                R.id.menu_home -> loadHomePage()
            }
            true
        }

        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        addTab(false, "about:blank")
        loadHomePage()
    }

    override fun onDestroy() {
        unregisterReceiver(downloadReceiver)
        tabs.forEach { it.webView.destroy() }
        super.onDestroy()
    }

    private fun openTabs() {
        tabsLauncher.launch(Intent(this, TabsActivity::class.java).putExtra("tabs", ArrayList(tabs.map { TabMeta(it.id, it.title, it.url, it.incognito) })))
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(incognito: Boolean): WebView {
        val webView = WebView(this)
        with(webView.settings) {
            javaScriptEnabled = settings.javascriptEnabled
            domStorageEnabled = !incognito
            javaScriptCanOpenWindowsAutomatically = !settings.blockPopups
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportMultipleWindows(!settings.blockPopups)
            userAgentString = if (settings.desktopModeDefault) desktopUA() else defaultUa
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, !settings.blockThirdPartyCookies)
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progress.progress = newProgress
                progress.isVisible = newProgress < 100
            }
            override fun onPermissionRequest(request: PermissionRequest?) { request?.grant(request.resources) }
            override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
                AlertDialog.Builder(this@MainActivity).setTitle("Geolocalizzazione")
                    .setMessage("Consentire geolocalizzazione per $origin?")
                    .setPositiveButton("Consenti") { _, _ -> callback?.invoke(origin, true, false) }
                    .setNegativeButton("Nega") { _, _ -> callback?.invoke(origin, false, false) }.show()
            }
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                if (settings.adblockEnabled && blocker.isBlocked(request?.url?.host)) return blocker.emptyResponse()
                return super.shouldInterceptRequest(view, request)
            }
            override fun onPageFinished(view: WebView, url: String) {
                val tab = currentTab() ?: return
                tab.title = view.title ?: tab.title
                tab.url = url
                urlInput.setText(url)
                if (!tab.incognito) lifecycleScope.launch {
                    (application as AuryxApp).db.historyDao().insert(HistoryEntity(title = tab.title, url = url, incognito = false))
                }
            }
        }
        webView.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            val req = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle(fileName)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                addRequestHeader("User-Agent", userAgent)
            }
            val id = (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
            lifecycleScope.launch {
                (application as AuryxApp).db.downloadDao().insert(DownloadEntity(url = url, fileName = fileName, mimeType = mimeType ?: "*/*", status = "In corso", downloadId = id))
            }
        })
        return webView
    }

    private fun addTab(incognito: Boolean, initialUrl: String) {
        tabs.add(BrowserTab(counter.incrementAndGet(), createWebView(incognito), incognito = incognito))
        currentIndex = tabs.lastIndex
        attachCurrentWebView()
        if (initialUrl == "about:blank") currentTab()?.webView?.loadUrl("about:blank") else loadInput(initialUrl)
    }

    private fun switchToTab(id: Long) { tabs.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { currentIndex = it; attachCurrentWebView() } }
    private fun closeTab(id: Long) {
        val idx = tabs.indexOfFirst { it.id == id }
        if (idx < 0) return
        tabs[idx].webView.destroy(); tabs.removeAt(idx)
        if (tabs.isEmpty()) addTab(false, "about:blank")
        currentIndex = currentIndex.coerceAtMost(tabs.lastIndex)
        attachCurrentWebView()
    }

    private fun attachCurrentWebView() {
        val container = findViewById<android.widget.FrameLayout>(R.id.webContainer)
        container.removeAllViews()
        currentTab()?.webView?.let {
            (it.parent as? android.view.ViewGroup)?.removeView(it)
            container.addView(it)
            urlInput.setText(it.url ?: "")
        }
    }

    private fun currentTab(): BrowserTab? = tabs.getOrNull(currentIndex)

    private fun addBookmark() {
        val tab = currentTab() ?: return
        lifecycleScope.launch { (application as AuryxApp).db.bookmarkDao().insert(BookmarkEntity(title = tab.title, url = tab.url)) }
    }

    private fun confirmClearData() {
        AlertDialog.Builder(this).setTitle("Clear data").setMessage("Cancellare cache/cookie/storage?")
            .setPositiveButton("Sì") { _, _ ->
                tabs.forEach { it.webView.clearCache(true); it.webView.clearHistory() }
                CookieManager.getInstance().removeAllCookies(null)
                lifecycleScope.launch { (application as AuryxApp).db.historyDao().clearAll() }
            }.setNegativeButton("No", null).show()
    }

    private fun toggleDesktopMode() {
        val w = currentTab()?.webView ?: return
        val desktop = w.settings.userAgentString.contains("X11")
        w.settings.userAgentString = if (desktop) defaultUa else desktopUA()
        w.reload()
    }

    private fun loadInput(raw: String) {
        val q = raw.trim(); if (q.isEmpty()) return
        val url = when {
            q.startsWith("http://") || q.startsWith("https://") -> q
            q.contains(" ") || !q.contains(".") -> searchUrl(q)
            else -> "https://$q"
        }
        currentTab()?.webView?.loadUrl(url, if (settings.doNotTrack) mapOf("DNT" to "1") else emptyMap())
    }

    private fun searchUrl(query: String): String = when (settings.searchEngine) {
        "google" -> "https://www.google.com/search?q=${Uri.encode(query)}"
        "bing" -> "https://www.bing.com/search?q=${Uri.encode(query)}"
        else -> "https://duckduckgo.com/?q=${Uri.encode(query)}"
    }

    private fun loadHomePage() {
        lifecycleScope.launch {
            val db = (application as AuryxApp).db
            val bookmarks = db.bookmarkDao().getAll().take(8)
            val recent = db.historyDao().recent(8).first()
            val html = buildString {
                append("<html><body style='background:#111315;color:#fff;font-family:sans-serif'><h2>AuryxBrowser</h2><h3>Preferiti</h3><ul>")
                bookmarks.forEach { append("<li><a href='${it.url}'>${it.title}</a></li>") }
                append("</ul><h3>Recenti</h3><ul>")
                recent.forEach { append("<li><a href='${it.url}'>${it.title}</a></li>") }
                append("</ul></body></html>")
            }
            currentTab()?.webView?.loadDataWithBaseURL("https://home.local", html, "text/html", "utf-8", null)
        }
    }

    override fun onBackPressed() {
        if (currentTab()?.webView?.canGoBack() == true) currentTab()?.webView?.goBack() else super.onBackPressed()
    }

    private fun desktopUA() = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123 Safari/537.36"
}
