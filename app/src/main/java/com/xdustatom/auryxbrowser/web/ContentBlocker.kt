package com.xdustatom.auryxbrowser.web

import android.content.Context
import java.io.ByteArrayInputStream

class ContentBlocker(context: Context) {
    private val blockedHosts: Set<String> = context.assets.open("blocked_hosts.txt").bufferedReader().useLines { lines ->
        lines.map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .toSet()
    }

    fun isBlocked(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        return blockedHosts.contains(host) || blockedHosts.any { host.endsWith(".$it") }
    }

    fun emptyResponse() = android.webkit.WebResourceResponse(
        "text/plain",
        "utf-8",
        ByteArrayInputStream(ByteArray(0))
    )
}
