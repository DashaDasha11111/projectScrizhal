package com.example.scrizhal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class WorkshopOrderItem(
    val clericId: Int,
    val clericName: String,
    val awardName: String,
    val workshopName: String,
    val price: String,
    val date: String,
    val size: String?,
    val comment: String?,
    val church: String = "",
    val status: String = "new",
    val createdDate: String = "",
    val completedDate: String = "",
    val rawDueDate: String = ""
)

class WorkshopOrderAdapter(
    private var orders: List<WorkshopOrderItem>,
    private var datePrefix: String = "до",
    private val onClick: (WorkshopOrderItem) -> Unit
) : RecyclerView.Adapter<WorkshopOrderAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvOrderTitle)
        val tvDate: TextView = view.findViewById(R.id.tvOrderDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.workshop_order_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = orders[position]
        holder.tvTitle.text = order.awardName
        holder.tvDate.text = if (datePrefix.isNotBlank()) "$datePrefix ${order.date}" else order.date
        holder.itemView.setOnClickListener { onClick(order) }
    }

    override fun getItemCount(): Int = orders.size

    fun updateData(newOrders: List<WorkshopOrderItem>, prefix: String = datePrefix) {
        this.orders = newOrders
        this.datePrefix = prefix
        notifyDataSetChanged()
    }
}
