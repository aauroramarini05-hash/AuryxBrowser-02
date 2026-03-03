package com.xdustatom.auryxbrowser.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xdustatom.auryxbrowser.R

class SimpleListAdapter(
    private var items: List<Pair<String, String>> = emptyList(),
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<SimpleListAdapter.VH>() {

    fun submit(data: List<Pair<String, String>>) {
        items = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH = VH(
        LayoutInflater.from(parent.context).inflate(R.layout.item_simple, parent, false)
    )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.title.text = items[position].first
        holder.subtitle.text = items[position].second
        holder.itemView.setOnClickListener { onClick(position) }
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.title)
        val subtitle: TextView = view.findViewById(R.id.subtitle)
    }
}
