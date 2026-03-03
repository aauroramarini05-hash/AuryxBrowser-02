package com.xdustatom.auryxbrowser.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xdustatom.auryxbrowser.AuryxApp
import com.xdustatom.auryxbrowser.R
import kotlinx.coroutines.launch

class BookmarksActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar).title = "Preferiti"
        val recycler = findViewById<RecyclerView>(R.id.recycler)
        recycler.layoutManager = LinearLayoutManager(this)
        val adapter = SimpleListAdapter { index ->
            lifecycleScope.launch {
                val all = (application as AuryxApp).db.bookmarkDao().getAll()
                val item = all.getOrNull(index) ?: return@launch
                setResult(Activity.RESULT_OK, Intent().putExtra("url", item.url))
                finish()
            }
        }
        recycler.adapter = adapter

        lifecycleScope.launch {
            (application as AuryxApp).db.bookmarkDao().observeAll().collect { bookmarks ->
                adapter.submit(bookmarks.map { it.title to it.url })
            }
        }
    }
}
