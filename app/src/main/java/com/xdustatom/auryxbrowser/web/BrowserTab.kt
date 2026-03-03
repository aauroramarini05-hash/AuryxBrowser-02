package com.xdustatom.auryxbrowser.web

import android.webkit.WebView

data class BrowserTab(
    val id: Long,
    val webView: WebView,
    var title: String = "Nuova scheda",
    var url: String = "about:blank",
    val incognito: Boolean = false
)
