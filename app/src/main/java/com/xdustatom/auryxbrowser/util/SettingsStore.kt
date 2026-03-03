package com.xdustatom.auryxbrowser.util

import android.content.Context

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("auryx_settings", Context.MODE_PRIVATE)

    var searchEngine: String
        get() = prefs.getString("search_engine", "duckduckgo") ?: "duckduckgo"
        set(value) = prefs.edit().putString("search_engine", value).apply()

    var desktopModeDefault: Boolean
        get() = prefs.getBoolean("desktop_mode", false)
        set(value) = prefs.edit().putBoolean("desktop_mode", value).apply()

    var javascriptEnabled: Boolean
        get() = prefs.getBoolean("javascript", true)
        set(value) = prefs.edit().putBoolean("javascript", value).apply()

    var adblockEnabled: Boolean
        get() = prefs.getBoolean("adblock", true)
        set(value) = prefs.edit().putBoolean("adblock", value).apply()

    var doNotTrack: Boolean
        get() = prefs.getBoolean("dnt", false)
        set(value) = prefs.edit().putBoolean("dnt", value).apply()

    var blockPopups: Boolean
        get() = prefs.getBoolean("block_popups", true)
        set(value) = prefs.edit().putBoolean("block_popups", value).apply()

    var blockThirdPartyCookies: Boolean
        get() = prefs.getBoolean("third_party", true)
        set(value) = prefs.edit().putBoolean("third_party", value).apply()

    var darkTheme: Boolean
        get() = prefs.getBoolean("dark_theme", true)
        set(value) = prefs.edit().putBoolean("dark_theme", value).apply()
}
