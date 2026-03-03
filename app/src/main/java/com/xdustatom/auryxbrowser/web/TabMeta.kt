package com.xdustatom.auryxbrowser.web

import java.io.Serializable

data class TabMeta(
    val id: Long,
    val title: String,
    val url: String,
    val incognito: Boolean
) : Serializable
