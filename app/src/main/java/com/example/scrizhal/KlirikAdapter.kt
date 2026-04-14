package com.example.scrizhal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class KlirikAdapter(
    private var kliriki: List<Cleric>,
    private var workshopOrderedIds: Set<Int> = emptySet(),
    private var moscowRequestedIds: Set<Int> = emptySet(),
    private val onClick: (Cleric) -> Unit
) : RecyclerView.Adapter<KlirikAdapter.KlirikViewHolder>() {

    class KlirikViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.date)
        val tvStatus: TextView = view.findViewById(R.id.status)
        val tvName: TextView = view.findViewById(R.id.name)
        val tvEventType: TextView = view.findViewById(R.id.event_type)
        val tvEventValue: TextView = view.findViewById(R.id.event_value)
        val btnAction: MaterialButton = view.findViewById(R.id.btn_card_action)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KlirikViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.klirik_item, parent, false)
        return KlirikViewHolder(view)
    }

    override fun onBindViewHolder(holder: KlirikViewHolder, position: Int) {
        val cleric = kliriki[position]

        with(holder) {
            tvName.text = cleric.name
            tvDate.text = cleric.date
            tvStatus.text = cleric.getHumanStatus()
            tvEventType.text = cleric.eventType

            tvEventValue.text = if (cleric.eventType == "Награды") {
                "Следующая: ${Awards.normalizedAwardName(cleric.eventValue)}"
            } else {
                cleric.eventValue
            }

            if (cleric.eventType == "Награды") {
                btnAction.visibility = View.VISIBLE

                if (cleric.id in moscowRequestedIds && cleric.awardType == "MOSCOW") {
                    btnAction.text = "В документе"
                    btnAction.isEnabled = false
                    btnAction.alpha = 0.6f
                    btnAction.setOnClickListener(null)
                } else if (cleric.id in workshopOrderedIds && cleric.awardType != "MOSCOW") {
                    btnAction.text = "Заказ оформлен"
                    btnAction.isEnabled = false
                    btnAction.alpha = 0.6f
                    btnAction.setOnClickListener(null)
                } else {
                    btnAction.isEnabled = true
                    btnAction.alpha = 1f
                    btnAction.text = if (cleric.awardType == "MOSCOW") {
                        "Сформировать в документ"
                    } else {
                        "Отправить в мастерскую"
                    }
                    btnAction.setOnClickListener { onClick(cleric) }
                }
            } else {
                btnAction.visibility = View.GONE
            }

            itemView.setOnClickListener { onClick(cleric) }
        }
    }

    override fun getItemCount(): Int = kliriki.size

    fun updateData(
        newList: List<Cleric>,
        workshopOrdered: Set<Int> = emptySet(),
        moscowRequested: Set<Int> = emptySet()
    ) {
        this.kliriki = newList
        this.workshopOrderedIds = workshopOrdered
        this.moscowRequestedIds = moscowRequested
        notifyDataSetChanged()
    }
}
