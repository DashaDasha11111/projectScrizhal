package com.example.scrizhal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class FeastItem(
    val name: String,
    val date: LocalDate
)

sealed class LiturgyListItem {
    data class ChurchHeader(val church: Church) : LiturgyListItem()
    data class FeastRow(
        val feast: FeastItem,
        val church: Church,
        val assignedClericIds: List<Int>
    ) : LiturgyListItem()
}

class LiturgyListAdapter(
    private var items: List<LiturgyListItem>,
    private val onFeastClick: (FeastItem, Church) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_CHURCH = 0
        private const val TYPE_FEAST = 1
    }

    class ChurchVH(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvChurchHeaderName)
    }

    class FeastVH(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvFeastName)
        val tvDate: TextView = view.findViewById(R.id.tvFeastDate)
        val tvStatus: TextView = view.findViewById(R.id.tvFeastStatus)
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is LiturgyListItem.ChurchHeader -> TYPE_CHURCH
        is LiturgyListItem.FeastRow -> TYPE_FEAST
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_CHURCH -> ChurchVH(
                inflater.inflate(R.layout.item_liturgy_church_header, parent, false)
            )
            else -> FeastVH(
                inflater.inflate(R.layout.item_feast, parent, false)
            )
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is LiturgyListItem.ChurchHeader -> {
                (holder as ChurchVH).tvName.text = item.church.name
            }
            is LiturgyListItem.FeastRow -> {
                holder as FeastVH
                holder.tvName.text = item.feast.name
                holder.tvDate.text = item.feast.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                val ctx = holder.itemView.context
                if (item.assignedClericIds.isEmpty()) {
                    holder.tvStatus.text = "Не назначено"
                    holder.tvStatus.setTextColor(
                        ContextCompat.getColor(ctx, android.R.color.holo_red_dark)
                    )
                } else {
                    holder.tvStatus.text = "Назначено: ${item.assignedClericIds.size} чел."
                    holder.tvStatus.setTextColor(
                        ContextCompat.getColor(ctx, android.R.color.holo_green_dark)
                    )
                }
                holder.itemView.setOnClickListener { onFeastClick(item.feast, item.church) }
            }
        }
    }

    fun updateData(newItems: List<LiturgyListItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
