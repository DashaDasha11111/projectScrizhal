package com.example.scrizhal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChurchesListAdapter(
    private var items: List<Church>,
    private val onItemClick: (Church) -> Unit
) : RecyclerView.Adapter<ChurchesListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.item_church_name)
        val location: TextView = view.findViewById(R.id.item_church_location)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_church_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val church = items[position]
        holder.name.text = church.name
        holder.location.text = church.address.ifEmpty { "" }.take(80).let { if (it.length >= 80) "$it…" else it }
        holder.itemView.setOnClickListener { onItemClick(church) }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newList: List<Church>) {
        items = newList
        notifyDataSetChanged()
    }
}
