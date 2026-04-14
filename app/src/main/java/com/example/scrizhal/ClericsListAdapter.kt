package com.example.scrizhal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ClericsListAdapter(
    private var items: List<Cleric>,
    private val onItemClick: (Cleric) -> Unit
) : RecyclerView.Adapter<ClericsListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.item_cleric_name)
        val rank: TextView = view.findViewById(R.id.item_cleric_rank)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cleric_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cleric = items[position]
        holder.name.text = cleric.name
        holder.rank.text = getDisplayRank(cleric)
        holder.itemView.setOnClickListener { onItemClick(cleric) }
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newList: List<Cleric>) {
        items = newList
        notifyDataSetChanged()
    }

    companion object {
        private val KNOWN_RANKS = setOf(
            "Иерей", "Протоиерей", "Диакон", "Протодиакон",
            "Иеродиакон", "Иеромонах", "Игумен", "Архимандрит"
        )

        fun getDisplayRank(cleric: Cleric): String {
            val firstWord = cleric.name.trim().split(" ").firstOrNull() ?: ""
            if (firstWord in KNOWN_RANKS) return firstWord
            return if (cleric.isMonk) "Иеромонах" else "Иерей"
        }
    }
}
