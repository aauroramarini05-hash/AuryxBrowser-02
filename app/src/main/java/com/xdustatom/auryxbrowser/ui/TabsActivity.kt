package com.xdustatom.auryxbrowser.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xdustatom.auryxbrowser.R
import com.xdustatom.auryxbrowser.web.TabMeta

class TabsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tabs)
        val tabs = (intent.getSerializableExtra("tabs") as? ArrayList<*>)
            ?.filterIsInstance<TabMeta>()
            ?: arrayListOf()

        val recycler = findViewById<RecyclerView>(R.id.tabsRecycler)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = TabsAdapter(tabs, onSelect = {
            setResult(Activity.RESULT_OK, Intent().putExtra("select", it.id))
            finish()
        }, onClose = {
            setResult(Activity.RESULT_OK, Intent().putExtra("close", it.id))
            finish()
        })

        findViewById<View>(R.id.newTabBtn).setOnClickListener {
            setResult(Activity.RESULT_OK, Intent().putExtra("new", true))
            finish()
        }
    }
}

private class TabsAdapter(
    private val tabs: List<TabMeta>,
    private val onSelect: (TabMeta) -> Unit,
    private val onClose: (TabMeta) -> Unit
) : RecyclerView.Adapter<TabsAdapter.VH>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH = VH(
        LayoutInflater.from(parent.context).inflate(R.layout.item_tab, parent, false)
    )

    override fun getItemCount() = tabs.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val tab = tabs[position]
        holder.title.text = tab.title.ifBlank { tab.url }
        holder.itemView.setOnClickListener { onSelect(tab) }
        holder.close.setOnClickListener { onClose(tab) }
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tabTitle)
        val close: ImageButton = view.findViewById(R.id.closeTabBtn)
    }
}
