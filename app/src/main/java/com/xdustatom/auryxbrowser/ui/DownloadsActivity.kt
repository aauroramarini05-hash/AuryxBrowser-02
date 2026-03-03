package com.xdustatom.auryxbrowser.ui

import android.app.DownloadManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xdustatom.auryxbrowser.AuryxApp
import com.xdustatom.auryxbrowser.R
import kotlinx.coroutines.launch

class DownloadsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar).title = "Download"

        val recycler = findViewById<RecyclerView>(R.id.recycler)
        recycler.layoutManager = LinearLayoutManager(this)
        val list = mutableListOf<com.xdustatom.auryxbrowser.data.DownloadEntity>()
        val adapter = SimpleListAdapter { idx ->
            val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            val id = list.getOrNull(idx)?.downloadId ?: return@SimpleListAdapter
            val uri = dm.getUriForDownloadedFile(id) ?: return@SimpleListAdapter
            startActivity(Intent(Intent.ACTION_VIEW).setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        }
        recycler.adapter = adapter

        lifecycleScope.launch {
            (application as AuryxApp).db.downloadDao().observeAll().collect {
                list.clear(); list.addAll(it)
                adapter.submit(it.map { d -> d.fileName to d.status })
            }
        }
    }
}
