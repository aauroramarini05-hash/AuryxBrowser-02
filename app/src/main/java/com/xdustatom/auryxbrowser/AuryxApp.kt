package com.xdustatom.auryxbrowser

import android.app.Application
import com.xdustatom.auryxbrowser.data.AppDatabase

class AuryxApp : Application() {
    val db by lazy { AppDatabase.getInstance(this) }
}
